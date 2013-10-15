package com.dreamlink.communication.ui.app;

import android.content.Intent;
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
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.MainFragmentActivity;
import com.dreamlink.communication.ui.MainUIFrame;
import com.dreamlink.communication.ui.help.HelpActivity;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.ui.settings.SettingsActivity;
import com.dreamlink.communication.util.Log;

public class RecommendFragment extends BaseFragment implements OnClickListener, OnMenuItemClickListener {
	private static final String TAG = "RecommendFragment";
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private LinearLayout mRefreshLayout;
	private LinearLayout mHistoryLayout;
	private LinearLayout mMenuLayout;
	private LinearLayout mSettingLayout;
	private int mAppId = -1;
	
	/**
	 * Create a new instance of AppFragment, providing "appid" as an
	 * argument.
	 */
	public static RecommendFragment newInstance(int appid) {
		RecommendFragment f = new RecommendFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView = inflater.inflate(R.layout.ui_recommend, container, false);
		getTitleVIews(rootView);
		return rootView;
	}
	
	public void getTitleVIews(View view){
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_tuijian);
		mRefreshLayout = (LinearLayout) titleLayout.findViewById(R.id.ll_refresh);
		mRefreshLayout.setVisibility(View.GONE);
		mHistoryLayout = (LinearLayout) titleLayout.findViewById(R.id.ll_history);
		mMenuLayout = (LinearLayout) titleLayout.findViewById(R.id.ll_menu_select);
		mMenuLayout.setOnClickListener(this);
		mRefreshLayout.setOnClickListener(this);
		mHistoryLayout.setOnClickListener(this);
		mSettingLayout = (LinearLayout) titleLayout.findViewById(R.id.ll_setting);
		mSettingLayout.setOnClickListener(this);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText("精品推荐");
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText(getResources().getString(R.string.num_format, 0));
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.ll_refresh:
			
			break;
			
		case R.id.ll_history:
			Intent intent = new Intent();
			intent.setClass(getActivity(), HistoryActivity.class);
			startActivity(intent);
			break;
			
		case R.id.ll_menu_select:
			PopupMenu popupMenu = new PopupMenu(getActivity(), mMenuLayout);
			popupMenu.setOnMenuItemClickListener(this);
			MenuInflater inflater = popupMenu.getMenuInflater();
			inflater.inflate(R.menu.main_menu_item, popupMenu.getMenu());
			popupMenu.show();
			break;
		case R.id.ll_setting:
			MainUIFrame.startSetting(getActivity());
			break;

		default:
			break;
		}
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.setting:
			intent = new Intent(getActivity(), SettingsActivity.class);
			startActivity(intent);
			break;
		case R.id.help:
			intent = new Intent(getActivity(), HelpActivity.class);
			startActivity(intent);
			break;
		default:
			MainFragmentActivity.instance.setCurrentItem(item.getOrder());
			break;
		}
		return true;
	}
}
