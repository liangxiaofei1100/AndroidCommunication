package com.dreamlink.communication.wifip2p;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.http.conn.ManagedClientConnection;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.wifip2p.WifiDirectReciver.WifiDirectDeviceNotify;

@TargetApi(14)
public class WifiDirectManager {
	@SuppressLint("NewApi")
	private WifiP2pManager mWifiP2pManager;
	private WifiManager mWifiManager;
	public IntentFilter mIntentFilter;
	private boolean serverFlag = false;
	public Channel channel;
	private Context context;
	public WifiDirectReciver mWifiDirectRecevier;
	private SocketCommunicationManager communicationManager;

	public WifiDirectManager(Context context, boolean flag) {
		mWifiP2pManager = (WifiP2pManager) context
				.getSystemService(Context.WIFI_P2P_SERVICE);
		mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		serverFlag = flag;
		this.context = context;
		this.communicationManager = SocketCommunicationManager
				.getInstance(context);
		if (mWifiManager.isWifiEnabled()) {
			channel = mWifiP2pManager.initialize(context,
					context.getMainLooper(), null);
		}
		registerReceiver();
	}

	private void registerReceiver() {
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter
				.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter
				.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		mWifiDirectRecevier = new WifiDirectReciver(mWifiP2pManager, channel,
				communicationManager, serverFlag);
		// context.registerReceiver(mWifiDirectRecevier, mIntentFilter);
	}

	@SuppressLint({ "NewApi", "NewApi" })
	public void discover() {
		if (serverFlag) {
			setDeviceName(mWifiP2pManager, channel, "DreamLink"
					+ mWifiManager.getConnectionInfo().getMacAddress(), null);
		} else {
			setDeviceName(mWifiP2pManager, channel, mWifiManager
					.getConnectionInfo().getMacAddress(), null);
		}
		mWifiP2pManager.discoverPeers(channel, null);
	}

	private void setDeviceName(WifiP2pManager manager, Channel channel,
			String name, ActionListener listener) {
		try {
			Method method = manager.getClass().getMethod("setDeviceName",
					Channel.class, String.class, ActionListener.class);
			method.invoke(manager, channel, name, listener);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param serverFlag
	 *            ,if true as the group owner,else as the client
	 * */
	public void searchDirect(Context context, boolean server) {

		serverFlag = server;
		discover();
	}

	@SuppressLint({ "NewApi", "NewApi" })
	public void stopSearch() {
		mWifiP2pManager.stopPeerDiscovery(channel, null);
	}

	public void stopConnect() {

		try {
			if (mWifiDirectRecevier.serverSocket != null) {
				mWifiDirectRecevier.serverSocket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mWifiP2pManager.cancelConnect(channel, null);
		mWifiP2pManager.removeGroup(channel, null);
	}

	public void registerObserver(WifiDirectDeviceNotify notify) {
		mWifiDirectRecevier.registerObserver(notify);
	}

	public void unRegisterObserver(WifiDirectDeviceNotify notify) {
		mWifiDirectRecevier.unRegisterObserver(notify);
	}

	public void connect(WifiP2pConfig config) {
		mWifiP2pManager.connect(channel, config, null);
	}
}
