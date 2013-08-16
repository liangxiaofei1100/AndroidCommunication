package com.dreamlink.communication.server.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.widget.Toast;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.search.SearchProtocol.OnSearchListener;
import com.dreamlink.communication.search.WiFiNameEncryption;
import com.dreamlink.communication.server.service.DirectService.DirectBinder;
import com.dreamlink.communication.server.service.WifiOrAPService.WifiOrAPBinder;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

public class ConnectHelper {
	private final String TAG = "CreateServer";
	private final String[] SERVER_TYPE = { "wifi", "wifi-ap", "wifi-direct" };
	private Context mContext;
	private final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
	private DirectService directService;
	private WifiOrAPService wifiOrAPService;
	private User user;
	private boolean flag;
	private String type;
	private OnSearchListener listener;
	private OnSearchListener direcrListener;
	private boolean is_wifi_on_before = false;
	private boolean is_ap_on_before = false;
	public static ConnectHelper mConnectHelper;

	private ConnectHelper(Context context) {
		this.mContext = context;
		WifiManager wifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		is_wifi_on_before = wifiManager.isWifiEnabled();
		is_ap_on_before = NetWorkUtil.isAndroidAPNetwork(mContext);
	}

	/**
	 * the param please use {@link Context#getApplicationContext()} ,maybe
	 * better. Thank you
	 */
	public static ConnectHelper getInstance(Context context) {
		if (mConnectHelper == null) {
			synchronized (context) {
				mConnectHelper = new ConnectHelper(
						context.getApplicationContext());
			}
		}
		return mConnectHelper;
	}

	public String[] getServerType() {
		return SERVER_TYPE;
	}

	/**
	 * create server
	 * 
	 * @param type
	 *            just like: "wifi", "wifi-ap", "wifi-direct" case insensitive
	 * @param searchListener
	 *            {@link OnSearchListener}
	 * */
	public void createServer(String type, OnSearchListener searchListener) {
		flag = true;
		if (type == null) {
			type = "wifi-ap";// default ap server
		}
		this.type = type.toLowerCase();
		this.listener = searchListener;
		this.direcrListener = searchListener;
		if (type.equals(SERVER_TYPE[0])) {
			startWifiServer();
		} else if (type.equals(SERVER_TYPE[1])) {
			startWifiAPserver();
		} else if (type.equals(SERVER_TYPE[2])) {
			startWifiDirectServer(null);
		} else {
			Log.e(TAG, "Unknow type!");
		}
	}

	/**
	 * @param user
	 *            which user do you want to search,just for direct ,in wifi
	 *            &wifi-ap case,this param is must be null
	 */
	public void searchServer(OnSearchListener searchListener) {
		if (NetWorkUtil.isWifiApEnabled(mContext)) {
			NetWorkUtil.setWifiAPEnabled(mContext, "", false);
		}// close wifi-ap server
		this.listener = searchListener;
		if (wifiOrAPService != null) {
			unbindServer(wifiConnection);
		}
		this.flag = false;
		Intent intent = new Intent();
		intent.setClass(mContext, WifiOrAPService.class);
		bindServer(intent, wifiConnection);
	}

