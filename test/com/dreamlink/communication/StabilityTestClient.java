package com.dreamlink.communication;

import java.util.ArrayList;

import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.util.LogFile;
import com.dreamlink.communication.util.Notice;
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
public class StabilityTestClient extends Activity implements
		OnCommunicationListener {
	private ListView mListView;
	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mData;
	private SocketCommunicationManager mCommunicationManager;
	private boolean mStop = false;
	private Notice mNotice;
	private static final String TAG = "StabilityTestClient";

	private LogFile mDataLogFile;
	private LogFile mErrorLogFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_stability);
		initView();
		mNotice = new Notice(this);
		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mCommunicationManager.registered(this);

		if (mCommunicationManager.getCommunications().size() > 0) {
			Log.d(TAG, "start Test");
			TestThread testThread = new TestThread();
			testThread.start();
		} else {
			Log.d(TAG, "No connection!");
			mNotice.showToast("No connection!");
		}

		mDataLogFile = new LogFile(getApplicationContext(),
				"StabilityTestClient-" + TimeUtil.getCurrentTime() + ".txt");
		mDataLogFile.open();

		mErrorLogFile = new LogFile(getApplicationContext(),
				"StabilityTestClient_error-" + TimeUtil.getCurrentTime()
						+ ".txt");
		mErrorLogFile.open();
	}

	private class TestThread extends Thread {

		@Override
		public void run() {
			Log.d(TAG, "start Test, run");
			int count = 0;
			while (!mStop) {
				Log.d(TAG, "Send message: " + count);
				mCommunicationManager.sendMessage((TimeUtil.getCurrentTime()
						+ "::" + String.valueOf(count) + '\n').getBytes(), 0);
				count++;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
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
		};
	};

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication ip) {
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mStop = true;
		mDataLogFile.close();
		mErrorLogFile.close();
	}
}
