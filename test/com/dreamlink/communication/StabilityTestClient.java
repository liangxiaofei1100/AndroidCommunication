package com.dreamlink.communication;

import java.util.ArrayList;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;
import com.dreamlink.communication.util.LogFile;
import com.dreamlink.communication.util.Notice;
import com.dreamlink.communication.util.TimeUtil;

/**
 * see {@code StabilityTest}.
 * 
 */
public class StabilityTestClient extends Activity implements
		OnCommunicationListenerExternal {
	private ListView mListView;
	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mData;
	private SocketCommunicationManager mCommunicationManager;
	private boolean mStop = false;
	private Notice mNotice;
	private static final String TAG = "StabilityTestClient";

	private LogFile mDataLogFile;
	private LogFile mErrorLogFile;

	private static final int SEND_MODE_ALL = 1;
	private static final int SEND_MODE_SINGLE = 2;
	private int mSendMode = SEND_MODE_ALL;
	private User mSendModeSigleReceiver;

	private UserManager mUserManager;

	/** Stability test app id */
	private int mAppID = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		mAppID = intent.getIntExtra(StabilityTest.EXTRA_APP_ID, 0);

		initView();
		mNotice = new Notice(this);
		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mCommunicationManager.registerOnCommunicationListenerExternal(this,
				mAppID);

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

		mUserManager = UserManager.getInstance();
	}

	private class TestThread extends Thread {

		@Override
		public void run() {
			Log.d(TAG, "start Test, run");
			int count = 0;
			while (!mStop) {
				Log.d(TAG, "Send message: " + count + ", send mode = "
						+ mSendMode);
				byte[] message = (TimeUtil.getCurrentTime() + "::"
						+ String.valueOf(count) + '\n').getBytes();
				switch (mSendMode) {
				case SEND_MODE_ALL:
					mCommunicationManager.sendMessageToAll(message, mAppID);
					break;
				case SEND_MODE_SINGLE:
					if (mSendModeSigleReceiver != null) {
						mCommunicationManager.sendMessageToSingle(message,
								mSendModeSigleReceiver, 100);
					}
					break;

				default:
					break;
				}
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
		setContentView(R.layout.test_stability);
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
	protected void onDestroy() {
		super.onDestroy();
		mCommunicationManager.unregisterOnCommunicationListenerExternal(this);
		mStop = true;
		mDataLogFile.close();
		mErrorLogFile.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.stability_test, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_send_to_all:
			mSendMode = SEND_MODE_ALL;
			mNotice.showToast("Send to all");
			break;
		case R.id.menu_send_to_single:
			mSendMode = SEND_MODE_SINGLE;
			showReceiverChooserMenu(item);

			break;
		case MENU_SEND_TO_SINGLE:
			String receiver = item.getTitle().toString();
			Map<Integer, User> allUser = mUserManager.getAllUser();
			for (Map.Entry<Integer, User> entry : allUser.entrySet()) {
				if (receiver.equals(entry.getValue().getUserName())) {
					mSendModeSigleReceiver = entry.getValue();
					// Get the first matched user. If there are users with the
					// same name, ignore.
					break;
				}
			}
			mNotice.showToast("Send to ID = "
					+ mSendModeSigleReceiver.getUserID() + ", name = "
					+ mSendModeSigleReceiver.getUserName());
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private static final int MENU_SEND_TO_SINGLE = 1;

	private void showReceiverChooserMenu(MenuItem item) {
		SubMenu subMenu = item.getSubMenu();
		subMenu.clear();
		Map<Integer, User> allUser = mUserManager.getAllUser();
		User localUser = mUserManager.getLocalUser();

		for (Map.Entry<Integer, User> entry : allUser.entrySet()) {
			if (localUser.getUserID() != (int) entry.getKey()) {
				subMenu.add(1, MENU_SEND_TO_SINGLE, 0, entry.getValue()
						.getUserName());
			}
		}
	}

	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) {
		if (sendUser == null) {
			Log.d(TAG, "User is lost connection.");
			return;
		}
		Message message = mHandler.obtainMessage();
		message.obj = "From " + sendUser.getUserName() + ": " + new String(msg);
		mHandler.sendMessage(message);
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
