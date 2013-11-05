package com.dreamlink.communication.ui.file;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileInfoAdapter extends BaseAdapter {
	private static final String TAG = "FileInfoAdapter";
	private List<FileInfo> mList = new ArrayList<FileInfo>();
	private LayoutInflater mInflater = null;

	private SparseBooleanArray mIsSelected = null;

	private AsyncImageLoader bitmapLoader;

	private boolean mIdleFlag = true;

	public int mMode = DreamConstant.MENU_MODE_NORMAL;

	public FileInfoAdapter(Context context, List<FileInfo> list) {
		mInflater = LayoutInflater.from(context);
		this.mList = list;
		mIsSelected = new SparseBooleanArray();
		bitmapLoader = new AsyncImageLoader(context);
	}

	/**
	 * Select All or not
	 * 
	 * @param isSelected
	 *            true or false
	 */
	public void selectAll(boolean isSelected) {
		int count = this.getCount();
		Log.d(TAG, "selectALl.count=" + count);
		for (int i = 0; i < count; i++) {
			setSelected(i, isSelected);
		}
	}

	/**
	 * set selected or not
	 * 
	 * @param position
	 *            the position that clicked
	 * @param isSelected
	 *            checked or not
	 */
	public void setSelected(int position, boolean isSelected) {
		mIsSelected.put(position, isSelected);
	}

	public void setSelected(int position) {
		mIsSelected.put(position, !isSelected(position));
	}

	public void clearSelected() {
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				setSelected(i, false);
			}
		}
	}

	/**
	 * return current position checked or not
	 * 
	 * @param position
	 *            current position
	 * @return checked or not
	 */
	public boolean isSelected(int position) {
		return mIsSelected.get(position);
	}

	/**
	 * get how many item that has cheked
	 * 
	 * @return checked items num.
	 */
	public int getSelectedItems() {
		int count = 0;
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				count++;
			}
		}
		return count;
	}

	public List<FileInfo> getSelectedList() {
		List<FileInfo> fileList = new ArrayList<FileInfo>();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				fileList.add(mList.get(i));
			}
		}
		return fileList;
	}

	public List<String> getSelectedFiles() {
		List<String> pathList = new ArrayList<String>();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				pathList.add(mList.get(i).filePath);
			}
		}

		return pathList;
	}

	public List<Integer> getCheckedItemIds() {
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				list.add(mIsSelected.keyAt(i));
			}
		}
		return list;
	}

	/**
	 * set scroll is idle or not
	 * 
	 * @param flag
	 */
	public void setFlag(boolean flag) {
		this.mIdleFlag = flag;
	}

	/**
	 * This method changes the display mode of adapter between MODE_NORMAL,
	 * MODE_EDIT
	 * 
	 * @param mode
	 *            the mode which will be changed to be.
	 */
	public void changeMode(int mode) {
		mMode = mode;
	}

	/**
	 * This method gets current display mode of the adapter.
	 * 
	 * @return current display mode of adapter
	 */
	public int getMode() {
		return mMode;
	}

	/**
	 * This method checks that current mode equals to certain mode, or not.
	 * 
	 * @param mode
	 *            the display mode of adapter
	 * @return true for equal, and false for not equal
	 */
	public boolean isMode(int mode) {
		return mMode == mode;
	}

	public List<FileInfo> getList() {
		return mList;
	}

	public void setList(List<FileInfo> fileList) {
		mList = fileList;
	}

	/**
	 * This method gets index of certain fileInfo(item) in fileInfoList
	 * 
	 * @param fileInfo
	 *            the fileInfo which wants to be located.
	 * @return the index of the item in the listView.
	 */
	public int getPosition(FileInfo fileInfo) {
		Log.d(TAG, "getPosition:" + fileInfo.filePath);
		for (int i = 0; i < mList.size(); i++) {
			if (fileInfo.filePath.equals(mList.get(i).filePath)) {
				return i;
			}
		}
		return mList.indexOf(fileInfo);
	}

	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public FileInfo getItem(int position) {
		if (mList.size() <= 0) {
			return null;
		}
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	class ViewHolder {
		ImageView iconView;
		TextView nameView;
		TextView dateAndSizeView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		ViewHolder holder = null;

		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			view = mInflater.inflate(R.layout.ui_file_item, parent, false);
			holder.iconView = (ImageView) view
					.findViewById(R.id.file_icon_imageview);
			holder.nameView = (TextView) view
					.findViewById(R.id.file_name_textview);
			holder.dateAndSizeView = (TextView) view
					.findViewById(R.id.file_info_textview);
			view.setTag(holder);
		} else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}

		FileInfo fileInfo = mList.get(position);
		String size = fileInfo.getFormatFileSize();
		String date = fileInfo.getFormateDate();
		// use async thread loader bitmap
		if (FileInfoManager.TYPE_IMAGE == fileInfo.type) {
			if (!mIdleFlag) {
				if (AsyncImageLoader.bitmapCache.size() > 0
						&& AsyncImageLoader.bitmapCache.get(fileInfo.filePath) != null) {
					holder.iconView.setImageBitmap(AsyncImageLoader.bitmapCache
							.get(fileInfo.filePath).get());
				} else {
					holder.iconView.setImageDrawable(fileInfo.icon);
				}
			} else {
				Bitmap bitmap = bitmapLoader.loadImage(fileInfo.filePath,
						fileInfo.type, holder.iconView,
						new ILoadImageCallback() {
							@Override
							public void onObtainBitmap(Bitmap bitmap,
									ImageView imageView) {
								imageView.setImageBitmap(bitmap);
							}
						});
				if (null != bitmap) {
					holder.iconView.setImageBitmap(bitmap);
				} else {
					holder.iconView.setImageDrawable(fileInfo.icon);
				}
			}
		} else if (FileInfoManager.TYPE_APK == fileInfo.type) {
			Bitmap cacheDrawable = bitmapLoader.loadImage(fileInfo.filePath,
					fileInfo.type, holder.iconView, new ILoadImageCallback() {
						@Override
						public void onObtainBitmap(Bitmap bitmap,
								ImageView imageView) {
							if (null != bitmap) {
								imageView.setImageBitmap(bitmap);
							}
						}
					});

			if (null != cacheDrawable) {
				holder.iconView.setImageBitmap(cacheDrawable);
			} else {
				holder.iconView.setImageResource(R.drawable.icon_apk);
			}
		} else if (FileInfoManager.TYPE_AUDIO == fileInfo.type) {
			holder.iconView.setImageDrawable(fileInfo.icon);
		} else if (FileInfoManager.TYPE_VIDEO == fileInfo.type) {
			Bitmap videoBitmap = bitmapLoader.loadImage(fileInfo.filePath,
					fileInfo.type, holder.iconView, new ILoadImageCallback() {
						@Override
						public void onObtainBitmap(Bitmap bitmap,
								ImageView imageView) {
							if (null != bitmap) {
								imageView.setImageBitmap(bitmap);
							}
						}
					});

			if (null != videoBitmap) {
				holder.iconView.setImageBitmap(videoBitmap);
			} else {
				holder.iconView.setImageDrawable(fileInfo.icon);
			}
		} else {
			holder.iconView.setImageDrawable(fileInfo.icon);
		}

		holder.nameView.setText(fileInfo.fileName);

		Boolean is = isSelected(position);
		if (null == is) {
			is = false;
		}

		if (fileInfo.isDir) {
			holder.dateAndSizeView.setText(date);
		} else {
			holder.dateAndSizeView.setText(date + " | " + size);
		}

		if (mMode == DreamConstant.MENU_MODE_EDIT) {
			updateListViewBackground(position, view);
		} else {
			view.setBackgroundResource(Color.TRANSPARENT);
		}

		return view;
	}

	private void updateListViewBackground(int position, View view) {
		if (isSelected(position)) {
			view.setBackgroundResource(R.color.bright_blue);
		} else {
			view.setBackgroundResource(Color.TRANSPARENT);
		}
	}

}
