package com.dreamlink.communication.client;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.Search;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketMessage;
import com.dreamlink.communication.AppListActivity;
import com.dreamlink.communication.client.SearchSever.OnSearchListener;
import com.dreamlink.communication.server.SocketServerTask;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;
import com.dreamlink.communication.wifip2p.WifiDirectManager;
import com.dreamlink.communication.wifip2p.WifiDirectReciver;
import com.dreamlink.communication.wifip2p.WifiDirectReciver.WifiDirectDeviceNotify;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

@TargetApi(14)
public class ServerListActivity extends Activity implements OnSearchListener,
		OnItemClickListener, OnClickListener, WifiDirectDeviceNotify {
	private static final String TAG = "ServerListActivity";
	private Context mContext;

	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mServer = new ArrayList<String>();
	private ListView mListView;
	private ListView directListView;
	private ArrayList<String> directList;
	private ArrayAdapter<String> directAdapter;
	private ArrayList<WifiP2pDevice> deviceList;
	private Notice mNotice;
	private SearchSever mSearchServer;
	private SocketCommunicationManager mCommunicationManager;
	private WifiDirectManager mWifiDirectManager;
	private WifiDirectReciver wifiDirectReciver;
	private WifiManager mWifiManager;
	private WifiP2pInfo info;

	private static final int MSG_SEARCH_SUCCESS = 1;
	private static final int MSG_SEARCH_FAIL = 2;
	private ProgressDialog progressDialog;

	private Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEARCH_SUCCESS:
				addServer((String) msg.obj, SERVER_TYPE_IP);
				break;
			case MSG_SEARCH_FAIL:

				break;
			case 90:
				directList.clear();
				directList.addAll((ArrayList<String>) msg.obj);
				directAdapter.notifyDataSetChanged();
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
		directListView = (ListView) findViewById(R.id.list_direct);
		directList = new ArrayList<String>();
		directAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, directList);
		directListView.setAdapter(directAdapter);
		directListView.setOnItemClickListener(new OnItemClickListener() {

			@TargetApi(14)
			@SuppressLint({ "NewApi", "NewApi", "NewApi", "NewApi" })
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				WifiP2pConfig config = new WifiP2pConfig();
				config.deviceAddress = deviceList.get(arg2).deviceAddress;
				config.groupOwnerIntent = 0;
				mWifiDirectManager.connect(config);
				progressDialog = new ProgressDialog(ServerListActivity.this);
				progressDialog.setTitle("Click to cancel it");
				progressDialog.setMessage("Waiting for connect...");
				progressDialog.show();
			}
		});
		directListView.setVisibility(View.VISIBLE);
		Button startButton = (Button) findViewById(R.id.btn_start);
		startButton.setOnClickListener(this);
		Button quitButton = (Button) findViewById(R.id.btn_quit);
		quitButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_start:
			if (mWifiDirectManager != null) {
				mWifiDirectManager.stopSearch();
				mWifiDirectManager.unRegisterObserver(this);
				this.unregisterReceiver(mWifiDirectManager.mWifiDirectRecevier);
				mWifiDirectManager = null;
			}
			launchFunction(false);
			break;
		case R.id.btn_quit:
			finish();
			break;
		default:
			break;
		}
	}

	private void launchFunction(boolean WifiP2p) {
		Intent intent = new Intent();
		intent.putExtra(AppListActivity.EXTRA_IS_SERVER, false);
		intent.setClass(this, AppListActivity.class);
		if (WifiP2p) {
			intent.putExtra("WifiP2p", WifiP2p);
		}
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
		forWifiP2p(false);
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
		if (mWifiDirectManager != null) {
			this.unregisterReceiver(wifiDirectReciver);
			mWifiDirectManager.unRegisterObserver(this);
			mWifiDirectManager.stopSearch();
			mWifiDirectManager = null;
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
		launchFunction(false);
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
			forWifiP2p(false);
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

	@TargetApi(14)
	@SuppressLint({ "NewApi", "NewApi" })
	@Override
	public void notifyDeviceChange(ArrayList<WifiP2pDevice> serverList) {
		// TODO Auto-generated method stub
		ArrayList<String> temp = new ArrayList<String>();
		if (serverList != null) {
			deviceList = serverList;
			for (WifiP2pDevice device : serverList) {
				temp.add(device.deviceName);
			}
			mHandler.obtainMessage(90, temp).sendToTarget();
		}
	}

	private void forWifiP2p(boolean flag) {
		if (mWifiManager.isWifiEnabled()) {
			if (Build.VERSION.SDK_INT >= 14) {
				if (mWifiDirectManager != null) {
					mWifiDirectManager.stopSearch();
					mWifiDirectManager.unRegisterObserver(this);
					this.unregisterReceiver(mWifiDirectManager.mWifiDirectRecevier);
					mWifiDirectManager = null;
				}
				mWifiDirectManager = new WifiDirectManager(this, flag);
				wifiDirectReciver = mWifiDirectManager.mWifiDirectRecevier;
				this.registerReceiver(wifiDirectReciver,
						mWifiDirectManager.mIntentFilter);
				mWifiDirectManager.registerObserver(this);
				new Thread() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						super.run();
						mWifiDirectManager.discover();
					}
				}.start();
			}
		}
	}

	@SuppressLint({ "NewApi", "NewApi" })
	@Override
	public void wifiP2pConnected(WifiP2pInfo info) {
		// TODO Auto-generated method stub
		Log.e("ArbiterLiu", "" + info.isGroupOwner);
		if (this.info == null) {
			this.info = info;

			if (info.isGroupOwner) {
				SocketServerTask serverTask = new SocketServerTask(mContext,
						mCommunicationManager);
				serverTask.execute(new String[] { SocketCommunication.PORT });
			} else {
				SocketClientTask clientTask = new SocketClientTask(mContext,
						mCommunicationManager,
						SocketMessage.MSG_SOCKET_CONNECTED);
				clientTask.execute(new String[] {
						info.groupOwnerAddress.getHostAddress(),
						SocketCommunication.PORT });
				if (mWifiDirectManager != null) {
					mWifiDirectManager.stopSearch();
					mWifiDirectManager.unRegisterObserver(this);
					this.unregisterReceiver(mWifiDirectManager.mWifiDirectRecevier);
					mWifiDirectManager = null;
				}
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				launchFunction(true);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.server_list, menu);
		if (Build.VERSION.SDK_INT < 14) {
			menu.removeItem(R.id.wifip2p_on);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (mWifiManager.isWifiEnabled()) {
			if (mWifiDirectManager != null) {
				mWifiDirectManager.stopSearch();
				mWifiDirectManager.unRegisterObserver(this);
				this.unregisterReceiver(mWifiDirectManager.mWifiDirectRecevier);
				mWifiDirectManager = null;
			}
			forWifiP2p(true);
			// directListView.setVisibility(View.GONE);
			directList.clear();
		} else {
			Toast.makeText(this, "Please waiting for wifi on",
					Toast.LENGTH_SHORT).show();
		}
		return super.onOptionsItemSelected(item);
	}

}
