package com.dreamlink.communication.ui;

import com.dreamlink.communication.util.Log;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;

public class BaseFragmentActivity extends FragmentActivity {
	private static final String TAG = "BaseFragmentActivity";
//	@Override
//	public void onBackPressed() {
//		Intent intent = new Intent(DreamConstant.EXIT_ACTION);
//		sendBroadcast(intent);
//	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "keyCode=" + keyCode);
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent(DreamConstant.EXIT_ACTION);
			sendBroadcast(intent);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
