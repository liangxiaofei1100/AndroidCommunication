package com.dreamlink.communication;

import java.text.DecimalFormat;

import com.dreamlink.communication.util.AppUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * This class is used for communication speed test.</br>
 * 
 * Server send byte[] to clients</br>
 * 
 * The byte[] size of one time communication is set by user.</br>
 * 
 * Calculate the transport speed every 1 second.</br>
 * 
 */
public class SpeedTest extends Activity {
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
			intent.setClass(this, SpeedTestServer.class);
		} else {
			intent.setClass(this, SpeedTestClient.class);
		}
		startActivity(intent);
	}

	public static String sizeFormat(double size) {
		if (size > 1024 * 1024) {
			Double dsize = (double) (size / (1024 * 1024));
			return new DecimalFormat("#.00").format(dsize) + "MB";
		} else if (size > 1024) {
			Double dsize = (double) size / (1024);
			return new DecimalFormat("#.00").format(dsize) + "KB";
		} else {
			return String.valueOf((int) size) + " Bytes";
		}
	}

	public static String getSpeedText(long totalSize, long startTime) {
		long currentTime = System.currentTimeMillis();
		long speed = totalSize / ((currentTime - startTime) / 1000);
		return "Total Size: " + SpeedTest.sizeFormat(totalSize) + "Speed: "
				+ SpeedTest.sizeFormat(speed) + "/S";
	}

}
