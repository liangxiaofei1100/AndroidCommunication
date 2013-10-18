package com.dreamlink.communication.ui.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant.Extra;

public class RecommendFragment extends BaseFragment {
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
		return rootView;
	}
}
