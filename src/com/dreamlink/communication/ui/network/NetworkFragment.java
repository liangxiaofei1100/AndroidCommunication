package com.dreamlink.communication.ui.network;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.impl.cookie.BestMatchSpec;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dreamlink.communication.AllowLoginDialog;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.AllowLoginDialog.AllowLoginCallBack;
import com.dreamlink.communication.CallBacks.ILoginRequestCallBack;
import com.dreamlink.communication.CallBacks.ILoginRespondCallback;
import com.dreamlink.communication.UserManager.OnUserChangedListener;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.search.SearchProtocol.OnSearchListener;
import com.dreamlink.communication.server.service.ConnectHelper;
import com.dreamlink.communication.server.service.ServerInfo;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.file.RemoteShareActivity;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

public class NetworkFragment extends BaseFragment implements
		View.OnClickListener, OnSearchListener, ILoginRequestCallBack,
		ILoginRespondCallback, OnUserChangedListener {
	@SuppressWarnings("unused")
	private static final String TAG = "NetworkFragment";
	private Context mContext;
	// Title
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;

	private View mBluetoothInviteView;
	private View mCreateNetworkView;
	private View mJoinNetworkView;
	private View mShareView;
	private View mUsersView;

	private View mDisconnectView;
	private ListView mUserListView;
	private UserListAdapter mUserListAdapter;
	private ArrayList<UserInfo> mUserInfos = new ArrayList<UserInfo>();

	private int mAppId = -1;
	private UserManager mUserManager;
	private ConnectHelper mConnectHelper;

	IntentFilter mFilter = new IntentFilter();

	private static final int MSG_SHOW_LOGIN_DIALOG = 1;
	private static final int MSG_UPDATE_NETWORK_STATUS = 2;
	private static final int MSG_UPDATE_USER = 3;

	/**
	 * Create a new instance of AppFragment, providing "appid" as an argument.
	 */
	public static NetworkFragment newInstance(int appid) {
		NetworkFragment f = new NetworkFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID)
				: 1;
	};

	private SocketCommunicationManager mSocketComMgr;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mContext = getActivity();
		View rootView = inflater.inflate(R.layout.ui_network_neighborhood,
				container, false);

		initTitle(rootView);
		initView(rootView);

		Log.d(TAG, "onCreate end");
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mSocketComMgr = SocketCommunicationManager.getInstance(mContext);
		mSocketComMgr.setLoginRequestCallBack(this);
		mSocketComMgr.setLoginRespondCallback(this);

		mConnectHelper = ConnectHelper.getInstance(mContext
				.getApplicationContext());
		mUserManager = UserManager.getInstance();
		mUserManager.registerOnUserChangedListener(this);

		mFilter.addAction(DreamConstant.SERVER_CREATED_ACTION);
		mContext.registerReceiver(mReceiver, mFilter);
	}

	private void initTitle(View view) {
		// Title icon
		mTitleIcon = (ImageView) view.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_network);
		// Title text
		mTitleView = (TextView) view.findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.network_neighborhood);
		// Title number
		mTitleNum = (TextView) view.findViewById(R.id.tv_title_num);
		mTitleNum.setVisibility(View.GONE);
		// Refresh icon
		mRefreshView = (ImageView) view.findViewById(R.id.iv_refresh);
		mRefreshView.setVisibility(View.GONE);
		// History icon
		mHistoryView = (ImageView) view.findViewById(R.id.iv_history);
		mHistoryView.setOnClickListener(this);
		mHistoryView.setVisibility(View.VISIBLE);
	}

	private void initView(View view) {
		mBluetoothInviteView = view
				.findViewById(R.id.ll_network_neighborhood_bluetooth);
		mBluetoothInviteView.setOnClickListener(this);
		mCreateNetworkView = view
				.findViewById(R.id.ll_network_neighborhood_create);
		mCreateNetworkView.setOnClickListener(this);
		mJoinNetworkView = view.findViewById(R.id.ll_network_neighborhood_join);
		mJoinNetworkView.setOnClickListener(this);
		mShareView = view.findViewById(R.id.ll_network_neighborhood_share);
		mShareView.setOnClickListener(this);

		mUsersView = view.findViewById(R.id.rl_network_neighborhood_users);
		mDisconnectView = view.findViewById(R.id.ll_network_disconnect);
		mDisconnectView.setOnClickListener(this);

		mUserListView = (ListView) view
				.findViewById(R.id.lv_network_neighborhood_users);
		mUserListAdapter = new UserListAdapter(mContext, mUserInfos);
		mUserListView.setAdapter(mUserListAdapter);
	}

	private void updateNetworkStatus() {
		if (mSocketComMgr.isServerAndCreated() || mSocketComMgr.isConnected()) {
			mCreateNetworkView.setVisibility(View.GONE);
			mJoinNetworkView.setVisibility(View.GONE);
			mShareView.setVisibility(View.VISIBLE);
			mUsersView.setVisibility(View.VISIBLE);
		} else {
			mCreateNetworkView.setVisibility(View.VISIBLE);
			mJoinNetworkView.setVisibility(View.VISIBLE);
			mShareView.setVisibility(View.GONE);
			mUsersView.setVisibility(View.GONE);
		}
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		switch (v.getId()) {
		case R.id.ll_network_neighborhood_bluetooth:
			intent.setClass(mContext, InviteBluetoothActivity.class);
			startActivity(intent);
			break;
		case R.id.ll_network_neighborhood_create:
			// intent.setClass(mContext, CreateNetworkActivity.class);
			// startActivity(intent);
			createServer();
			break;
		case R.id.ll_network_neighborhood_join:
			intent.setClass(mContext, JoinNetworkActivity.class);
			startActivity(intent);
			break;
		case R.id.ll_network_neighborhood_share:
			intent.setClass(mContext, RemoteShareActivity.class);
			startActivity(intent);
			break;
		case R.id.ll_network_disconnect:
			disconnect();
			updateNetworkStatus();
			break;
		case R.id.iv_history:
			intent.setClass(mContext, HistoryActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}

	}

	private void disconnect() {
		// 1. clear user list.
		mUserInfos.clear();
		mUserListAdapter.notifyDataSetChanged();
		// 2. stop search if this is server.
		mConnectHelper.stopSearch();
		// 3. close all communications.
		mSocketComMgr.closeAllCommunication();
		// 4. reset local user.
		mUserManager.resetLocalUserID();
		// 5. close WiFi AP if WiFi AP is enabled.
		if (NetWorkUtil.isWifiApEnabled(mContext)) {
			NetWorkUtil.setWifiAPEnabled(mContext, null, false);
		}
		// 6. disconnect current network if connected to the WiFi AP created by
		// our application.
		NetWorkUtil.clearWifiConnectHistory(mContext);
	}

	public void createServer() {
		// Disable wifi AP first to avoid wifi AP name is not the newest.
		if (NetWorkUtil.isWifiApEnabled(mContext)) {
			NetWorkUtil.setWifiAPEnabled(mContext, null, false);
		}
		UserHelper userHelper = new UserHelper(mContext);
		User localUser = userHelper.loadUser();
		mUserManager.setLocalUser(localUser);

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
		final OnSearchListener listener = NetworkFragment.this;

		new AlertDialog.Builder(mContext)
				.setTitle(R.string.choose_network_type_title)
				.setView(view)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								String networkType = "";
								if (wifiButton.isChecked()) {
									networkType = "Wi-Fi";
									mConnectHelper.createServer("wifi",
											listener);
								} else if (wifiApButton.isChecked()) {
									networkType = "Wi-Fi AP";
									mConnectHelper.createServer("wifi-ap",
											listener);
								} else {
									networkType = "Wi-Fi Direct";
									mConnectHelper.createServer("wifi-direct",
											listener);
								}
								Toast.makeText(mContext, getString(R.string.creating_network, networkType),
										Toast.LENGTH_SHORT).show();
							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mConnectHelper.stopSearch();
							}
						}).create().show();
	}

	@Override
	public void onSearchSuccess(String serverIP, String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSearchSuccess(ServerInfo serverInfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSearchStop() {
		// TODO Auto-generated method stub

	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (DreamConstant.SERVER_CREATED_ACTION.equals(action)) {
				updateNetworkStatus();
				updateUserList();
			}
		}
	};

	@Override
	public void onLoginFail(int failReason, SocketCommunication communication) {
		Log.d(TAG, "onLoginFail");
		// mNotice.showToast("User Login Fail.Reason:" + failReason);
	}

	@Override
	public void onLoginRequest(final User user,
			final SocketCommunication communication) {
		// ****************test auto allow*************
		if (DreamConstant.UREY_TEST) {
			mSocketComMgr.respondLoginRequest(user, communication, true);
		}
		// ****************test auto allow*************
		else {
			Message message = mHandler.obtainMessage();
			LoginRequestData data = new LoginRequestData();
			data.user = user;
			data.communication = communication;
			message.obj = data;
			mHandler.sendMessage(message);
		}
	}

	private void showAllowLoginDialog(User user,
			SocketCommunication communication) {
		AllowLoginDialog dialog = new AllowLoginDialog(mContext);
		AllowLoginCallBack callBack = new AllowLoginCallBack() {

			@Override
			public void onLoginComfirmed(User user,
					SocketCommunication communication, boolean isAllow) {
				mSocketComMgr.respondLoginRequest(user, communication, isAllow);

			}
		};
		dialog.show(user, communication, callBack);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateNetworkStatus();
		updateUserList();
	}

	@Override
	public void onLoginSuccess(User localUser, SocketCommunication communication) {

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiverSafe(mReceiver);
		mUserManager.unregisterOnUserChangedListener(this);
	}

	private void unregisterReceiverSafe(BroadcastReceiver receiver) {
		try {
			mContext.unregisterReceiver(receiver);
		} catch (Exception e) {
			Log.e(TAG, "unregisterReceiverSafe " + e);
		}
	}

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SHOW_LOGIN_DIALOG:
				LoginRequestData data = (LoginRequestData) msg.obj;
				showAllowLoginDialog(data.user, data.communication);
				break;
			case MSG_UPDATE_NETWORK_STATUS:
				updateNetworkStatus();
				break;
			case MSG_UPDATE_USER:
				updateUserList();
				break;
			default:
				break;
			}
		}
	};

	private class LoginRequestData {
		User user;
		SocketCommunication communication;
	}

	private void updateUserList() {
		if (mUsersView.getVisibility() != View.VISIBLE) {
			return;
		}
		mUserInfos.clear();

		Map<Integer, User> users = mUserManager.getAllUser();
		UserInfo userInfo = null;
		// TODO User icon is not implement.
		Drawable userIcon = mContext.getResources().getDrawable(
				R.drawable.user_icon_default);
		for (Entry<Integer, User> entry : users.entrySet()) {
			if (UserManager.isManagerServer(entry.getValue())) {
				userInfo = new UserInfo(userIcon, entry.getValue()
						.getUserName(), mContext.getResources().getString(
						R.string.network_creator));
			} else {
				userInfo = new UserInfo(userIcon, entry.getValue()
						.getUserName(), mContext.getResources().getString(
						R.string.connected));
			}

			mUserInfos.add(userInfo);
		}
		mUserListAdapter.notifyDataSetChanged();
	}

	@Override
	public void onUserConnected(User user) {
		Log.d(TAG, "onUserConnected " + user);
		mHandler.sendEmptyMessage(MSG_UPDATE_NETWORK_STATUS);
		mHandler.sendEmptyMessage(MSG_UPDATE_USER);
	}

	@Override
	public void onUserDisconnected(User user) {
		Log.d(TAG, "onUserDisconnected " + user);
		mHandler.sendEmptyMessage(MSG_UPDATE_NETWORK_STATUS);
		mHandler.sendEmptyMessage(MSG_UPDATE_USER);
	}

}
