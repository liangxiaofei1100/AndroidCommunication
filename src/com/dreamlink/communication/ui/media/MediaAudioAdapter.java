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
	private FileInfoManager mFileInfoManager = null;
	private Notice mNotice;

	public MediaAudioAdapter(Context context, List<MediaInfo> data) {
		mInflater = LayoutInflater.from(context);
		mList = data;

		mContext = context;
		// bitmapLoader = new AsyncImageLoader(context);

		mFileInfoManager = new FileInfoManager(context);
		mNotice = new Notice(context);
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

	class ViewHolder {
		ImageView iconView;
		TextView titleView;
		TextView timeView;
		TextView artistView;
		TextView sizeView;
		LinearLayout mainLayout;
		LinearLayout menuLayout;
		LinearLayout playLayout;
		LinearLayout sendLayout;
		LinearLayout deleteLayout;
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

			holder.mainLayout = (LinearLayout) view
					.findViewById(R.id.audio_layout_main);
			holder.menuLayout = (LinearLayout) view
					.findViewById(R.id.layout_expand);
			holder.playLayout = (LinearLayout) view
					.findViewById(R.id.item_play);
			holder.sendLayout = (LinearLayout) view
					.findViewById(R.id.item_send);
			holder.deleteLayout = (LinearLayout) view
					.findViewById(R.id.item_delete);
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
		holder.iconView.setImageResource(R.drawable.default_audio_iv);

		holder.mainLayout
				.setOnClickListener(new MainOnClickLinstener(position));
		holder.playLayout.setOnClickListener(new PlayOnClickListener(position));
		holder.sendLayout.setOnClickListener(new SendOnClickListener(position));
		holder.deleteLayout.setOnClickListener(new DeleteOnClickListener(
				position));
		if (current_position == position) {
			holder.menuLayout.setVisibility(View.VISIBLE);
			holder.playLayout.setClickable(true);
			holder.sendLayout.setClickable(true);
			holder.deleteLayout.setClickable(true);
		} else {
			holder.menuLayout.setVisibility(View.GONE);
			holder.playLayout.setClickable(false);
			holder.sendLayout.setClickable(false);
			holder.deleteLayout.setClickable(false);
		}

		return view;
	}

	private class MainOnClickLinstener implements OnClickListener {
		int position;

		MainOnClickLinstener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			if (current_position == position) {
				current_position = -1;
			} else {
				current_position = position;
			}
			notifyDataSetChanged();
		}

	}

	private class PlayOnClickListener implements OnClickListener {
		int position;

		PlayOnClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			current_position = -1;
			
			Intent intent = FileInfoManager.getAudioFileIntent(mList.get(
					position).getUrl());
			mContext.startActivity(intent);
			notifyDataSetChanged();
		}
	}

	private class SendOnClickListener implements OnClickListener {
		int position;

		SendOnClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			current_position = -1;
			notifyDataSetChanged();
		}

	}

	private class DeleteOnClickListener implements OnClickListener {
		int position;

		DeleteOnClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			final String path = mList.get(position).getUrl();
			final FileDeleteDialog deleteDialog = new FileDeleteDialog(mContext, R.style.TransferDialog, path);
			deleteDialog.setOnClickListener(new OnDelClickListener() {
				@Override
				public void onClick(View view, String path) {
					switch (view.getId()) {
					case R.id.left_button:
						DeleteTask deleteTask = new DeleteTask();
						deleteTask.execute(position);
						break;
					default:
						break;
					}
				}
			});
			deleteDialog.show();
		}

	}

	private class DeleteTask extends AsyncTask<Integer, Void, Boolean> {
		ProgressDialog dialog = null;
		int del_pos;

		@Override
		protected Boolean doInBackground(Integer... params) {
			del_pos = params[0];
			String path = mList.get(del_pos).getUrl();
			boolean ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.AUDIO_URI,
					path);
			return ret;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog = new ProgressDialog(mContext);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setCancelable(false);
			dialog.setMessage("Deleteing...");
			dialog.show();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (null != dialog) {
				dialog.cancel();
			}

			if (result) {
				mList.remove(del_pos);
				current_position = -1;
				notifyDataSetChanged();

				Intent intent = new Intent(DreamConstant.MEDIA_AUDIO_ACTION);
				intent.putExtra(Extra.AUDIO_SIZE, mList.size());
				mContext.sendBroadcast(intent);
			}else {
				mNotice.showToast(R.string.delete_fail);
			}
		}
	}

}
