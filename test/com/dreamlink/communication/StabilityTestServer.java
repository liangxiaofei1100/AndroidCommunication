package com.dreamlink.communication;

import java.util.ArrayList;

import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.util.LogFile;
import com.dreamlink.communication.util.TimeUtil;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * see {@code StabilityTest}.
 * 
 */
public class StabilityTestServer extends Activity implements
		OnCommunicationListener {
	private static final String TAG = "StabilityTestServer";
	private ListView mListView;
	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mData;
	private SocketCommunicationManager mCommunicationManager;
	private LogFile mDataLogFile;
	private LogFile mErrorLogFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_stability);
		initView();

		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mCommunicationManager.registered(this);

		mDataLogFile = new LogFile(getApplicationContext(),
				"StabilityTestServer-" + TimeUtil.getCurrentTime() + ".txt");
		mDataLogFile.open();

		mErrorLogFile = new LogFile(getApplicationContext(),
				"StabilityTestServer_error-" + TimeUtil.getCurrentTime()
						+ ".txt");
		mErrorLogFile.open();
	}

	private void initView() {
		initData();
		mListView = (ListView) findViewById(R.id.lstStabilityTest);
		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mData);
		mListView.setAdapter(mAdapter);
	}

	private void initData() {
		mData = new ArrayList<String>();

	}

	Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			String messageString = msg.obj.toString();
			if (mData.size() > 100) {
				mData.clear();
			}
			mData.add(messageString);
			mAdapter.notifyDataSetChanged();
			mDataLogFile.writeLog(TimeUtil.getCurrentTime() + " Received: \n");
			mDataLogFile.writeLog(messageString);

			mCommunicationManager.sendMessage(messageString.getBytes(), 0);
		};
	};

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication ip) {
		Log.d(TAG, "onReceiveMessage:" + String.valueOf(msg));
		Message message = mHandler.obtainMessage();
		message.obj = new String(msg);
		mHandler.sendMessage(message);
	}

	@Override
	public void onSendResult(byte[] msg) {

	}

	@Override
	public void notifyConnectChanged() {

	}
}
