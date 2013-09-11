package com.dreamlink.communication.ui.history;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.FileReceiver;
import com.dreamlink.communication.FileReceiver.OnReceiveListener;
import com.dreamlink.communication.FileSender.OnFileSendListener;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.SocketCommunicationManager.OnFileTransportListener;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.BaseFragmentActivity;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.FileInfo;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.util.Log;

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class HistoryActivityTest extends BaseFragmentActivity implements OnFileSendListener,OnFileTransportListener,
					OnReceiveListener, OnScrollListener, OnItemClickListener {
	private static final String TAG = "HistoryActivityTest";
	private int mAppId = -1;
	private Context mContext;
	
	//view
	private TextView mStorageTV;
	private ListView mHistoryMsgLV;
	
	//adapter
	private HistoryCursorAdapter mAdapter;
	
	/**the list that send or receive history info*/
	private List<HistoryInfo> mHistoryList = new ArrayList<HistoryInfo>();
	// 用来保存ListView中每个Item的图片，以便释放
	public static Map<String, Bitmap> bitmapCaches = new HashMap<String, Bitmap>();
	
	private Notice mNotice;
	private SocketCommunicationManager communicationManager;
	
	private FileInfoManager mFileInfoManager = null;
	private UserManager mUserManager = null;
	
	//msg
	private static final int MSG_SEND_FILE = 0;
	private static final int MSG_UPDATE_UI = 1;
	private static final int MSG_UPDATE_SEND_PROGRESS = 2;
	private static final int MSG_UPDATE_SEND_STATUS = 3;
	private static final int MSG_UPDATE_RECEIVE_STATUS = 4;
	private static final int MSG_UPDATE_RECEIVE_PROGRESS = 5;
	private static final int MSG_SEND_FINISHED = 6;
	private static final int MSG_RECEIVE_FINISHED = 7;
	
	private QueryHandler queryHandler = null;
	private HistoryManager mHistoryManager = null;
	
	public BroadcastReceiver historyReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.i(TAG, "action = " + action);
			Bundle bundle;
			if (DreamConstant.SEND_FILE_ACTION.equals(action)) {
				bundle = intent.getExtras();
				if (null != bundle) {
//					FileInfo fileInfo = bundle.getParcelable(Extra.SEND_FILE);
					FileTransferInfo fileInfo = bundle.getParcelable(Extra.SEND_FILE);
					List<User> userList = bundle.getParcelableArrayList(Extra.SEND_USER);
					sendFile(fileInfo, userList);
				}
			}
		}
	};
	
	 Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEND_FILE:
				break;
			case MSG_UPDATE_UI:
				break;
				
			case MSG_UPDATE_SEND_PROGRESS:
				Log.d(TAG, "MSG_UPDATE_SEND_PROGRESS");
				HistoryInfo historyInfo2 = (HistoryInfo) msg.obj;
				Collections.sort(mHistoryList, HistoryManager.DATE_COMPARATOR);
				int position2 = mHistoryList.indexOf(historyInfo2);
				Log.i(TAG, "position2=" + position2);
				mHistoryMsgLV.setSelection(position2);
				mAdapter.notifyDataSetChanged();
				break;
				
			case MSG_UPDATE_RECEIVE_PROGRESS:
				HistoryInfo historyInfo3 = (HistoryInfo) msg.obj;
				Collections.sort(mHistoryList, HistoryManager.DATE_COMPARATOR);
				int positior3 = mHistoryList.indexOf(historyInfo3);
				Log.i(TAG, "positior3=" + positior3);
				mHistoryMsgLV.setSelection(positior3);
				mAdapter.notifyDataSetChanged();
				break;
				
			case MSG_UPDATE_SEND_STATUS:
			case MSG_UPDATE_RECEIVE_STATUS:
				int status = msg.arg1;
				mAdapter.setStatus(status);
				mAdapter.notifyDataSetChanged();
				
				//tell mainui update ui
				break;
			case MSG_SEND_FINISHED:
				boolean success = (Boolean) msg.obj;
				break;
			case MSG_RECEIVE_FINISHED:
				break;

			default:
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_history);
		mContext = this;
		mAppId = AppUtil.getAppID(getParent());
		mNotice = new Notice(mContext);
		communicationManager = SocketCommunicationManager.getInstance(mContext);
		communicationManager.registerOnFileTransportListener(this, mAppId);
		
		mFileInfoManager = new FileInfoManager(mContext);
		
		queryHandler = new QueryHandler(getContentResolver());
		mHistoryManager = new HistoryManager(mContext);
		mUserManager = UserManager.getInstance();
		
		//register broadcast
		IntentFilter filter = new IntentFilter(DreamConstant.SEND_FILE_ACTION);
		registerReceiver(historyReceiver, filter);
		
