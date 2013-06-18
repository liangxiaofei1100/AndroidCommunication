package com.dreamlink.communication.util;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import com.dreamlink.communication.Search;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.text.TextUtils;

public class NetWorkUtil {
	private static final String TAG = "NetWorkUtil";
	private static MulticastLock mMulticastLock;

	public static boolean isNetworkConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();

		boolean isWifiApEnabled = isWifiApEnabled(context);

		if (networkInfo == null && isWifiApEnabled == false) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean isWifiApEnabled(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		try {
			Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
			boolean enabled = (Boolean) method.invoke(wifiManager);
			return enabled;
		} catch (Exception e) {
			Log.e(TAG, "Cannot get wifi AP sate: " + e);
			return false;
		}
	}

	public static String getLocalIpAddress2(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		if (!wifiManager.isWifiEnabled()) {
			return null;
		}
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String ip = intToIp(ipAddress);
		return ip;
	}

	private static String intToIp(int i) {
		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + (i >> 24 & 0xFF);
	}

	/**
	 * get local ip address string. like 192.168.1.3
	 * 
	 * @return
	 */
	public static String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					Log.d(TAG, "name = " + intf.getDisplayName() + ", ip: "
							+ inetAddress.getHostAddress());
					if (!inetAddress.isLoopbackAddress()
							&& !inetAddress.isLinkLocalAddress()
							&& (intf.getDisplayName().contains("wlan")
									|| intf.getDisplayName().contains("eth") || intf
									.getDisplayName().contains("ap"))) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(TAG, "getLocalIpAddress() fail. " + ex.toString());
		}
		return "";
	}

	public static String getLocalMacAddress(Context context) {
		WifiManager wifi = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifi.getConnectionInfo();
		return info.getMacAddress();
	}

	public synchronized static void acquireWifiMultiCastLock(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (mMulticastLock == null) {
			mMulticastLock = wifiManager.createMulticastLock("multicast.test");
			mMulticastLock.acquire();
		}

	}

	public synchronized static void releaseWifiMultiCastLock() {
		if (mMulticastLock != null) {
			mMulticastLock.release();
			mMulticastLock = null;
		}
	}

	public static boolean isAndroidAPNetwork(Context context) {
		String ipAddress = getLocalIpAddress();
		if (!TextUtils.isEmpty(ipAddress)
				&& ipAddress.startsWith(Search.ANDROID_STA_ADDRESS_START)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Enable WiFi AP or close.
	 * 
	 * @param enabled
	 *            ? enable AP : close AP.
	 */
	public static boolean setWifiAPEnabled(Context context, boolean enabled) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (enabled) {
			// disable wifi.
			wifiManager.setWifiEnabled(false);
		}

		try {
			WifiConfiguration configuration = new WifiConfiguration();
			configuration.SSID = Search.WIFI_AP_NAME;
			configuration.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.NONE);
			Method method = wifiManager.getClass().getMethod(
					"setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
			boolean open = (Boolean) method.invoke(wifiManager, configuration,
					enabled);
			return open;
		} catch (Exception e) {
			Log.e(TAG, "Can not set WiFi AP state, " + e);
			return false;
		}
	}

}
