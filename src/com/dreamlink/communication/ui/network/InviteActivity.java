package com.dreamlink.communication.ui.network;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class InviteActivity extends BaseActivity implements OnClickListener{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_invite);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title_color));
		actionBar.setIcon(R.drawable.title_tiandi);
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.hide();
		
		//modify title text color
		int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
		TextView titleView = (TextView) findViewById(titleId);
		titleView.setTextColor(getResources().getColor(R.color.white));
		
		setTitle(R.string.invite_install);
		
		initTitle(R.string.invite_install, R.drawable.title_tiandi);
		
		initView();
	}
	
	private void initView(){
		View weixinView = findViewById(R.id.ll_invite_weixin);
		View qqView = findViewById(R.id.ll_invite_qq);
		View bluetoothView = findViewById(R.id.ll_invite_bluetooth);
		View weiboSinaView = findViewById(R.id.ll_invite_weibo_sina);
		View weiboTencentView = findViewById(R.id.ll_invite_weibo_tencent);
		View zeroGprsView = findViewById(R.id.ll_invite_zero_gprs);
		
		weixinView.setOnClickListener(this);
		qqView.setOnClickListener(this);
		bluetoothView.setOnClickListener(this);
		weiboSinaView.setOnClickListener(this);
		weiboTencentView.setOnClickListener(this);
		zeroGprsView.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		switch (v.getId()) {
		case R.id.ll_invite_weixin:
			break;
		case R.id.ll_invite_qq:
			break;
		case R.id.ll_invite_bluetooth:
			intent.setClass(this, InviteBluetoothActivity.class);
			startActivity(intent);
			break;
		case R.id.ll_invite_weibo_sina:
			break;
		case R.id.ll_invite_weibo_tencent:
			break;
		case R.id.ll_invite_zero_gprs:
			intent.setClass(this, HttpShareActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.finish();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
}
