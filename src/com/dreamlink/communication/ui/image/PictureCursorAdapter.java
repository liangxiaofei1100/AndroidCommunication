package com.dreamlink.communication.ui.image;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.common.BaseCursorAdapter;
import com.dreamlink.communication.ui.image.AsyncPictureLoader.ILoadImagesCallback;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

public class PictureCursorAdapter extends BaseCursorAdapter {
	private  static final String TAG = "PictureCursorAdapter";
	private LayoutInflater mInflater = null;
	private AsyncPictureLoader pictureLoader = null;
	private boolean mIdleFlag = true;

	public PictureCursorAdapter(Context context) {
		super(context, null , true);
		mInflater = LayoutInflater.from(context);
		pictureLoader = new AsyncPictureLoader(context);
	}
	
	public void setIdleFlag(boolean flag){
		this.mIdleFlag = flag;
	}
	
	@Override
	public void selectAll(boolean isSelected) {
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
	
	/**
	 * get Select item url list
	 * @return
	 */
	public List<String> getSelectItemList(){
		Log.d(TAG, "getSelectItemList");
		List<String> list = new ArrayList<String>();
		Cursor cursor = getCursor();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				cursor.moveToPosition(i);
				String url = cursor.getString(cursor
						.getColumnIndex(MediaStore.Images.Media.DATA));
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
						.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
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
//				.getColumnIndex(MediaColumns._ID));
//		
//		if (!mIdleFlag) {
//			if (AsyncPictureLoader.bitmapCaches.size() >0 &&
//					AsyncPictureLoader.bitmapCaches.get(id) != null) {
//				holder.imageView.setImageBitmap(AsyncPictureLoader.bitmapCaches.get(id).get());
//			}else {
//				holder.imageView.setImageResource(R.drawable.photo_l);
//			}
//		}else {
//			Bitmap bitmap = pictureLoader.loadBitmap(id, new ILoadImagesCallback() {
//				@Override
//				public void onObtainBitmap(Bitmap bitmap, long id) {
//					holder.imageView.setImageBitmap(bitmap);
//				}
//			});
//			
//			if (null == bitmap) {
//				//在图片没有读取出来的情况下预先放一张图
//				holder.imageView.setImageResource(R.drawable.photo_l);
//			}else {
//				holder.imageView.setImageBitmap(bitmap);
//			}
//		}
		
		final PictureGridItem item = (PictureGridItem) view;
		
		long id = cursor.getLong(cursor.getColumnIndex(MediaColumns._ID));
		if (!mIdleFlag) {
			if (AsyncPictureLoader.bitmapCaches.size() > 0
					&& AsyncPictureLoader.bitmapCaches.get(id) != null) {
				item.setIconBitmap(AsyncPictureLoader.bitmapCaches.get(id)
						.get());
			} else {
				item.setIconResId(R.drawable.photo_l);
			}
		} else {
			Bitmap bitmap = pictureLoader.loadBitmap(id,
					new ILoadImagesCallback() {
						@Override
						public void onObtainBitmap(Bitmap bitmap, long id) {
							item.setIconBitmap(bitmap);
						}
					});

			if (null == bitmap) {
				// 在图片没有读取出来的情况下预先放一张图
				item.setIconResId(R.drawable.photo_l);
			} else {
				item.setIconBitmap(bitmap);
			}
		}
		
		item.setChecked(mIsSelected.get(cursor.getPosition()));
	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
//		View view = mInflater.inflate(R.layout.ui_picture_item, null);
//		ViewHolder  holder = new ViewHolder();
//		holder.imageView = (ImageView) view.findViewById(R.id.iv_picture_item);
//		view.setTag(holder);
//		return view;
		
		//1029 test
		PictureGridItem item = new PictureGridItem(arg0);
		item.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		return item;
		//1029 test
	}
	
	public class ViewHolder{
		ImageView imageView;
	}

}
