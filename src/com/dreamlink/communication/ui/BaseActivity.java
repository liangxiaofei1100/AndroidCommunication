package com.dreamlink.communication.ui;

import com.dreamlink.communication.R;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

public class BaseActivity extends FragmentActivity {

	// title view
	protected View mCustomTitleView;
	protected View mSelectView;
	protected ImageView mTitleIconView;
	protected TextView mTitleNameView;
	protected TextView mTitleNumView;
	protected View mHistroyView;
	protected View mSettingView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	protected void initTitle(int titleName, int titleIcon) {
		mCustomTitleView = findViewById(R.id.title);
		// select view
		mSelectView = mCustomTitleView.findViewById(R.id.ll_menu_select);

		// title icon view
		mTitleIconView = (ImageView) mCustomTitleView
				.findViewById(R.id.iv_title_icon);
		mTitleIconView.setImageResource(titleIcon);

		// title name view
		mTitleNameView = (TextView) mCustomTitleView
				.findViewById(R.id.tv_title_name);
		mTitleNameView.setText(titleName);

		mTitleNumView = (TextView) mCustomTitleView
				.findViewById(R.id.tv_title_num);
		mTitleNumView.setVisibility(View.GONE);

		// history button
		mHistroyView = mCustomTitleView.findViewById(R.id.ll_history);
		mHistroyView.setVisibility(View.GONE);

		// setting button
		mSettingView = mCustomTitleView.findViewById(R.id.ll_setting);
		mSettingView.setVisibility(View.GONE);
	}

}
