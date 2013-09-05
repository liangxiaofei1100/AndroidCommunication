package com.dreamlink.communication.util;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;

import com.dreamlink.communication.search.Search;
import com.dreamlink.communication.search.WiFiNameEncryption;

import android.content.Context;
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

		boolean isWifiDirectEnabled = isWifiDirectEnabled(context);

		if (networkInfo == null && isWifiApEnabled == false
				&& isWifiDirectEnabled == false) {
			return false;
		} else {
			return true;
		}
	}

	private static boolean isWifiDirectEnabled(Context context) {
		// TODO need implement.
		return true;
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
	 * get local ip address bytes.
	 * 
	 * @return
	 */
	public static byte[] getLocalIpAddressBytes() {
		InetAddress inetAddress = getLocalInetAddress();
		if (inetAddress != null) {
			return inetAddress.getAddress();
		} else {
			return new byte[4];
		}
	}

	/**
	 * get local ip address string. like 192.168.1.3
	 * 
	 * @return
	 */
	public static String getLocalIpAddress() {
		InetAddress inetAddress = getLocalInetAddress();
		if (inetAddress != null) {
			return inetAddress.getHostAddress();
		} else {
			return "";
		}
	}

	public static InetAddress getLocalInetAddress() {
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
						return inetAddress;
					} else if (inetAddress.getHostAddress().equals(
							"192.168.43.1")) {
						return inetAddress;
					}
				}
			}
		} catch (SocketException ex) {
			Log.e(TAG, "getLocalIpAddress() fail. " + ex.toString());
		}
		return null;
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
	 * Enable WiFi AP or close
	 * 
	 * @param context
	 * @param apName
	 *            If enable, apName is needed. If disable, apName is not needed,
	 *            just set null.
	 * @param enabled
	 *            ? enable AP : close AP.
	 * @return
	 */
	public static boolean setWifiAPEnabled(Context context, String apName,
			boolean enabled) {
		// TODO If disable wifi ap, should get ap name from ap info in case of
		// the name is different from the enabled name.
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		WifiConfiguration configuration = null;
		if (enabled) {
			// disable wifi.
			wifiManager.setWifiEnabled(false);
			if (apName == null) {
				Log.e(TAG, "setWifiAPEnabled, Ap name is null");
				return false;
			}
			configuration = new WifiConfiguration();
			configuration.SSID = apName;
			configuration.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.NONE);
		} else {
			try {
				Method method = wifiManager.getClass().getMethod(
						"getWifiApConfiguration");
				configuration = (WifiConfiguration) method.invoke(wifiManager);
			} catch (Exception e) {
				Log.e(TAG, "Can not get WiFi AP configuration, " + e);
			}
		}

		try {
			Method method = wifiManager.getClass().getMethod(
					"setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
			boolean result = (Boolean) method.invoke(wifiManager,
					configuration, enabled);
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Can not set WiFi AP state, " + e);
			return false;
		}
	}

	public static void clearWifiConnectHistory(Context context) {
		Log.d(TAG, "clearWifiConnectHistory");
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		List<WifiConfiguration> configurations = wifiManager
				.getConfiguredNetworks();
		if (configurations == null) {
			return;
		}
		for (WifiConfiguration configuration : configurations) {
			// SSID format is "ABC@PKQDJTTRJOATQMVX", so remove the "".
			String SSID = configuration.SSID.substring(1,
					configuration.SSID.length() - 1);
			if (WiFiNameEncryption.checkWiFiName(SSID)) {
				wifiManager.removeNetwork(configuration.networkId);
			}
		}
	}
}
