package com.dreamlink.communication.ui.media;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragmentActivity;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;

public class MediaFragmentActivity extends BaseFragmentActivity implements
		TabContentFactory, OnTabChangeListener {
	private static final String TAG = "MediaFragmentActivity";
	private LayoutInflater layoutInflater;
	private TabHost mTabHost;
	private TextView audioView;
	private TextView videoView;

	private static final String TAG_ONE = "音频";
	private static final String TAG_TWO = "视频";
	private static final String[] TAB_TAG = {TAG_ONE, TAG_TWO};

	private static String AUDIO_TITLE;
	private static String VIDEO_TITLE;

	public BroadcastReceiver mediaReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (DreamConstant.MEDIA_AUDIO_ACTION.equals(action)) {
				int audioSize = intent.getIntExtra(Extra.AUDIO_SIZE, 0);
				audioView.setText(AUDIO_TITLE + "(" + audioSize + ")");
			} else if (DreamConstant.MEDIA_VIDEO_ACTION.equals(action)) {
				int videoSize = intent.getIntExtra(Extra.VIDEO_SIZE, 0);
				videoView.setText(VIDEO_TITLE + "(" + videoSize + ")");
			}
		}
	};

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		Log.d(TAG, "onCreate begin");
		setContentView(R.layout.ui_media_tab);

		if (arg0 != null) {
			mTabHost.setCurrentTabByTag(arg0.getString("media_tab"));
		}

		AUDIO_TITLE = getResources().getString(R.string.audio);
		VIDEO_TITLE = getResources().getString(R.string.video);

		IntentFilter filter = new IntentFilter();
		filter.addAction(DreamConstant.MEDIA_AUDIO_ACTION);
		filter.addAction(DreamConstant.MEDIA_VIDEO_ACTION);
		registerReceiver(mediaReceiver, filter);

		initViews();
		Log.d(TAG, "onCreate end");
	}

	public void initViews() {
		layoutInflater = LayoutInflater.from(this);
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG[0]).setIndicator(getTabItemView(0))
				.setContent(R.id.media_frag1));
		mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG[1]).setIndicator(getTabItemView(1))
				.setContent(R.id.media_frag2));

		mTabHost.setCurrentTab(0);
		mTabHost.setOnTabChangedListener(this);
		audioView = (TextView) mTabHost.getTabWidget().getChildAt(0)
				.findViewById(R.id.textview);
		videoView = (TextView) mTabHost.getTabWidget().getChildAt(1)
				.findViewById(R.id.textview);
	}
	
	/**
	 * 使用自定义tabhost tilte view
	 */
	private static final int[] VIS = {View.VISIBLE, View.GONE};
	private static final int[] VIS2 = {View.GONE, View.VISIBLE};
	private static final int[] COLORs = {0xff33b5e5, Color.GRAY};
	private View getTabItemView(int index){
		View view = layoutInflater.inflate(R.layout.tab_view, null);
	
		ImageView imageView = (ImageView) view.findViewById(R.id.cursor1);
		ImageView imageView2 = (ImageView) view.findViewById(R.id.cursor2);
		imageView.setVisibility(VIS[index]);
		imageView2.setVisibility(VIS2[index]);
		
		TextView textView = (TextView) view.findViewById(R.id.textview);		
		textView.setText(TAB_TAG[index]);
		textView.setTextColor(COLORs[index]);
		return view;
	}

	@Override
	public void onTabChanged(String tabId) {
		for (int j = 0; j < TAB_TAG.length; j++) {
			TextView textView = (TextView) mTabHost.getTabWidget().getChildAt(j).findViewById(R.id.textview);
			ImageView imageView = (ImageView) mTabHost.getTabWidget().getChildAt(j).findViewById(R.id.cursor1);
			ImageView imageView2 = (ImageView) mTabHost.getTabWidget().getChildAt(j).findViewById(R.id.cursor2);
			if (tabId.equals(TAB_TAG[j])) {
				textView.setTextColor(0xff33b5e5);
				imageView.setVisibility(View.VISIBLE);
				imageView2.setVisibility(View.GONE);
			}else {
				textView.setTextColor(Color.GRAY);
				imageView.setVisibility(View.GONE);
				imageView2.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public View createTabContent(String tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mediaReceiver);
	}

}
