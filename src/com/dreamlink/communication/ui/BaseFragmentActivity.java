package com.dreamlink.communication.ui;

import com.dreamlink.communication.util.Log;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

public class BaseFragmentActivity extends FragmentActivity {
	private static final String TAG = "BaseFragmentActivity";
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(DreamConstant.EXIT_ACTION);
		sendBroadcast(intent);
	}
}
