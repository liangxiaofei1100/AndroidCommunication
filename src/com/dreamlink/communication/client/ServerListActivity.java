package com.dreamlink.communication.client;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.Search;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketMessage;
import com.dreamlink.communication.AppListActivity;
import com.dreamlink.communication.client.SearchSever.OnSearchListener;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class ServerListActivity extends Activity implements OnSearchListener,
		OnItemClickListener, OnClickListener {
	private static final String TAG = "ServerListActivity";
	private Context mContext;

	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mServer = new ArrayList<String>();
	private ListView mListView;

	private Notice mNotice;
	private SearchSever mSearchServer;
	private SocketCommunicationManager mCommunicationManager;

	private WifiManager mWifiManager;

	private static final int MSG_SEARCH_SUCCESS = 1;
	private static final int MSG_SEARCH_FAIL = 2;

	private Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEARCH_SUCCESS:
				addServer((String) msg.obj, SERVER_TYPE_IP);
				break;
			case MSG_SEARCH_FAIL:

				break;

			default:
				break;
			}
		}
	};

	private static final int SERVER_TYPE_IP = 1;
	private static final int SERVER_TYPE_AP = 2;

	/**
	 * add found server to server list. If server type is IP, just add and wait
	 * user to choose. If server is AP, connect to it, Then search will found
	 * the server IP.
	 * 
	 * @param name
	 * @param type
	 */
	private void addServer(String name, int type) {
		switch (type) {
		case SERVER_TYPE_IP:
			// This device is connected to WiFi, So add the server IP.
			mServer.add(name);
			mAdapter.notifyDataSetChanged();
			break;
		case SERVER_TYPE_AP:
			// Found a AP, maybe not connected. So connect to it.After connected
			// to the AP, the search process is the same as connected process.
			connetAP(name);
		default:
			break;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_list);
		mContext = this;
		mNotice = new Notice(mContext);

		initView();

		mSearchServer = SearchSever.getInstance(this);
		mSearchServer.setOnSearchListener(this);

		mCommunicationManager = SocketCommunicationManager.getInstance(this);

		mNotice.showToast("Start Search");

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		setWifiEnabled(true);

		mWiFiFilter = new IntentFilter();
		mWiFiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		mWiFiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mWiFiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
	}

	/**
	 * Enable wifi or disable wifi.
	 * 
	 * @param enable
	 *            ? enable : disable.
	 */
	private void setWifiEnabled(boolean enable) {
		if (enable) {
			// If Android AP is enabled, disable AP.
			if (NetWorkUtil.isAndroidAPNetwork(mContext)) {
				NetWorkUtil.setWifiAPEnabled(mContext, Search.WIFI_AP_NAME
						+ UserHelper.getUserName(mContext), false);
			}
			// Enable WiFi.
			if (!mWifiManager.isWifiEnabled()) {
				mWifiManager.setWifiEnabled(true);
			}
		} else {
			// Enable WiFi.
			if (mWifiManager.isWifiEnabled()) {
				mWifiManager.setWifiEnabled(false);
			}
		}
	}

	private void initView() {
		mListView = (ListView) findViewById(R.id.list_client);
		mAdapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_list_item_1, mServer);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);

		Button startButton = (Button) findViewById(R.id.btn_start);
		startButton.setOnClickListener(this);
		Button quitButton = (Button) findViewById(R.id.btn_quit);
		quitButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_start:
			launchFunction();
			break;
		case R.id.btn_quit:
			finish();
			break;
		default:
			break;
		}
	}

	private void launchFunction() {
		Intent intent = new Intent();
		intent.putExtra(AppListActivity.EXTRA_IS_SERVER, false);
		intent.setClass(this, AppListActivity.class);
		startActivity(intent);
	}

	private void unregisterReceiverSafe(BroadcastReceiver receiver) {
		try {
			unregisterReceiver(receiver);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mWifiBroadcastReceiver, mWiFiFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiverSafe(mWifiBroadcastReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mSearchServer != null) {
			mSearchServer.stopSearch();
		}
		unregisterReceiverSafe(mWifiBroadcastReceiver);
	}

	@Override
	public void onSearchSuccess(String serverIP) {
		if (!mServer.contains(serverIP)) {
			Message message = mHandler.obtainMessage(MSG_SEARCH_SUCCESS);
			message.obj = serverIP;
			mHandler.sendMessage(message);
		}
	}

	@Override
	public void onSearchFail() {
		Message message = mHandler.obtainMessage(MSG_SEARCH_FAIL);
		mHandler.sendMessage(message);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long arg3) {
		String ip = mServer.get(position);
		connectServer(ip);
		launchFunction();
	}

	private void connectServer(String ip) {
		SocketClientTask clientTask = new SocketClientTask(mContext,
				mCommunicationManager, SocketMessage.MSG_SOCKET_CONNECTED);
		clientTask.execute(new String[] { ip, SocketCommunication.PORT });
	}

	public void connetAP(String SSID) {
		Log.d(TAG, "connetAP: " + SSID);
		WifiInfo info = mWifiManager.getConnectionInfo();
		if (info != null) {
			String connectedSSID = info.getSSID();
			if (connectedSSID.equals(SSID)) {
				// Already connected to the ssid ignore.
				Log.d(TAG, "Already connected to the ssid ignore. " + SSID);
				return;
			}
		}

		WifiConfiguration configuration = new WifiConfiguration();
		configuration.SSID = "\"" + SSID + "\"";
		configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		int netId = mWifiManager.addNetwork(configuration);
		mWifiManager.saveConfiguration();
		boolean result = mWifiManager.enableNetwork(netId, true);
		Log.d(TAG, "enable network result: " + result);
	}

	private IntentFilter mWiFiFilter;
	private BroadcastReceiver mWifiBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
				handleWifiStateChanged(intent.getIntExtra(
						WifiManager.EXTRA_WIFI_STATE,
						WifiManager.WIFI_STATE_UNKNOWN));
			} else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
				handleScanReuslt();
			} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
				handleNetworkSate((NetworkInfo) intent
						.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO));
			}

		}
	};

	private void handleNetworkSate(NetworkInfo networkInfo) {
		if (networkInfo.isConnected()) {
			mSearchServer.startSearch();
		} else {
			mSearchServer.stopSearch();
		}
	}

	private void handleScanReuslt() {
		Log.d(TAG, "handleScanReuslt()");
		final List<ScanResult> results = mWifiManager.getScanResults();
		if (results != null) {
			for (ScanResult result : results) {
				Log.d(TAG, "handleScanReuslt, found wifi: " + result.SSID);
				if (result.SSID.contains(Search.WIFI_AP_NAME)) {
					addServer(result.SSID, SERVER_TYPE_AP);
				}
			}
		}
	}

	private void handleWifiStateChanged(int wifiState) {
		switch (wifiState) {
		case WifiManager.WIFI_STATE_ENABLING:
			Log.d(TAG, "WIFI_STATE_ENABLING");
			break;
		case WifiManager.WIFI_STATE_ENABLED:
			Log.d(TAG, "WIFI_STATE_ENABLED");
			Log.d(TAG, "Start WiFi scan.");
			mWifiManager.startScan();
			break;
		case WifiManager.WIFI_STATE_DISABLING:
			Log.d(TAG, "WIFI_STATE_DISABLING");
			break;
		case WifiManager.WIFI_STATE_DISABLED:
			Log.d(TAG, "WIFI_STATE_DISABLED");
			break;

		default:
			break;
		}
	}
}
