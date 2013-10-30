package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.common.BaseCursorAdapter;
import com.dreamlink.communication.ui.image.AsyncPictureLoader;
import com.dreamlink.communication.ui.media.AsyncVideoLoader.ILoadVideoCallback;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

public class VideoCursorAdapter extends BaseCursorAdapter {
	private static final String TAG = "VideoCursorAdapter";
	private LayoutInflater mInflater = null;
	private AsyncVideoLoader asyncVideoLoader;
	private boolean mIdleFlag = true;

	public VideoCursorAdapter(Context context) {
		super(context, null, true);
		mInflater = LayoutInflater.from(context);
		asyncVideoLoader = new AsyncVideoLoader(context);
	}
	
	public void setIdleFlag(boolean flag){
		this.mIdleFlag = flag;
	}
	
	@Override
	public void selectAll(boolean isSelected) {
		super.selectAll(isSelected);
		int count = this.getCount();
		for (int i = 0; i < count; i++) {
			setSelected(i, isSelected);
		}
	}
	
	@Override
	public int getSelectedItemsCount() {
		int count = 0;
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				count ++;
			}
		}
		return count;
	}
	
	@Override
	public List<Integer> getSelectedItemPos() {
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				list.add(i);
			}
		}
		return list;
	}
	
	public List<String> getSelectItemList(){
		List<String> list = new ArrayList<String>();
		Cursor cursor = getCursor();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				cursor.moveToPosition(i);
				String url = cursor.getString(cursor
						.getColumnIndex(MediaStore.Video.Media.DATA));
				Log.d(TAG, "getSelectItemList:" + url);
				list.add(url);
			}
		}
		return list;
	}
	
	/**
	 * get Select item filename  list
	 * @return
	 */
	public List<String> getSelectItemNameList(){
		List<String> list = new ArrayList<String>();
		Cursor cursor = getCursor();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				cursor.moveToPosition(i);
				String name = cursor.getString(cursor
						.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
				Log.d(TAG, "getSelectItemNameList:" + name);
				list.add(name);
			}
		}
		return list;
	}

	@Override
	public void bindView(View view, Context arg1, Cursor cursor) {
//		final ViewHolder holder = (ViewHolder) view.getTag();
//		long id = cursor.getLong(cursor
//				.getColumnIndex(MediaStore.Video.Media._ID));
//		long duration = cursor.getLong(cursor
//				.getColumnIndex(MediaStore.Video.Media.DURATION)); // 时长
//		
//		Bitmap bitmap = asyncVideoLoader.loadBitmap(id, new ILoadVideoCallback() {
//			@Override
//			public void onObtainBitmap(Bitmap bitmap, long id) {
//				holder.iconView.setImageBitmap(bitmap);
//			}
//		});
//		
//		if (null == bitmap) {
//			//在图片没有读取出来的情况下预先放一张图
//			holder.iconView.setImageResource(R.drawable.default_video_iv);
//		}else {
//			holder.iconView.setImageBitmap(bitmap);
//		}
//		
//		holder.timeView.setText(DreamUtil.mediaTimeFormat(duration));
//		
//		boolean isSelected = isSelected(cursor.getPosition());
//		updateViewBackground(isSelected, cursor.getPosition(), view);
		
		final VideoGridItem item = (VideoGridItem) view;
		long id = cursor.getLong(cursor
				.getColumnIndex(MediaStore.Video.Media._ID));
		long duration = cursor.getLong(cursor
				.getColumnIndex(MediaStore.Video.Media.DURATION)); // 时长
		
		if (!mIdleFlag) {
			if (AsyncVideoLoader.bitmapCaches.size() > 0
					&& AsyncVideoLoader.bitmapCaches.get(id) != null) {
				item.setIconBitmap(AsyncVideoLoader.bitmapCaches.get(id)
						.get());
			} else {
				item.setIconResId(R.drawable.default_video_iv);
			}
		}else {
			Bitmap bitmap = asyncVideoLoader.loadBitmap(id,
					new ILoadVideoCallback() {
						@Override
						public void onObtainBitmap(Bitmap bitmap, long id) {
							item.setIconBitmap(bitmap);
						}
					});

			if (null == bitmap) {
				// 在图片没有读取出来的情况下预先放一张图
				item.setIconResId(R.drawable.default_video_iv);
			} else {
				item.setIconBitmap(bitmap);
			}
		}

		item.setVideoTime(duration);
		item.setChecked(mIsSelected.get(cursor.getPosition()));
	}

	@Override
	public View newView(Context arg0, Cursor cursor, ViewGroup arg2) {
//		View view = mInflater.inflate(R.layout.ui_media_video_item, null);
//		
//		ViewHolder holder = new ViewHolder();
//		holder.iconView = (ImageView) view.findViewById(R.id.iv_video_icon);
//		holder.timeView = (TextView) view.findViewById(R.id.tv_video_time);
//		
//		view.setTag(holder);
		
		VideoGridItem item = new VideoGridItem(arg0);
		item.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		return item;
	}
	
	public class ViewHolder{
		ImageView iconView;
		TextView timeView;
	}

}
