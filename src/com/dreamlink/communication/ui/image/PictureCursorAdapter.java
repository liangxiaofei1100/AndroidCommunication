package com.dreamlink.communication.ui.image;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.image.AsyncPictureLoader.ILoadImagesCallback;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class PictureCursorAdapter extends CursorAdapter {
	private  static final String TAG = "ImageCursorAdapter";
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
	public void bindView(View view, Context arg1, Cursor cursor) {
		final ViewHolder holder = (ViewHolder) view.getTag();
		long id = cursor.getLong(cursor
				.getColumnIndex(MediaColumns._ID));
		
		if (!mIdleFlag) {
			if (AsyncPictureLoader.bitmapCaches.size() >0 &&
					AsyncPictureLoader.bitmapCaches.get(id) != null) {
				holder.imageView.setImageBitmap(AsyncPictureLoader.bitmapCaches.get(id).get());
			}else {
				holder.imageView.setImageResource(R.drawable.photo_l);
			}
		}else {
			Bitmap bitmap = pictureLoader.loadBitmap(id, new ILoadImagesCallback() {
				@Override
				public void onObtainBitmap(Bitmap bitmap, long id) {
					holder.imageView.setImageBitmap(bitmap);
				}
			});
			
			if (null == bitmap) {
				//在图片没有读取出来的情况下预先放一张图
				holder.imageView.setImageResource(R.drawable.photo_l);
			}else {
				holder.imageView.setImageBitmap(bitmap);
			}
		}
	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
		View view = mInflater.inflate(R.layout.ui_picture_item, null);
		ViewHolder  holder = new ViewHolder();
		holder.imageView = (ImageView) view.findViewById(R.id.picture_imageview);
		view.setTag(holder);
		return view;
	}
	
	public class ViewHolder{
		ImageView imageView;
	}

}
