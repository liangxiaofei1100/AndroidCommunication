package com.dreamlink.communication.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.LoginActivity;
import com.dreamlink.communication.ui.UserInfoSetting;
import com.dreamlink.communication.ui.history.HistoryActivity;

public class SettingsFragment extends BaseFragment implements View.OnClickListener{

	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	private int mAppId = -1;
	
	private View mUserInfoSettingView;
	private Button mLogoutButton;

	/**
	 * Create a new instance of AppFragment, providing "appid" as an argument.
	 */
	public static SettingsFragment newInstance(int appid) {
		SettingsFragment f = new SettingsFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID)
				: 1;
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.ui_settings, container,
				false);
		getTitleVIews(rootView);
		
		initView(rootView);
		return rootView;
	}

	private void initView(View rootView) {
		mLogoutButton = (Button) rootView.findViewById(R.id.btn_settings_logout);	
		mLogoutButton.setOnClickListener(this);
		
		mUserInfoSettingView = rootView.findViewById(R.id.ll_settings_user_info);
		mUserInfoSettingView.setOnClickListener(this);
	}

	public void getTitleVIews(View view) {
		RelativeLayout titleLayout = (RelativeLayout) view
				.findViewById(R.id.layout_title);
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
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		switch (v.getId()) {
		case R.id.iv_refresh:

			break;

		case R.id.iv_history:
			intent.setClass(getActivity(), HistoryActivity.class);
			startActivity(intent);
			break;
			
		case R.id.btn_settings_logout:
			intent.setClass(getActivity(), LoginActivity.class);
			startActivity(intent);
			break;
			
		case R.id.ll_settings_user_info:
			intent.setClass(getActivity(), UserInfoSetting.class);
			startActivity(intent);
			break;

		default:
			break;
		}
	}


}
