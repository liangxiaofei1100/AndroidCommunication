package com.dreamlink.communication.ui.file;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;

public class FileClassifyAdapter extends BaseAdapter{
	private LayoutInflater inflater;
	private List<FileInfo> mItemList = new ArrayList<FileInfo>();
	private List<List<FileInfo>> mAllList = new ArrayList<List<FileInfo>>();

	private int mSize = 0;
	public boolean isMain = true;

	private Context mContext;

	private AsyncImageLoader bitmapLoader;
	private FileInfoManager fileInfoManager;

	private static final int[] TYPE_ICONS = { R.drawable.default_video_icon,
			R.drawable.default_document_icon, R.drawable.default_ebook_icon,
			R.drawable.default_apk_icon, R.drawable.default_archives_icon,
			R.drawable.default_big_file_icon };

	private int file_type = -1;

	private int current_position = -1;

	public FileClassifyAdapter(Context context, List<FileInfo> itemList,
			int type) {
		mContext = context;
		inflater = LayoutInflater.from(context);
		this.mItemList = itemList;
		mSize = itemList.size();
		isMain = false;
		file_type = type;
		bitmapLoader = new AsyncImageLoader(context);
		fileInfoManager = new FileInfoManager(context);
	}

	public FileClassifyAdapter(FragmentActivity activity,
			List<List<FileInfo>> allList) {
		mContext = activity;
		inflater = LayoutInflater.from(activity);
		this.mAllList = allList;
		mSize = FileClassifyFragment.file_types.length;
		isMain = true;
	}

