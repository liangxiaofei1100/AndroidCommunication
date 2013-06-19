package com.dreamlink.communication.util;

public class Log {
	public static final boolean isDebug = true;
	private static final String TAG = "dmLink/";

	public static void d(String tag, String message) {
		if (isDebug) {
			android.util.Log.d(TAG + tag, message);
		}
	}

	public static void e(String tag, String message) {
		if (isDebug) {
			android.util.Log.e(TAG + tag, message);
		}
	}
}
