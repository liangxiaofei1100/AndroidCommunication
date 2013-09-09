package com.dreamlink.communication.ui.history;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.FileReceiver;
import com.dreamlink.communication.FileReceiver.OnReceiveListener;
import com.dreamlink.communication.FileReceiverByYuri;
import com.dreamlink.communication.FileSender.OnFileSendListener;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnFileTransportListener;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.BaseFragmentActivity;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.file.FileInfo;
import com.dreamlink.communication.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.widget.ListView;
import android.widget.TextView;

public class HistoryActivity extends BaseFragmentActivity implements OnFileTransportListener, com.dreamlink.communication.FileSenderByYuri.OnFileSendListener, OnReceiveListener {
	private static final String TAG = "HistoryActivity";
	private int mAppId = -1;
	private Context mContext;
	
	//view
	private TextView mStorageTV;
	private ListView mHistoryMsgLV;
	
	//adapter
	private HistoryMsgAdapter mAdapter;
	
	/**the list that send or receive history info*/
	private List<HistoryInfo> mHistoryList = new ArrayList<HistoryInfo>();
	
	private Notice mNotice;
	private SocketCommunicationManager communicationManager;
	
	//msg
	private static final int MSG_SEND_FILE = 0;
	private static final int MSG_UPDATE_UI = 1;
	private static final int MSG_UPDATE_SEND_PROGRESS = 2;
	private static final int MSG_UPDATE_SEND_STATUS = 3;
	private static final int MSG_UPDATE_RECEIVE_STATUS = 4;
	private static final int MSG_UPDATE_RECEIVE_PROGRESS = 5;
	
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
					FileInfo fileInfo = bundle.getParcelable(Extra.SEND_FILE);
					List<User> userList = bundle.getParcelableArrayList(Extra.SEND_USER);
					sendFile(fileInfo, userList);
				}
			}
		}
	};
	
	double len  = 0;
	 Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEND_FILE:
				break;
			case MSG_UPDATE_UI:
				HistoryInfo historyInfo1 = (HistoryInfo) msg.obj;
				Collections.sort(mHistoryList, HistoryManager.DATE_COMPARATOR);
				int position = mHistoryList.indexOf(historyInfo1);
				mHistoryList.get(position).setProgress(len);
				Log.i(TAG, "position=" + position);
				mHistoryMsgLV.setSelection(0);
				mAdapter.notifyDataSetChanged();
				break;
				
			case MSG_UPDATE_SEND_PROGRESS:
				Log.d(TAG, "MSG_UPDATE_SEND_PROGRESS");
//				Bundle data = msg.getData();
//				long sentBytes = data.getLong(KEY_SENT_BYTES);
//				long totalBytes = data.getLong(KEY_TOTAL_BYTES);
//				int progress = (int) ((sentBytes / (float) totalBytes) * 100);
//				Log.d(TAG, "sentBytes = " + sentBytes + ", totalBytes = "
//						+ totalBytes + "progress = " + progress);
				HistoryInfo historyInfo2 = (HistoryInfo) msg.obj;
				Collections.sort(mHistoryList, HistoryManager.DATE_COMPARATOR);
				int position2 = mHistoryList.indexOf(historyInfo2);
				mHistoryMsgLV.setSelection(position2);
				mAdapter.notifyDataSetChanged();
				break;
				
			case MSG_UPDATE_RECEIVE_PROGRESS:
				Bundle data = msg.getData();
				long receivedBytes = data.getLong(KEY_RECEIVE_BYTES);
				long totalBytes = data.getLong(KEY_TOTAL_BYTES);
				int progress = (int) ((receivedBytes / (float) totalBytes) * 100);
//				mProgressBar.setProgress(progress);
				Log.d(TAG, "receivedBytes = " + receivedBytes
						+ ", totalBytes = " + totalBytes + "progress = "
						+ progress);
