package com.dreamlink.communication.server.service;

import android.net.wifi.p2p.WifiP2pDevice;

import com.dreamlink.communication.search.SearchProtocol.OnSearchListener;

/**
 * use this to notify search result </br> 
 * 
 * be used in {@link OnSearchListener} to notify use
 * */

public class ServerInfo {
	/** the server name to show */
	private String server_name;
	/** three type : wifi,wifi-ap and wifi-direct */
	private String server_type;
	/** if wifi ,use this to connect */
	private String server_ip;
	/** if wifi-direct ,use this to get connect info to connect */
	private WifiP2pDevice server_device;
	/** if wifi-ap ,use this to get connect info to connect */
	private String server_ssid;

	public String getServer_name() {
		return server_name;
	}

	public void setServer_name(String server_name) {
		this.server_name = server_name;
	}

	public String getServer_type() {
		return server_type;
	}

	public void setServer_type(String server_type) {
		this.server_type = server_type;
	}

	public String getServer_ip() {
		return server_ip;
	}

	public void setServer_ip(String server_ip) {
		this.server_ip = server_ip;
	}

	public WifiP2pDevice getServer_device() {
		return server_device;
	}

	public void setServer_device(WifiP2pDevice server_device) {
		this.server_device = server_device;
	}

	public String getServer_ssid() {
		return server_ssid;
	}

	public void setServer_ssid(String server_ssid) {
		this.server_ssid = server_ssid;
	}

	@Override
	public String toString() {
		return "ServerInfo [server_name=" + server_name + ", server_type="
				+ server_type + ", server_ip=" + server_ip + ", server_device="
				+ server_device + ", server_ssid=" + server_ssid + "]";
	}
}
