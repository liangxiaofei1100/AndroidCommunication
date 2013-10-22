package com.dreamlink.communication.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
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
import com.dreamlink.communication.util.Log;

public class SettingsActivity extends Activity implements View.OnClickListener{
	private static final String TAG = "SettingsActivity";
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	private View mMoreView;
	
	private View mUserInfoSettingView;
	private Button mLogoutButton;
	private View mAboutView;
	private View mCheckUpdateView;
	private TextView mCurrentVisionTextView;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_settings);
		
		initTitleVIews();
		initView();
		
		updateCurrentVersion();
	}

	private void initView() {
		mLogoutButton = (Button) findViewById(R.id.btn_settings_logout);	
		mLogoutButton.setOnClickListener(this);
		
		mUserInfoSettingView = findViewById(R.id.ll_settings_user_info);
		mUserInfoSettingView.setOnClickListener(this);
		
		mCheckUpdateView = findViewById(R.id.ll_help_check_update);
		mCheckUpdateView.setOnClickListener(this);
		mCurrentVisionTextView = (TextView) findViewById(R.id.tv_help_current_version);

		mAboutView = findViewById(R.id.ll_help_about);
		mAboutView.setOnClickListener(this);
		
		//statistics
		View statisticsView = findViewById(R.id.ll_statistics);
		statisticsView.setOnClickListener(this);
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
		
		mMoreView = (LinearLayout) titleLayout.findViewById(R.id.ll_setting);
		mMoreView.setVisibility(View.GONE);
	}
	
	private void updateCurrentVersion() {
		PackageInfo packageInfo = null;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Get current version fail." + e);
		}
		if (packageInfo != null) {
			mCurrentVisionTextView.setText(getString(
					R.string.help_current_version, packageInfo.versionName));
		} else {
			mCurrentVisionTextView.setText(getString(
					R.string.help_current_version, ""));
		}
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
			
		case R.id.ll_help_about:
			intent.setClass(SettingsActivity.this, AboutActivity.class);
			startActivity(intent);
			break;
		case R.id.ll_statistics:
			//流量统计
			break;
		default:
			break;
		}
	}

}
