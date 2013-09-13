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
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.search.SearchProtocol.OnSearchListener;
import com.dreamlink.communication.search.Search;
import com.dreamlink.communication.server.SocketServer;
import com.dreamlink.communication.server.service.ConnectHelper;
import com.dreamlink.communication.server.service.ServerInfo;
import com.dreamlink.communication.ui.ConnectFriendActivity;
import com.dreamlink.communication.ui.MainUIFrame;
import com.dreamlink.communication.ui.ServerAdapter;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

/***
 * use this class to search server and connect
 */
@TargetApi(14)
public class CreateNetworkActivity extends Activity implements OnClickListener,
		OnSearchListener {
	private static final String TAG = "CreateNetworkActivity";
	private ImageView mCloseBtn;
	private ProgressBar mSearchBar;
	private TextView mSearchView;
	private Button createConnectBtn;

	private ListView mServerListView;// show server list

	private Notice mNotice;
	private Context mContext;
	private boolean sever_flag = false;

	private ServerAdapter mServerAdapter;

	/**
	 * save server data Map structure: </br>
	 * KEY_NAME - server name</br>
	 * KEY_TYPE - server network type: IP, AP, WiFi Direct</br>
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
	private static final int MSG_SEARCH_STOP = 6;
	private static final int MSG_SEARCHING = 7;

	private static final int SEARCHING = 0;
	private static final int SEARCHED = 1;
	private static final int SEARCH_FAILED = 2;
	private static final int SEARCH_OVER = 3;
	private static final int CONNECTING = 4;
	
	private Timer mTimeoutTimer = null;
	/**set search time out 15s*/
	private static final int TIME_OUT = 15 * 1000;

	private ConnectHelper connectHelper;

	private Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEARCH_SUCCESS:
				addServer((ServerInfo) msg.obj);
				break;
			case MSG_SEARCH_FAIL:
				updateUI(SEARCH_FAILED);
				mNotice.showToast("Search failed");
				updateUI(SEARCH_FAILED);
				break;
			case MSG_SEARCH_STOP:
//				mNotice.showToast("Search Stop");
				updateUI(SEARCH_OVER);
				break;
			case MSG_SEARCHING:
				updateUI(SEARCHING);
				break;
			case MSG_CONNECT_SERVER:
				updateUI(CONNECTING);
				ServerInfo info = (ServerInfo) msg.obj;
				connectHelper.connenctToServer(info);
				if (!info.getServer_type().equals("wifi-ap")) {
					//connecting
					Intent intent = new Intent();
					intent.putExtra("status", MainUIFrame.CONNECTING);
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
		setContentView(R.layout.ui_connect_friend);
		mContext = this;
		mNotice = new Notice(mContext);
		connectHelper = ConnectHelper.getInstance(getApplicationContext());
		initViews();

		mUserManager = UserManager.getInstance();
		UserHelper userHelper = new UserHelper(mContext);
		User localUser = userHelper.loadUser();
		mUserManager.setLocalUser(localUser);
		startSearch();
	}

	private void initViews() {
		mCloseBtn = (ImageView) findViewById(R.id.close_imageview);
		createConnectBtn = (Button) findViewById(R.id.create_connect_btn);
		mSearchBar = (ProgressBar) findViewById(R.id.searching_bar);
		mSearchView = (TextView) findViewById(R.id.search_view);

		mServerListView = (ListView) findViewById(R.id.server_listview);
		mServerListView.setEmptyView(findViewById(R.id.no_server_view));
		mCloseBtn.setOnClickListener(this);
		createConnectBtn.setOnClickListener(this);
		mSearchView.setOnClickListener(this);
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
	}

	/**
	 * according to search server status to update ui</br>
	 * 1.when is searching, show progress  bar to tell user that is searching,please wait</br>
	 * 2.when is search success,show the server list to user to choose connect</br>
	 * 3.when is search failed,show the re-search ui allow user re-search
	 * @param status search status
	 */
	private void updateUI(int status) {
		if (SEARCHING == status) {// searching
			mSearchBar.setVisibility(View.VISIBLE);
			mSearchView.setText(R.string.searching_sever);
			mSearchView.setClickable(false);
		} else if (SEARCH_OVER == status) {// found
			mSearchBar.setVisibility(View.INVISIBLE);
			mSearchView.setText(R.string.search_sever);
			mSearchView.setClickable(true);
		} else if (SEARCH_FAILED == status) {// not found
		}else if (CONNECTING == status) {
			mSearchBar.setVisibility(View.INVISIBLE);
			mSearchView.setText(R.string.connecting);
			mSearchView.setClickable(false);
			mServerListView.setFocusable(false);
		}
	}
	
	/**start search server*/
	private void startSearch(){
		NetWorkUtil.clearWifiConnectHistory(getApplicationContext());
		clearServerList();
		if (SocketServer.getInstance().isServerStarted()) {
			SocketServer.getInstance().stopServer();
		}	
		
		connectHelper.searchServer(this);
		Message message = mHandler.obtainMessage(MSG_SEARCHING);
		mHandler.sendMessage(message);
		
		mTimeoutTimer = new Timer();
		mTimeoutTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				connectHelper.stopSearch(false);
				Message message = mHandler.obtainMessage(MSG_SEARCH_STOP);
				mHandler.sendMessage(message);
			}
		}, TIME_OUT);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	/**
	 * catch broadcast not register exception.
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
				Log.d(TAG,"wifi.addServer()	ignore, name = " + info.getServer_name());
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
			Log.i(TAG, "type:" + info.getServer_type() + ",name:" + info.getServer_name()
					+ "    mServerData.size:" + mServerData.size());
			mServerAdapter.notifyDataSetChanged();
		} else if (info.getServer_type().equals("wifi-ap")) {
			if (isServerAlreadyAdded(info.getServer_name(),
					info.getServer_ssid())) {
				// TODO if two server has the same name, How to do?
				Log.d(TAG,
						"wifiAp.addServer()	ignore, name = " + info.getServer_name());
				return;
			}
			// Found a AP, add the user name to the server list.
			HashMap<String, Object> apServer = new HashMap<String, Object>();
			apServer.put(KEY_NAME, info.getServer_name());
			apServer.put(KEY_TYPE, info.getServer_ssid());
			apServer.put(KEY_VALUE, info);
			mServerData.add(apServer);
			Log.i(TAG, "type:" + info.getServer_type() + ",name:" + info.getServer_name()
					+ "    mServerData.size:" + mServerData.size());
			mServerAdapter.notifyDataSetChanged();
		} else {
			if (isServerAlreadyAdded(info.getServer_name(),
					info.getServer_device().deviceAddress)) {
				Log.d(TAG,
						"another.addServer()	ignore, name = " + info.getServer_name());
				return;
			}
			HashMap<String, Object> apServer = new HashMap<String, Object>();
			apServer.put(KEY_NAME, info.getServer_name());
			apServer.put(KEY_TYPE, info.getServer_device().deviceAddress);
			apServer.put(KEY_VALUE, info);
			mServerData.add(apServer);
			Log.i(TAG, "type:" + info.getServer_type() + ",name:" + info.getServer_name()
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

	private void clearServerList() {
		mServerData.clear();
		mServerAdapter.notifyDataSetChanged();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.close_imageview:
			setResult(RESULT_CANCELED);
			CreateNetworkActivity.this.finish();
			break;
		case R.id.create_connect_btn:
			LayoutInflater inflater = LayoutInflater.from(mContext);
			View view = inflater.inflate(R.layout.ui_create_server, null);
			final RadioButton wifiButton = (RadioButton) view
					.findViewById(R.id.radio_wifi);
			final RadioButton wifiApButton = (RadioButton) view
					.findViewById(R.id.radio_wifi_ap);
			final RadioButton wifiDirectButton = (RadioButton) view
					.findViewById(R.id.radio_wifi_direct);
			if (!mContext.getPackageManager().hasSystemFeature(
					PackageManager.FEATURE_WIFI_DIRECT)) {
				wifiDirectButton.setVisibility(View.GONE);
			}
			new AlertDialog.Builder(CreateNetworkActivity.this)
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
												CreateNetworkActivity.this);
									} else if (wifiApButton.isChecked()) {
										msg = "To Create Wifi AP Server";
										connectHelper.createServer("wifi-ap",
												CreateNetworkActivity.this);
									} else {
										connectHelper.createServer(
												"wifi-direct",
												CreateNetworkActivity.this);
										msg = "To Create Wifi Direct Server";
									}
									Toast.makeText(CreateNetworkActivity.this,
											msg, Toast.LENGTH_SHORT).show();
									Intent intent = new Intent();
									// data
									intent.putExtra("status", MainUIFrame.CREATING);
									setResult(RESULT_OK, intent);
									sever_flag = true;
									if(null != mTimeoutTimer){
										mTimeoutTimer.cancel();
									}
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
			
		case R.id.search_view:
			startSearch();
			break;
//		case R.id.search_failed_layout:
//			updateUI(SEARCHING);
//			connectHelper.searchServer(ConnectFriendActivity.this);
//			break;
		default:
			break;
		}
	}

	@Override
	public void onSearchSuccess(String serverIP, String serverName) {
		Log.d(TAG, "mIsApSelected:" + mIsAPSelected + "\n"
				+ "serverIP:" + serverIP+ "\n"
				+ "serverName:" + serverName);
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
			message.obj = info;
			mHandler.sendMessage(message);
		}
	}

	@Override
	public void onSearchSuccess(ServerInfo serverInfo) {
		Log.d(TAG, "onSearchSuccess.serverInfo=" + serverInfo.getServer_name());
		Message message = mHandler.obtainMessage(MSG_SEARCH_SUCCESS);
		message.obj = serverInfo;
		message.sendToTarget();
	}

	@Override
	public void onSearchStop() {
		//stop by user or force stop last search
		Message message = mHandler.obtainMessage(MSG_SEARCH_STOP);
		mHandler.sendMessage(message);
	}

	@Override
	public void finish() {
		super.finish();
		if(null != mTimeoutTimer){
			mTimeoutTimer.cancel();
		}
		mIsAPSelected=false;
		if (!sever_flag) {
			connectHelper.stopSearch();
		}
	}
}
