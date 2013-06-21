package com.dreamlink.communication.data;

import android.os.Build;

public class SystemInfo {
	public String mAndroidVersion;
	public int mAndroidVersionCode;
	public boolean mIsWiFiDirectSupported;

	public SystemInfo() {

	}

	public SystemInfo getLocalSystemInfo() {
		mAndroidVersion = Build.VERSION.RELEASE;
		mAndroidVersionCode = Build.VERSION.SDK_INT;
		// After Android 4.0, Wi-Fi direct is supported.
		mIsWiFiDirectSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
		return this;
	}

}
