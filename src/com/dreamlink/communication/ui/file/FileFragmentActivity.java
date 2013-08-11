package com.dreamlink.communication.ui.file;


import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.MainUIFrame;
import com.dreamlink.communication.util.Log;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

public class FileFragmentActivity extends FragmentActivity implements TabContentFactory, OnTabChangeListener{
	private static final String TAG = "FileFragmentActivity";
	
	private TabHost mTabHost;
	
	private static final String TAG_ONE = "one";
	private static final String TAG_TWO = "two";
	
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		setContentView(R.layout.ui_file_tab);
		if (arg0 != null){
			mTabHost.setCurrentTabByTag(arg0.getString("file_tab"));
		}
		
		initViews();
		Log.d(TAG, "onCreate end");
	}
	
	
	public void initViews(){
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabHost.addTab(mTabHost.newTabSpec(TAG_ONE).setIndicator("文件分类").setContent(R.id.file_frag1));
		mTabHost.addTab(mTabHost.newTabSpec(TAG_TWO).setIndicator("全部文件").setContent(R.id.file_frag2));
		
		mTabHost.setCurrentTab(0);
		mTabHost.setOnTabChangedListener(this);
		//得到Tab标题中的Textview控件，可以用此修改标题，颜色，字体大小等等
		TextView textView = (TextView) mTabHost.getTabWidget().getChildAt(0).findViewById(android.R.id.title);
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
	
}
