package com.dreamlink.communication.ui;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import com.dreamlink.communication.R;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.notification.NotificationMgr;
import com.dreamlink.communication.ui.DreamConstant.Extra;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class StartLoader extends Activity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_loader);
		
		mHandler.sendEmptyMessage(LOADING);
		File file  = new File(DreamConstant.DEFAULT_SAVE_FOLDER);
		if (!file.exists()) {
			file.mkdirs();
		}
	}
	
	private static final int LOADING = 0x111;
	private static final int LOADED = 0x112;
	private Handler mHandler = new Handler() {
		Timer mTimer = new Timer();
		TimerTask mTask = new TimerTask() {
			@Override
			public void run() {
				sendEmptyMessage(LOADED);
			}
		};

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case LOADING:
				mTimer.schedule(mTask, 1500);
				break;
			case LOADED:
				if (UserHelper.getUserName(getApplicationContext()) == null) {
					launchInfo();
				}else {
					launchMain();
				}
				finish();
				overridePendingTransition(R.anim.push_right_in, R.anim.push_left_out);
				break;
			default:
				break;
			}
		};
	};
	
	public void launchInfo(){
		Intent intent = new Intent();
		intent.putExtra(Extra.IS_FIRST_START, true);
		intent.setClass(this, UserInfoSetting.class);
		startActivity(intent);
	}
	
	public void launchMain(){
		Intent intent = new Intent();
		intent.setClass(StartLoader.this, MainUIFrame2.class);
		startActivity(intent);
	}
}
