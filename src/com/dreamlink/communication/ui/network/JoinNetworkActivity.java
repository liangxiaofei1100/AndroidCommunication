package com.dreamlink.communication.ui.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.search.SearchProtocol.OnSearchListener;
import com.dreamlink.communication.search.Search;
import com.dreamlink.communication.server.SocketServer;
import com.dreamlink.communication.server.service.ConnectHelper;
import com.dreamlink.communication.server.service.ServerInfo;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/***
 * use this class to search server and connect
 */
@TargetApi(14)
public class JoinNetworkActivity extends Activity implements OnClickListener,
		OnSearchListener, OnItemClickListener {
	private static final String TAG = "ConnectFriendActivity";

	// Views

	private ProgressBar mSearchBar;
	private TextView mSearchView;
	private ListView mServerListView;
	private ServerAdapter mServerAdapter;
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;

	// Members

	private Context mContext;
	private UserManager mUserManager;
	private ConnectHelper mConnectHelper;

	/**
	 * save server data Map structure: </br> KEY_NAME - server name</br>
	 * KEY_TYPE - server network type: IP, AP, WiFi Direct</br> KEY_IP - server
	 * IP. This is only used in WiFi network.
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

	private static final int MSG_SEARCH_SUCCESS = 1;
	private static final int MSG_CONNECT_SERVER = 3;
	private static final int MSG_SEARCH_WIFI_DIRECT_FOUND = 4;
	private static final int MSG_SEARCH_STOP = 6;
	private static final int MSG_SEARCHING = 7;

	private static final int STATUS_SEARCHING = 0;
	private static final int STATUS_SEARCH_OVER = 3;
	private static final int STATUS_CONNECTING = 4;

	private Timer mStopSearchTimer = null;
	/** set search time out 15s */
	private static final int SEARCH_TIME_OUT = 15 * 1000;

	private Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEARCH_SUCCESS:
				addServer((ServerInfo) msg.obj);
				break;
			case MSG_SEARCH_STOP:
				updateUI(STATUS_SEARCH_OVER);
				break;
			case MSG_SEARCHING:
				updateUI(STATUS_SEARCHING);
				break;
			case MSG_CONNECT_SERVER:
				updateUI(STATUS_CONNECTING);
				ServerInfo info = (ServerInfo) msg.obj;
				mConnectHelper.connenctToServer(info);
				if (!info.getServerType().equals("wifi-ap")) {
					// connecting
					Intent intent = new Intent();
					// intent.putExtra("status", MainUIFrame.CONNECTING);
					setResult(RESULT_OK, intent);
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
		setContentView(R.layout.ui_join_network);
		mContext = this;
		mConnectHelper = ConnectHelper.getInstance(getApplicationContext());
		initTitle();
		initViews();

		mUserManager = UserManager.getInstance();

		resetLocalUser();
		startSearch();
	}

	private void resetLocalUser() {
		UserHelper userHelper = new UserHelper(mContext);
		User localUser = userHelper.loadUser();
		mUserManager.setLocalUser(localUser);
	}

	private void initTitle() {
		// Title icon
		mTitleIcon = (ImageView) findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.network_neighborhood_join);
		// Title text
		mTitleView = (TextView) findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.join_network);
		// Title number
		mTitleNum = (TextView) findViewById(R.id.tv_title_num);
		mTitleNum.setVisibility(View.GONE);
		// Refresh icon
		mRefreshView = (ImageView) findViewById(R.id.iv_refresh);
		mRefreshView.setVisibility(View.GONE);
		// History icon
		mHistoryView = (ImageView) findViewById(R.id.iv_history);
		mHistoryView.setVisibility(View.GONE);
	}

	private void initViews() {
		mSearchBar = (ProgressBar) findViewById(R.id.searching_bar);
		mSearchView = (TextView) findViewById(R.id.search_view);

		mServerListView = (ListView) findViewById(R.id.server_listview);
		mServerListView.setEmptyView(findViewById(R.id.no_server_view));
		mSearchView.setOnClickListener(this);
		mServerListView.setOnItemClickListener(this);
		mServerAdapter = new ServerAdapter(mContext, mServerData);
		mServerListView.setAdapter(mServerAdapter);
	}

	/**
	 * according to search server status to update ui</br> 1.when is searching,
	 * show progress bar to tell user that is searching,please wait</br> 2.when
	 * is search success,show the server list to user to choose connect</br>
	 * 3.when is search failed,show the re-search ui allow user re-search
	 * 
	 * @param status
	 *            search status
	 */
	private void updateUI(int status) {
		if (STATUS_SEARCHING == status) {
			mSearchBar.setVisibility(View.VISIBLE);
			mSearchView.setText(R.string.searching_sever);
			mSearchView.setClickable(false);
		} else if (STATUS_SEARCH_OVER == status) {
			mSearchBar.setVisibility(View.INVISIBLE);
			mSearchView.setText(R.string.search_sever);
			mSearchView.setClickable(true);
		} else if (STATUS_CONNECTING == status) {
			mSearchBar.setVisibility(View.INVISIBLE);
			mSearchView.setText(R.string.connecting);
			mSearchView.setClickable(false);
			mServerListView.setFocusable(false);
		}
	}

	/** start search server */
	private void startSearch() {
		NetWorkUtil.clearWifiConnectHistory(getApplicationContext());
		clearServerList();
		if (SocketServer.getInstance().isServerStarted()) {
			SocketServer.getInstance().stopServer();
		}

		mConnectHelper.searchServer(this);

		Message message = mHandler.obtainMessage(MSG_SEARCHING);
		mHandler.sendMessage(message);

		setStopSearchTimer();
	}

	private void setStopSearchTimer() {
		if (mStopSearchTimer != null) {
			try {
				mStopSearchTimer.cancel();
			} catch (Exception e) {
				Log.d(TAG, "setStopSearchTimer cancel time." + e);
			}

		}
		mStopSearchTimer = new Timer();
		mStopSearchTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				mStopSearchTimer = null;
				mConnectHelper.stopSearch(false);
				Message message = mHandler.obtainMessage(MSG_SEARCH_STOP);
				mHandler.sendMessage(message);
			}
		}, SEARCH_TIME_OUT);
	}

	/**
	 * catch broadcast not register exception.
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
		if (info.getServerType().equals(ConnectHelper.SERVER_TYPE_WIFI)) {
			addWifiServer(info);
		} else if (info.getServerType().equals(
				ConnectHelper.SERVER_TYPE_WIFI_AP)) {
			addWifiApServer(info);
		} else {
			if (isServerAlreadyAdded(info.getServerName(),
					info.getServerDevice().deviceAddress)) {
				Log.d(TAG,
						"another.addServer()	ignore, name = "
								+ info.getServerName());
				return;
			}
			HashMap<String, Object> apServer = new HashMap<String, Object>();
			apServer.put(KEY_NAME, info.getServerName());
			apServer.put(KEY_TYPE, info.getServerDevice().deviceAddress);
			apServer.put(KEY_VALUE, info);
			mServerData.add(apServer);
			Log.i(TAG,
					"type:" + info.getServerType() + ",name:"
							+ info.getServerName() + "    mServerData.size:"
							+ mServerData.size());
			mServerAdapter.notifyDataSetChanged();
		}
	}

	private void addWifiApServer(ServerInfo info) {
		if (isServerAlreadyAdded(info.getServerName(), info.getServerSsid())) {
			// TODO if two server has the same name, How to do?
			Log.d(TAG,
					"wifiAp.addServer()	ignore, name = " + info.getServerName());
			return;
		}
		// Found a AP, add the user name to the server list.
		HashMap<String, Object> apServer = new HashMap<String, Object>();
		apServer.put(KEY_NAME, info.getServerName());
		apServer.put(KEY_TYPE, info.getServerSsid());
		apServer.put(KEY_VALUE, info);
		mServerData.add(apServer);
		Log.i(TAG,
				"type:" + info.getServerType() + ",name:"
						+ info.getServerName() + "    mServerData.size:"
						+ mServerData.size());
		mServerAdapter.notifyDataSetChanged();
	}

	private void addWifiServer(ServerInfo info) {
		if (isServerAlreadyAdded(info.getServerName(), info.getServerIp())) {
			Log.d(TAG,
					"wifi.addServer()	ignore, name = " + info.getServerName());
			return;
		}
		if (Search.ANDROID_AP_ADDRESS.equals(info.getServerIp())) {
			Log.d(TAG,
					"This ip is android wifi ap, ignore, name = "
							+ info.getServerName());
			return;
		}
		// This device is connected to WiFi, So add the server IP.
		HashMap<String, Object> ipServer = new HashMap<String, Object>();
		ipServer.put(KEY_NAME, info.getServerName());
		ipServer.put(KEY_TYPE, info.getServerIp());
		ipServer.put(KEY_VALUE, info);
		mServerData.add(ipServer);
		Log.i(TAG,
				"type:" + info.getServerType() + ",name:"
						+ info.getServerName() + "    mServerData.size:"
						+ mServerData.size());
		mServerAdapter.notifyDataSetChanged();
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

	private void clearServerList() {
		mServerData.clear();
		mServerAdapter.notifyDataSetChanged();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.search_view:
			startSearch();
			break;
		// case R.id.search_failed_layout:
		// updateUI(SEARCHING);
		// connectHelper.searchServer(ConnectFriendActivity.this);
		// break;
		default:
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		ServerInfo info = (ServerInfo) mServerData.get(arg2).get(KEY_VALUE);
		if (info.getServerType().equals("wifi-ap"))
			mIsAPSelected = true;
		mHandler.obtainMessage(MSG_CONNECT_SERVER, info).sendToTarget();
	}

	@Override
	public void onSearchSuccess(String serverIP, String serverName) {
		Log.d(TAG, "mIsApSelected:" + mIsAPSelected + "\n" + "serverIP:"
				+ serverIP + "\n" + "serverName:" + serverName);
		if (mIsAPSelected && serverIP.equals(Search.ANDROID_AP_ADDRESS)) {
			// Auto connect to the server.
			Message message = mHandler.obtainMessage(MSG_CONNECT_SERVER);
			ServerInfo info = new ServerInfo();
			info.setServerType("wifi");
			info.setServerIp(serverIP);
			info.setServerName(serverName);
			message.obj = info;
			mHandler.sendMessage(message);
		} else {

			Log.i(TAG, "serverIp:" + serverIP + "-->serverName:" + serverName);
			// Add to server list and wait user for choose.
			Message message = mHandler.obtainMessage(MSG_SEARCH_SUCCESS);
			ServerInfo info = new ServerInfo();
			info.setServerType("wifi");
			info.setServerIp(serverIP);
			info.setServerName(serverName);
			message.obj = info;
			mHandler.sendMessage(message);
		}
	}

	@Override
	public void onSearchSuccess(ServerInfo serverInfo) {
		Log.d(TAG, "onSearchSuccess.serverInfo=" + serverInfo.getServerName());
		Message message = mHandler.obtainMessage(MSG_SEARCH_SUCCESS);
		message.obj = serverInfo;
		message.sendToTarget();
	}

	@Override
	public void onSearchStop() {
		// stop by user or force stop last search
		Message message = mHandler.obtainMessage(MSG_SEARCH_STOP);
		mHandler.sendMessage(message);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (null != mStopSearchTimer) {
			mStopSearchTimer.cancel();
			mStopSearchTimer = null;
		}
		mIsAPSelected = false;
		mConnectHelper.stopSearch();
	}

}
