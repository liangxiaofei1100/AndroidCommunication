package com.dreamlink.communication.server;

import java.util.ArrayList;
import java.util.Vector;

import com.dreamlink.communication.R;
import com.dreamlink.communication.Search;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.AppListActivity;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.server.SearchClient.OnSearchListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;
import com.dreamlink.communication.wifip2p.WifiDirectManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

/**
 * This class is used for show clients that connected to this server in the WiFi
 * network.</br>
 * 
 * Because client will connect the server, we can listen Communicate manager to
 * see there is new communicate established and get all communications.</br>
 * 
 */
public class ClientListActivity extends Activity implements OnSearchListener,
		OnClickListener, OnCommunicationListener {
	private static final String TAG = "ClientListActivity";
	private Context mContext;

	//����ؼ���ʱ�ȹر�����l��
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			mCommunicationManager.closeCommunication();
		}
		return super.onKeyDown(keyCode, event);
	}

	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mClients = new ArrayList<String>();
	private ListView mListView;

	private Notice mNotice;
	private SearchClient mSearchClient;

	private static final int MSG_SEARCH_SUCCESS = 1;
	private static final int MSG_SEARCH_FAIL = 2;
	private static final int MSG_UPDATE_LIST = 3;

	private SocketCommunicationManager mCommunicationManager;

	private static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
	private static final String EXTRA_WIFI_AP_STATE = "wifi_state";
	private static final int WIFI_AP_STATE_ENABLING = 12;
	private static final int WIFI_AP_STATE_ENABLED = 13;
	private static final int WIFI_AP_STATE_DISABLING = 10;
	private static final int WIFI_AP_STATE_DISABLED = 11;
	private static final int WIFI_AP_STATE_FAILED = 14;
	
	private UserManager mUserManager;

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
			case MSG_UPDATE_LIST:
				mClients.clear();
				Vector<SocketCommunication> vector = mCommunicationManager
						.getCommunications();
				synchronized (vector) {
					for (SocketCommunication com : vector) {
						mClients.add(com.getConnectIP().getHostAddress());
					}
				}
				mAdapter.notifyDataSetChanged();
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
		// mCommunicationManager.startServer(mContext);

		mSearchClient = SearchClient.getInstance(this);
		mSearchClient.setOnSearchListener(this);
		mNotice.showToast("Start Search");

		NetWorkUtil.setWifiAPEnabled(mContext,
				Search.WIFI_AP_NAME + UserHelper.getUserName(mContext), true);

		IntentFilter filter = new IntentFilter();
		filter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
		registerReceiver(mBroadcastReceiver, filter);
		
		mUserManager = UserManager.getInstance();
		mUserManager.addLocalServerUser();
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

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
				mSearchClient.stopSearch();
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

	@Override
	protected void onResume() {
		super.onResume();
		mSearchHandler.sendEmptyMessage(MSG_UPDATE_LIST);
		mCommunicationManager.startServer(mContext);
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

	@Override
	public void onSearchSuccess(String serverIP) {
		Message message = mSearchHandler.obtainMessage(MSG_SEARCH_SUCCESS);
		message.obj = serverIP;
		mSearchHandler.sendMessage(message);
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
		mSearchHandler.sendEmptyMessage(MSG_UPDATE_LIST);
	}
}
