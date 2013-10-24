package com.dreamlink.communication.ui.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant.Extra;

public class RecommendFragment extends Fragment {
	private static final String TAG = "RecommendFragment";
	private int mAppId = -1;
	
	/**
	 * Create a new instance of RecommendFragment, providing "appid" as an
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
		View rootView = inflater.inflate(R.layout.ui_recommend, container, false);
		initTitle(rootView);
		return rootView;
	}
	
	protected void initTitle(View rootView) {
		View customTitleView = rootView.findViewById(R.id.title);

		// title icon view
		ImageView mTitleIconView = (ImageView) customTitleView
				.findViewById(R.id.iv_title_icon);
		mTitleIconView.setImageResource(R.drawable.title_tuijian);

		// title name view
		TextView mTitleNameView = (TextView) customTitleView
				.findViewById(R.id.tv_title_name);
		mTitleNameView.setText(R.string.recommend);

		// history button
		View mHistroyView = customTitleView.findViewById(R.id.ll_history);
		mHistroyView.setVisibility(View.GONE);

		// setting button
		View mSettingView = customTitleView.findViewById(R.id.ll_setting);
		mSettingView.setVisibility(View.GONE);
	}
}
