package com.dreamlink.communication.ui.help;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.util.Log;

public class HelpActivity extends Activity implements OnClickListener {
	private static final String TAG = "HelpFragment";
	private Context mContext;
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	private View mMoreView;

	private View mAboutView;
	private View mCheckUpdateView;
	private TextView mCurrentVisionTextView;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_help);
		initTitleVIews();

		initView();

		updateCurrentVersion();
	};

	private void initView() {
		mCheckUpdateView = findViewById(R.id.ll_help_check_update);
		mCheckUpdateView.setOnClickListener(this);
		mCurrentVisionTextView = (TextView) findViewById(R.id.tv_help_current_version);

		mAboutView = findViewById(R.id.ll_help_about);
		mAboutView.setOnClickListener(this);
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

	public void initTitleVIews() {
		RelativeLayout titleLayout = (RelativeLayout) findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_help);

		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.help_title);

		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
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

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		switch (v.getId()) {
		case R.id.iv_refresh:

			break;

		case R.id.iv_history:
			intent.setClass(HelpActivity.this, HistoryActivity.class);
			startActivity(intent);
			break;
			
		case R.id.ll_help_about:
			intent.setClass(HelpActivity.this, AboutActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
	}

}
