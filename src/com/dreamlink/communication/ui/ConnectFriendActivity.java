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
import com.dreamlink.communication.server.service.ConnectHelper;
import com.dreamlink.communication.server.service.ServerInfo;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;
import com.dreamlink.communication.wifip2p.WifiDirectManager;
import com.dreamlink.communication.wifip2p.WifiDirectManager.ManagerP2pDeivce;
import com.dreamlink.communication.wifip2p.WifiDirectReciver;

import android.annotation.TargetApi;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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
 * use this class to search server and connect
 */
@TargetApi(14)
public class ConnectFriendActivity extends Activity implements OnClickListener,
		OnSearchListener {
	private static final String TAG = ConnectFriendActivity.class.getName();
	private ImageView mCloseBtn;
	private RelativeLayout mSearchingLayout;
	private RelativeLayout mNotFoundLayout;
	private Button createConnectBtn;

	private ListView mServerListView;// show server list

	private Notice mNotice;
	private Context mContext;
	private boolean sever_flag = false;

	private ServerAdapter mServerAdapter;

	/**
	 * save server data Map structure: </br>
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
	private WifiManager mWifiManager;

	private UserManager mUserManager;

	private static final int MSG_SEARCH_SUCCESS = 1;
	private static final int MSG_SEARCH_FAIL = 2;
	/** Connect to the server and launch app list activity. */
	private static final int MSG_CONNECT_SERVER = 3;
	private static final int MSG_SEARCH_WIFI_DIRECT_FOUND = 4;
	private static final int MSG_LOGIN_REQEUST = 5;

	private static final int SEARCHING = 0;
	private static final int SEARCHED = 1;
	private static final int NOT_SEARCHED = 2;

	private ConnectHelper connectHelper;

	private Handler mHandler = new Handler() {

		@SuppressWarnings("unchecked")
		public void handleMessage(android.os.Message msg) {
			updateUI(SEARCHED);
			switch (msg.what) {
			case MSG_SEARCH_SUCCESS:
				addServer((ServerInfo) msg.obj);
				break;
			case MSG_SEARCH_FAIL:
				mNotice.showToast("Search failed");
				break;
			case MSG_CONNECT_SERVER:
				ServerInfo info = (ServerInfo) msg.obj;
				connectHelper.connenctToServer(info);
				if (!info.getServer_type().equals("wifi-ap")) {
					finish();
				}
				break;
			case MSG_SEARCH_WIFI_DIRECT_FOUND:
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
		mContext = this;
		mNotice = new Notice(mContext);
		connectHelper = ConnectHelper.getInstance(getApplicationContext());
		initViews();

		mUserManager = UserManager.getInstance();
		UserHelper userHelper = new UserHelper(mContext);
		User localUser = userHelper.loadUser();
		mUserManager.setLocalUser(localUser);
	}

	private void initViews() {
		mCloseBtn = (ImageView) findViewById(R.id.close_imageview);
		mSearchingLayout = (RelativeLayout) findViewById(R.id.search_layout);
		mNotFoundLayout = (RelativeLayout) findViewById(R.id.not_found_layout);
		createConnectBtn = (Button) findViewById(R.id.create_connect_btn);

		mServerListView = (ListView) findViewById(R.id.server_listview);
		mCloseBtn.setOnClickListener(this);
		createConnectBtn.setOnClickListener(this);
		mServerListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				ServerInfo info = (ServerInfo) mServerData.get(arg2).get(
						KEY_VALUE);
				if (info.getServer_type().equals("wifi-ap"))
					mIsAPSelected = true;
				mHandler.obtainMessage(MSG_CONNECT_SERVER, info).sendToTarget();
				// clearServerList();
			}
		});
		mServerAdapter = new ServerAdapter(mContext, mServerData);
		mServerListView.setAdapter(mServerAdapter);
		updateUI(SEARCHING);
	}

	private void updateUI(int type) {
		if (SEARCHING == type) {// searching
			mSearchingLayout.setVisibility(View.VISIBLE);
			mServerListView.setVisibility(View.GONE);
			mNotFoundLayout.setVisibility(View.GONE);
		} else if (SEARCHED == type) {// found
			mSearchingLayout.setVisibility(View.GONE);
			mServerListView.setVisibility(View.VISIBLE);
			mNotFoundLayout.setVisibility(View.GONE);
		} else if (NOT_SEARCHED == type) {// not found
			mSearchingLayout.setVisibility(View.GONE);
			mServerListView.setVisibility(View.GONE);
			mNotFoundLayout.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		connectHelper.searchServer(ConnectFriendActivity.this);
		// connectHelper.searchDirectServer(ConnectFriendActivity.this, null);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	/**
	 * catch broadcast not register exception. <<<<<<< HEAD =======
	 * 
	 * >>>>>>> 35a589fa38c575069fd510e9f35f9256b4b336d7
	 * 
	 * @param receiver
	 */
	@SuppressWarnings("unused")
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
	 * @param name
	 *            user name.
	 * @param type
	 * @param value
	 *            Server type IP, value is IP. Server type AP, value is AP SSID.
	 */
	@SuppressWarnings("unused")
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
			Log.i(TAG,
					"type:" + type + "    mServerData.size:"
							+ mServerData.size());
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
			Log.i(TAG,
					"type:" + type + "    mServerData.size:"
							+ mServerData.size());
			mServerAdapter.notifyDataSetChanged();
		default:
			break;
		}
	}

	private void addServer(ServerInfo info) {
		if (info.getServer_type().equals("wifi")) {
			if (isServerAlreadyAdded(info.getServer_name(), info.getServer_ip())) {
				Log.d(TAG,
						"addServer()	ignore, name = " + info.getServer_name());
				return;
			}
			if (Search.ANDROID_AP_ADDRESS.equals(info.getServer_ip())) {
				Log.d(TAG,
						"This ip is android wifi ap, ignore, name = "
								+ info.getServer_name());
			}
			// This device is connected to WiFi, So add the server IP.
			HashMap<String, Object> ipServer = new HashMap<String, Object>();
			ipServer.put(KEY_NAME, info.getServer_name());
			ipServer.put(KEY_TYPE, info.getServer_ip());
			ipServer.put(KEY_VALUE, info);
			mServerData.add(ipServer);
			Log.i(TAG, "type:" + info.getServer_type()
					+ "    mServerData.size:" + mServerData.size());
			mServerAdapter.notifyDataSetChanged();
		} else if (info.getServer_type().equals("wifi-ap")) {
			if (isServerAlreadyAdded(info.getServer_name(),
					info.getServer_ssid())) {
				// TODO if two server has the same name, How to do?
				Log.d(TAG,
						"addServer()	ignore, name = " + info.getServer_name());
				return;
			}
			// Found a AP, add the user name to the server list.
			HashMap<String, Object> apServer = new HashMap<String, Object>();
			apServer.put(KEY_NAME, info.getServer_name());
			apServer.put(KEY_TYPE, info.getServer_ssid());
			apServer.put(KEY_VALUE, info);
			mServerData.add(apServer);
			Log.i(TAG, "type:" + info.getServer_type()
					+ "    mServerData.size:" + mServerData.size());
			mServerAdapter.notifyDataSetChanged();
		} else {
			if (isServerAlreadyAdded(info.getServer_name(),
					info.getServer_device().deviceAddress)) {
				Log.d(TAG,
						"addServer()	ignore, name = " + info.getServer_name());
				return;
			}
			HashMap<String, Object> apServer = new HashMap<String, Object>();
			apServer.put(KEY_NAME, info.getServer_name());
			apServer.put(KEY_TYPE, info.getServer_device().deviceAddress);
			apServer.put(KEY_VALUE, info);
			mServerData.add(apServer);
			Log.i(TAG, "type:" + info.getServer_type()
					+ "    mServerData.size:" + mServerData.size());
			mServerAdapter.notifyDataSetChanged();
		}
	}

	private boolean isServerAlreadyAdded(String name, String ip) {
		for (Map<String, Object> map : mServerData) {
			if (name.equals(map.get(KEY_NAME)) && ip.equals(map.get(KEY_TYPE))) {
				// The server is already added to list.
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	private void clearServerList() {
		mServerData.clear();
		mServerAdapter.notifyDataSetChanged();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.close_imageview:
			ConnectFriendActivity.this.finish();
			break;
		case R.id.create_connect_btn:
			LayoutInflater inflater = LayoutInflater
					.from(ConnectFriendActivity.this);
			View view = inflater.inflate(R.layout.ui_create_server, null);
			final RadioButton wifiButton = (RadioButton) view
					.findViewById(R.id.radio_wifi);
			final RadioButton wifiApButton = (RadioButton) view
					.findViewById(R.id.radio_wifi_ap);
			final RadioButton wifiDirectButton = (RadioButton) view
					.findViewById(R.id.radio_wifi_direct);

			new AlertDialog.Builder(ConnectFriendActivity.this)
					.setTitle("Please choose the server type")
					.setView(view)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									String msg = "";
									if (wifiButton.isChecked()) {
										msg = "To Create Wifi Server";
										connectHelper.createServer("wifi",
												ConnectFriendActivity.this);
									} else if (wifiApButton.isChecked()) {
										msg = "To Create Wifi AP Server";
										connectHelper.createServer("wifi-ap",
												ConnectFriendActivity.this);
									} else {
										connectHelper.createServer(
												"wifi-direct",
												ConnectFriendActivity.this);
										msg = "To Create Wifi Direct Server";
									}
									Toast.makeText(ConnectFriendActivity.this,
											msg, Toast.LENGTH_SHORT).show();
									Intent intent = new Intent();
									// data
									setResult(RESULT_OK, intent);
									sever_flag = true;
									finish();
								}
							})
					.setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									setResult(RESULT_CANCELED);
								}
							}).create().show();
			break;
		default:
			break;
		}
	}

	@Override
	public void onSearchSuccess(String serverIP, String serverName) {
		if (mIsAPSelected && serverIP.equals(Search.ANDROID_AP_ADDRESS)) {
			// Auto connect to the server.
			Message message = mHandler.obtainMessage(MSG_CONNECT_SERVER);
			ServerInfo info = new ServerInfo();
			info.setServer_type("wifi");
			info.setServer_ip(serverIP);
			info.setServer_name(serverName);
			message.obj = info;
			mHandler.sendMessage(message);
		} else {

			Log.i(TAG, "serverIp:" + serverIP + "-->serverName:" + serverName);
			// Add to server list and wait user for choose.
			Message message = mHandler.obtainMessage(MSG_SEARCH_SUCCESS);
			ServerInfo info = new ServerInfo();
			info.setServer_type("wifi");
			info.setServer_ip(serverIP);
			info.setServer_name(serverName);
			Log.e("ArbiterLiu", serverName + "        " + info.getServer_name());
			message.obj = info;
			mHandler.sendMessage(message);
		}
	}

	@Override
	public void onSearchSuccess(ServerInfo serverInfo) {
		Log.e("ArbiterLiu",
				serverInfo.getServer_type() + "  "
						+ serverInfo.getServer_name());
		Message message = mHandler.obtainMessage(MSG_SEARCH_SUCCESS);
		message.obj = serverInfo;
		message.sendToTarget();
	}

	@Override
	public void onSearchStop() {
		Message message = mHandler.obtainMessage(MSG_SEARCH_FAIL);
		mHandler.sendMessage(message);
	}

	@Override
	public void finish() {
		super.finish();
		if (!sever_flag)
			connectHelper.stopSearch();
	}
}
