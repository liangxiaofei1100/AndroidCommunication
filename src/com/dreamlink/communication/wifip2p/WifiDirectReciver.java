package com.dreamlink.communication.wifip2p;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import com.dreamlink.communication.SocketCommunicationManager;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

@TargetApi(14)
public class WifiDirectReciver extends BroadcastReceiver {
	private WifiP2pManager wifiP2pManager;
	private Channel channel;
	private boolean connectFlag = false;
	private boolean serverFlag = false;
	private SocketCommunicationManager communicationManager;
	public ServerSocket serverSocket;
	private ArrayList<WifiP2pDevice> serverList;
	private ArrayList<WifiDirectDeviceNotify> observerList;

	public interface WifiDirectDeviceNotify {
		public void notifyDeviceChange(ArrayList<WifiP2pDevice> serverList);

		public void wifiP2pConnected(WifiP2pInfo info);
	}

	public void registerObserver(WifiDirectDeviceNotify notify) {
		observerList.add(notify);
		if (serverList != null) {
			notify.notifyDeviceChange(serverList);
		}
	}

	public void unRegisterObserver(WifiDirectDeviceNotify notify) {
		observerList.remove(notify);
	}

	public WifiDirectReciver(WifiP2pManager wifiP2pManager, Channel channel,
			SocketCommunicationManager communicationManager, boolean server) {
		this.wifiP2pManager = wifiP2pManager;
		this.communicationManager = communicationManager;
		this.channel = channel;
		this.serverFlag = server;
		serverList = new ArrayList<WifiP2pDevice>();
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
			wifiP2pManager.requestPeers(channel, new PeerListListener() {
				@SuppressLint({ "NewApi", "NewApi", "NewApi", "NewApi",
						"NewApi", "NewApi", "NewApi", "NewApi", "NewApi",
						"NewApi", "NewApi", "NewApi", "NewApi", "NewApi" })
				public void onPeersAvailable(WifiP2pDeviceList peers) {
					// TODO Auto-generated method stub
					serverList.clear();
					Log.e("ArbiterLiu", peers.getDeviceList().size() + "");
					if (!serverFlag) {
						for (WifiP2pDevice device : peers.getDeviceList()) {
							if (device.deviceName.contains("DreamLink")) {
								serverList.add(device);
							}
						}
					}
					if (observerList != null) {
						for (WifiDirectDeviceNotify f : observerList) {
							f.notifyDeviceChange(serverList);
						}
					}
				}
			});
		} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
				.equals(action)) {
			if (((NetworkInfo) intent.getParcelableExtra("networkInfo"))
					.isConnected()) {
				if (!connectFlag) {
					connectFlag = true;
					this.wifiP2pManager.requestConnectionInfo(this.channel,
							new ConnectionInfoListener() {
								public void onConnectionInfoAvailable(
										WifiP2pInfo info) {
									// TODO Auto-generated method stub
									if (observerList != null) {
										for (WifiDirectDeviceNotify f : observerList) {
											f.wifiP2pConnected(info);
										}
									}
								}
							});
				}
			}
		} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
				.equals(action)) {
		}
	}

}