//				mSpeedTextView.setText(FileTransportTest.getSpeedText(
//						receivedBytes, mStartTime));
				break;
				
			case MSG_UPDATE_SEND_STATUS:
				break;
			case MSG_UPDATE_RECEIVE_STATUS:
				int status = msg.arg1;
				mAdapter.setReciveStatus(status);
				break;

			default:
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_history);
		mContext = this;
		mAppId = AppUtil.getAppID(getParent());
		mNotice = new Notice(mContext);
		communicationManager = SocketCommunicationManager.getInstance(mContext);
		communicationManager.registerOnFileTransportListener(this, mAppId);
		
		//register broadcast
		IntentFilter filter = new IntentFilter(DreamConstant.SEND_FILE_ACTION);
		registerReceiver(historyReceiver, filter);
		
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
		//test=================
		HistoryInfo historyInfo = null;
		for (int i = 0; i < 10; i++) {
			historyInfo = new HistoryInfo();
			FileInfo fileInfo = new FileInfo("testFile" + i);
			historyInfo.setFileInfo(fileInfo);
			historyInfo.setDate(System.currentTimeMillis() + i * 1000);
			if (i % 2 == 0) {
				historyInfo.setSendUserName("name" + i);
				historyInfo.setMsgType(HistoryManager.TYPE_RECEIVE);
			}else {
				historyInfo.setSendUserName(HistoryManager.ME);
				historyInfo.setMsgType(HistoryManager.TYPE_SEND);
			}
			mHistoryList.add(historyInfo);
		}
		Collections.sort(mHistoryList, HistoryManager.DATE_COMPARATOR);
		//test=================
		mAdapter = new HistoryMsgAdapter(mContext, mHistoryList);
		mHistoryMsgLV.setAdapter(mAdapter);
		mHistoryMsgLV.setSelection(0);
	}
	
	public void sendFile(FileInfo fileInfo, List<User> list){
		for (int i = 0; i < list.size(); i++) {
			User receiverUser = list.get(i);
			SendFileThread sendFileThread = new SendFileThread(fileInfo, receiverUser);
			sendFileThread.start();
		}
	}
	
    class SendFileThread extends Thread {
    	FileInfo fileInfo = null;
    	User receiveUser = null;
    	SendFileThread(FileInfo fileInfo, User receiveUser){
    		this.fileInfo = fileInfo;
    		this.receiveUser = receiveUser;
    	}
    	
		@Override
		public void run() {
			HistoryInfo historyInfo = new HistoryInfo();
			historyInfo.setFileInfo(fileInfo);
			historyInfo.setReceiveUser(receiveUser);
			historyInfo.setSendUserName(HistoryManager.ME);
			historyInfo.setMsgType(HistoryManager.TYPE_SEND);
			historyInfo.setDate(System.currentTimeMillis());
			
			mHistoryList.add(historyInfo);
			
//			communicationManager.sendFile(new File(fileInfo.filePath),
//					HistoryActivity.this, receiveUser, mAppId);
			
			communicationManager.sendFile(historyInfo, HistoryActivity.this, mAppId);
		}
	}
    
	@Override
	public void onReceiveFile(FileReceiver fileReceiver) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onReceiveFile:" + fileReceiver.getFileInfo().mFileName + "," + fileReceiver.getSendUser().getUserName());
		//define a file to save the receive file
		File fileDir = new File(DreamConstant.DEFAULT_SAVE_FOLDER);
		if (!fileDir.exists()) {
			fileDir.mkdirs();
		}
		
		File file = new File(DreamConstant.DEFAULT_SAVE_FOLDER + fileReceiver.getFileInfo().mFileName);
		if (file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		HistoryInfo historyInfo = new HistoryInfo();
		FileInfo fileInfo = new FileInfo(fileReceiver.getFileInfo().mFileName);
		fileInfo.fileDate = file.lastModified();
		fileInfo.filePath = file.getAbsolutePath();
		fileInfo.fileSize = file.length();
		historyInfo.setFileInfo(fileInfo);
		historyInfo.setSendUserName(fileReceiver.getSendUser().getUserName());
		historyInfo.setMsgType(HistoryManager.TYPE_RECEIVE);
		historyInfo.setDate(System.currentTimeMillis());
		mHistoryList.add(historyInfo);
		
		fileReceiver.receiveFile(file, this);
		
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_RECEIVE_STATUS;
		message.arg1 = HistoryManager.STATUS_RECEIVING;
		mHandler.sendMessage(message);
	}

	@Override
	public void onSendProgress(HistoryInfo historyInfo) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onSendProgress=" + historyInfo.getProgress());
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_SEND_PROGRESS;
		message.obj = historyInfo;
		mHandler.sendMessage(message);
	}

	@Override
	public void onSendFinished(boolean success) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onReceiveProgress(long receivedBytes, long totalBytes) {
		// TODO Auto-generated method stub
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_RECEIVE_PROGRESS;
		Bundle data = new Bundle();
		data.putLong(KEY_RECEIVE_BYTES, receivedBytes);
		data.putLong(KEY_TOTAL_BYTES, totalBytes);
		message.setData(data);
		mHandler.sendMessage(message);
	}

	@Override
	public void onReceiveFinished(boolean success) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReceiveFileByYuri(FileReceiverByYuri fileReceiverByYuri) {
		// TODO Auto-generated method stub
		
	}

	
}
