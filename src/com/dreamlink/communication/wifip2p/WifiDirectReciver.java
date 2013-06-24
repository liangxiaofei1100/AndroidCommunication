package com.dreamlink.communication.wifip2p;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;

@TargetApi(14)
public class WifiDirectReciver extends BroadcastReceiver {
	private boolean connectFlag = false;
	private ArrayList<WifiDirectDeviceNotify> observerList;

	public interface WifiDirectDeviceNotify {
		public void notifyDeviceChange();

		public void wifiP2pConnected();
	}

	public void registerObserver(WifiDirectDeviceNotify notify) {
		observerList.add(notify);
		notify.notifyDeviceChange();
	}

	public void unRegisterObserver(WifiDirectDeviceNotify notify) {
		observerList.remove(notify);
	}

	public WifiDirectReciver() {
		observerList = new ArrayList<WifiDirectReciver.WifiDirectDeviceNotify>();
	}

	@SuppressLint({ "NewApi", "NewApi", "NewApi", "NewApi" })
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {

		} else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
			// Check to see if Wi-Fi is enabled and notify appropriate activity
		} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
			// Call WifiP2pManager.requestPeers() to get a list of current peers
			for (WifiDirectDeviceNotify f : observerList) {
				f.notifyDeviceChange();
			}
		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
				.equals(action)) {
			if (((NetworkInfo) intent.getParcelableExtra("networkInfo"))
					.isConnected()) {
				if (!connectFlag) {
					connectFlag = true;
					if (observerList != null) {
						for (WifiDirectDeviceNotify f : observerList) {
							f.wifiP2pConnected();
						}
					}
				}
			} else {
				connectFlag = false;
			}
		} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
				.equals(action)) {
		}
	}

}
