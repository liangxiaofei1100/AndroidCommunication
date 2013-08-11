package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class MediaVideoAdapter extends BaseAdapter{
	private List<MediaInfo> mList = new ArrayList<MediaInfo>();
	private LayoutInflater mInflater = null;
	private Context mContext;
	private int current_position = -1;
	
	public MediaVideoAdapter(Context context, List<MediaInfo> data){
		mInflater = LayoutInflater.from(context);
		mList = data;
		
		mContext = context;
	}
	
	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void setPosition(int position){
		this.current_position = position;
	}

	class ViewHolder{
		ImageView iconView;
		TextView timeView;
	}
	
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		View view = null;
		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			view = mInflater.inflate(R.layout.ui_media_video_item, null);
			
			holder.iconView = (ImageView) view.findViewById(R.id.video_icon_imageview);
			holder.timeView = (TextView) view.findViewById(R.id.videotime_textview);
			
			view.setTag(holder);
		}else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}
		
		MediaInfo mediaInfo = mList.get(position);
		
		holder.iconView.setImageBitmap(mediaInfo.getIcon());
		holder.timeView.setText(mediaInfo.formatTime());
		
		
		return view;
	}

}
