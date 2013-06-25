package com.dreamlink.communication.chat;

import com.dreamlink.communication.AppListActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Launch chat activity base on server or client mode.
 * 
 */
public class ChatLauncher extends Activity {

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
			intent.setClass(this, ChatServerActivity.class);
		} else {
			intent.setClass(this, ChatClientActivity.class);
		}
		startActivity(intent);
	}

}
