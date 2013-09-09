package com.dreamlink.communication;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.dreamlink.communication.lib.util.AppUtil;

public class FileTransportTest extends Activity {
	private static final String TAG = "FileTransportTest";

	public static final String EXTRA_APP_ID = "app_id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_readme);

		TextView readmeTextView = (TextView) findViewById(R.id.tv_test_readme);
		readmeTextView.setText(R.string.readme_file_transport_test);
	}

	private void launchActivity(boolean isServer) {
		Intent intent = new Intent();

		int appID = AppUtil.getAppID(this);
		intent.putExtra(EXTRA_APP_ID, appID);

		if (isServer) {
			intent.setClass(this, FileTransportTestServer.class);
		} else {
			intent.setClass(this, FileTransportTestClient.class);
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
		long speed = 0;
		if (currentTime - startTime != 0) {
			try {
				speed = totalSize / ((currentTime - startTime) / 1000);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
			
		}
		return "Total Size: " + sizeFormat(totalSize) + "Speed: "
				+ sizeFormat(speed) + "/S";
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_client:
			launchActivity(false);
			finish();
			break;
		case R.id.menu_server:
			launchActivity(true);
			finish();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.test_main, menu);
		return true;
	}

}
