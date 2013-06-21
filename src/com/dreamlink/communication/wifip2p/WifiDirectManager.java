package com.dreamlink.communication.wifip2p;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import com.dreamlink.communication.wifip2p.WifiDirectReciver.WifiDirectDeviceNotify;

@TargetApi(14)
public class WifiDirectManager implements WifiDirectDeviceNotify {
	@SuppressLint("NewApi")
	private WifiP2pManager mWifiP2pManager;
	private WifiManager mWifiManager;
	private boolean serverFlag = false;
	public Channel channel;
	private ArrayList<ManagerP2pDeivce> observerList;

	public interface ManagerP2pDeivce {
		public void deviceChange(ArrayList<WifiP2pDevice> list);

		public void hasConnect(WifiP2pInfo info);
	};

	public WifiDirectManager(Context context) {
		mWifiP2pManager = (WifiP2pManager) context
				.getSystemService(Context.WIFI_P2P_SERVICE);
		mWifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (mWifiManager.isWifiEnabled()) {
			channel = mWifiP2pManager.initialize(context,
					context.getMainLooper(), null);
			observerList = new ArrayList<WifiDirectManager.ManagerP2pDeivce>();
		}
	}

	public WifiDirectManager(Context context, boolean flag) {
		this(context);
		this.serverFlag = flag;
	}

	@SuppressLint({ "NewApi", "NewApi" })
	public void discover() {
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
		mWifiP2pManager.cancelConnect(channel, null);
		mWifiP2pManager.removeGroup(channel, null);
	}

	public void registerObserver(ManagerP2pDeivce notify) {
		observerList.add(notify);
	}

	public void unRegisterObserver(ManagerP2pDeivce notify) {
		observerList.remove(notify);
	}

	public void connect(WifiP2pConfig config) {
		mWifiP2pManager.connect(channel, config, null);
	}

	@Override
	public void wifiP2pConnected() {
		// TODO Auto-generated method stub
		notifyConnect();
	}

	private void getPeerDevice(final boolean flag) {
		final ArrayList<WifiP2pDevice> serverList = new ArrayList<WifiP2pDevice>();
		mWifiP2pManager.requestPeers(channel, new PeerListListener() {
			@SuppressLint({ "NewApi", "NewApi", "NewApi", "NewApi", "NewApi",
					"NewApi", "NewApi", "NewApi", "NewApi", "NewApi", "NewApi",
					"NewApi", "NewApi", "NewApi" })
			public void onPeersAvailable(WifiP2pDeviceList peers) {
				// // TODO Auto-generated method stub
				if (!flag) {
					for (WifiP2pDevice device : peers.getDeviceList()) {
						if (device.deviceName.contains("DreamLink")) {
							serverList.add(device);
						}
					}
				}
				if (observerList != null) {
					for (ManagerP2pDeivce f : observerList) {
						f.deviceChange(serverList);
					}
				}
			}
		});
	}

	private void notifyConnect() {
		mWifiP2pManager.requestConnectionInfo(this.channel,
				new ConnectionInfoListener() {
					public void onConnectionInfoAvailable(WifiP2pInfo info) {
						// TODO Auto-generated method stub
						if (observerList != null) {
							for (ManagerP2pDeivce f : observerList) {
								f.hasConnect(info);
							}
						}
					}
				});
	}

	@Override
	public void notifyDeviceChange() {
		// TODO Auto-generated method stub
		getPeerDevice(serverFlag);
	}

	public void setServerFlag(boolean flag) {
		if (serverFlag == flag) {
			return;
		}
		this.serverFlag = flag;
		this.stopSearch();
		if (serverFlag) {
			setDeviceName(mWifiP2pManager, channel, "DreamLink"
					+ mWifiManager.getConnectionInfo().getMacAddress(), null);
		} else {
			setDeviceName(mWifiP2pManager, channel, mWifiManager
					.getConnectionInfo().getMacAddress(), null);
		}
		this.discover();
		getPeerDevice(serverFlag);
	}

}