//		HistoryContent historyContent = new HistoryContent(new Handler());
//		getContentResolver().registerContentObserver(MetaData.History.CONTENT_URI, true, historyContent);
		
		initView();
	}
	
	private static final String[] PROJECTION = {
		MetaData.History._ID,MetaData.History.FILE_PATH,MetaData.History.FILE_NAME,
		MetaData.History.FILE_SIZE,MetaData.History.SEND_USERNAME,MetaData.History.RECEIVE_USERNAME,
		MetaData.History.PROGRESS,MetaData.History.DATE,
		MetaData.History.STATUS,MetaData.History.MSG_TYPE,MetaData.History.FILE_TYPE
	};
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		query();
	}
	
	public void query(){
		queryHandler.startQuery(11, null, MetaData.History.CONTENT_URI, PROJECTION, null, null, MetaData.History.SORT_ORDER_DEFAULT);
	}
	
	private void initView(){
		mStorageTV = (TextView) findViewById(R.id.tv_storage);
		String space = getResources().getString(R.string.storage_space, 
				DreamUtil.getFormatSize(Environment.getExternalStorageDirectory().getTotalSpace()),
				DreamUtil.getFormatSize(Environment.getExternalStorageDirectory().getFreeSpace()));
		mStorageTV.setText(space);
		mHistoryMsgLV = (ListView) findViewById(R.id.lv_history_msg);
		mHistoryMsgLV.setEmptyView(findViewById(R.id.tv_empty));
		mHistoryMsgLV.setOnScrollListener(this);
		mHistoryMsgLV.setOnItemClickListener(this);
		
//		mAdapter.setStatus(HistoryManager.STATUS_DEFAULT);
		mHistoryMsgLV.setAdapter(mAdapter);
		mHistoryMsgLV.setSelection(0);
	}
	
	public void setAdapter(){
		Log.d(TAG, "setAdapter()");
		if (null == mAdapter) {
			Log.d(TAG, "setAdapter() null");
			mAdapter = new HistoryCursorAdapter(mContext, mHistoryList);
			mHistoryMsgLV.setAdapter(mAdapter);
		}else {
			Log.d(TAG, "setAdapter() changeCursor.count=" + mCursor.getCount());
			mAdapter.changeCursor(mCursor);
		}
	}
	
	private class HistoryContent extends ContentObserver{

		public HistoryContent(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			// TODO Auto-generated method stub
			super.onChange(selfChange);
			Log.d(TAG, "onChange");
			mCursor.requery();
		}
		
		
	}
	
	private Cursor mCursor = null;
	//query db
	public class QueryHandler extends AsyncQueryHandler{

		public QueryHandler(ContentResolver cr) {
			super(cr);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			// TODO Auto-generated method stub
//			super.onQueryComplete(token, cookie, cursor);
			Log.d(TAG, "onQueryComplete");
			if (null  != cursor) {
				mCursor = cursor;
				Log.d(TAG, "onQueryComplete.count=" + cursor.getCount());
//				mAdapter.changeCursor(cursor);
				setAdapter();
			}
		}
		
		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			// TODO Auto-generated method stub
			super.onInsertComplete(token, cookie, uri);
			HistoryInfo historyInfo = (HistoryInfo) cookie;
			Log.d(TAG, "onInsertComplete.filename= " + historyInfo.getFileInfo().getFileName());
			historyInfo.setUri(uri);
			query();
		}
		
		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			// TODO Auto-generated method stub
			super.onUpdateComplete(token, cookie, result);
			Log.d(TAG, "onUpdateComplete.result=" + result);
			query();
		}
		
	}
	
	public void sendFile(FileTransferInfo fileInfo, List<User> list){
		Log.d(TAG, "sendFile.name:" + fileInfo.getFileName());
		for (int i = 0; i < list.size(); i++) {
			User receiverUser = list.get(i);
			Log.d(TAG, "receiverUser[" + i + "]=" + receiverUser.getUserName());
			SendFileThread sendFileThread = new SendFileThread(fileInfo, receiverUser);
			sendFileThread.start();
		}
	}
	
    class SendFileThread extends Thread {
    	FileTransferInfo fileInfo = null;
    	User receiveUser = null;
    	SendFileThread(FileTransferInfo fileInfo, User receiveUser){
    		this.fileInfo = fileInfo;
    		this.receiveUser = receiveUser;
    	}
    	
		@Override
		public void run() {
			HistoryInfo historyInfo = new HistoryInfo();
			historyInfo.setFileInfo(fileInfo);
			historyInfo.setReceiveUser(receiveUser);
			historyInfo.setSendUserName(getResources().getString(R.string.me));
			historyInfo.setMsgType(HistoryManager.TYPE_SEND);
			historyInfo.setDate(System.currentTimeMillis());
			historyInfo.setStatus(HistoryManager.STATUS_PRE_SEND);
			historyInfo.setProgress(0);
			historyInfo = mFileInfoManager.getHistoryInfo(historyInfo);
			
			mHistoryList.add(historyInfo);
			ContentValues values = mHistoryManager.getInsertValues(historyInfo);
			queryHandler.startInsert(11, historyInfo, MetaData.History.CONTENT_URI, values);
			
//			Message message = mHandler.obtainMessage();
//			message.what = MSG_UPDATE_SEND_STATUS;
//			mHandler.sendMessage(message);
			
			communicationManager.sendFile(historyInfo, HistoryActivityTest.this, mAppId);
			
		}
	}

	@Override
	public void onSendProgress(HistoryInfo historyInfo) {
		Log.d(TAG, "onSendProgress=" + historyInfo.getProgress() +  ",uri=" + historyInfo.getUri());
		ContentValues values = new ContentValues();
		values.put(MetaData.History.STATUS, HistoryManager.STATUS_SENDING);
		values.put(MetaData.History.PROGRESS, historyInfo.getProgress());
		queryHandler.startUpdate(11, null, historyInfo.getUri(), values, null, null);
		
//		Message message = mHandler.obtainMessage();
//		message.what = MSG_UPDATE_SEND_PROGRESS;
//		message.obj = historyInfo;
//		mHandler.sendMessage(message);
	}

	@Override
	public void onSendFinished(HistoryInfo historyInfo, boolean success) {
		Log.d(TAG, "onSendFinished.success" + success + ",uri=" + historyInfo.getUri());
		ContentValues values = new ContentValues();
		values.put(MetaData.History.STATUS, HistoryManager.STATUS_SENDING);
		queryHandler.startUpdate(11, null, historyInfo.getUri(), values, null, null);
//		Message message = mHandler.obtainMessage();
//		message.what = MSG_SEND_FINISHED;
//		message.obj = success;
//		mHandler.sendMessage(message);
	}

	@Override
	public void onReceiveFile(FileReceiver fileReceiver) {
		String fileName = fileReceiver.getFileTransferInfo().getFileName();
		String sendUserName = fileReceiver.getSendUser().getUserName();
		Log.d(TAG, "onReceiveFile:" + fileName + "," + sendUserName);
		//define a file to save the receive file
		File fileDir = new File(DreamConstant.DEFAULT_SAVE_FOLDER);
		if (!fileDir.exists()) {
			fileDir.mkdirs();
		}
		
		String filePath = DreamConstant.DEFAULT_SAVE_FOLDER + File.separator +fileName;
		File file = new File(filePath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				Log.e(TAG, "create file error:" + e.toString());
				mNotice.showToast("can not create the file:" + fileName);
				return;
			}
		}else {
			//if file is exist,auto rename
			fileName  = FileInfoManager.autoRename(fileName);
			while(new File(DreamConstant.DEFAULT_SAVE_FOLDER + File.separator +fileName).exists()) {
				fileName = FileInfoManager.autoRename(fileName);
			}
			filePath = DreamConstant.DEFAULT_SAVE_FOLDER + File.separator +fileName;
		}
		
		HistoryInfo historyInfo = new HistoryInfo();
		FileTransferInfo fileInfo = new FileTransferInfo();
		fileInfo.setFilePath(filePath);
		fileInfo.setFileName(fileName);
		fileInfo.setFileSize(fileReceiver.getFileTransferInfo().getFileSize());
		historyInfo.setFileInfo(fileInfo);
		historyInfo.setSendUserName(sendUserName);
		historyInfo.setReceiveUser(mUserManager.getLocalUser());
		historyInfo.setMsgType(HistoryManager.TYPE_RECEIVE);
		historyInfo.setDate(System.currentTimeMillis());
		historyInfo.setStatus(HistoryManager.STATUS_PRE_RECEIVE);
		historyInfo = mFileInfoManager.getHistoryInfo(historyInfo);
		mHistoryList.add(historyInfo);
		
		ContentValues values = mHistoryManager.getInsertValues(historyInfo);
		queryHandler.startInsert(11, historyInfo, MetaData.History.CONTENT_URI, values);
		
		fileReceiver.receiveFile(historyInfo, this)	;
		
