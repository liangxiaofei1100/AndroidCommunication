package com.dreamlink.communication;

import java.util.Timer;
import java.util.TimerTask;

import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * see {@code SpeedTest}.
 * 
 */
public class SpeedTestServer extends Activity implements
		OnCommunicationListener {
	private static final String TAG = "SpeedTestServer";
	private Notice mNotice;
	private SocketCommunicationManager mCommunicationManager;
	private TextView mSpeedTextView;
	private EditText mSizeEditText;
	private Button mStartButton;
	private boolean mIsStarted;
	private boolean mStop;

	private long mStartTime = 0;
	private long mTotalSize = 0;

	/** Show speed ever 1 second. */
	private Timer mShowSpeedTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_speed);
		initView();
		mNotice = new Notice(this);
		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mCommunicationManager.registered(this);
		mStop = false;
		mIsStarted = false;
	}

	private void initView() {
		mSpeedTextView = (TextView) findViewById(R.id.tvSpeedTestSpeed);
		mSizeEditText = (EditText) findViewById(R.id.etSpeedTest);
		mStartButton = (Button) findViewById(R.id.btnSpeedTestStart);

		mStartButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String sizeString = mSizeEditText.getText().toString();
				if (mIsStarted) {
					mNotice.showToast("Test is already started");
					return;
				}
				if (!TextUtils.isEmpty(sizeString)) {
					int size = Integer.valueOf(sizeString);
					mIsStarted = true;
					startTest(size);
				}
			}
		});
	}

	private void startTest(int size) {
		if (mCommunicationManager.getCommunications().size() > 0) {
			Log.d(TAG, "start Test");
			TestThread testThread = new TestThread(size);
			testThread.start();

			mShowSpeedTimer = new Timer();
			mShowSpeedTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					mHandler.sendEmptyMessage(0);
				}
			}, 1000, 1000);
		} else {
			Log.d(TAG, "No connection!");
			mNotice.showToast("No connection!");
		}
	}

	private class TestThread extends Thread {
		private int mSize = 0;
		private byte[] mData;

		public TestThread(int size) {
			mSize = size;
			mData = new byte[size];
		}

		@Override
		public void run() {
			Log.d(TAG, "start Test, run");

			mStartTime = System.currentTimeMillis();
			while (!mStop) {
				mCommunicationManager.sendMessage(mData, 0);
				mTotalSize += mSize;
			}
		}
	}

	Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			mSpeedTextView.setText(SpeedTest.getSpeedText(mTotalSize,
					mStartTime));
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mStop = true;
		mCommunicationManager.unregistered(this);
		if (mShowSpeedTimer != null) {
			mShowSpeedTimer.cancel();
			mShowSpeedTimer = null;
		}
	}
}
