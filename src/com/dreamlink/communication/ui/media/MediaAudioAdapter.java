package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MediaAudioAdapter extends BaseAdapter {
	private List<MediaInfo> mList = new ArrayList<MediaInfo>();
	private LayoutInflater mInflater = null;
	private Context mContext;
	private int current_position = -1;
	private AsyncImageLoader bitmapLoader;

	public MediaAudioAdapter(Context context, List<MediaInfo> data) {
		mInflater = LayoutInflater.from(context);
		mList = data;

		mContext = context;
		// bitmapLoader = new AsyncImageLoader(context);
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

	public void setPosition(int position) {
		this.current_position = position;
	}

	private class ViewHolder {
		ImageView iconView;
		TextView titleView;
		TextView timeView;
		TextView artistView;
		TextView sizeView;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		View view = null;
		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			view = mInflater.inflate(R.layout.ui_media_audio_item, null);

			holder.iconView = (ImageView) view.findViewById(R.id.audio_icon);
			holder.titleView = (TextView) view.findViewById(R.id.audio_title);
			holder.timeView = (TextView) view.findViewById(R.id.audio_time);
			holder.artistView = (TextView) view.findViewById(R.id.audio_artist);
			holder.sizeView = (TextView) view.findViewById(R.id.audio_size);

			view.setTag(holder);
		} else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}

		MediaInfo mediaInfo = mList.get(position);

		holder.titleView.setText((position + 1) + "." + mediaInfo.getDisplayName());
		holder.timeView.setText(mediaInfo.formatTime());
		holder.artistView.setText(mediaInfo.getArtist());
		holder.sizeView.setText(mediaInfo.getFormatSize());
		// no need show album, i don't wanna do this now
		// Bitmap bitmap = bitmapLoader.loadAudioBitmap(mediaInfo.getId(),
		// mediaInfo.getAlbumId(), holder.iconView,
		// new ILoadImageCallback() {
		// @Override
		// public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
		// imageView.setImageBitmap(bitmap);
		// }
		// @Override
		// public void onObtainDrawable(Drawable drawable, ImageView imageView)
		// {
		// // TODO Auto-generated method stub
		// }
		// });
		//
		// if (bitmap != null) {
		// Log.i("Yuri", "not null name:" + mediaInfo.getDisplayName());
		// holder.iconView.setImageBitmap(bitmap);
		// }else {
		// Log.e("Yuri", "null name:"+ mediaInfo.getDisplayName());
		// holder.iconView.setImageResource(R.drawable.default_audio_iv);
		// }
		holder.iconView.setImageResource(R.drawable.icon_audio);

		return view;
	}
}
