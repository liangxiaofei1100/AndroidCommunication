package com.dreamlink.communication;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;
import com.dreamlink.communication.util.Notice;

/**
 * see {@code SpeedTest}.
 * 
 */
public class SpeedTestClient extends Activity implements
		OnCommunicationListenerExternal {
	private SocketCommunicationManager mCommunicationManager;
	private Notice mNotice;
	private static final String TAG = "SpeedTestClient";
	private TextView mSpeedTextView;
	private EditText mSizeEditText;
	private Button mStartButton;
	private long mStartTime;
	private long mTotalSize;
	/** Show speed ever 1 second. */
	private Timer mShowSpeedTimer;

	/** Speed test app id */
	private int mAppID = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		mAppID = intent.getIntExtra(SpeedTest.EXTRA_APP_ID, 0);

		setContentView(R.layout.test_speed);
		initView();
		mNotice = new Notice(this);
		mCommunicationManager = SocketCommunicationManager.getInstance(this);
	}

	private void initView() {
		mSpeedTextView = (TextView) findViewById(R.id.tvSpeedTestSpeed);
		mSizeEditText = (EditText) findViewById(R.id.etSpeedTest);
		mSizeEditText.setVisibility(View.GONE);
		mStartButton = (Button) findViewById(R.id.btnSpeedTestStart);
		mStartButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mStartTime = System.currentTimeMillis();
				mCommunicationManager.registerOnCommunicationListenerExternal(
						SpeedTestClient.this, mAppID);

				// Show speed ever 1 second.
				mShowSpeedTimer = new Timer();
				mShowSpeedTimer.schedule(new TimerTask() {

					@Override
					public void run() {
						mHandler.sendEmptyMessage(0);
					}
				}, 1000, 1000);
			}
		});
	}

	Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			mSpeedTextView.setText(SpeedTest.getSpeedText(mTotalSize,
					mStartTime));
		};
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCommunicationManager.unregisterOnCommunicationListenerExternal(this);
		if (mShowSpeedTimer != null) {
			mShowSpeedTimer.cancel();
			mShowSpeedTimer = null;
		}
	}

	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) {
		mTotalSize += msg.length;

	}

	@Override
	public void onUserConnected(User user) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserDisconnected(User user) {
		// TODO Auto-generated method stub

	}

	@Override
	public IBinder asBinder() {
		// TODO Auto-generated method stub
		return null;
	}
}
