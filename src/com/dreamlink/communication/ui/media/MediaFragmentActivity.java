package com.dreamlink.communication.ui.media;


import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.MainUIFrame;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

public class MediaFragmentActivity extends FragmentActivity implements TabContentFactory, OnTabChangeListener{
	private static final String TAG = "MediaFragmentActivity";
	
	private TabHost mTabHost;
	private TextView audioView;
	private TextView videoView;
	
	private static final String TAG_ONE = "one";
	private static final String TAG_TWO = "two";
	
	private static String AUDIO_TITLE;
	private static String VIDEO_TITLE;
	
	public BroadcastReceiver mediaReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (DreamConstant.MEDIA_AUDIO_ACTION.equals(action)) {
				int audioSize = intent.getIntExtra(Extra.AUDIO_SIZE, 0);
				audioView.setText(AUDIO_TITLE  + "(" + audioSize + ")");
			}else if (DreamConstant.MEDIA_VIDEO_ACTION.equals(action)) {
				int videoSize = intent.getIntExtra(Extra.VIDEO_SIZE, 0);
				videoView.setText(VIDEO_TITLE + "(" + videoSize + ")");
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		setContentView(R.layout.ui_media_tab);
		
		if (arg0 != null){
			mTabHost.setCurrentTabByTag(arg0.getString("media_tab"));
		}
		
		AUDIO_TITLE = getResources().getString(R.string.audio);
		VIDEO_TITLE = getResources().getString(R.string.video);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(DreamConstant.MEDIA_AUDIO_ACTION);
		filter.addAction(DreamConstant.MEDIA_VIDEO_ACTION);
		registerReceiver(mediaReceiver, filter);
		
		initViews();
		Log.d(TAG, "onCreate end");
	}
	
	
	public void initViews(){
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabHost.addTab(mTabHost.newTabSpec(TAG_ONE).setIndicator(AUDIO_TITLE).setContent(R.id.media_frag1));
		mTabHost.addTab(mTabHost.newTabSpec(TAG_TWO).setIndicator(VIDEO_TITLE).setContent(R.id.media_frag2));
		
		mTabHost.setCurrentTab(0);
		mTabHost.setOnTabChangedListener(this);
		audioView = (TextView) mTabHost.getTabWidget().getChildAt(0).findViewById(android.R.id.title);
		videoView = (TextView) mTabHost.getTabWidget().getChildAt(1).findViewById(android.R.id.title);
	}

	@Override
	public void onTabChanged(String tabId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public View createTabContent(String tag) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed");
		Intent intent = new Intent(MainUIFrame.EXIT_ACTION);
		sendBroadcast(intent);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mediaReceiver);
	}
	
}
