package com.dreamlink.communication.ui.file;


import com.dreamlink.communication.R;
import com.dreamlink.communication.util.Log;

import android.app.Activity;
import android.os.Bundle;

/**
 * access remote server
 */
public class RemoteShareActivity extends Activity{
	private static final String TAG = "RemoteShareActivity";
	
	
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		setContentView(R.layout.ui_remote_share);
		Log.d(TAG, "onCreate end");
	}
	
}
