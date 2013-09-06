package com.dreamlink.communication.ui.file;


import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragmentActivity;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.util.Log;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

public class FileFragmentActivity extends BaseFragmentActivity implements TabContentFactory, OnTabChangeListener{
	private static final String TAG = "FileFragmentActivity";
	
	private TabHost mTabHost;
	private LayoutInflater layoutInflater;
	
	private static final String TAG_ONE = "全部文件";
	private static final String TAG_TWO = "文件分类";
	private static final String[] TAB_TAG = {TAG_ONE, TAG_TWO};
	
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
		layoutInflater = LayoutInflater.from(this);
		
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabHost.addTab(mTabHost.newTabSpec(TAG_ONE).setIndicator(getTabItemView(0)).setContent(R.id.file_frag2));
		mTabHost.addTab(mTabHost.newTabSpec(TAG_TWO).setIndicator(getTabItemView(1)).setContent(R.id.file_frag1));
		
		mTabHost.setCurrentTab(0);
		mTabHost.setOnTabChangedListener(this);
	}
	
	/**
	 * 给Tab按钮设置图标和文字
	 */
	private static final int[] VIS = {View.VISIBLE, View.GONE};
	private static final int[] VIS2 = {View.GONE, View.VISIBLE};
	private static final int[] COLORs = {0xff33b5e5, Color.GRAY};
	private View getTabItemView(int index){
		View view = layoutInflater.inflate(R.layout.tab_view, null);
	
		ImageView imageView = (ImageView) view.findViewById(R.id.cursor1);
		ImageView imageView2 = (ImageView) view.findViewById(R.id.cursor2);
		imageView.setVisibility(VIS[index]);
		imageView2.setVisibility(VIS2[index]);
		
		TextView textView = (TextView) view.findViewById(R.id.textview);		
		textView.setText(TAB_TAG[index]);
		textView.setTextColor(COLORs[index]);
		return view;
	}

	@Override
	public void onTabChanged(String tabId) {
		for (int j = 0; j < TAB_TAG.length; j++) {
			TextView textView = (TextView) mTabHost.getTabWidget().getChildAt(j).findViewById(R.id.textview);
			ImageView imageView = (ImageView) mTabHost.getTabWidget().getChildAt(j).findViewById(R.id.cursor1);
			ImageView imageView2 = (ImageView) mTabHost.getTabWidget().getChildAt(j).findViewById(R.id.cursor2);
			if (tabId.equals(TAB_TAG[j])) {
				textView.setTextColor(0xff33b5e5);
				imageView.setVisibility(View.VISIBLE);
				imageView2.setVisibility(View.GONE);
			}else {
				textView.setTextColor(Color.GRAY);
				imageView.setVisibility(View.GONE);
				imageView2.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public View createTabContent(String tag) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
