package com.dreamlink.communication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.history.HistoryActivity;

public class HelpFragment extends BaseFragment implements OnClickListener {

	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	private int mAppId = -1;

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
		View rootView = inflater.inflate(R.layout.ui_help, container,
				false);
		getTitleVIews(rootView);
		return rootView;
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
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_refresh:

			break;

		case R.id.iv_history:
			Intent intent = new Intent();
			intent.setClass(getActivity(), HistoryActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
	}

}
