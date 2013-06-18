package com.dreamlink.communication.fileshare;

import com.dreamlink.communication.AppListActivity;
import com.dreamlink.communication.chat.ServerActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Launch file share activity base on server or client mode.
 * 
 */
public class FileShareLauncher extends Activity {

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
		if (isServer) {
			intent.setClass(this, ServerActivity.class);
		} else {
			intent.setClass(this, FileMainUI.class);
		}
		startActivity(intent);
	}
}