	public void searchDirectServer(OnSearchListener searchListener, User user) {
		if (!mContext.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_WIFI_DIRECT)) {
			/*
			 * is support wifi-direct. Reference:
			 * http://blog.csdn.net/snow25bz/article/details/8004106
			 */
			Toast.makeText(mContext, "Not support wifi-direct",
					Toast.LENGTH_SHORT).show();
			return;
		}
		this.direcrListener = searchListener;
		this.flag = false;
		this.user = user;
		if (NetWorkUtil.isWifiApEnabled(mContext)) {
			NetWorkUtil.setWifiAPEnabled(mContext, null, false);
			if (wifiOrAPService != null)
				mContext.unbindService(wifiConnection);
		}
		if (directService != null) {
			mContext.unbindService(directConnection);
		}
		Intent intent = new Intent();
		intent.setClass(mContext, DirectService.class);
		bindServer(intent, directConnection);
	}

	private void startWifiDirectServer(User user) {
		if (!mContext.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_WIFI_DIRECT)) {
			Toast.makeText(mContext, "Not support wifi-direct",
					Toast.LENGTH_SHORT).show();
			return;
		}
		this.user = user;
		if (NetWorkUtil.isWifiApEnabled(mContext)) {
			NetWorkUtil.setWifiAPEnabled(mContext, null, false);
		}
		if (directService != null) {
			unbindServer(directConnection);
		}
		if (NetWorkUtil.isAndroidAPNetwork(mContext)) {
			NetWorkUtil.setWifiAPEnabled(mContext, null, false);
			if (wifiOrAPService != null)
				unbindServer(wifiConnection);
		}
		Intent intent = new Intent();
		intent.setClass(mContext, DirectService.class);
		bindServer(intent, directConnection);
	}

	private void startWifiAPserver() {
		NetWorkUtil.setWifiAPEnabled(mContext, WiFiNameEncryption
				.generateWiFiName(UserHelper.getUserName(mContext)), true);
		if (directService != null) {
			unbindServer(directConnection);
		}
		if (wifiOrAPService != null) {
			unbindServer(wifiConnection);
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
		Intent intent = new Intent();
		intent.setClass(mContext, WifiOrAPService.class);
		bindServer(intent, wifiConnection);
	}

	private void startWifiServer() {
		if (NetWorkUtil.isWifiApEnabled(mContext)) {
			NetWorkUtil.setWifiAPEnabled(mContext, "", false);
		}// close wifi-ap server
		if (wifiOrAPService != null) {
			unbindServer(wifiConnection);
		}
		Intent intent = new Intent();
		intent.setClass(mContext, WifiOrAPService.class);
		bindServer(intent, wifiConnection);
	}

	public void stopSearch() {
		if (wifiOrAPService != null)
			unbindServer(wifiConnection);
		if (directService != null)
			unbindServer(directConnection);
	}

	public void connenctToServer(ServerInfo info) {
		Log.d(TAG, "connenctToServer");
		if (info.getServer_type().equals("wifi-direct")) {
			if (directService != null)
				directService.connectToServer(info);
		} else if (info.getServer_type().equals("wifi")
				|| info.getServer_type().equals("wifi-ap")) {
			if (wifiOrAPService != null)
				wifiOrAPService.connectToServer(info);
		}
	}

	private ServiceConnection directConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			directService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (service instanceof DirectBinder) {
				DirectBinder binder = (DirectBinder) service;
				directService = binder.getService();
				directService.startDirect(flag, user, direcrListener);
			} else if (service instanceof WifiOrAPBinder) {
				WifiOrAPBinder binder = (WifiOrAPBinder) service;
				wifiOrAPService = binder.getService();
				if (flag) {
					wifiOrAPService.startServer(type.toUpperCase(), listener);
				} else {
					wifiOrAPService.startSearch(listener);
				}
			}
		}
	};

	private ServiceConnection wifiConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			wifiOrAPService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (service instanceof DirectBinder) {
				DirectBinder binder = (DirectBinder) service;
				directService = binder.getService();
				directService.startDirect(flag, user, direcrListener);
			} else if (service instanceof WifiOrAPBinder) {
				WifiOrAPBinder binder = (WifiOrAPBinder) service;
				wifiOrAPService = binder.getService();
				if (flag) {
					wifiOrAPService.startServer(type.toUpperCase(), listener);
				} else {
					wifiOrAPService.startSearch(listener);
				}
			}
		}
	};

	private void bindServer(Intent intent, ServiceConnection connection) {
		mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}

	private void unbindServer(ServiceConnection connection) {
		try {
			mContext.unbindService(connection);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** when exit program ,use this disconnect and close wifi or wifi-ap */
	public void exitConnect() {
		stopSearch();
		SocketCommunicationManager.getInstance(mContext)
				.closeAllCommunication();
		if (!is_ap_on_before) {
			NetWorkUtil.setWifiAPEnabled(mContext, "", false);
		}
		if (!is_wifi_on_before) {
			WifiManager wifiManager = (WifiManager) mContext
					.getSystemService(Context.WIFI_SERVICE);
			wifiManager.setWifiEnabled(false);
		}
	}


}
