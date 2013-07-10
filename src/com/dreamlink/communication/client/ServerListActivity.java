package com.dreamlink.communication.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.AppListActivity;
import com.dreamlink.communication.CallBacks.ILoginRespondCallback;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.search.Search;
import com.dreamlink.communication.search.SearchProtocol.OnSearchListener;
import com.dreamlink.communication.search.SearchSever;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;
import com.dreamlink.communication.wifip2p.WifiDirectManager;
import com.dreamlink.communication.wifip2p.WifiDirectManager.ManagerP2pDeivce;
import com.dreamlink.communication.wifip2p.WifiDirectReciver;

/**
 * This class is used for search server in the WiFi network.</br>
 * 
 * It can search WiFi AP server, WiFi STA server and WiFi direct server. </br>
 * 
 */
@TargetApi(14)
public class ServerListActivity extends Activity implements OnSearchListener,
		OnItemClickListener, OnClickListener, ManagerP2pDeivce,
		OnCommunicationListener, ILoginRespondCallback {
	private static final String TAG = "ServerListActivity";
	private Context mContext;

	/**
	 * Map structure: </br>
	 * 
	 * KEY_NAME - server name</br>
	 * 
	 * KEY_TYPE - server network type: IP, AP, WiFi Direct</br>
	 * 
	 * KEY_IP - server IP. This is only used in WiFi network.
	 */
	private Vector<Map<String, Object>> mServerData = new Vector<Map<String, Object>>();
	/** Server name */
	private static final String KEY_NAME = "name";
	/** Server type */
	private static final String KEY_TYPE = "type";
	/** Server ip */
	private static final String KEY_IP = "ip";
	/** Server is a WiFi STA */
	private static final int SERVER_TYPE_IP = 1;
	/** Server is a WiFi AP */
	private static final int SERVER_TYPE_AP = 2;
	private SimpleAdapter mServerAdapter;
	/** Flag for decide to auto connect AP or not. */
	private boolean mIsAPSelected = false;
	/** WiFi network server ListView */
	private ListView mListView;
	private WifiManager mWifiManager;

	/** WiFi network server ListView */
	private ListView directListView;
	private ArrayList<String> directList;
	private ArrayAdapter<String> directAdapter;
	private ArrayList<WifiP2pDevice> deviceList;
	private WifiDirectManager mWifiDirectManager;
	private WifiDirectReciver wifiDirectReciver;
	private IntentFilter mWifiDirectIntentFilter;
	private ProgressDialog progressDialog;

	private Notice mNotice;

	private SearchSever mSearchServer;
	private SocketCommunicationManager mCommunicationManager;

	private UserManager mUserManager;

	private static final int MSG_SEARCH_SUCCESS = 1;
	private static final int MSG_SEARCH_FAIL = 2;
	/** Connect to the server and launch app list activity. */
	private static final int MSG_CONNECT_SERVER = 3;
	private static final int MSG_SEARCH_WIFI_DIRECT_FOUND = 4;
	private static final int MSG_LOGIN_REQEUST = 5;
	private boolean WifiP2pServer = false;

	// 监听返回键，断开一切连接
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (Build.VERSION.SDK_INT >= 14) {
				if (mWifiDirectManager != null) {
					mWifiDirectManager.stopConnect();
				}
			}
			if (mCommunicationManager != null) {
				mCommunicationManager.closeCommunication();
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private Handler mHandler = new Handler() {

		@SuppressWarnings("unchecked")
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEARCH_SUCCESS:
				Bundle bundle = msg.getData();
				addServer(bundle.getString(KEY_NAME), SERVER_TYPE_IP,
						bundle.getString(KEY_IP));
				break;
			case MSG_SEARCH_FAIL:

				break;
			case MSG_CONNECT_SERVER:
				connectServer((String) msg.obj);
				mIsAPSelected = false;
				if (WifiP2pServer) {
					mSearchServer.stopSearch();
					mServerData.clear();
					return;
				}
				break;
			case MSG_SEARCH_WIFI_DIRECT_FOUND:
				directList.clear();
				mServerData.clear();
				directList.addAll((ArrayList<String>) msg.obj);
				directAdapter.notifyDataSetChanged();
				if (WifiP2pServer) {
					directListView.setEnabled(false);
				} else {
					directListView.setEnabled(true);
				}
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
		mSearchServer = SearchSever.getInstance(this);
		mSearchServer.setOnSearchListener(this);
		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mCommunicationManager.registered(this);
		mCommunicationManager.setLoginRespondCallback(this);
		mNotice.showToast("Start Search");
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		setWifiEnabled(true);
		mWiFiFilter = new IntentFilter();
		mWiFiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		mWiFiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mWiFiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		initReceiver();

		mIsAPSelected = false;

		mUserManager = UserManager.getInstance();
		UserHelper userHelper = new UserHelper(mContext);
		User localUser = userHelper.loadUser();
		mUserManager.setLocalUser(localUser);
	}

	/**
	 * add found server to server list. If server type is IP, just add and wait
	 * user to choose. If server is AP, show the user name.
	 * 
	 * @param name
	 * @param type
	 * @param ip
	 */
	private void addServer(String name, int type, String ip) {
		switch (type) {
		case SERVER_TYPE_IP:
			if (isServerAlreadyAdded(name, ip)) {
				Log.d(TAG, "addServer()	ignore, name = " + name);
				break;
			}
			if (name.equals(Search.ANDROID_AP_ADDRESS)) {
				Log.d(TAG, "This ip is android wifi ap, ignore, name = " + name);
				break;
			}
			// This device is connected to WiFi, So add the server IP.
			HashMap<String, Object> ipServer = new HashMap<String, Object>();
			ipServer.put(KEY_NAME, name);
			ipServer.put(KEY_TYPE, SERVER_TYPE_IP);
			ipServer.put(KEY_IP, ip);
			mServerData.add(ipServer);
			mServerAdapter.notifyDataSetChanged();
			break;
		case SERVER_TYPE_AP:
			if (isServerAlreadyAdded(apName2UserName(name))) {
				// TODO if two server has the same name, How to do?
				Log.d(TAG, "addServer()	ignore, name = " + name);
				return;
			}
			// Found a AP, add the user name to the server list.
			HashMap<String, Object> apServer = new HashMap<String, Object>();
			apServer.put(KEY_NAME, apName2UserName(name));
			apServer.put(KEY_TYPE, SERVER_TYPE_AP);
			mServerData.add(apServer);
			mServerAdapter.notifyDataSetChanged();
		default:
			break;
		}
	}

	private boolean isServerAlreadyAdded(String name, String ip) {
		for (Map<String, Object> map : mServerData) {
			if (name.equals(map.get(KEY_NAME)) && ip.equals(map.get(KEY_IP))) {
				// The server is already added to list.
				return true;
			}
		}
		return false;
	}

	private void clearServerList() {
		mServerData.clear();
		mServerAdapter.notifyDataSetChanged();
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
			// Disable WiFi.
			if (mWifiManager.isWifiEnabled()) {
				mWifiManager.setWifiEnabled(false);
			}
		}
	}

	private void initView() {
		mListView = (ListView) findViewById(R.id.list_client);
		mServerAdapter = new SimpleAdapter(this, mServerData,
				android.R.layout.simple_list_item_1, new String[] { KEY_NAME },
				new int[] { android.R.id.text1 });
		mListView.setAdapter(mServerAdapter);
		// mAdapter = new ArrayAdapter<String>(mContext,
		// android.R.layout.simple_list_item_1, mServer);
		// mListView.setAdapter(mAdapter);
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
				wifiDirectReciver.unRegisterObserver(mWifiDirectManager);
				mWifiDirectManager.unRegisterObserver(this);
				mWifiDirectManager = null;
			}
			launchAppList();
			// TODO finish it to avoid connect repeat.
			finish();
			break;
		case R.id.btn_quit:
			finish();
			break;
		default:
			break;
		}
	}

	/***
	 * launch app list activity.
	 */
	private void launchAppList() {
		Intent intent = new Intent();
		intent.putExtra(AppListActivity.EXTRA_IS_SERVER, false);
		intent.setClass(this, AppListActivity.class);
		startActivity(intent);
	}

	/**
	 * catch broadcast not register exception.
	 * 
	 * @param receiver
	 */
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
		this.registerReceiver(wifiDirectReciver, mWifiDirectIntentFilter);
		registerReceiver(mWifiBroadcastReceiver, mWiFiFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiverSafe(mWifiBroadcastReceiver);
		unregisterReceiverSafe(wifiDirectReciver);
	}

	@SuppressLint("HandlerLeak")
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mSearchServer != null) {
			mSearchServer.stopSearch();
		}
		mCommunicationManager.unregistered(this);
		if (mWifiDirectManager != null) {
			mWifiDirectManager.unRegisterObserver(this);
			mWifiDirectManager.stopSearch();
			wifiDirectReciver.unRegisterObserver(mWifiDirectManager);
			mWifiDirectManager = null;
		}
	}

	/**
	 * Check whether the server is already added to list or not.
	 * 
	 * @param serverName
	 * @return
	 */
	private boolean isServerAlreadyAdded(String serverName) {
		for (Map<String, Object> map : mServerData) {
			if (serverName.equals(map.get(KEY_NAME))) {
				// The server is already added to list.
				return true;
			}
		}
		return false;
	}

	@SuppressLint("HandlerLeak")
	@Override
	public void onSearchSuccess(String serverIP, String serverName) {
		if (mIsAPSelected && serverIP.equals(Search.ANDROID_AP_ADDRESS)) {
			// Auto connect to the server.
			Message message = mHandler.obtainMessage(MSG_CONNECT_SERVER);
			message.obj = serverIP;
			mHandler.sendMessage(message);
		} else {
			// Add to server list and wait user for choose.
			Message message = mHandler.obtainMessage(MSG_SEARCH_SUCCESS);
			Bundle bundle = new Bundle();
			bundle.putString(KEY_NAME, serverName);
			bundle.putString(KEY_IP, serverIP);
			message.setData(bundle);
			mHandler.sendMessage(message);
		}
	}

	@Override
	public void onSearchStop() {
		Message message = mHandler.obtainMessage(MSG_SEARCH_FAIL);
		mHandler.sendMessage(message);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long arg3) {
		HashMap<String, Object> server = (HashMap<String, Object>) mServerData
				.get(position);
		int type = (Integer) server.get(KEY_TYPE);
		switch (type) {
		case SERVER_TYPE_AP:
			mIsAPSelected = true;
			String apName = (String) server.get(KEY_NAME);
			connetAP(userName2ApName(apName));
			break;
		case SERVER_TYPE_IP:
			String ip = (String) server.get(KEY_IP);
			connectServer(ip);
			if (!WifiP2pServer) {
				if (mWifiDirectManager != null) {
					mWifiDirectManager.stopSearch();
					wifiDirectReciver.unRegisterObserver(mWifiDirectManager);
					mWifiDirectManager.unRegisterObserver(this);
					mWifiDirectManager = null;
				}
			}
			// launchAppList();
			// TODO finish it to avoid connect repeat.
			// finish();
			break;

		default:
			break;
		}
	}

	/**
	 * {@link #MSG_CONNECT_SERVER}
	 * 
	 * @param ip
	 */
	private void connectServer(String ip) {
		mCommunicationManager.connectServer(mContext, ip);

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
		clearServerList();
		final List<ScanResult> results = mWifiManager.getScanResults();
		if (results != null) {
			for (ScanResult result : results) {
				Log.d(TAG, "handleScanReuslt, found wifi: " + result.SSID);
				if (result.SSID.contains(Search.WIFI_AP_NAME)) {
					addServer(result.SSID, SERVER_TYPE_AP,
							Search.ANDROID_AP_ADDRESS);
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

	private void forWifiP2p(boolean flag) {
		if (mWifiManager.isWifiEnabled()) {
			if (WifiP2pServer != flag) {
				flag = WifiP2pServer;
			}
			if (Build.VERSION.SDK_INT >= 14) {
				if (Build.VERSION.SDK_INT >= 14) {
					if (mWifiDirectManager != null) {
						mWifiDirectManager.setServerFlag(flag);
						return;
					}
					mWifiDirectManager = new WifiDirectManager(this, flag);
					mWifiDirectManager.registerObserver(this);
					wifiDirectReciver.registerObserver(mWifiDirectManager);
					new Thread() {
						@Override
						public void run() {
							super.run();
							mWifiDirectManager.discover();
						}
					}.start();
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.server_list, menu);
		if (Build.VERSION.SDK_INT < 14) {
			menu.removeItem(R.id.wifip2p_on);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mWifiManager.isWifiEnabled()) {
			WifiP2pServer = true;
			forWifiP2p(true);
			// directListView.setVisibility(View.GONE);
			directList.clear();
		} else {
			Toast.makeText(this, "Please waiting for wifi on",
					Toast.LENGTH_SHORT).show();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void deviceChange(ArrayList<WifiP2pDevice> serverList) {
		ArrayList<String> temp = new ArrayList<String>();
		if (WifiP2pServer) {
			return;
		}
		if (serverList != null) {
			deviceList = serverList;
			for (WifiP2pDevice device : serverList) {
				String name = device.deviceName;
				temp.add(name.substring("DreamLink".length()));
			}
			mHandler.obtainMessage(MSG_SEARCH_WIFI_DIRECT_FOUND, temp)
					.sendToTarget();
		}
	}

	@Override
	public void hasConnect(WifiP2pInfo info) {
		Log.e("ArbiterLiu", "" + info.isGroupOwner);

		if (info.isGroupOwner) {
			mCommunicationManager.startServer(mContext);
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
		} else {
			connectServer(info.groupOwnerAddress.getHostAddress());
			if (mWifiDirectManager != null) {
				mWifiDirectManager.stopSearch();
				mWifiDirectManager.unRegisterObserver(this);
				wifiDirectReciver.unRegisterObserver(mWifiDirectManager);
				mWifiDirectManager = null;
			}
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
			launchAppList();
		}
	}

	private void initReceiver() {
		mWifiDirectIntentFilter = new IntentFilter();
		mWifiDirectIntentFilter
				.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		mWifiDirectIntentFilter
				.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mWifiDirectIntentFilter
				.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mWifiDirectIntentFilter
				.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mWifiDirectIntentFilter
				.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		wifiDirectReciver = new WifiDirectReciver();
	}

	/**
	 * Get user name from WiFi AP name.</br>
	 * 
	 * WiFi AP naming rule: WiFi AP name = Search.WIFI_AP_NAME + User name
	 * 
	 * @param SSID
	 *            WiFi AP SSID.
	 * @return
	 */
	private String apName2UserName(String SSID) {
		if (SSID.startsWith(Search.WIFI_AP_NAME)) {
			return SSID.substring(Search.WIFI_AP_NAME.length());
		} else {
			Log.e(TAG, "apName2UserName(), SSID is not match. SSID =" + SSID);
			return "";
		}
	}

	/**
	 * Get WiFi AP name from user name.</br>
	 * 
	 * WiFi AP naming rule: WiFi AP name = Search.WIFI_AP_NAME + User name
	 * 
	 * @param SSID
	 *            WiFi AP SSID.
	 * @return
	 */
	private String userName2ApName(String userName) {
		return Search.WIFI_AP_NAME + userName;
	}

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication ip) {
		// TODO Auto-generated method stub
		// ignore
	}

	@Override
	public void onSendResult(byte[] msg) {
		// TODO Auto-generated method stub
		// ignore
	}

	@Override
	public void notifyConnectChanged() {
		if (WifiP2pServer) {
			mSearchServer.stopSearch();
			ArrayList<String> temp = new ArrayList<String>();
			for (SocketCommunication com : mCommunicationManager
					.getCommunications()) {
				temp.add(com.getConnectIP().getHostName() + "     已连接");
			}
			mHandler.obtainMessage(MSG_SEARCH_WIFI_DIRECT_FOUND, temp)
					.sendToTarget();
		}
		// Not wifi direct.
		// Send login request.
		// TODO notifyConnectChanged should notify the connection is establish
		// or lost.
		// If connection is established, send login request, if lost, do not
		// send login request.
		Log.d(TAG, "Send login request: " + mUserManager.getLocalUser());

		mCommunicationManager.sendLoginRequest();
		Log.d(TAG, "Connect success, waiting for server's login allowance.");
	}

	@Override
	public void onLoginSuccess(User localUser, SocketCommunication communication) {
		Log.d(TAG, "Login sucess");
		launchAppList();
		finish();
	}

	@Override
	public void onLoginFail(int failReason, SocketCommunication communication) {
		Log.d(TAG, "Login fail");
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mNotice.showToast("Server disallowed the login request.");
			}
		});
	}
}
