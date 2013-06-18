package com.dreamlink.communication.server;

import java.net.Socket;
import java.util.ArrayList;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.SocketMessage;
import com.dreamlink.communication.AppListActivity;
import com.dreamlink.communication.server.SearchClient.OnSearchListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class ClientListActivity extends Activity implements OnSearchListener,
		OnClickListener, OnCommunicationListener {
	private static final String TAG = "ClientListActivity";
	private Context mContext;

	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mClients = new ArrayList<String>();
	private ListView mListView;

	private Notice mNotice;
	private SearchClient mSearchClient;

	private static final int MSG_SEARCH_SUCCESS = 1;
	private static final int MSG_SEARCH_FAIL = 2;

	private SocketCommunicationManager mCommunicationManager;

	private Handler mSearchHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEARCH_SUCCESS:
				// Got another server.
				mNotice.showToast("Found another server: " + msg.obj);
				break;
			case MSG_SEARCH_FAIL:
				mNotice.showToast("Seach client fail.");
				break;

			default:
				break;
			}
		}
	};

	private Handler mSockMessageHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {

			case SocketMessage.MSG_SOCKET_CONNECTED:
				// Socket socket = (Socket) msg.obj;
				// mCommunicationManager.addCommunication(socket);
				// addClient(socket.getInetAddress().getHostAddress());
				mClients.clear();
				for (SocketCommunication com : mCommunicationManager
						.getCommunications()) {
					mClients.add(com.getConnectIP().getHostAddress());
				}
				mAdapter.notifyDataSetChanged();
				break;
			case SocketMessage.MSG_SOCKET_MESSAGE:
				String message = (String) msg.obj;
				mNotice.showToast(message);
				break;
			case SocketMessage.MSG_SOCKET_NOTICE:
				String notice = (String) msg.obj;
				mNotice.showToast(notice);
				break;

			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_list);
		mContext = this;
		mNotice = new Notice(mContext);

		initView();

		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mCommunicationManager.registered(this);
		SocketServerTask serverTask = new SocketServerTask(mContext,
				mCommunicationManager);
		serverTask.execute(new String[] { SocketCommunication.PORT });

		mSearchClient = SearchClient.getInstance(this);
		mSearchClient.setOnSearchListener(this);
		mNotice.showToast("Start Search");
		mSockMessageHandler.obtainMessage(SocketMessage.MSG_SOCKET_CONNECTED)
				.sendToTarget();

		NetWorkUtil.setWifiAPEnabled(mContext, true);
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
		registerReceiver(mBroadcastReceiver, filter);
	}

	WifiManager mWifiManager;
	private static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
	private static final String EXTRA_WIFI_AP_STATE = "wifi_state";
	private static final int WIFI_AP_STATE_ENABLING = 12;
	private static final int WIFI_AP_STATE_ENABLED = 13;
	private static final int WIFI_AP_STATE_DISABLING = 10;
	private static final int WIFI_AP_STATE_DISABLED = 11;
	private static final int WIFI_AP_STATE_FAILED = 14;

	BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "onReceive, action: " + action);
			if (WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
				handleWifiApchanged(intent.getIntExtra(EXTRA_WIFI_AP_STATE,
						WIFI_AP_STATE_FAILED));
			}
		}

		private void handleWifiApchanged(int wifiApState) {
			switch (wifiApState) {
			case WIFI_AP_STATE_ENABLING:
				Log.d(TAG, "WIFI_AP_STATE_ENABLING");
				break;
			case WIFI_AP_STATE_ENABLED:
				Log.d(TAG, "WIFI_AP_STATE_ENABLED");
				mSearchClient.startSearch();
				break;
			case WIFI_AP_STATE_DISABLING:
				Log.d(TAG, "WIFI_AP_STATE_DISABLING");
				break;
			case WIFI_AP_STATE_DISABLED:
				Log.d(TAG, "WIFI_AP_STATE_DISABLED");
				break;
			case WIFI_AP_STATE_FAILED:
				Log.d(TAG, "WIFI_AP_STATE_FAILED");
				break;

			default:
				Log.d(TAG, "handleWifiApchanged, unkown state: " + wifiApState);
				break;
			}
		}

	};

	private void initView() {
		mListView = (ListView) findViewById(R.id.list_client);
		mAdapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_list_item_1, mClients);
		mListView.setAdapter(mAdapter);

		Button startButton = (Button) findViewById(R.id.btn_start);
		startButton.setOnClickListener(this);
		Button quitButton = (Button) findViewById(R.id.btn_quit);
		quitButton.setOnClickListener(this);
	}

	private void unregisterReceiverSafe(BroadcastReceiver receiver) {
		try {
			unregisterReceiver(receiver);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mSearchClient != null) {
			mSearchClient.stopSearch();
		}
		unregisterReceiverSafe(mBroadcastReceiver);
	}

	private void addClient(String ip) {
		// if (!mClients.contains(ip)) {
		// mClients.add(ip);
		// mAdapter.notifyDataSetChanged();
		// }
	}

	@Override
	public void onSearchSuccess(String clientIP) {
		if (!mClients.contains(clientIP)) {
			Message message = mSearchHandler.obtainMessage(MSG_SEARCH_SUCCESS);
			message.obj = clientIP;
			mSearchHandler.sendMessage(message);
		}
	}

	@Override
	public void onSearchFail() {
		Message message = mSearchHandler.obtainMessage(MSG_SEARCH_FAIL);
		mSearchHandler.sendMessage(message);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_start:
			Intent intent = new Intent();
			intent.putExtra(AppListActivity.EXTRA_IS_SERVER, true);
			intent.setClass(this, AppListActivity.class);
			startActivity(intent);
			break;
		case R.id.btn_quit:
			finish();
			break;
		default:
			break;
		}
	}

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSendResult(byte[] msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyConnectChanged() {
		Log.e("ArbiterLiu", "notifyConnectChanged");
		mSockMessageHandler.obtainMessage(SocketMessage.MSG_SOCKET_CONNECTED)
				.sendToTarget();
	}
}
