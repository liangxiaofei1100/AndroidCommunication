package com.dreamlink.communication.ui.history;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;
import com.dreamlink.communication.ui.common.FileSendUtil;
import com.dreamlink.communication.ui.file.FileBrowserFragment;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.util.Log;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HistoryMsgAdapter extends BaseAdapter {
	private static final String TAG = "HistoryMsgAdapter";
	private LayoutInflater inflater = null;
	private List<HistoryInfo> list = new ArrayList<HistoryInfo>();
	private int status = -1;
	private Context mContext;
	private AsyncImageLoader bitmapLoader = null;
	private boolean scrollFlag = true;
	
	public HistoryMsgAdapter(Context context, List<HistoryInfo> list){
		this.mContext = context;
		inflater = LayoutInflater.from(context);
		this.list = list;
		
		bitmapLoader = new AsyncImageLoader(context);
	}
	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public int getItemViewType(int position) {
		HistoryInfo historyInfo = list.get(position);
		if (HistoryManager.TYPE_RECEIVE == historyInfo.getMsgType()) {
			return 0;
		}else {
			return 1;
		}
	}
	
	@Override
	public int getViewTypeCount() {
		//如果你的list中有不同的视图类型，就一定要重写这个方法，并配合getItemViewType一起使用
		return 2;
	}
	
	public void setStatus(int status){
		this.status = status;
	}
	
	public void setFlag(boolean flag){
		this.scrollFlag = flag;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		View view = null;
		HistoryInfo historyInfo = list.get(position);
		int type = historyInfo.getMsgType();
		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			if (HistoryManager.TYPE_RECEIVE == type) {
				view = inflater.inflate(R.layout.ui_history_item_msg_left, null);
			}else {
				view = inflater.inflate(R.layout.ui_history_item_msg_right, null);
			}
			
			holder.receiveUserNameView = (TextView) view.findViewById(R.id.tv_send_title_msg);
			holder.titleBar = (ProgressBar) view.findViewById(R.id.bar_send_title);
			holder.transferBar = (ProgressBar) view.findViewById(R.id.bar_progressing);
			holder.transferBar.setMax(100);
			holder.iconView = (ImageView) view.findViewById(R.id.iv_send_file_icon);
			holder.dateView = (TextView) view.findViewById(R.id.tv_sendtime);
			holder.userNameView = (TextView) view.findViewById(R.id.tv_username);
			holder.fileNameView = (TextView) view.findViewById(R.id.tv_send_file_name);
			holder.fileSizeView = (TextView) view.findViewById(R.id.tv_send_file_size);
			holder.msgLayout = (LinearLayout) view.findViewById(R.id.layout_chatcontent);
			holder.msgLayout.setOnClickListener(new MsgOnClickListener(position));
			
			view.setTag(holder);
		}else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}
		
		
		String time = historyInfo.getFormatDate();
		String fileName = historyInfo.getFileInfo().getFileName();
		String userName = historyInfo.getSendUserName();
		double max = historyInfo.getMax();
		double progress = historyInfo.getProgress();
		int status = historyInfo.getStatus();
