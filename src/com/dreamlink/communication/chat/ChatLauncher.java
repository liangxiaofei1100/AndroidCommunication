package com.dreamlink.communication.chat;

import com.dreamlink.communication.AppListActivity;
import com.dreamlink.communication.util.AppUtil;
import com.dreamlink.communication.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Launch chat activity base on server or client mode.
 * 
 */
public class ChatLauncher extends Activity {
	private static final String TAG = "ChatLauncher";
	public static final String EXTRA_APP_ID = "app_id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null) {
			launchActivity(intent.getBooleanExtra(
					AppListActivity.EXTRA_IS_SERVER, false));
			finish();
		}
	}

	private void launchActivity(boolean isServer) {
		Intent intent = new Intent();

		int appID = AppUtil.getAppID(this);
		Log.d(TAG, "appID = " + appID);
		intent.putExtra(EXTRA_APP_ID, appID);

		if (isServer) {
			intent.setClass(this, ChatServerActivity.class);
		} else {
			intent.setClass(this, ChatClientActivity.class);
		}
		startActivity(intent);
	}

}
