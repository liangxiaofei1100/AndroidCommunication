package com.dreamlink.communication.ui;

import com.dreamlink.communication.util.Log;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;

public class BaseFragmentActivity extends FragmentActivity {

//	@Override
//	public void onBackPressed() {
//		Intent intent = new Intent(DreamConstant.EXIT_ACTION);
//		sendBroadcast(intent);
//	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (KeyEvent.KEYCODE_BACK == keyCode) {
			Intent intent = new Intent(DreamConstant.EXIT_ACTION);
			sendBroadcast(intent);
		}
		return true;
	}
}
