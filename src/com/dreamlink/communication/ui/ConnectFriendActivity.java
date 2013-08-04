package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.CallBacks.ILoginRespondCallback;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.search.SearchProtocol.OnSearchListener;
import com.dreamlink.communication.search.Search;
import com.dreamlink.communication.search.SearchSever;
import com.dreamlink.communication.search.WiFiNameEncryption;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;
import com.dreamlink.communication.wifip2p.WifiDirectManager;
import com.dreamlink.communication.wifip2p.WifiDirectManager.ManagerP2pDeivce;
import com.dreamlink.communication.wifip2p.WifiDirectReciver;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/***
 * two 乱了，wait good 方法
 */
public class ConnectFriendActivity extends Activity implements OnClickListener, OnSearchListener,
					OnCommunicationListener, ILoginRespondCallback, ManagerP2pDeivce {
	private static final String TAG = ConnectFriendActivity.class.getName();
	private ImageView mCloseBtn;
	private LinearLayout mServerListLayout;
	private RelativeLayout mSearchingLayout;
	private RelativeLayout mNotFoundLayout;
	private Button createConnectBtn;
	
	private ListView mServerListView;//show server list
	
	private Notice mNotice;
	private Context mContext;
	
	private ServerAdapter mServerAdapter;
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
	/** Server type IP, value is IP. Server type AP, value is AP SSID */
	private static final String KEY_VALUE = "value";
	/** Server is a WiFi STA */
	private static final int SERVER_TYPE_IP = 1;
	/** Server is a WiFi AP */
	private static final int SERVER_TYPE_AP = 2;
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
	
	private static final int SEARCHING = 0;
	private static final int SEARCHED = 1;
	private static final int NOT_SEARCHED = 2;
	
	private Handler mHandler = new Handler() {

		@SuppressWarnings("unchecked")
		public void handleMessage(android.os.Message msg) {
			updateUI(SEARCHED);
			switch (msg.what) {
			case MSG_SEARCH_SUCCESS:
				Bundle bundle = msg.getData();
				addServer(bundle.getString(KEY_NAME), SERVER_TYPE_IP,
						bundle.getString(KEY_VALUE));
				break;
			case MSG_SEARCH_FAIL:
				mNotice.showToast("Search failed");
				break;
			case MSG_CONNECT_SERVER:
//				connectServer((String) msg.obj);
//				mIsAPSelected = false;
//				if (WifiP2pServer) {
//					mSearchServer.stopSearch();
//					mServerData.clear();
//					return;
//				}
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
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_connect_friend);
		mContext =this;
		mNotice = new Notice(mContext);
		initViews();
		
		//init search server instance
		mSearchServer = SearchSever.getInstance(this);
		mSearchServer.setOnSearchListener(this);
		
		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mCommunicationManager.registered(this);
		mCommunicationManager.setLoginRespondCallback(this);
		
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		setWifiEnabled(true);
		initReceiver();
		
		mIsAPSelected = false;
		
		mUserManager = UserManager.getInstance();
		UserHelper userHelper = new UserHelper(mContext);
		User localUser = userHelper.loadUser();
		mUserManager.setLocalUser(localUser);
	}
	
	private void initViews() {
		mCloseBtn = (ImageView) findViewById(R.id.close_imageview);
//		mServerListLayout = (LinearLayout) findViewById(R.id.list_layout);
		mSearchingLayout = (RelativeLayout) findViewById(R.id.search_layout);
		mNotFoundLayout = (RelativeLayout) findViewById(R.id.not_found_layout);
		createConnectBtn = (Button) findViewById(R.id.create_connect_btn);

		mServerListView = (ListView) findViewById(R.id.server_listview);
		mCloseBtn.setOnClickListener(this);
		createConnectBtn.setOnClickListener(this);
		
//		mServerAdapter = new SimpleAdapter(this, mServerData,
//				android.R.layout.simple_list_item_1, new String[] { KEY_NAME },
//				new int[] { android.R.id.text1 });
		mServerAdapter = new ServerAdapter(mContext, mServerData);
		mServerListView.setAdapter(mServerAdapter);
		updateUI(SEARCHING);
	}
	
	private void updateUI(int type){
		if (SEARCHING == type) {//searching
			mSearchingLayout.setVisibility(View.VISIBLE);
//			mServerListLayout.setVisibility(View.INVISIBLE);
			mServerListView.setVisibility(View.GONE);
			mNotFoundLayout.setVisibility(View.GONE);
		}else if (SEARCHED == type) {//found
			mSearchingLayout.setVisibility(View.GONE);
//			mServerListLayout.setVisibility(View.VISIBLE);
			mServerListView.setVisibility(View.VISIBLE);
			mNotFoundLayout.setVisibility(View.GONE);
		}else if (NOT_SEARCHED == type) {//not found
			mSearchingLayout.setVisibility(View.GONE);
//			mServerListLayout.setVisibility(View.GONE);
			mServerListView.setVisibility(View.GONE);
			mNotFoundLayout.setVisibility(View.VISIBLE);
		}
	}
	
	private void initReceiver() {
		mWiFiFilter = new IntentFilter();
		mWiFiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		mWiFiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mWiFiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		
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
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		forWifiP2p(false);
		this.registerReceiver(wifiDirectReciver, mWifiDirectIntentFilter);
		registerReceiver(mWifiBroadcastReceiver, mWiFiFilter);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		unregisterReceiverSafe(mWifiBroadcastReceiver);
		unregisterReceiverSafe(wifiDirectReciver);
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
	
	/**
	 * add found server to server list. If server type is IP, just add and wait
	 * user to choose. If server is AP, show the user name.
	 * 
	 * @param name user name.
	 * @param type
	 * @param value
	 *            Server type IP, value is IP. Server type AP, value is AP SSID.
	 */
	private void addServer(String name, int type, String value) {
		Log.i(TAG, "addServer.name:" + name + "-->type:" + type);
		switch (type) {
		case SERVER_TYPE_IP:
			if (isServerAlreadyAdded(name, value)) {
				Log.d(TAG, "addServer()	ignore, name = " + name);
				break;
			}
			if (Search.ANDROID_AP_ADDRESS.equals(value)) {
				Log.d(TAG, "This ip is android wifi ap, ignore, name = " + name);
				break;
			}
			// This device is connected to WiFi, So add the server IP.
			HashMap<String, Object> ipServer = new HashMap<String, Object>();
			ipServer.put(KEY_NAME, name);
			ipServer.put(KEY_TYPE, SERVER_TYPE_IP);
			ipServer.put(KEY_VALUE, value);
			mServerData.add(ipServer);
			Log.i(TAG, "type:" + type + "    mServerData.size:" + mServerData.size());
//			mServerListView.setAdapter(mServerAdapter);
			mServerAdapter.notifyDataSetChanged();
			break;
		case SERVER_TYPE_AP:
			if (isServerAlreadyAdded(name, value)) {
				// TODO if two server has the same name, How to do?
				Log.d(TAG, "addServer()	ignore, name = " + name);
				return;
			}
			// Found a AP, add the user name to the server list.
			HashMap<String, Object> apServer = new HashMap<String, Object>();
			apServer.put(KEY_NAME, name);
			apServer.put(KEY_TYPE, SERVER_TYPE_AP);
			apServer.put(KEY_VALUE, value);
			mServerData.add(apServer);
			Log.i(TAG, "type:" + type + "    mServerData.size:" + mServerData.size());
//			mServerListView.setAdapter(mServerAdapter);
			mServerAdapter.notifyDataSetChanged();
		default:
			break;
		}
	}

	private boolean isServerAlreadyAdded(String name, String ip) {
		for (Map<String, Object> map : mServerData) {
			if (name.equals(map.get(KEY_NAME)) && ip.equals(map.get(KEY_VALUE))) {
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
	
	private IntentFilter mWiFiFilter;
	private BroadcastReceiver mWifiBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.i(TAG, "wifi broadcast.action=" + action);
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
		updateUI(SEARCHED);
		final List<ScanResult> results = mWifiManager.getScanResults();
		if (results != null) {
			for (ScanResult result : results) {
				Log.d(TAG, "handleScanReuslt, found wifi: " + result.SSID);
				if (WiFiNameEncryption.checkWiFiName(result.SSID)) {
					addServer(WiFiNameEncryption.getUserName(result.SSID),
							SERVER_TYPE_AP, result.SSID);
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
				NetWorkUtil.setWifiAPEnabled(mContext,
						null + UserHelper.getUserName(mContext), false);
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.close_imageview:
			ConnectFriendActivity.this.finish();
			break;
		case R.id.create_connect_btn:
			LayoutInflater inflater = LayoutInflater.from(ConnectFriendActivity.this);
			View view = inflater.inflate(R.layout.ui_create_server, null);
			final RadioButton wifiButton = (RadioButton) view.findViewById(R.id.radio_wifi);
			final RadioButton wifiApButton = (RadioButton) view.findViewById(R.id.radio_wifi_ap);
			final RadioButton wifiDirectButton = (RadioButton) view.findViewById(R.id.radio_wifi_direct);
			
			new AlertDialog.Builder(ConnectFriendActivity.this)
				.setTitle("Please choose the server type")
				.setView(view)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						String msg = "";
						if (wifiButton.isChecked()) {
							msg = "To Create Wifi Server";
						}else if (wifiApButton.isChecked()) {
							msg = "To Create Wifi AP Server";
						}else {
							msg = "To Create Wifi Direct Server";
						}
						Toast.makeText(ConnectFriendActivity.this, msg, Toast.LENGTH_SHORT).show();
						Intent intent = new Intent();
						//data
						setResult(RESULT_OK, intent);
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						setResult(RESULT_CANCELED);
					}
				})
				.create().show();
			break;
		default:
			break;
		}
	}

	@Override
	public void onSearchSuccess(String serverIP, String serverName) {
		// TODO Auto-generated method stub
		if (mIsAPSelected && serverIP.equals(Search.ANDROID_AP_ADDRESS)) {
			// Auto connect to the server.
			Message message = mHandler.obtainMessage(MSG_CONNECT_SERVER);
			message.obj = serverIP;
			mHandler.sendMessage(message);
		} else {
			Log.i(TAG, "serverIp:" + serverIP + "-->serverName:" + serverName);
			// Add to server list and wait user for choose.
			Message message = mHandler.obtainMessage(MSG_SEARCH_SUCCESS);
			Bundle bundle = new Bundle();
			bundle.putString(KEY_NAME, serverName);
			bundle.putString(KEY_VALUE, serverIP);
			message.setData(bundle);
			mHandler.sendMessage(message);
		}
	}

	@Override
	public void onSearchStop() {
		// TODO Auto-generated method stub
		Message message = mHandler.obtainMessage(MSG_SEARCH_FAIL);
		mHandler.sendMessage(message);
	}

	@Override
	public void onLoginSuccess(User localUser, SocketCommunication communication) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLoginFail(int failReason, SocketCommunication communication) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication communication) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSendResult(byte[] msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyConnectChanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deviceChange(ArrayList<WifiP2pDevice> list) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void hasConnect(WifiP2pInfo info) {
		// TODO Auto-generated method stub
		
	}
	
}
