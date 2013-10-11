package com.dreamlink.communication.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.LoginActivity;
import com.dreamlink.communication.ui.UserInfoSetting;
import com.dreamlink.communication.ui.history.HistoryActivity;

public class SettingsActivity extends Activity implements View.OnClickListener{

	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	private View mMoreView;
	
	private View mUserInfoSettingView;
	private Button mLogoutButton;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_settings);
		
		initTitleVIews();
		initView();
	};

	private void initView() {
		mLogoutButton = (Button) findViewById(R.id.btn_settings_logout);	
		mLogoutButton.setOnClickListener(this);
		
		mUserInfoSettingView = findViewById(R.id.ll_settings_user_info);
		mUserInfoSettingView.setOnClickListener(this);
	}

	public void initTitleVIews() {
		RelativeLayout titleLayout = (RelativeLayout) findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_setting);
		
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.settings_title);
		
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText("");
		mTitleNum.setVisibility(View.GONE);
		
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mRefreshView.setOnClickListener(this);
		mRefreshView.setVisibility(View.GONE);
		
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mHistoryView.setOnClickListener(this);
		mHistoryView.setVisibility(View.GONE);
		
		mMoreView = (LinearLayout) titleLayout.findViewById(R.id.ll_more);
		mMoreView.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		switch (v.getId()) {
		case R.id.iv_refresh:

			break;

		case R.id.iv_history:
			intent.setClass(SettingsActivity.this, HistoryActivity.class);
			startActivity(intent);
			break;
			
		case R.id.btn_settings_logout:
			intent.setClass(SettingsActivity.this, LoginActivity.class);
			startActivity(intent);
			break;
			
		case R.id.ll_settings_user_info:
			intent.setClass(SettingsActivity.this, UserInfoSetting.class);
			startActivity(intent);
			break;
		default:
			break;
		}
	}

}
