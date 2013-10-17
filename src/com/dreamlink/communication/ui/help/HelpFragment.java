package com.dreamlink.communication.ui.help;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.MainFragmentActivity;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.util.Log;

public class HelpFragment extends BaseFragment implements OnClickListener, OnMenuItemClickListener {
	private static final String TAG = "HelpFragment";
	private Context mContext;
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	private View mMoreView;
	private View mMenuView;
	private int mAppId = -1;

	private View mAboutView;
	private View mCheckUpdateView;
	private TextView mCurrentVisionTextView;

	/**
	 * Create a new instance of AppFragment, providing "appid" as an argument.
	 */
	public static HelpFragment newInstance(int appid) {
		HelpFragment f = new HelpFragment();

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
		mContext = getActivity();
		View rootView = inflater.inflate(R.layout.ui_help, container, false);
		getTitleVIews(rootView);

		initView(rootView);

		updateCurrentVersion();
		return rootView;
	}

	private void initView(View rootView) {
		mCheckUpdateView = rootView.findViewById(R.id.ll_help_check_update);
		mCheckUpdateView.setOnClickListener(this);
		mCurrentVisionTextView = (TextView) rootView
				.findViewById(R.id.tv_help_current_version);

		mAboutView = rootView.findViewById(R.id.ll_help_about);
		mAboutView.setOnClickListener(this);
	}

	private void updateCurrentVersion() {
		PackageInfo packageInfo = null;
		try {
			packageInfo = mContext.getPackageManager().getPackageInfo(
					mContext.getPackageName(), 0);
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

	public void getTitleVIews(View view) {
		RelativeLayout titleLayout = (RelativeLayout) view
				.findViewById(R.id.layout_title);
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
		
		mMenuView = (LinearLayout) titleLayout.findViewById(R.id.ll_menu_select);
		mMenuView.setOnClickListener(this);
		
		mMoreView = (LinearLayout) titleLayout.findViewById(R.id.ll_setting);
		mMoreView.setOnClickListener(this);
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
			
		case R.id.ll_help_about:
			intent.setClass(getActivity(), AboutActivity.class);
			startActivity(intent);
			break;
			
		case R.id.ll_menu_select:
			PopupMenu popupMenu = new PopupMenu(getActivity(), mMenuView);
			popupMenu.setOnMenuItemClickListener(this);
			MenuInflater inflater = popupMenu.getMenuInflater();
			inflater.inflate(R.menu.main_menu_item, popupMenu.getMenu());
			popupMenu.show();
			break;
		case R.id.ll_setting:
			PopupMenu popupMenu2 = new PopupMenu(getActivity(), mMoreView);
			popupMenu2.setOnMenuItemClickListener(this);
			MenuInflater inflater2 = popupMenu2.getMenuInflater();
			inflater2.inflate(R.menu.more_menu_item, popupMenu2.getMenu());
			popupMenu2.show();
			break;

		default:
			break;
		}
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		mFragmentActivity.setCurrentItem(item.getOrder());
		return true;
	}


}
