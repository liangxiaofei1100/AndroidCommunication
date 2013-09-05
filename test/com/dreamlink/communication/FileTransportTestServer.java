package com.dreamlink.communication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;
import com.dreamlink.communication.FileSender.OnFileSendListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

/**
 * see {@code SpeedTest}.
 * 
 */
public class FileTransportTestServer extends Activity implements
		OnCommunicationListenerExternal, OnFileSendListener {
	private static final String TAG = "FileTransportTestServer";
	private Context mContext;
	private Notice mNotice;
	private SocketCommunicationManager mCommunicationManager;
	private UserManager mUserManager;

	private TextView mStatusTextView;
	private Button mSendFileButton;
	private ProgressBar mProgressBar;
	private TextView mSpeedTextView;

	private Spinner mSpinner;
	private SpinnerAdapter mSpinnerAdapter;

	private long mStartTime;

	/** Speed test app id */
	private int mAppID = 0;

	private static final int MSG_UPDATE_SEND_PROGRESS = 1;
	private static final int MSG_FINISHED = 2;
	private static final int MSG_UPDATE_STATUS = 3;
	private static final int MSG_CREATE_FILE_BEGIN = 4;
	private static final int MSG_CREATE_FILE_END = 5;

	private static final String KEY_SENT_BYTES = "KEY_SENT_BYTES";
	private static final String KEY_TOTAL_BYTES = "KEY_TOTAL_BYTES";

	Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_STATUS:
				mStatusTextView.setText(msg.obj.toString());
				break;

			case MSG_UPDATE_SEND_PROGRESS:
				Bundle data = msg.getData();
				long sentBytes = data.getLong(KEY_SENT_BYTES);
				long totalBytes = data.getLong(KEY_TOTAL_BYTES);
				int progress = (int) ((sentBytes / (float) totalBytes) * 100);
				Log.d(TAG, "sentBytes = " + sentBytes + ", totalBytes = "
						+ totalBytes + "progress = " + progress);
				mProgressBar.setProgress(progress);

				mSpeedTextView.setText(FileTransportTest.getSpeedText(
						sentBytes, mStartTime));
				break;
			case MSG_FINISHED:
				mStatusTextView.setText("Send Finished.");
				break;
			case MSG_CREATE_FILE_BEGIN:
				mStatusTextView.setText("Creating File...");
				break;
			case MSG_CREATE_FILE_END:
				File file = (File) msg.obj;
				if (file != null && file.exists()) {
					mStatusTextView.setText("Create file success");

					User receiveUser = getUser(mSpinner.getSelectedItem()
							.toString());
					if (receiveUser == null) {
						mStatusTextView.setText("Get user fail.");
						Log.e(TAG, "Get user fail.");
						return;
					}
					mStatusTextView.setText("Sending file```");
					mCommunicationManager.sendFile(file,
							FileTransportTestServer.this, receiveUser, mAppID);

					mStartTime = System.currentTimeMillis();
				} else {
					mStatusTextView.setText("Create file fail");
				}
				break;
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		Intent intent = getIntent();
		mAppID = intent.getIntExtra(SpeedTest.EXTRA_APP_ID, 0);

		setContentView(R.layout.test_file_transport);

		mNotice = new Notice(this);
		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mCommunicationManager.registerOnCommunicationListenerExternal(this,
				mAppID);

		mUserManager = UserManager.getInstance();

		initView();
	}

	private void initView() {
		mStatusTextView = (TextView) findViewById(R.id.tvStatus);
		mSpeedTextView = (TextView) findViewById(R.id.tvSpeed);
		mSpinner = (Spinner) findViewById(R.id.spinnerUsers);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_spinner_item, getAllUser());
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(adapter);

		mProgressBar = (ProgressBar) findViewById(R.id.pbTransport);
		mProgressBar.setMax(100);

		mSendFileButton = (Button) findViewById(R.id.btnSendFile);
		mSendFileButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CreateFileThread createFileThread = new CreateFileThread();
				createFileThread.start();
			}
		});
	}

	/**
	 * Get a test user
	 * 
	 * @return
	 */
	private ArrayList<String> getAllUser() {
		ArrayList<String> allUsers = new ArrayList<String>();
		User localUser = mUserManager.getLocalUser();
		for (Map.Entry<Integer, User> entry : mUserManager.getAllUser()
				.entrySet()) {
			if (localUser.getUserID() != (int) entry.getKey()) {
				allUsers.add(entry.getValue().getUserName());
			}
		}
		return allUsers;
	}

	private User getUser(String userName) {
		for (Map.Entry<Integer, User> entry : mUserManager.getAllUser()
				.entrySet()) {
			if (entry.getValue().getUserName().equals(userName)) {
				return entry.getValue();
			}
		}
		return null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCommunicationManager.unregisterOnCommunicationListenerExternal(this);
	}

	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) {
		Log.d(TAG, "onReceiveMessage:" + String.valueOf(msg));

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
	public void onProgress(long sentBytes, long totalBytes) {
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_SEND_PROGRESS;
		Bundle data = new Bundle();
		data.putLong(KEY_SENT_BYTES, sentBytes);
		data.putLong(KEY_TOTAL_BYTES, totalBytes);
		message.setData(data);
		mHandler.sendMessage(message);
	}

	@Override
	public void onFinished(boolean success) {
		mHandler.sendEmptyMessage(MSG_FINISHED);
	}

	class CreateFileThread extends Thread {
		@Override
		public void run() {
			mHandler.sendEmptyMessage(MSG_CREATE_FILE_BEGIN);

			File file = createFile();

			Message message = mHandler.obtainMessage();
			message.what = MSG_CREATE_FILE_END;
			message.obj = file;
			mHandler.sendMessage(message);
		}
	}

	private File createFile() {
		File file = null;
		try {
			file = File.createTempFile("test", "txt");
			FileOutputStream outputStream = new FileOutputStream(file);
			for (int i = 0; i < 100; i++) {
				byte[] buffer = new byte[1024 * 1024];
				outputStream.write(buffer);
			}
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}
}
