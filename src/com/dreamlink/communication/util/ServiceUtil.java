package com.dreamlink.communication.util;

import java.util.List;

import android.app.ActivityManager;
import android.content.Context;

public class ServiceUtil {

	/**
	 * Whether the service is running.
	 * @param context
	 * @param className
	 * @return
	 */
	public static boolean isServiceRunning(Context context, String className) {
		boolean isRunning = false;
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager
				.getRunningServices(100);
		for (ActivityManager.RunningServiceInfo serviceInfo : serviceList) {
			if (serviceInfo.service.getClassName().equals(className)) {
				isRunning = true;
				break;
			}
		}
		return isRunning;
	}
}
