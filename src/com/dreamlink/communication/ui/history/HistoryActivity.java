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
import com.dreamlink.communication.SocketCommunicationManager.OnFileTransportListener;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.BaseFragmentActivity;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.file.FileInfo;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.util.Log;

import android.R.integer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HistoryActivity extends FragmentActivity implements OnFileSendListener,OnFileTransportListener,
					OnReceiveListener, OnScrollListener, OnItemClickListener {
	private static final String TAG = "HistoryActivity";
	private int mAppId = -1;
	private Context mContext;
	
	//view
	private TextView mStorageTV;
	private ListView mHistoryMsgLV;
	
	//title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	
	//adapter
	private HistoryMsgAdapter mAdapter;
	
	/**the list that send or receive history info*/
	private List<HistoryInfo> mHistoryList = new ArrayList<HistoryInfo>();
	// 用来保存ListView中每个Item的图片，以便释放
	public static Map<String, Bitmap> bitmapCaches = new HashMap<String, Bitmap>();
	
	private Notice mNotice;
	private SocketCommunicationManager communicationManager;
	
	private FileInfoManager mFileInfoManager = null;
	
	//msg
	private static final int MSG_SEND_FILE = 0;
	private static final int MSG_UPDATE_UI = 1;
	private static final int MSG_UPDATE_SEND_PROGRESS = 2;
	private static final int MSG_UPDATE_SEND_STATUS = 3;
	private static final int MSG_UPDATE_RECEIVE_STATUS = 4;
	private static final int MSG_UPDATE_RECEIVE_PROGRESS = 5;
	private static final int MSG_SEND_FINISHED = 6;
	private static final int MSG_RECEIVE_FINISHED = 7;
	
	private static final String KEY_RECEIVE_BYTES = "KEY_RECEIVE_BYTES";
	private static final String KEY_SENT_BYTES = "KEY_SENT_BYTES";
	private static final String KEY_TOTAL_BYTES = "KEY_TOTAL_BYTES";
	
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
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_history);
		mContext = this;
		mAppId = getIntent().getIntExtra(Extra.APP_ID, -1);
		mNotice = new Notice(mContext);
		communicationManager = SocketCommunicationManager.getInstance(mContext);
		communicationManager.registerOnFileTransportListener(this, mAppId);
		
		mFileInfoManager = new FileInfoManager(mContext);
		
		//register broadcast
		IntentFilter filter = new IntentFilter(DreamConstant.SEND_FILE_ACTION);
		registerReceiver(historyReceiver, filter);
		
		initTitleVIews();
		initView();
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
		//test=================
//		HistoryInfo historyInfo = null;
//		for (int i = 0; i < 10; i++) {
//			historyInfo = new HistoryInfo();
//			FileTransferInfo fileInfo = new FileTransferInfo();
//			fileInfo.setFileName("test" + i);
//			historyInfo.setFileInfo(fileInfo);
//			historyInfo.setDate(System.currentTimeMillis() + i * 1000);
//			if (i % 2 == 0) {
//				historyInfo.setSendUserName("name" + i);
//				historyInfo.setMsgType(HistoryManager.TYPE_RECEIVE);
//			}else {
//				historyInfo.setSendUserName(HistoryManager.ME);
//				historyInfo.setMsgType(HistoryManager.TYPE_SEND);
//			}
//			mHistoryList.add(historyInfo);
//		}
//		Collections.sort(mHistoryList, HistoryManager.DATE_COMPARATOR);
		//test=================
		mAdapter = new HistoryMsgAdapter(mContext, mHistoryList);
		mAdapter.setStatus(HistoryManager.STATUS_DEFAULT);
		mHistoryMsgLV.setAdapter(mAdapter);
		mHistoryMsgLV.setSelection(0);
	}
	
	private void initTitleVIews(){
		RelativeLayout titleLayout = (RelativeLayout)findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_tiandi);
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText("传输记录");
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText("");
		mRefreshView.setVisibility(View.GONE);
		mHistoryView.setVisibility(View.GONE);
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
			historyInfo = mFileInfoManager.getHistoryInfo(historyInfo);
			
			mHistoryList.add(historyInfo);
			
			Message message = mHandler.obtainMessage();
			message.what = MSG_UPDATE_SEND_STATUS;
			mHandler.sendMessage(message);
			
			communicationManager.sendFile(historyInfo, HistoryActivity.this, mAppId);
			
		}
	}

	@Override
	public void onSendProgress(HistoryInfo historyInfo) {
		Log.d(TAG, "onSendProgress=" + historyInfo.getProgress());
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_SEND_PROGRESS;
		message.obj = historyInfo;
		mHandler.sendMessage(message);
	}

	@Override
	public void onSendFinished(HistoryInfo historyInfo, boolean success) {
		Log.d(TAG, "onSendFinished.success" + success);
		Message message = mHandler.obtainMessage();
		message.what = MSG_SEND_FINISHED;
		message.obj = success;
		mHandler.sendMessage(message);
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
		historyInfo.setMsgType(HistoryManager.TYPE_RECEIVE);
		historyInfo.setDate(System.currentTimeMillis());
		historyInfo.setStatus(HistoryManager.STATUS_PRE_RECEIVE);
		historyInfo = mFileInfoManager.getHistoryInfo(historyInfo);
		mHistoryList.add(historyInfo);
		
		fileReceiver.receiveFile(historyInfo, this)	;
		
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_RECEIVE_STATUS;
		mHandler.sendMessage(message);
	}

	@Override
	public void onReceiveProgress(HistoryInfo historyInfo) {
		Log.d(TAG, "onReceiveProgress.progress" + historyInfo.getProgress());
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_RECEIVE_PROGRESS;
		message.obj = historyInfo;
		mHandler.sendMessage(message);
	}

	@Override
	public void onReceiveFinished(HistoryInfo historyInfo, boolean success) {
		Log.d(TAG, "onReceiveFinished.success" + success);
		Message message = mHandler.obtainMessage();
		message.what = MSG_RECEIVE_FINISHED;
		message.obj = success;
		mHandler.sendMessage(message);
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
		recycleBitmapCaches(0, firstVisibleItem);
		recycleBitmapCaches(firstVisibleItem + visibleItemCount, totalItemCount);
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
