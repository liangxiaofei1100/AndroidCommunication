package com.dreamlink.communication.ui.network;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.UserHelper;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.search.SearchProtocol.OnSearchListener;
import com.dreamlink.communication.search.Search;
import com.dreamlink.communication.server.SocketServer;
import com.dreamlink.communication.server.service.ConnectHelper;
import com.dreamlink.communication.server.service.ServerInfo;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
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
	private Vector<ServerInfo> mServerData = new Vector<ServerInfo>();
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	private ImageView mMoreView;

	// Members

	private Context mContext;
	private UserManager mUserManager;
	private ConnectHelper mConnectHelper;

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

	@SuppressLint("HandlerLeak")
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
		// menu more icon
		mMoreView = (ImageView) findViewById(R.id.iv_more);
		mMoreView.setVisibility(View.GONE);
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

	private void addServer(ServerInfo info) {
		if (isServerAlreadyAdded(info)) {
			Log.d(TAG, "Server is already added. " + info);
			return;
		}
		mServerData.add(info);
		Log.i(TAG, "addServer:" + info);
		mServerAdapter.notifyDataSetChanged();
	}

	private boolean isServerAlreadyAdded(ServerInfo info) {
		boolean result = false;

		for (ServerInfo serverInfo : mServerData) {
			if (ConnectHelper.SERVER_TYPE_WIFI.equals(info.getServerType())
					&& ConnectHelper.SERVER_TYPE_WIFI.equals(serverInfo
							.getServerType())) {
				if (info.getServerName().equals(serverInfo.getServerName())
						&& info.getServerIp().equals(serverInfo.getServerIp())) {
					// The server is already added to list.
					result = true;
					break;
				}
			} else if (ConnectHelper.SERVER_TYPE_WIFI_AP.equals(info
					.getServerType())
					&& ConnectHelper.SERVER_TYPE_WIFI_AP.equals(serverInfo
							.getServerType())) {
				if (info.getServerName().equals(serverInfo.getServerName())
						&& info.getServerSsid().equals(
								serverInfo.getServerSsid())) {
					// The server is already added to list.
					result = true;
					break;
				}
			} else if (ConnectHelper.SERVER_TYPE_WIFI_DIRECT.equals(info
					.getServerType())
					&& ConnectHelper.SERVER_TYPE_WIFI_DIRECT.equals(serverInfo
							.getServerType())) {
				if (info.getServerName().equals(serverInfo.getServerName())
						&& info.getServerDevice().deviceAddress
								.equals(serverInfo.getServerDevice().deviceAddress)) {
					// The server is already added to list.
					result = true;
					break;
				}
			}
		}

		return result;
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
		ServerInfo info = mServerData.get(arg2);
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
		if (null != mStopSearchTimer) {
			mStopSearchTimer.cancel();
			mStopSearchTimer = null;
		}
		mIsAPSelected = false;
		if (mConnectHelper != null) {
			// Stop search will release listener.
			mConnectHelper.stopSearch();
		}
		super.onDestroy();
	}

}
