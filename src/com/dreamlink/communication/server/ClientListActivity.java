package com.dreamlink.communication.server;

import java.util.ArrayList;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
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

import com.dreamlink.aidl.User;
import com.dreamlink.communication.AllowLoginDialog;
import com.dreamlink.communication.AllowLoginDialog.AllowLoginCallBack;
import com.dreamlink.communication.AppListActivity;
import com.dreamlink.communication.CallBacks.ILoginRequestCallBack;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.search.Search;
import com.dreamlink.communication.search.SearchClient;
import com.dreamlink.communication.search.SearchProtocol.OnSearchListener;
import com.dreamlink.communication.search.WiFiNameEncryption;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;
import com.dreamlink.communication.wifip2p.WifiDirectManager;
import com.dreamlink.communication.wifip2p.WifiDirectReciver;

/**
 * This class is used for show clients that connected to this server in the WiFi
 * network.</br>
 * 
 * Because client will connect the server, we can listen Communicate manager to
 * see there is new communicate established and get all communications.</br>
 * 
 */
public class ClientListActivity extends Activity implements OnSearchListener,
		OnClickListener, OnCommunicationListener, ILoginRequestCallBack,
		com.dreamlink.aidl.OnCommunicationListenerExternal {
	private static final String TAG = "ClientListActivity";
	private Context mContext;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
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

				Map<Integer, User> allUsers = mUserManager.getAllUser();
				for (Map.Entry<Integer, User> entry : allUsers.entrySet()) {
					mClients.add(entry.getValue().getUserName());
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
		mCommunicationManager.registerOnCommunicationListenerExternal(this, 0);
		mCommunicationManager.setLoginRequestCallBack(this);
		// mCommunicationManager.startServer(mContext);

		mSearchClient = SearchClient.getInstance(this);
		mSearchClient.setOnSearchListener(this);

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
		mClients.add("Please choose the server type : ");
		mClients.add("Wifi Server");
		mClients.add("Wifi-AP Server");
		mClients.add("Wifi-Direct Server");
		mListView = (ListView) findViewById(R.id.list_client);
		mAdapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_list_item_1, mClients);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				switch (arg2) {
				case 1:
					startWifiServer();
					break;
				case 2:
					startWifiAPserver();
					break;
				case 3:
					startWifiDirectServer();
					break;
				default:
					/* no possible going here */
					break;
				}
				mClients.clear();
				mListView.setEnabled(false);
				mAdapter.notifyDataSetChanged();
			}
		});
		Button startButton = (Button) findViewById(R.id.btn_start);
		startButton.setOnClickListener(this);
		Button quitButton = (Button) findViewById(R.id.btn_quit);
		quitButton.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// mSearchHandler.sendEmptyMessage(MSG_UPDATE_LIST);
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
	public void onSearchSuccess(String serverIP, String serverName) {
		Message message = mSearchHandler.obtainMessage(MSG_SEARCH_SUCCESS);
		message.obj = serverIP;
		mSearchHandler.sendMessage(message);
	}

	@Override
	public void onSearchStop() {
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		getMenuInflater().inflate(R.menu.server_type, menu);
		if (Build.VERSION.SDK_INT < 14
				|| !getPackageManager().hasSystemFeature(
						PackageManager.FEATURE_WIFI_DIRECT)) {
			menu.removeItem(R.id.server_type_wifi_direct);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.server_type_wifi:
			startWifiServer();
			break;
		case R.id.server_type_wifi_AP:
			startWifiAPserver();
			break;
		case R.id.server_type_wifi_direct:
			startWifiDirectServer();
			break;
		default:
			break;
		}
		mNotice.showToast("Start Search");
		return super.onOptionsItemSelected(item);
	}

	private void startWifiDirectServer() {
		if (NetWorkUtil.isWifiApEnabled(this)) {
			NetWorkUtil.setWifiAPEnabled(this, null, false);
		}
		if (mSearchClient != null) {
			mSearchClient.stopSearch();
		}
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wifiDirectReciver == null) {
			initReceiver();
		}
		if (!wifiManager.isWifiEnabled()) {
			startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 1);
		} else {
			mWifiDirectManager = new WifiDirectManager(this, true);
			mWifiDirectManager.discover();
		}
	}

	private void startWifiAPserver() {
		if (mWifiDirectManager != null) {
			mWifiDirectManager.stopSearch();
			mWifiDirectManager.stopConnect();
			this.unregisterReceiver(wifiDirectReciver);
			mWifiDirectManager = null;
		}
		if (mSearchClient != null) {
			mSearchClient.stopSearch();
		}
		NetWorkUtil.setWifiAPEnabled(mContext, WiFiNameEncryption
				.generateWiFiName(UserHelper.getUserName(mContext)), true);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
		registerReceiver(mBroadcastReceiver, filter);
	}

	private void startWifiServer() {
		if (mWifiDirectManager != null) {
			mWifiDirectManager.stopSearch();
			mWifiDirectManager.stopConnect();
			this.unregisterReceiver(wifiDirectReciver);
			mWifiDirectManager = null;
		}
		if (NetWorkUtil.isWifiApEnabled(this)) {
			NetWorkUtil.setWifiAPEnabled(this, "", false);
		}
		if (mSearchClient != null) {
			mSearchClient.stopSearch();
		}
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (wifiManager.isWifiEnabled() && cm.getActiveNetworkInfo() != null) {
			mSearchClient.startSearch();
		} else {
			startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 0);
		}
	}

	private WifiDirectManager mWifiDirectManager;
	private WifiDirectReciver wifiDirectReciver;
	private IntentFilter mWifiDirectIntentFilter;

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
		this.registerReceiver(wifiDirectReciver, mWifiDirectIntentFilter);
	}

	/**
	 * @param requestCode
	 *            if 0 ,wifi server ; if 1, wifi-direct server
	 * */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0) {
			startWifiServer();
		} else if (requestCode == 1) {
			startWifiDirectServer();
		}
	}

	private void showAllowLoginDialog(User user,
			SocketCommunication communication) {
		AllowLoginDialog dialog = new AllowLoginDialog(mContext);
		AllowLoginCallBack callBack = new AllowLoginCallBack() {

			@Override
			public void onLoginComfirmed(User user,
					SocketCommunication communication, boolean isAllow) {
				mCommunicationManager.respondLoginRequest(user, communication,
						isAllow);

			}
		};
		dialog.show(user, communication, callBack);
	}

	@Override
	public void onLoginRequest(final User user,
			final SocketCommunication communication) {
		Log.d(TAG, "onLoginRequest(), user = " + user + ", communication = "
				+ communication.getConnectIP().getHostAddress());
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				showAllowLoginDialog(user, communication);
			}
		});
	}

	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserConnected(User user) {
		mSearchHandler.sendEmptyMessage(MSG_UPDATE_LIST);
	}

	@Override
	public void onUserDisconnected(User user) {
		mSearchHandler.sendEmptyMessage(MSG_UPDATE_LIST);
	}

	@Override
	public IBinder asBinder() {
		// TODO Auto-generated method stub
		return null;
	}

}
