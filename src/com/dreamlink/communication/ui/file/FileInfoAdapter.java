package com.dreamlink.communication.ui.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.fileshare.FileInfo;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class FileInfoAdapter extends BaseAdapter {
	private List<FileInfo> mList;
	private LayoutInflater mInflater = null;
	private boolean[] mCheckedArray = null;
	
	private Context mContext;
	
	private HashMap<Integer, Boolean> isSelected = null;
	
	private GridView mGridView;
	private ImageLoader imageLoader;
	private DisplayImageOptions options;
	
	private AsyncImageLoader bitmapLoader;
	
	public FileInfoAdapter(Context context, List<FileInfo> list){
		new FileInfoAdapter(context, list, null, null);
	}
	
	public FileInfoAdapter(Context context, List<FileInfo> list, ImageLoader loader, DisplayImageOptions options){
		mInflater = LayoutInflater.from(context);
		this.mList = list;
		this.mContext = context;
		isSelected = new HashMap<Integer, Boolean>();
		selectAll(false);
		
		this.imageLoader = loader;
		this.options = options;
		
		bitmapLoader = new AsyncImageLoader(context);
	}
	
	public void selectAll(boolean isChecked){
		int count = this.getCount();
		for (int i = 0; i < count; i++) {
			setChecked(i, isChecked);
		}
	}
	
	public void setChecked(int position, boolean isChecked){
//		mCheckedArray[position] = !mCheckedArray[position];
		isSelected.put(position, isChecked);
//		notifyDataSetChanged();
	}
	
	public boolean isChecked(int position){
		return isSelected.get(position);
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mList.size();
	}

	@Override
	public FileInfo getItem(int position) {
		// TODO Auto-generated method stub
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	class ViewHolder{
		ImageView iconView;
		TextView nameView;
		TextView dateAndSizeView;
		CheckBox checkBox;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		ViewHolder holder = null;
		
		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			view = mInflater.inflate(R.layout.ui_file_all_item, parent, false);
			holder.iconView = (ImageView) view.findViewById(R.id.file_icon_imageview);
			holder.nameView = (TextView) view.findViewById(R.id.file_name_textview);
			holder.dateAndSizeView = (TextView) view.findViewById(R.id.file_info_textview);
			holder.checkBox = (CheckBox) view.findViewById(R.id.file_checkbox);
			view.setTag(holder);
		}else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}
		
		FileInfo fileInfo = mList.get(position);
		String size = fileInfo.getFormatFileSize();
		String date = fileInfo.getFormateDate();
		
		if(FileInfoManager.TYPE_IMAGE  == fileInfo.type){
			String path = "file://" + fileInfo.filePath;
			imageLoader.displayImage(path, holder.iconView, options);
		} else if (FileInfoManager.TYPE_APK == fileInfo.type) {
			Drawable cacheDrawable = bitmapLoader.loadApkDrawable(fileInfo.filePath, holder.iconView, 
					new ILoadImageCallback() {
				@Override
				public void onObtainDrawable(Drawable drawable, ImageView imageView) {
					imageView.setImageDrawable(drawable);
				}
				@Override
				public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
					// TODO Auto-generated method stub
				}
			});
			
			if (null != cacheDrawable) {
				holder.iconView.setImageDrawable(cacheDrawable);
			}else {
				holder.iconView.setImageResource(R.drawable.icon_apk);
			}
		}else {
			holder.iconView.setImageDrawable(fileInfo.icon);
		}
		
		holder.nameView.setText(fileInfo.fileName);
		
		Boolean is = isSelected.get(position);
		if (null == is) {
			is = false;
		}
		
		if (fileInfo.isDir) {
			holder.dateAndSizeView.setText(date);
			holder.checkBox.setVisibility(View.INVISIBLE);
		}else {
			holder.dateAndSizeView.setText(date + " | " + size);
			holder.checkBox.setVisibility(View.VISIBLE);
			holder.checkBox.setChecked(is);
		}
		
		
		return view;
	}

}