//		Message message = mHandler.obtainMessage();
//		message.what = MSG_UPDATE_RECEIVE_STATUS;
//		mHandler.sendMessage(message);
	}

	@Override
	public void onReceiveProgress(HistoryInfo historyInfo) {
		Log.d(TAG, "onReceiveProgress.progress" + historyInfo.getUri());
		ContentValues values = new ContentValues();
		values.put(MetaData.History.STATUS, HistoryManager.STATUS_RECEIVING);
		values.put(MetaData.History.PROGRESS, historyInfo.getProgress());
		queryHandler.startUpdate(11, null, historyInfo.getUri(), values, null, null);
//		Message message = mHandler.obtainMessage();
//		message.what = MSG_UPDATE_RECEIVE_PROGRESS;
//		message.obj = historyInfo;
//		mHandler.sendMessage(message);
	}

	@Override
	public void onReceiveFinished(HistoryInfo historyInfo, boolean success) {
		Log.d(TAG, "onReceiveFinished.success=" + historyInfo.getUri());
		ContentValues values = new ContentValues();
		values.put(MetaData.History.STATUS, HistoryManager.STATUS_SENDING);
		queryHandler.startUpdate(11, null, historyInfo.getUri(), values, null, null);
//		Message message = mHandler.obtainMessage();
//		message.what = MSG_RECEIVE_FINISHED;
//		message.obj = success;
//		mHandler.sendMessage(message);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_FLING:
			mAdapter.setFlag(false);
			break;
		case OnScrollListener.SCROLL_STATE_IDLE:
			mAdapter.setFlag(true);
			break;
		case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
			mAdapter.setFlag(false);
			break;

		default:
			break;
		}
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// 注释：firstVisibleItem为第一个可见的Item的position，从0开始，随着拖动会改变
		// visibleItemCount为当前页面总共可见的Item的项数
		// totalItemCount为当前总共已经出现的Item的项数
//		recycleBitmapCaches(0, firstVisibleItem);
//		recycleBitmapCaches(firstVisibleItem + visibleItemCount, totalItemCount);
	}

	// release bitmap
	private void recycleBitmapCaches(int fromPosition, int toPosition) {
		Bitmap delBitmap = null;
		for (int del = fromPosition; del < toPosition; del++) {
			delBitmap = bitmapCaches.get(mHistoryList.get(del));
			if (delBitmap != null) {
				// 如果非空则表示有缓存的bitmap，需要清理
				Log.d(TAG, "release position:" + del);
				// 从缓存中移除该del->bitmap的映射
				bitmapCaches.remove(mHistoryList.get(del));
				delBitmap.recycle();
				delBitmap = null;
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		mNotice.showToast(position+"");
	}
}
