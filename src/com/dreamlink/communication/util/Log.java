package com.dreamlink.communication.util;

public class Log {
	public static final boolean isDebug = true;

	public static void d(String tag, String message) {
		if (isDebug) {
			android.util.Log.d(tag, message);
		}
	}

	public static void e(String tag, String message) {
		if (isDebug) {
			android.util.Log.e(tag, message);
		}
	}
}
