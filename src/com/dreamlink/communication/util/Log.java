package com.dreamlink.communication.util;

import android.R.anim;

public class Log {
	public static final boolean isDebug = true;
	private static final String TAG = "dmLink/";

	public static void i(String tag, String message){
		if (isDebug) {
			android.util.Log.i(TAG + tag, message);
		}
	}
	
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
