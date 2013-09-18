package com.dreamlink.communication.ui.history;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.AsyncImageLoader;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.AsyncImageLoader.ILoadImageCallback;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.FileBrowserFragment;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.util.Log;

import android.R.integer;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HistoryCursorAdapter extends CursorAdapter {
	private static final String TAG = "HistoryMsgAdapter";
	private LayoutInflater inflater = null;
	private List<HistoryInfo> list = new ArrayList<HistoryInfo>();
	private int status = -1;
	private Notice mNotice = null;
	private Context mContext;
	private AsyncImageLoader bitmapLoader = null;
	private boolean scrollFlag = true;
	
	public HistoryCursorAdapter(Context context, List<HistoryInfo> list){
		super(context, null, true);
		
		this.mContext = context;
		mNotice  = new Notice(context);
		inflater = LayoutInflater.from(context);
		this.list = list;
		
		bitmapLoader = new AsyncImageLoader(context);
	}
	
	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return super.getItem(position);
	}
	
	@Override
	public int getItemViewType(int position) {
		Cursor cursor = (Cursor) getItem(position);
		int type = cursor.getInt(cursor.getColumnIndex(MetaData.History.MSG_TYPE));
		if (HistoryManager.TYPE_RECEIVE == type) {
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
	
	private Cursor mCursor = null;
	@Override
	public void bindView(View view, Context arg1, Cursor cursor) {
//		mCursor = cursor;
		Log.d(TAG, "bindView.count=" + cursor.getCount());
		// TODO Auto-generated method stub
		ViewHolder holder = (ViewHolder) view.getTag();
		holder.position = cursor.getPosition();
		
		int id = cursor.getInt(cursor.getColumnIndex(MetaData.History._ID));
		long time = cursor.getLong(cursor.getColumnIndex(MetaData.History.DATE));
		String filePath = cursor.getString(cursor.getColumnIndex(MetaData.History.FILE_PATH));
		String fileName = cursor.getString(cursor.getColumnIndex(MetaData.History.FILE_NAME));
		String sendUserName = cursor.getString(cursor.getColumnIndex(MetaData.History.SEND_USERNAME));
		String reveiveUserName = cursor.getString(cursor.getColumnIndex(MetaData.History.RECEIVE_USERNAME));
		long fileSize = cursor.getLong(cursor.getColumnIndex(MetaData.History.FILE_SIZE));
		double progress = cursor.getDouble(cursor.getColumnIndex(MetaData.History.PROGRESS));
		int status = cursor.getInt(cursor.getColumnIndex(MetaData.History.STATUS));
		int fileType = cursor.getInt(cursor.getColumnIndex(MetaData.History.FILE_TYPE));
		
		holder.dateView.setText(DreamUtil.getFormatDate(time));
		holder.userNameView.setText(sendUserName);
		holder.fileNameView.setText(fileName);
		holder.fileSizeView.setTextColor(Color.BLACK);
		holder.receiveUserNameView.setTextColor(Color.BLACK);
		
		/*****use async thread loader bitmap start*********/
		//just load image,video,apk file icon;others use default icon
		Log.d(TAG, "scroll flag=" + scrollFlag);
		if (!scrollFlag) {
			if (AsyncImageLoader.bitmapCache.size() > 0
					&& AsyncImageLoader.bitmapCache.get(filePath) != null) {
				holder.iconView.setImageBitmap(AsyncImageLoader.bitmapCache.get(filePath).get());
			} else {
				holder.iconView.setImageResource(R.drawable.default_document_icon);
			}
		}else {
			if (FileInfoManager.TYPE_IMAGE == fileType) {
				Bitmap bitmap = bitmapLoader.loadImage(filePath, fileType,
						holder.iconView, new ILoadImageCallback() {
							@Override
							public void onObtainBitmap(Bitmap bitmap, ImageView imageView) {
								imageView.setImageBitmap(bitmap);
							}
						});
				if (null != bitmap) {
					holder.iconView.setImageBitmap(bitmap);
				} else {
					Log.d(TAG, "image bitamp is null");
					holder.iconView.setImageResource(R.drawable.default_document_icon);
				}
			} else if (FileInfoManager.TYPE_APK == fileType) {
				Bitmap cacheDrawable = bitmapLoader.loadImage(filePath, fileType,
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
					Log.d(TAG, "apk bitamp is null");
					holder.iconView.setImageResource(R.drawable.default_document_icon);
				}
			} else if (FileInfoManager.TYPE_VIDEO == fileType) {
				Bitmap videoBitmap = bitmapLoader.loadImage(filePath, fileType,
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
					Log.d(TAG, "video bitamp is null");
					holder.iconView.setImageResource(R.drawable.default_document_icon);
				}
			} else {
				holder.iconView.setImageResource(R.drawable.default_document_icon);
			}
		}
		/*****use async thread loader bitmap end*********/
		Log.d(TAG, "status=" + status);
		switch (status) {
		case HistoryManager.STATUS_PRE_SEND:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.pre_send, 
					reveiveUserName));
			holder.transferBar.setVisibility(View.GONE);
			holder.titleBar.setVisibility(View.VISIBLE);
			holder.fileSizeView.setText(DreamUtil.getFormatSize(fileSize));
			break;
			
		case HistoryManager.STATUS_SENDING:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.sending, 
					reveiveUserName));
		case HistoryManager.STATUS_RECEIVING:
			holder.transferBar.setVisibility(View.VISIBLE);
			holder.titleBar.setVisibility(View.GONE);
			double percent = progress / fileSize;
			Log.d(TAG, "percent=" + HistoryManager.nf.format(percent));
			holder.transferBar.setProgress((int)(percent * 100));
			holder.fileSizeView.setText(HistoryManager.nf.format(percent) + " | " 
					+ DreamUtil.getFormatSize(fileSize));
			break;
		case HistoryManager.STATUS_SEND_SUCCESS:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.send_ok, 
					reveiveUserName));
		case HistoryManager.STATUS_RECEIVE_SUCCESS:
			holder.transferBar.setVisibility(View.GONE);
			holder.titleBar.setVisibility(View.GONE);
			holder.fileSizeView.setText(DreamUtil.getFormatSize(fileSize));
			break;
		case HistoryManager.STATUS_SEND_FAIL:
			holder.receiveUserNameView.setText(mContext.getResources().getString(R.string.send_fail, 
					reveiveUserName));
			holder.receiveUserNameView.setTextColor(Color.RED);
			holder.transferBar.setVisibility(View.VISIBLE);
			double percent2 = progress / fileSize;
			Log.d(TAG, "percent=" + HistoryManager.nf.format(percent2));
			holder.transferBar.setProgress((int)(percent2 * 100));
			holder.fileSizeView.setText(HistoryManager.nf.format(percent2) + " | " + DreamUtil.getFormatSize(fileSize));
		case HistoryManager.STATUS_RECEIVE_FAIL:
			holder.titleBar.setVisibility(View.GONE);
			holder.fileSizeView.setText(R.string.receive_fail);
			holder.fileSizeView.setTextColor(Color.RED);
			break;

		default:
			break;
		}
	}
	
	@Override
	public View newView(Context arg0, Cursor cursor, ViewGroup arg2) {
		// TODO Auto-generated method stub
		int type = cursor.getInt(cursor.getColumnIndex(MetaData.History.MSG_TYPE));
		View view  = null;
		if (HistoryManager.TYPE_RECEIVE == type) {
			view = inflater.inflate(R.layout.ui_history_item_left, null);
		}else {
			view = inflater.inflate(R.layout.ui_history_item_right, null);
		}
		
		ViewHolder holder = new ViewHolder();
		
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
		holder.msgLayout.setTag(cursor.getPosition());
		holder.msgLayout.setOnClickListener(mClickListener);
		view.setTag(holder);
		
		return view;
	}
	MsgOnClickListener mClickListener = new MsgOnClickListener();
	
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
		int position;
	}
	
	class MsgOnClickListener implements OnClickListener{
		
		@Override
		public void onClick(View v) {
			final Cursor cursor = getCursor();
			cursor.moveToPosition((Integer) v.getTag());
			final String filePath = cursor.getString(cursor.getColumnIndex(MetaData.History.FILE_PATH));
			String fileName = cursor.getString(cursor.getColumnIndex(MetaData.History.FILE_NAME));
			new AlertDialog.Builder(mContext)
				.setTitle(fileName)
				.setItems(R.array.history_menu, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								//send
								FileTransferUtil fileSendUtil = new FileTransferUtil(mContext);
								fileSendUtil.sendFile(filePath);
								break;
							case 1:
								//open
								FileInfoManager fileInfoManager = new FileInfoManager(mContext);
								fileInfoManager.openFile(filePath);
								break;
							case 2:
								//delete
								mNotice.showToast("尚不可用");
								break;
							}
					}
				})
				.create().show();
		}
		
	}

}
