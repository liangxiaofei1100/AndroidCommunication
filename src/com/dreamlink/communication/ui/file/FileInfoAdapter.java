package com.dreamlink.communication.ui.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.fileshare.FileInfo;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;
import com.dreamlink.communication.ui.image.AsyncImageLoader2;
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
	private AsyncImageLoader2 bitmapLoader2;
	
	private boolean flag = true;
	
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
		bitmapLoader2 = new AsyncImageLoader2(context);
	}
	
	public void selectAll(boolean isChecked){
		int count = this.getCount();
		for (int i = 0; i < count; i++) {
			setChecked(i, isChecked);
		}
	}
	
	public void setChecked(int position, boolean isChecked){
		isSelected.put(position, isChecked);
//		notifyDataSetChanged();
	}
	
	public boolean isChecked(int position){
		return isSelected.get(position);
	}
	
	public void setFlag(boolean flag){
		this.flag = flag;
	}
	
	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public FileInfo getItem(int position) {
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
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
		//太乱了，需要整理
		if(FileInfoManager.TYPE_IMAGE  == fileInfo.type){
//			String path = DreamConstant.FILE_EX + fileInfo.filePath;
//			imageLoader.displayImage(path, holder.iconView, options);
			
			if (!flag) {
				if (AsyncImageLoader2.bitmapCache.size() > 0 &&
						AsyncImageLoader2.bitmapCache.get(fileInfo.filePath) != null) {
					holder.iconView.setImageBitmap(AsyncImageLoader2.bitmapCache.get(fileInfo.filePath).get());
				}else {
					holder.iconView.setImageDrawable(fileInfo.icon);
				}
			}else {
				Bitmap bitmap = bitmapLoader2.loadImage(fileInfo.filePath, FileBrowserFragment.bitmapCaches, 
						holder.iconView, 
						new com.dreamlink.communication.ui.image.AsyncImageLoader2.ILoadImageCallback() {
							@Override
							public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
								imageView.setImageBitmap(bitmap);
							}
						});
				if (null != bitmap) {
					holder.iconView.setImageBitmap(bitmap);
				}else {
					holder.iconView.setImageDrawable(fileInfo.icon);
				}
			}
		} else if (FileInfoManager.TYPE_APK == fileInfo.type) {
			Bitmap cacheDrawable = bitmapLoader.loadImage(fileInfo.filePath, fileInfo.type, holder.iconView, new ILoadImageCallback() {
				@Override
				public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
					// TODO Auto-generated method stub
					if (null != bitmap) {
						imageView.setImageBitmap(bitmap);
					}
				}
			});
			
			if (null != cacheDrawable) {
				holder.iconView.setImageBitmap(cacheDrawable);
			}else {
				holder.iconView.setImageResource(R.drawable.icon_apk);
			}
		}else if (FileInfoManager.TYPE_AUDIO == fileInfo.type) {
			holder.iconView.setImageDrawable(fileInfo.icon);
		}else if (FileInfoManager.TYPE_VIDEO == fileInfo.type) {
			Bitmap videoBitmap = bitmapLoader.loadImage(fileInfo.filePath, fileInfo.type, holder.iconView, new ILoadImageCallback() {
				@Override
				public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
					if (null != bitmap) {
						imageView.setImageBitmap(bitmap);
					}
				}
			});
			
			if (null != videoBitmap) {
				holder.iconView.setImageBitmap(videoBitmap);
			}else {
				holder.iconView.setImageDrawable(fileInfo.icon);
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
