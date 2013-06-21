package com.dreamlink.communication.data;

import java.io.Serializable;

public class User implements Serializable {

	private String mName = "Unkown";
	public static final int NETWORK_MODE_UNKOWN = 0;
	public static final int NETWORK_MODE_AP = 1;
	public static final int NETWORK_MODE_STA = 2;
	public static final int NETWORK_MODE_WIFI_DIRECT = 3;
	public static final int NETWORK_MODE_STA_AND_WIFI_DIRECT = 4;
	private int mNetworkMode = NETWORK_MODE_UNKOWN;

	private SystemInfo mSystemInfo;

	public User() {

	}

	public void setUserName(String name) {
		mName = name;
	}

	public String getUserName() {
		return mName;
	}

	public int getNetworkMode() {
		return mNetworkMode;
	}

	public void setNetworkMode(int mode) {
		mNetworkMode = mode;
	}

	public void setSystemInfo(SystemInfo systemInfo) {
		mSystemInfo = systemInfo;
	}

	public SystemInfo getSystemInfo() {
		return mSystemInfo;
	}
}
