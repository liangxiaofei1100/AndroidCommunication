package com.dreamlink.communication;

import java.io.File;
import java.util.Timer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.dreamlink.communication.aidl.OnCommunicationListenerExternal;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.FileReceiver.OnReceiveListener;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnFileTransportListener;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.util.Log;

/**
 * see {@code SpeedTest}.
 * 
 */
public class FileTransportTestClient extends Activity implements
		OnCommunicationListenerExternal, OnFileTransportListener,
		OnReceiveListener {
	private SocketCommunicationManager mCommunicationManager;
	private Notice mNotice;
	private static final String TAG = "FileTransportTestClient";
	private long mStartTime;
	/** Show speed ever 1 second. */
	private Timer mShowSpeedTimer;

	/** Speed test app id */
	private int mAppID = 0;

	private TextView mStatusTextView;
	private Button mStopReceiveButton;
	private ProgressBar mProgressBar;
	private TextView mSpeedTextView;
	private Spinner mSpinner;

	private static final int MSG_UPDATE_RECEIVE_PROGRESS = 1;
	private static final int MSG_FINISHED = 2;
	private static final int MSG_UPDATE_STATUS = 3;

	private static final String KEY_RECEIVE_BYTES = "KEY_RECEIVE_BYTES";
	private static final String KEY_TOTAL_BYTES = "KEY_TOTAL_BYTES";

	private FileReceiver mFileReceiver;
	
	private long mReceivedFileTotalLength;

	Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_STATUS:
				mStatusTextView.setText(msg.obj.toString());
				break;

			case MSG_UPDATE_RECEIVE_PROGRESS:
				Bundle data = msg.getData();
				long receivedBytes = data.getLong(KEY_RECEIVE_BYTES);
				long totalBytes = data.getLong(KEY_TOTAL_BYTES);
				int progress = (int) ((receivedBytes / (float) totalBytes) * 100);
				mProgressBar.setProgress(progress);
				Log.d(TAG, "receivedBytes = " + receivedBytes
						+ ", totalBytes = " + totalBytes + "progress = "
						+ progress);
				mSpeedTextView.setText(FileTransportTest.getSpeedText(
						receivedBytes, mStartTime));
				break;
			case MSG_FINISHED:
				boolean result = false;
				if (msg.arg1 == 0) {
					result = true;
				} else {
					result = false;
				}
				mStatusTextView.setText("Send Finished. result = " + result);
				break;
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		mAppID = intent.getIntExtra(SpeedTest.EXTRA_APP_ID, 0);

		setContentView(R.layout.test_file_transport);
		initView();
		mNotice = new Notice(this);
		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mCommunicationManager.registerOnFileTransportListener(this, mAppID);

	}

	private void initView() {
		mStatusTextView = (TextView) findViewById(R.id.tvStatus);
		mSpeedTextView = (TextView) findViewById(R.id.tvSpeed);
		mSpinner = (Spinner) findViewById(R.id.spinnerUsers);
		mSpinner.setVisibility(View.GONE);

		mProgressBar = (ProgressBar) findViewById(R.id.pbTransport);
		mProgressBar.setMax(100);
		mStopReceiveButton = (Button) findViewById(R.id.btnSendFile);
		mStopReceiveButton.setText("Stop Receive.");
		mStopReceiveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mFileReceiver != null) {
					mFileReceiver.stopReceive();
					mNotice.showToast("Receive stoped.");
				} else {
					mNotice.showToast("Receive is not started.");
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCommunicationManager.unregisterOnCommunicationListenerExternal(this);
		if (mShowSpeedTimer != null) {
			mShowSpeedTimer.cancel();
			mShowSpeedTimer = null;
		}
		mCommunicationManager.unregisterOnFileTransportListener(this);
	}

	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) {

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


	@Override
	public void onReceiveFile(FileReceiver fileReceiver) {
		Log.d(TAG, "onReceiveFile " + fileReceiver);
		mReceivedFileTotalLength = fileReceiver.getFileTransferInfo().mFileSize;
		File file = new File("/sdcard/receivedFile.txt");
		fileReceiver.receiveFile(file, this, new Object());
		mFileReceiver = fileReceiver;
		mStartTime = System.currentTimeMillis();

		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_STATUS;
		message.obj = "Receiving file "
				+ fileReceiver.getFileTransferInfo().mFileName;
		mHandler.sendMessage(message);

	}

	@Override
	public void onReceiveProgress(long receivedBytes, File file, Object key) {
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_RECEIVE_PROGRESS;
		Bundle data = new Bundle();
		data.putLong(KEY_RECEIVE_BYTES, receivedBytes);
		data.putLong(KEY_TOTAL_BYTES, mReceivedFileTotalLength);
		message.setData(data);
		mHandler.sendMessage(message);
		
	}

	@Override
	public void onReceiveFinished(boolean success, File file, Object key) {
		Message message = mHandler.obtainMessage(MSG_FINISHED);
		if (success) {
			message.arg1 = 0;
		} else {
			message.arg1 = 1;
		}
		mHandler.sendMessage(message);
	}
}
