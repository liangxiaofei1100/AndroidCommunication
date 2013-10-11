package com.dreamlink.communication.ui.help;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Debug;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.debug.DebugActivity;
import com.dreamlink.communication.util.Log;

public class AboutActivity extends Activity implements OnClickListener {
	private static final String TAG = "AboutActivity";
	private static final boolean ENABLE_DEBUG = true;
	private Context mContext;
	// Title
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	private View mMoreMenuView;

	private ImageView mLogoImageView;
	private TextView mAppNameVersionTextView;
	private Button mOKButton;

	private int mClickCount = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_about);
		initTitle();
		initView();

		updateCurrentVersion();
		mClickCount = 0;
	}

	private void updateCurrentVersion() {
		PackageInfo packageInfo = null;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(),
					0);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Get current version fail." + e);
		}
		if (packageInfo != null) {
			mAppNameVersionTextView.setText(getString(
					R.string.about_app_name_version,
					packageInfo.applicationInfo.loadLabel(getPackageManager()),
					packageInfo.versionName));
		} else {
			mAppNameVersionTextView.setText(getString(
					R.string.about_app_name_version, "", ""));
		}
	}

	private void initView() {
		mLogoImageView = (ImageView) findViewById(R.id.iv_about_logo);
		mLogoImageView.setOnClickListener(this);

		mAppNameVersionTextView = (TextView) findViewById(R.id.tv_about_app_name_version);

		mOKButton = (Button) findViewById(R.id.btn_about_ok);
		mOKButton.setOnClickListener(this);
	}

	private void initTitle() {
		// Title icon
		mTitleIcon = (ImageView) findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.logo);
		// Title text
		mTitleView = (TextView) findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.help_about);
		// Title number
		mTitleNum = (TextView) findViewById(R.id.tv_title_num);
		mTitleNum.setVisibility(View.GONE);
		// Refresh icon
		mRefreshView = (ImageView) findViewById(R.id.iv_refresh);
		mRefreshView.setVisibility(View.GONE);
		// History icon
		mHistoryView = (ImageView) findViewById(R.id.iv_history);
		mHistoryView.setVisibility(View.GONE);
		
		//more menu view
		mMoreMenuView = findViewById(R.id.ll_more);
		mMoreMenuView.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_about_logo:
			mClickCount++;
			if (ENABLE_DEBUG && mClickCount > 6) {
				Intent intent = new Intent();
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				intent.setClass(this, DebugActivity.class);
				startActivity(intent);
				finish();
			}
			break;

		case R.id.btn_about_ok:
			finish();
			break;
		default:
			break;
		}
	}
}
