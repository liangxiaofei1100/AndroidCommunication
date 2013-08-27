package com.dreamlink.communication.ui.app;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant;
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

public class AppFragmentActivity extends FragmentActivity implements TabContentFactory, OnTabChangeListener{
	private static final String TAG = "AppFragmentActivity";
	
	private TabHost mTabHost;
	
	private static final String TAG_ONE = "one";
	private static final String TAG_TWO = "two";
	
	private TextView mTextView1;
	private TextView mTextView2;
	
	private BroadcastReceiver appReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "get receiver");
			int normal_app_size = intent.getIntExtra(AppManager.NORMAL_APP_SIZE, 0);
			int game_app_size = intent.getIntExtra(AppManager.GAME_APP_SIZE, 0);
			
			mTextView1.setText(getResources().getString(R.string.app) + "(" + normal_app_size + ")");
			mTextView2.setText(getResources().getString(R.string.game) +  "(" + game_app_size + ")");
		}
	};
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		setContentView(R.layout.ui_app_tab);
		
		if (null != arg0){
			mTabHost.setCurrentTabByTag(arg0.getString("app_tab"));
		}
		
		IntentFilter filter = new IntentFilter(AppManager.ACTION_REFRESH_APP);
		registerReceiver(appReceiver, filter);
		
		initViews();
		Log.d(TAG, "onCreate end");
	}
	
	public void initViews(){
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabHost.addTab(mTabHost.newTabSpec(TAG_ONE).
				setIndicator(getResources().getString(R.string.app)).setContent(R.id.app_frag1));
		mTabHost.addTab(mTabHost.newTabSpec(TAG_TWO).
				setIndicator(getResources().getString(R.string.game)).setContent(R.id.app_frag2));
		
		mTabHost.setCurrentTab(0);
		mTabHost.setOnTabChangedListener(this);
		mTextView1 = (TextView) mTabHost.getTabWidget().getChildAt(0).findViewById(android.R.id.title);
		mTextView2 = (TextView) mTabHost.getTabWidget().getChildAt(1).findViewById(android.R.id.title);
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
		Intent intent = new Intent(DreamConstant.EXIT_ACTION);
		sendBroadcast(intent);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(appReceiver);
	}
	
}
