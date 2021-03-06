package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.common.BaseCursorAdapter;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class AudioCursorAdapter extends BaseCursorAdapter {
	private static final String TAG = "AudioCursorAdapter";
	private LayoutInflater mInflater = null;

	public AudioCursorAdapter(Context context) {
		super(context, null, true);
		mInflater = LayoutInflater.from(context);
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
	
	public List<String> getSelectItemList(){
		List<String> list = new ArrayList<String>();
		Cursor cursor = getCursor();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				cursor.moveToPosition(i);
				String url = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.DATA));
				Log.d(TAG, "getSelectItemList:" + url);
				list.add(url);
			}
		}
		return list;
	}

	@Override
	public void bindView(View view, Context arg1, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		String title = cursor.getString((cursor
				.getColumnIndex(MediaStore.Audio.Media.TITLE))); // 音乐标题
		String artist = cursor.getString(cursor
				.getColumnIndex(MediaStore.Audio.Media.ARTIST)); // 艺术家
		long duration = cursor.getLong(cursor
				.getColumnIndex(MediaStore.Audio.Media.DURATION)); // 时长
		long size = cursor.getLong(cursor
				.getColumnIndex(MediaStore.Audio.Media.SIZE)); // 文件大小
		
		holder.iconView.setImageResource(R.drawable.icon_audio);
		holder.titleView.setText((cursor.getPosition() + 1) + "." + title);
		holder.artistView.setText(artist);
		holder.timeView.setText(DreamUtil.mediaTimeFormat(duration));
		holder.sizeView.setText(DreamUtil.getFormatSize(size));
		
		boolean isSelected = isSelected(cursor.getPosition());
		updateViewBackground(isSelected, cursor.getPosition(), view);
	}

	@Override
	public View newView(Context arg0, Cursor cursor, ViewGroup arg2) {
		View view = mInflater.inflate(R.layout.ui_media_audio_item, null);
		ViewHolder holder = new ViewHolder();
		holder.iconView = (ImageView) view.findViewById(R.id.audio_icon);
		holder.titleView = (TextView) view.findViewById(R.id.audio_title);
		holder.timeView = (TextView) view.findViewById(R.id.audio_time);
		holder.artistView = (TextView) view.findViewById(R.id.audio_artist);
		holder.sizeView = (TextView) view.findViewById(R.id.audio_size);
		
		view.setTag(holder);
		return view;
	}
	
	public class ViewHolder {
		ImageView iconView;
		TextView titleView;
		TextView timeView;
		TextView artistView;
		TextView sizeView;
	}

}
