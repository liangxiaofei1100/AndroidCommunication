package com.dreamlink.communication.ui.media;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.media.AsyncVideoLoader.ILoadVideoCallback;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class VideoCursorAdapter extends CursorAdapter {
	private static final String TAG = "VideoCursorAdapter";
	private LayoutInflater mInflater = null;
	private AsyncVideoLoader asyncVideoLoader;

	public VideoCursorAdapter(Context context) {
		super(context, null, true);
		mInflater = LayoutInflater.from(context);
		asyncVideoLoader = new AsyncVideoLoader(context);
	}

	@Override
	public void bindView(View view, Context arg1, Cursor cursor) {
		final ViewHolder holder = (ViewHolder) view.getTag();
		long id = cursor.getLong(cursor
				.getColumnIndex(MediaStore.Video.Media._ID));
		long duration = cursor.getLong(cursor
				.getColumnIndex(MediaStore.Video.Media.DURATION)); // 时长
		
		Bitmap bitmap = asyncVideoLoader.loadBitmap(id, new ILoadVideoCallback() {
			@Override
			public void onObtainBitmap(Bitmap bitmap, long id) {
				holder.iconView.setImageBitmap(bitmap);
			}
		});
		
		if (null == bitmap) {
			//在图片没有读取出来的情况下预先放一张图
			holder.iconView.setImageResource(R.drawable.default_video_iv);
		}else {
			holder.iconView.setImageBitmap(bitmap);
		}
		
		holder.timeView.setText(DreamUtil.mediaTimeFormat(duration));
	}

	@Override
	public View newView(Context arg0, Cursor cursor, ViewGroup arg2) {
		View view = mInflater.inflate(R.layout.ui_media_video_item, null);
		
		ViewHolder holder = new ViewHolder();
		holder.iconView = (ImageView) view.findViewById(R.id.video_icon_imageview);
		holder.timeView = (TextView) view.findViewById(R.id.videotime_textview);
		
		view.setTag(holder);
		return view;
	}
	
	private class ViewHolder{
		ImageView iconView;
		TextView timeView;
	}

}