//		Log.d(TAG, "getView().status=" + status);
//		
		holder.dateView.setText(time);
		holder.userNameView.setText(userName);
		holder.fileNameView.setText(fileName);
		holder.fileSizeView.setTextColor(Color.BLACK);
		
		/*****use async thread loader bitmap start*********/
		//just load image,video,apk file icon;others use default icon
		if (!scrollFlag) {
			if (AsyncImageLoader.bitmapCache.size() > 0
					&& AsyncImageLoader.bitmapCache.get(historyInfo.getFileInfo().getFilePath()) != null) {
				holder.iconView.setImageBitmap(AsyncImageLoader.bitmapCache.get(historyInfo.getFileInfo().getFilePath()).get());
			} else {
				holder.iconView.setImageDrawable(historyInfo.getIcon());
			}
		}else {
			if (FileInfoManager.TYPE_IMAGE == historyInfo.getFileType()) {
				Bitmap bitmap = bitmapLoader.loadImage(historyInfo.getFileInfo().getFilePath(), historyInfo.getFileType(),
						FileBrowserFragment.bitmapCaches, holder.iconView, new ILoadImageCallback() {
							@Override
							public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
								imageView.setImageBitmap(bitmap);
							}
						});
				if (null != bitmap) {
					holder.iconView.setImageBitmap(bitmap);
				} else {
					holder.iconView.setImageDrawable(historyInfo.getIcon());
				}
			} else if (FileInfoManager.TYPE_APK == historyInfo.getFileType()) {
				Bitmap cacheDrawable = bitmapLoader.loadImage(historyInfo.getFileInfo().getFilePath(), historyInfo.getFileType(),
						holder.iconView, new ILoadImageCallback() {
							@Override
							public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
								if (null != bitmap) {
									imageView.setImageBitmap(bitmap);
								}
							}
						});

				if (null != cacheDrawable) {
					holder.iconView.setImageBitmap(cacheDrawable);
				} else {
					holder.iconView.setImageDrawable(historyInfo.getIcon());
				}
			} else if (FileInfoManager.TYPE_VIDEO == historyInfo.getFileType()) {
				Bitmap videoBitmap = bitmapLoader.loadImage(historyInfo.getFileInfo().getFilePath(), historyInfo.getFileType(),
						holder.iconView, new ILoadImageCallback() {
							@Override
							public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
								if (null != bitmap) {
									imageView.setImageBitmap(bitmap);
								}
							}
						});

				if (null != videoBitmap) {
					holder.iconView.setImageBitmap(videoBitmap);
				} else {
					holder.iconView.setImageDrawable(historyInfo.getIcon());
				}
			} else {
				holder.iconView.setImageDrawable(historyInfo.getIcon());
			}
		}
		/*****use async thread loader bitmap end*********/
		
		switch (status) {
		case HistoryManager.STATUS_PRE_SEND:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.pre_send, 
					historyInfo.getReceiveUser().getUserName()));
			holder.transferBar.setVisibility(View.GONE);
			holder.titleBar.setVisibility(View.VISIBLE);
			holder.fileSizeView.setText(DreamUtil.getFormatSize(max));
			break;
			
		case HistoryManager.STATUS_SENDING:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.sending, 
					historyInfo.getReceiveUser().getUserName()));
		case HistoryManager.STATUS_RECEIVING:
			holder.transferBar.setVisibility(View.VISIBLE);
			holder.titleBar.setVisibility(View.GONE);
			double percent = progress / max;
			Log.d(TAG, "percent=" + HistoryManager.nf.format(percent));
			holder.transferBar.setProgress((int)(percent * 100));
			holder.fileSizeView.setText(HistoryManager.nf.format(percent) + " | " 
					+ DreamUtil.getFormatSize(max) + " | " + historyInfo.getSpeed() + "/s");
			break;
		case HistoryManager.STATUS_SEND_SUCCESS:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.send_ok, 
					historyInfo.getReceiveUser().getUserName()));
		case HistoryManager.STATUS_RECEIVE_SUCCESS:
			holder.transferBar.setVisibility(View.GONE);
			holder.titleBar.setVisibility(View.GONE);
			holder.fileSizeView.setText(DreamUtil.getFormatSize(max));
			break;
		case HistoryManager.STATUS_SEND_FAIL:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.sending, 
					historyInfo.getReceiveUser().getUserName()));
			holder.transferBar.setVisibility(View.VISIBLE);
			double percent2 = progress / max;
			holder.transferBar.setProgress((int)(percent2 * 100));
			holder.fileSizeView.setText(mContext.getResources().getString(R.string.receive_fail) + " | " + DreamUtil.getFormatSize(max));
			holder.fileSizeView.setTextColor(Color.RED);
			break;
		case HistoryManager.STATUS_RECEIVE_FAIL:
			holder.transferBar.setVisibility(View.VISIBLE);
			double percent3 = progress / max;
			holder.transferBar.setProgress((int)(percent3 * 100));
			holder.fileSizeView.setText(R.string.receive_fail);
			holder.fileSizeView.setTextColor(Color.RED);
			break;

		default:
			break;
		}
		
		return view;
	}
	
	class ViewHolder{
		ProgressBar titleBar;
		ProgressBar transferBar;
		TextView dateView;
		TextView userNameView;
		TextView fileNameView;
		TextView fileSizeView;
		TextView receiveUserNameView;
		ImageView iconView;
		//msg layout
		LinearLayout msgLayout;
	}
	
	class MsgOnClickListener implements OnClickListener{
		int position = -1;
		MsgOnClickListener(int position){
			this.position = position;	
		}
		
		@Override
		public void onClick(View v) {
			final HistoryInfo historyInfo = list.get(position);
			new AlertDialog.Builder(mContext)
				.setTitle(historyInfo.getFileInfo().getFileName())
				.setItems(R.array.history_menu, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								//send
								FileTransferInfo fileTransferInfo = new FileTransferInfo(new File(historyInfo.getFileInfo().getFilePath()));

								FileSendUtil fileSendUtil = new FileSendUtil(mContext);
								fileSendUtil.sendFile(fileTransferInfo);
								break;
							case 1:
								//open
								FileInfoManager fileInfoManager = new FileInfoManager(mContext);
								fileInfoManager.openFile(historyInfo.getFileInfo().getFilePath());
								break;
							case 2:
								//delete
								
								break;
							}
					}
				})
				.create().show();
		}
		
	}

}
