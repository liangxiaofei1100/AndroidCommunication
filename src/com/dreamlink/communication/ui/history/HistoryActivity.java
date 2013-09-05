package com.dreamlink.communication.ui.history;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.util.AppUtil;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class HistoryActivity extends Activity implements OnCommunicationListenerExternal {
	
	private int mAppId = -1;
	private SocketCommunicationManager mCommunicationManager = null;
	private Context mContext;
	
	//view
	private TextView mStorageTV;
	private ListView mHistoryMsgLV;
	
	//adapter
	private HistoryMsgAdapter mAdapter;
	
	/////just for test
	private List<HistoryInfo> testList = new ArrayList<HistoryInfo>();
	private Button testbtn1,testbtn2;
	/////just for test
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_history);
		mContext = this;
		mAppId = AppUtil.getAppID(getParent());
		mCommunicationManager = SocketCommunicationManager.getInstance(mContext);
		mCommunicationManager.registerOnCommunicationListenerExternal(this, mAppId);
		initView();
	}
	
	private void initView(){
		mStorageTV = (TextView) findViewById(R.id.tv_storage);
		String space = getResources().getString(R.string.storage_space, 
				DreamUtil.getFormatSize(Environment.getExternalStorageDirectory().getTotalSpace()),
				DreamUtil.getFormatSize(Environment.getExternalStorageDirectory().getFreeSpace()));
		mStorageTV.setText(space);
		mHistoryMsgLV = (ListView) findViewById(R.id.lv_history_msg);
		
		//test=================
		HistoryInfo historyInfo = null;
		for (int i = 0; i < 10; i++) {
			historyInfo = new HistoryInfo();
			if (i / 2 == 0) {
				historyInfo.setDate(System.currentTimeMillis());
				historyInfo.setMsgType(false);
				testList.add(historyInfo);
			}else {
				historyInfo.setDate(System.currentTimeMillis());
				historyInfo.setMsgType(true);
				testList.add(historyInfo);
			}
		}
		testbtn1 = (Button) findViewById(R.id.btn_test1);
		testbtn1.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				HistoryInfo historyInfo = new HistoryInfo();
				historyInfo.setDate(System.currentTimeMillis());
				historyInfo.setMsgType(true);
				testList.add(historyInfo);
				mAdapter.notifyDataSetChanged();
				mHistoryMsgLV.setSelection(mHistoryMsgLV.getCount() -1);
			}
		});
		testbtn2 = (Button) findViewById(R.id.btn_test2);
		testbtn2.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				HistoryInfo historyInfo = new HistoryInfo();
				historyInfo.setDate(System.currentTimeMillis());
				historyInfo.setMsgType(false);
				testList.add(historyInfo);
				mAdapter.notifyDataSetChanged();
				mHistoryMsgLV.setSelection(mHistoryMsgLV.getCount() -1);
			}
		});
		//test=================
		mAdapter = new HistoryMsgAdapter(mContext, testList);
		mHistoryMsgLV.setAdapter(mAdapter);
		mHistoryMsgLV.setSelection(mHistoryMsgLV.getCount() -1);
	}
	
	@Override
	public IBinder asBinder() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onUserConnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onUserDisconnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
}
