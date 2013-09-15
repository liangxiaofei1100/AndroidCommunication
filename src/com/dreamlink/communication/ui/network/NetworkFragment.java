package com.dreamlink.communication.ui.network;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.app.RecommendFragment;
import com.dreamlink.communication.ui.file.RemoteShareActivity;
import com.dreamlink.communication.util.Log;

public class NetworkFragment extends BaseFragment implements
		View.OnClickListener {
	@SuppressWarnings("unused")
	private static final String TAG = "NetworkFragment";
	private Context mContext;
	// Title
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;

	private View mBluetoothInviteView;
	private View mCreateNetworkView;
	private View mJoinNetworkView;
	private View mShareView;
	
	private int mAppId = -1;
	
	/**
	 * Create a new instance of AppFragment, providing "appid" as an
	 * argument.
	 */
	public static NetworkFragment newInstance(int appid) {
		NetworkFragment f = new NetworkFragment();

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.ui_network_neighborhood,
				container, false);
		mContext = getActivity();
		
		initTitle(rootView);
		initView(rootView);

		Log.d(TAG, "onCreate end");
		return rootView;
	}

	private void initTitle(View view) {
		// Title icon
		mTitleIcon = (ImageView) view.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.network_title);
		// Title text
		mTitleView = (TextView) view.findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.network_neighborhood);
		// Title number
		mTitleNum = (TextView) view.findViewById(R.id.tv_title_num);
		mTitleNum.setVisibility(View.GONE);
		// Refresh icon
		mRefreshView = (ImageView) view.findViewById(R.id.iv_refresh);
		mRefreshView.setVisibility(View.GONE);
		// History icon
		mHistoryView = (ImageView) view.findViewById(R.id.iv_history);
		mHistoryView.setVisibility(View.GONE);
	}

	private void initView(View view) {
		mBluetoothInviteView = view
				.findViewById(R.id.lv_network_neighborhood_bluetooth);
		mBluetoothInviteView.setOnClickListener(this);
		mCreateNetworkView = view
				.findViewById(R.id.lv_network_neighborhood_create);
		mCreateNetworkView.setOnClickListener(this);
		mJoinNetworkView = view.findViewById(R.id.lv_network_neighborhood_join);
		mJoinNetworkView.setOnClickListener(this);
		mShareView = view.findViewById(R.id.lv_network_neighborhood_share);
		mShareView.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		switch (v.getId()) {
		case R.id.lv_network_neighborhood_bluetooth:
			intent.setClass(mContext, InviteBluetoothActivity.class);
			startActivity(intent);
			break;
		case R.id.lv_network_neighborhood_create:
			intent.setClass(mContext, CreateNetworkActivity.class);
			startActivity(intent);
			break;
		case R.id.lv_network_neighborhood_join:
			intent.setClass(mContext, JoinNetworkActivity.class);
			startActivity(intent);
			break;
		case R.id.lv_network_neighborhood_share:
			intent.setClass(mContext, RemoteShareActivity.class);
			startActivity(intent);
		default:
			break;
		}

	}
}
