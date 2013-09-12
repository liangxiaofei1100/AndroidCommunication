package com.dreamlink.communication.ui;


import com.dreamlink.communication.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Win8 style main ui
 * @author yuri
 * @date 2013年9月11日16:50:57
 */
public class MainUIFrame2 extends Activity implements OnClickListener, OnItemClickListener {
	private static final String TAG = "MainUIFrame2";
	
	private GridView mGridView;
	private MainUIAdapter mAdapter;
	
	private ImageView mUpLoadView,mSettingView, mHelpView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_main_new);
		
		initView();
	}
	
	public void initView(){
		mUpLoadView = (ImageView) findViewById(R.id.iv_upload);
		mSettingView = (ImageView) findViewById(R.id.iv_setting);
		mHelpView = (ImageView) findViewById(R.id.iv_help);
		mUpLoadView.setOnClickListener(this);
		mSettingView.setOnClickListener(this);
		mHelpView.setOnClickListener(this);
		
		mGridView = (GridView) findViewById(R.id.gv_main_menu);
		mAdapter = new MainUIAdapter(this, mGridView);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.iv_upload:
			break;
		case R.id.iv_setting:
			break;
		case R.id.iv_help:
			break;
		default:
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		Intent intent = new Intent(MainUIFrame2.this, MainFragmentActivity.class);
		intent.putExtra("position", position);
		startActivity(intent);
	}
	
}