	@Override
	public int getCount() {
		return mSize;
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
	
	public int getFileType(){
		return file_type;
	}

	private class ViewHolder {
		ImageView iconView;
		TextView titleView;
		TextView tipView;
	}

	private class ViewHolderItem {
		ImageView iconView;
		TextView nameView;
		TextView infoView;
		TextView menuNameView;

		LinearLayout mainLayout;
		LinearLayout expandLayout;
		LinearLayout openLayout;
		LinearLayout sendLayout;
		LinearLayout deleteLayout;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;

		if (isMain) {
			ViewHolder holder = null;
			if (null == convertView || null == convertView.getTag()) {
				holder = new ViewHolder();
				view = inflater.inflate(R.layout.ui_file_classify_item, null);
				holder.iconView = (ImageView) view.findViewById(R.id.classify_icon);
				holder.titleView = (TextView) view.findViewById(R.id.classify_name);
				holder.tipView = (TextView) view.findViewById(R.id.classify_tip);

				view.setTag(holder);
			} else {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}

			holder.iconView.setImageResource(TYPE_ICONS[position]);
			int size = 0;
			if (mAllList.size() > 0) {
				size = mAllList.get(position).size();
			}
			holder.titleView.setText(FileClassifyFragment.file_types[position] + "(" + size + ")");
			holder.tipView.setText(FileClassifyFragment.file_types_tips[position]);
		} else {
			// item view
			ViewHolderItem holderItem = null;
			if (null == convertView || null == convertView.getTag()) {
				holderItem = new ViewHolderItem();
				view = inflater.inflate(R.layout.ui_file_classify_item2, null);

				holderItem.iconView = (ImageView) view.findViewById(R.id.file_icon);
				holderItem.nameView = (TextView) view.findViewById(R.id.file_name_textview);
				holderItem.infoView = (TextView) view.findViewById(R.id.file_info_textview);
				holderItem.menuNameView = (TextView) view.findViewById(R.id.file_item_open_textview);

				//20130909 do not use expand menu
				/*holderItem.mainLayout = (LinearLayout) view.findViewById(R.id.file_layout_main);
				holderItem.expandLayout = (LinearLayout) view.findViewById(R.id.file_layout_expand);
				holderItem.openLayout = (LinearLayout) view.findViewById(R.id.file_item_open);
				holderItem.sendLayout = (LinearLayout) view.findViewById(R.id.file_item_send);
				holderItem.deleteLayout = (LinearLayout) view.findViewById(R.id.file_item_delete);*/

				view.setTag(holderItem);
			} else {
				view = convertView;
				holderItem = (ViewHolderItem) view.getTag();
			}

			FileInfo fileInfo = mItemList.get(position);
			holderItem.nameView.setText(fileInfo.fileName);
			holderItem.infoView.setText(fileInfo.getFormateDate() + " | "
					+ fileInfo.getFormatFileSize());
			file_type = fileInfo.type;
			// icon view
			if (FileInfoManager.TYPE_VIDEO == file_type) {
				// do not use thumbnail in video
				holderItem.iconView.setImageDrawable(fileInfo.icon);
				holderItem.menuNameView.setText(R.string.menu_play);
			} else if (FileInfoManager.TYPE_AUDIO == file_type) {
				holderItem.iconView.setImageDrawable(fileInfo.icon);
				holderItem.menuNameView.setText(R.string.menu_play);
			} else if (FileInfoManager.TYPE_APK == file_type) {
				Bitmap cacheBitmap = null;
				// ////////////方法一////////////
				// bitmapLoader.setImageView(holderItem.iconView);
				// cacheBitmap = bitmapLoader.loadImage(fileInfo.filePath,
				// fileInfo.type);
				// ////////////方法三////////////
				cacheBitmap = bitmapLoader.loadImage(fileInfo.filePath,
						fileInfo.type, holderItem.iconView,
						new ILoadImageCallback() {
							@Override
							public void onObtainBitmap(Bitmap bitmap,
									ImageView imageView) {
								// TODO Auto-generated method stub
								if (null != bitmap) {
									imageView.setImageBitmap(bitmap);
								}
							}
						});

				if (null != cacheBitmap) {
					holderItem.iconView.setImageBitmap(cacheBitmap);
				} else {
					holderItem.iconView.setImageDrawable(fileInfo.icon);
				}
				holderItem.menuNameView.setText(R.string.menu_install);
			} else {
				holderItem.iconView.setImageDrawable(fileInfo.icon);
				holderItem.menuNameView.setText(R.string.menu_open);
			}

			/*holderItem.mainLayout.setOnClickListener(new MainOnClickLinstener(
					position));
			holderItem.openLayout.setOnClickListener(new OpenOnClickListener(
					position));
			holderItem.sendLayout.setOnClickListener(new SendOnClickListener(
					position));
			holderItem.deleteLayout
					.setOnClickListener(new DeleteOnClickListener(position,
							file_type));

			if (current_position == position) {
				holderItem.expandLayout.setVisibility(View.VISIBLE);
				holderItem.openLayout.setClickable(true);
				holderItem.sendLayout.setClickable(true);
				holderItem.deleteLayout.setClickable(true);
			} else {
				holderItem.expandLayout.setVisibility(View.GONE);
				holderItem.openLayout.setClickable(false);
				holderItem.sendLayout.setClickable(false);
				holderItem.deleteLayout.setClickable(false);
			}*/
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

	private class OpenOnClickListener implements OnClickListener {
		int position;

		OpenOnClickListener(int position) {
			this.position = position;
		}

		@Override
		public void onClick(View v) {
			current_position = -1;
			String filePath = mItemList.get(position).filePath;
			fileInfoManager.openFile(filePath);
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
			// send share transfer
			current_position = -1;
			notifyDataSetChanged();
		}

	}

	private class DeleteOnClickListener implements OnClickListener {
		int position;
		int fileType;

		DeleteOnClickListener(int position, int fileType) {
			this.position = position;
			this.fileType = fileType;
		}

		@Override
		public void onClick(View v) {
			String path = mItemList.get(position).filePath;
			final FileDeleteDialog deleteDialog = new FileDeleteDialog(mContext, R.style.TransferDialog, path);
			deleteDialog.setOnClickListener(new OnDelClickListener() {
				@Override
				public void onClick(View view, String path) {
					switch (view.getId()) {
					case R.id.left_button:
						DeleteTask deleteTask = new DeleteTask();
						deleteTask.execute(position, fileType);
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
			int fileType = params[1];
			String path = mItemList.get(del_pos).filePath;
			switch (fileType) {
			case FileInfoManager.TYPE_AUDIO:
				fileInfoManager.deleteFileInMediaStore(DreamConstant.AUDIO_URI,
						path);
				break;
			case FileInfoManager.TYPE_VIDEO:
				fileInfoManager.deleteFileInMediaStore(DreamConstant.VIDEO_URI,
						path);
				break;
			case FileInfoManager.TYPE_IMAGE:
				fileInfoManager.deleteFileInMediaStore(DreamConstant.IMAGE_URI,
						path);
			default:
				fileInfoManager.deleteFile(path);
				break;
			}
			return null;
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
			mItemList.remove(del_pos);
			mSize = mItemList.size();
			current_position = -1;
			notifyDataSetChanged();
		}

	}

}