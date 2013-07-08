package com.dreamlink.communication;

import com.dreamlink.communication.util.AppUtil;
import com.dreamlink.communication.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * This class is used for communication stability test.</br>
 * 
 * Client every 100ms send message to server. </br>
 * 
 * Server show the received data and write to log file, then send the received
 * data return to clients.</br>
 * 
 * Clients show the received data from server and write to log file.
 * 
 */
public class StabilityTest extends Activity {
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
		intent.putExtra(EXTRA_APP_ID, appID);

		if (isServer) {
			intent.setClass(this, StabilityTestServer.class);
		} else {
			intent.setClass(this, StabilityTestClient.class);
		}
		startActivity(intent);
	}

}
