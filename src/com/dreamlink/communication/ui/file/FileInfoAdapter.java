package com.dreamlink.communication.ui.file;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;
import com.dreamlink.communication.ui.MountManager;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class FileInfoAdapter extends BaseAdapter {
	private static final String TAG = "FileInfoAdapter";
	private List<FileInfo> mList = new ArrayList<FileInfo>();
	private List<Integer> homeList = new ArrayList<Integer>();
	private LayoutInflater mInflater = null;
	
	private Context mContext;
	
	private SparseBooleanArray mIsSelected = null;
	
	private AsyncImageLoader bitmapLoader;
	
	private boolean flag = true;
	public boolean isHome = true;
	
	public FileInfoAdapter(Context context, boolean home, List<Integer> list){
		isHome = home;
		mInflater = LayoutInflater.from(context);
		homeList = list;
	}
	
	public FileInfoAdapter(Context context, List<FileInfo> list){
		isHome = false;
		mInflater = LayoutInflater.from(context);
		this.mList = list;
		this.mContext = context;
		mIsSelected = new SparseBooleanArray();
		//init checkbox
		selectAll(false);
		bitmapLoader = new AsyncImageLoader(context);
	}
	
	/**
	 * Select All or not
	 * @param isChecked true or false
	 */
	public void selectAll(boolean isChecked){
		int count = this.getCount();
		for (int i = 0; i < count; i++) {
			setChecked(i, isChecked);
		}
	}
	
	/**
	 * set checkbox checked or not
	 * @param position the position that clicked
	 * @param isChecked checked or not
	 */
	public void setChecked(int position, boolean isChecked){
		mIsSelected.put(position, isChecked);
	}
	
	/**
	 * return current position checked or not
	 * @param position current position
	 * @return checked or not
	 */
	public boolean isChecked(int position){
		return mIsSelected.get(position);
	}
	
	/**
	 * get how many item that has cheked
	 * @return checked items num.
	 */
	public int getCheckedItems(){
		if (isHome) {
			return 0;
		}
		
		int count = 0;
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				count ++;
			}
		}
		return count;
	}
	
	public List<String> getCheckedFiles(){
		if (isHome) {
			return null;
		}
		
		List<String> pathList = new ArrayList<String>();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				pathList.add(mList.get(i).filePath);
			}
		}
		
		return pathList;
	}
	
	public void setFlag(boolean flag){
		this.flag = flag;
	}
	
	@Override
	public int getCount() {
		if (isHome) {
			return homeList.size();
		}else {
			return mList.size();
		}
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
			view = mInflater.inflate(R.layout.ui_file_item, parent, false);
			holder.iconView = (ImageView) view.findViewById(R.id.file_icon_imageview);
			holder.nameView = (TextView) view.findViewById(R.id.file_name_textview);
			holder.dateAndSizeView = (TextView) view.findViewById(R.id.file_info_textview);
			holder.checkBox = (CheckBox) view.findViewById(R.id.file_checkbox);
			view.setTag(holder);
		}else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}
		
		//home view ui
		if (isHome) {
			holder.checkBox.setVisibility(View.GONE);
			holder.dateAndSizeView.setVisibility(View.GONE);
			if (MountManager.INTERNAL == homeList.get(position)) {
				holder.iconView.setImageResource(R.drawable.storage_internal_n);
				holder.nameView.setText(R.string.internal_sdcard);
			}else {
				holder.iconView.setImageResource(R.drawable.storage_sd_card_n);
				holder.nameView.setText(R.string.sdcard);
			}
			
			return view;
		}
		
		FileInfo fileInfo = mList.get(position);
		String size = fileInfo.getFormatFileSize();
		String date = fileInfo.getFormateDate();
		//use async thread loader bitmap
		if(FileInfoManager.TYPE_IMAGE  == fileInfo.type){
			if (!flag) {
				if (AsyncImageLoader.bitmapCache.size() > 0 &&
						AsyncImageLoader.bitmapCache.get(fileInfo.filePath) != null) {
					holder.iconView.setImageBitmap(AsyncImageLoader.bitmapCache.get(fileInfo.filePath).get());
				}else {
					holder.iconView.setImageDrawable(fileInfo.icon);
				}
			}else {
				Bitmap bitmap = bitmapLoader.loadImage(fileInfo.filePath, fileInfo.type, FileBrowserFragment.bitmapCaches, 
						holder.iconView, 
						new ILoadImageCallback() {
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
		
		Boolean is = isChecked(position);
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
