package com.dreamlink.communication.chat;

import com.dreamlink.communication.AppListActivity;
import com.dreamlink.communication.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

/**
 * Launch chat activity base on server or client mode.
 * 
 */
public class ChatLauncher extends Activity {
	private static final String TAG = "ChatLauncher";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int appID = getAppID();
		Log.d(TAG, "appID = " + appID);

		Intent intent = getIntent();
		if (intent != null) {
			launchActivity(intent.getBooleanExtra(
					AppListActivity.EXTRA_IS_SERVER, false));
			finish();
		}
	}

	private int getAppID() {
		int appID = -1;
		try {
			ActivityInfo activityInfo = getPackageManager().getActivityInfo(
					getComponentName(), PackageManager.GET_META_DATA);
			appID = activityInfo.metaData.getInt("app_id");
		} catch (NameNotFoundException e) {
			Log.e(TAG, "getAppID fail. " + e);
		}
		return appID;
	}

	private void launchActivity(boolean isServer) {
		Intent intent = new Intent();
		if (isServer) {
			intent.setClass(this, ChatServerActivity.class);
		} else {
			intent.setClass(this, ChatClientActivity.class);
		}
		startActivity(intent);
	}

}
