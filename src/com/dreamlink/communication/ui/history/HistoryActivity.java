package com.dreamlink.communication.ui.history;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.FileReceiver;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnFileTransportListener;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.ui.BaseFragmentActivity;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.file.FileInfo;
import com.dreamlink.communication.util.AppUtil;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.TextView;

public class HistoryActivity extends BaseFragmentActivity implements OnFileTransportListener {
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
	private UserManager mUserManager;
	
	//msg
	private static final int MSG_SEND_FILE = 0;
	private static final int MSG_UPDATE_UI = 1;
	
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
					CreateFileThread createFileThread = new CreateFileThread(fileInfo);
					createFileThread.start();
				}
			}
		}
	};
	
	double len  = 0;
	 Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEND_FILE:
				FileInfo fileInfo = (FileInfo) msg.obj;
				final double max = fileInfo.fileSize;
				mAdapter.setMax(fileInfo.fileSize);
				mAdapter.setStatus(HistoryManager.STATUS_SENDING);
				//模拟发送文件,查看UI效果
				HistoryInfo historyInfo = new HistoryInfo();
				User user = new User();
				user.setUserID(1111);
				user.setUserName("大众化");
				historyInfo.setUser(user);
				historyInfo.setFileInfo(fileInfo);
				historyInfo.setDate(System.currentTimeMillis());
				mHistoryList.add(historyInfo);
				int position = mHistoryList.indexOf(historyInfo);
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						len += max * 0.1;
						Log.i(TAG, "len=" + len);
						if (len > max) {
							cancel();
						}else {
							mAdapter.setProgress(len);
							mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_UI));
						}
					}
				}, 0, 1000);
				break;
			case MSG_UPDATE_UI:
				Collections.sort(mHistoryList, HistoryManager.DATE_COMPARATOR);
				mHistoryMsgLV.setSelection(0);
				mAdapter.notifyDataSetChanged();
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
		mUserManager = UserManager.getInstance();
		
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
//		HistoryInfo historyInfo = null;
//		for (int i = 0; i < 10; i++) {
//			historyInfo = new HistoryInfo();
//			if (i % 2 == 0) {
//				historyInfo.setDate(System.currentTimeMillis() + i * 1000);
//				historyInfo.setMsgType(false);
//				mHistoryList.add(historyInfo);
//			}else {
//				historyInfo.setDate(System.currentTimeMillis() + i * 1000);
//				historyInfo.setMsgType(true);
//				mHistoryList.add(historyInfo);
//			}
//		}
//		Collections.sort(mHistoryList, DATE_COMPARATOR);
		//test=================
		mAdapter = new HistoryMsgAdapter(mContext, mHistoryList);
		mHistoryMsgLV.setAdapter(mAdapter);
		mHistoryMsgLV.setSelection(0);
	}
	
    class CreateFileThread extends Thread {
    	FileInfo fileInfo = null;
    	CreateFileThread(FileInfo fileInfo){
    		this.fileInfo = fileInfo;
    	}
    	
		@Override
		public void run() {
			Message message = mHandler.obtainMessage();
			message.what = MSG_SEND_FILE;
			message.obj = fileInfo;
			message.sendToTarget();
		}
	}
    
	@Override
	public void onReceiveFile(FileReceiver fileReceiver) {
		// TODO Auto-generated method stub
		
	}
	
}
