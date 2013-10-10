package com.dreamlink.communication.ui.history;

import java.io.File;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.AsyncImageLoader2;
import com.dreamlink.communication.ui.AsyncImageLoader2.ILoadImageCallback;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.util.Log;

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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HistoryCursorAdapter extends CursorAdapter {
	private static final String TAG = "HistoryCursorAdapter";
	private LayoutInflater mLayoutInflater = null;
	private int mStatus = -1;
	private Notice mNotice = null;
	private Context mContext;
	private AsyncImageLoader2 bitmapLoader2 = null;
	private boolean mIdleFlag = true;
	private MsgOnClickListener mClickListener = new MsgOnClickListener();
	private DeleteOnClick mDeleteOnClick = new DeleteOnClick(0);
	private ListView mListView; 
	
	public HistoryCursorAdapter(Context context, ListView listView) {
		super(context, null, true);
		this.mContext = context;
		mListView = listView;
		mNotice = new Notice(context);
		mLayoutInflater = LayoutInflater.from(context);
		bitmapLoader2 = new AsyncImageLoader2(context);
	}

	@Override
	public Object getItem(int position) {
		return super.getItem(position);
	}

	@Override
	public int getItemViewType(int position) {
		Cursor cursor = (Cursor) getItem(position);
		int type = cursor.getInt(cursor
				.getColumnIndex(MetaData.History.MSG_TYPE));
		if (HistoryManager.TYPE_RECEIVE == type) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public int getViewTypeCount() {
		// 如果你的list中有不同的视图类型，就一定要重写这个方法，并配合getItemViewType一起使用
		return 2;
	}

	public void setStatus(int status) {
		this.mStatus = status;
	}

	public void setIdleFlag(boolean flag) {
		this.mIdleFlag = flag;
	}

	@Override
	public void bindView(View view, Context arg1, Cursor cursor) {
		Log.d(TAG, "bindView.count=" + cursor.getCount());
		ViewHolder holder = (ViewHolder) view.getTag();
		holder.position = cursor.getPosition();

		int id = cursor.getInt(cursor.getColumnIndex(MetaData.History._ID));
		int type = cursor.getInt(cursor
				.getColumnIndex(MetaData.History.MSG_TYPE));
		long time = cursor
				.getLong(cursor.getColumnIndex(MetaData.History.DATE));
		String filePath = cursor.getString(cursor
				.getColumnIndex(MetaData.History.FILE_PATH));
		String fileName = cursor.getString(cursor
				.getColumnIndex(MetaData.History.FILE_NAME));
		String sendUserName = cursor.getString(cursor
				.getColumnIndex(MetaData.History.SEND_USERNAME));
		String reveiveUserName = cursor.getString(cursor
				.getColumnIndex(MetaData.History.RECEIVE_USERNAME));
		long fileSize = cursor.getLong(cursor
				.getColumnIndex(MetaData.History.FILE_SIZE));
		double progress = cursor.getDouble(cursor
				.getColumnIndex(MetaData.History.PROGRESS));
		int status = cursor.getInt(cursor
				.getColumnIndex(MetaData.History.STATUS));
		int fileType = cursor.getInt(cursor
				.getColumnIndex(MetaData.History.FILE_TYPE));

		holder.iconView.setTag(filePath);
		holder.dateView.setText(DreamUtil.getFormatDate(time));
		holder.userNameView.setText(sendUserName);
		holder.fileNameView.setText(fileName);
		holder.fileSizeView.setTextColor(Color.BLACK);
		holder.receiveUserNameView.setTextColor(Color.BLACK);
		holder.msgLayout.setTag(new MsgData(id, fileName, filePath, type));

		setIconView(holder, holder.iconView, filePath, fileType);
		setSendReceiveStatus(holder, status, reveiveUserName, fileSize,
				progress);
	}

	private void setSendReceiveStatus(ViewHolder holder, int status,
			String reveiveUserName, long fileSize, double progress) {
		Log.d(TAG, "status=" + status);
		switch (status) {
		case HistoryManager.STATUS_PRE_SEND:
			holder.receiveUserNameView.setText(mContext.getResources()
					.getString(R.string.pre_send, reveiveUserName));
			holder.transferBar.setVisibility(View.GONE);
			holder.titleBar.setVisibility(View.VISIBLE);
			holder.fileSizeView.setText(DreamUtil.getFormatSize(fileSize));
			break;

		case HistoryManager.STATUS_SENDING:
			holder.receiveUserNameView.setText(mContext.getResources()
					.getString(R.string.sending, reveiveUserName));
		case HistoryManager.STATUS_RECEIVING:
			holder.transferBar.setVisibility(View.VISIBLE);
			holder.titleBar.setVisibility(View.GONE);
			double percent = progress / fileSize;
			Log.d(TAG, "percent=" + HistoryManager.nf.format(percent));
			holder.transferBar.setProgress((int) (percent * 100));
			holder.fileSizeView.setText(HistoryManager.nf.format(percent)
					+ " | " + DreamUtil.getFormatSize(fileSize));
			break;
		case HistoryManager.STATUS_SEND_SUCCESS:
			holder.receiveUserNameView.setText(mContext.getResources()
					.getString(R.string.send_ok, reveiveUserName));
		case HistoryManager.STATUS_RECEIVE_SUCCESS:
			holder.transferBar.setVisibility(View.GONE);
			holder.titleBar.setVisibility(View.GONE);
			holder.fileSizeView.setText(DreamUtil.getFormatSize(fileSize));
			break;
		case HistoryManager.STATUS_SEND_FAIL:
			holder.receiveUserNameView.setText(mContext.getResources()
					.getString(R.string.send_fail, reveiveUserName));
			holder.receiveUserNameView.setTextColor(Color.RED);
			holder.transferBar.setVisibility(View.VISIBLE);
			double percent2 = progress / fileSize;
			Log.d(TAG, "percent=" + HistoryManager.nf.format(percent2));
			holder.transferBar.setProgress((int) (percent2 * 100));
			holder.fileSizeView.setText(HistoryManager.nf.format(percent2)
					+ " | " + DreamUtil.getFormatSize(fileSize));
		case HistoryManager.STATUS_RECEIVE_FAIL:
			holder.titleBar.setVisibility(View.GONE);
			holder.fileSizeView.setText(R.string.receive_fail);
			holder.fileSizeView.setTextColor(Color.RED);
			break;

		default:
			break;
		}
	}

	/**
	 * use async thread loader bitmap.
	 * 
	 * @param iconView
	 * @param filePath
	 * @param fileType
	 */
	private void setIconView(ViewHolder holder, final ImageView iconView, final String filePath, int fileType) {
		Log.d(TAG, "scroll flag=" + mIdleFlag);
		if (!mIdleFlag) {
			if (AsyncImageLoader2.bitmapCache.size() > 0
					&& AsyncImageLoader2.bitmapCache.get(filePath) != null) {
				iconView.setImageBitmap(AsyncImageLoader2.bitmapCache.get(
						filePath).get());
			} else {
				iconView.setImageResource(R.drawable.default_document_icon);
			}
			return;
		} else {
			// just load image,video,apk file icon;others use default icon
			if (FileInfoManager.TYPE_IMAGE == fileType
					|| FileInfoManager.TYPE_APK == fileType
					|| FileInfoManager.TYPE_VIDEO == fileType) {
				Log.i(TAG, filePath + ":444");
				Bitmap bitmap = bitmapLoader2.loadImage(filePath, fileType, new ILoadImageCallback() {
					
					@Override
					public void onObtainBitmap(Bitmap bitmap, String url) {
						// TODO Auto-generated method stub
						ImageView imageView = (ImageView) mListView.findViewWithTag(filePath);
						if (null != bitmap) {
							imageView.setImageBitmap(bitmap);
						}
					}
				});
				
				if (null == bitmap) {
					iconView.setImageResource(R.drawable.default_document_icon);
				} else {
					iconView.setImageBitmap(bitmap);
				}
			} else {
				iconView.setImageResource(R.drawable.default_document_icon);
			}
		}
	}

	@Override
	public View newView(Context arg0, Cursor cursor, ViewGroup arg2) {
		int type = cursor.getInt(cursor
				.getColumnIndex(MetaData.History.MSG_TYPE));
		View view = null;
		if (HistoryManager.TYPE_RECEIVE == type) {
			view = mLayoutInflater.inflate(R.layout.ui_history_item_left, null);
		} else {
			view = mLayoutInflater.inflate(R.layout.ui_history_item_right, null);
		}

		ViewHolder holder = new ViewHolder();

		holder.receiveUserNameView = (TextView) view
				.findViewById(R.id.tv_send_title_msg);
		holder.titleBar = (ProgressBar) view.findViewById(R.id.bar_send_title);
		holder.transferBar = (ProgressBar) view
				.findViewById(R.id.bar_progressing);
		holder.transferBar.setMax(100);
		holder.iconView = (ImageView) view.findViewById(R.id.iv_send_file_icon);
		holder.dateView = (TextView) view.findViewById(R.id.tv_sendtime);
		holder.userNameView = (TextView) view.findViewById(R.id.tv_username);
		holder.fileNameView = (TextView) view
				.findViewById(R.id.tv_send_file_name);
		holder.fileSizeView = (TextView) view
				.findViewById(R.id.tv_send_file_size);
		holder.msgLayout = (LinearLayout) view
				.findViewById(R.id.layout_chatcontent);
		holder.msgLayout.setOnClickListener(mClickListener);
		view.setTag(holder);

		return view;
	}

	class ViewHolder {
		ProgressBar titleBar;
		ProgressBar transferBar;
		TextView dateView;
		TextView userNameView;
		TextView fileNameView;
		TextView fileSizeView;
		TextView receiveUserNameView;
		ImageView iconView;
		// msg layout
		LinearLayout msgLayout;
		int position;
	}

	class MsgData {
		int itemID;
		String fileName;
		String filePath;
		int type;

		public MsgData(int itemID, String fileName, String filePath, int type) {
			this.itemID = itemID;
			this.fileName = fileName;
			this.filePath = filePath;
			this.type = type;
		}
	}

	class MsgOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			MsgData data = (MsgData) v.getTag();
			Log.d(TAG, "onClick id = " + data.itemID + ", name = "
					+ data.fileName);
			final int id = data.itemID;
			final String filePath = data.filePath;
			String fileName = data.fileName;
			final int type = data.type;
			

			new AlertDialog.Builder(mContext)
					.setTitle(fileName)
					.setItems(R.array.history_menu,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									switch (which) {
									case 0:
										// send
										File file = new File(filePath);
										if (!file.exists()) {
											Log.d(TAG,
													"Send file fail. File is not exits. "
															+ filePath);
											mNotice.showToast("文件不存在");
											break;
										} else {
											FileTransferUtil fileSendUtil = new FileTransferUtil(
													mContext);
											fileSendUtil.sendFile(filePath);
										}
										
										break;
									case 1:
										// open
										FileInfoManager fileInfoManager = new FileInfoManager(
												mContext);
										fileInfoManager.openFile(filePath);
										break;
									case 2:
										// delete
//										delete(new File(filePath), id);
										showDeleteDialog(new File(filePath), id, type);
										break;
									}
								}
							}).create().show();
		}

	}

	/**
	 * Delete the transfer record in DB.
	 * @param id the transfer record id id db
	 */
	private void deleteHistory(int id) {
		// Do not delete file current.
		String selection = MetaData.History._ID + "=" + id;
		int result = mContext.getContentResolver().delete(
				MetaData.History.CONTENT_URI, selection, null);
		if (result > 0) {
			mNotice.showToast("已刪除记录");
		} else {
			mNotice.showToast("刪除记录失败");
		}
	}
	
	/**
	 * Delete the tranfser record in DB and delelte the file
	 * @param file the file that need to delete
	 * @param id the transfer record id id db
	 */
	private void deleteFileAndHistory(File file, int id){
		deleteHistory(id);
		
		boolean ret = false;
		if (file.exists()) {
			ret = file.delete();
			if (!ret) {
				mNotice.showToast("删除文件失败：" + file.getAbsolutePath());
			}
		}
		
	}
	
	/**
	 * show delete transfer record dialog</br>
	 * if the record is send to others,user only can delete record</br>
	 * if the record is receive from others,user can delete record and delete file in system
	 * @param file
	 * @param id
	 * @param type send or receive
	 */
	public void showDeleteDialog(File file, int id, int type){
		int resId = -1;
		if (HistoryManager.TYPE_SEND == type) {
			resId = R.array.send_history_delete_menu;
		}else {
			resId = R.array.receive_history_delete_menu;
		}
		int defaultSelectItem = 0;
		mDeleteOnClick.setId(id);
		mDeleteOnClick.setFile(file);
		new AlertDialog.Builder(mContext)
        .setTitle(R.string.delete_history_msg)
        .setSingleChoiceItems(resId, defaultSelectItem, mDeleteOnClick)
        .setPositiveButton(R.string.ok, mDeleteOnClick)
        .setNegativeButton(R.string.cancel, null)
        .create().show();
	}
	
	private class DeleteOnClick implements DialogInterface.OnClickListener{

		private int index;
		private int id;
		private File file;
		
		public DeleteOnClick(int index){
			this.index = index;
		}
		
		public void setId(int id){
			this.id = id;
		}
		
		public void setFile(File file){
			this.file = file;
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			//which表示单击的按钮索引，所有的列表选项的索引都是大于0的，按钮的索引都是小于0的
			if (which >= 0) {
				//如果单击的是列表项，保存索引
				index = which;
			}else {
				//单击的是按钮，这里只可能是确定按钮
				switch (index) {
				case 0:
					deleteHistory(id);
					break;
				case 1:
					deleteFileAndHistory(file, id);
					//init the index to 0,because default select is 0
					index = 0;
					break;
				}
			}
		}
		
	}
}
