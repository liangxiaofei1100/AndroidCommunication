package com.dreamlink.communication.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.TrafficStatics;
import com.dreamlink.communication.ui.BaseActivity;
import com.dreamlink.communication.ui.DreamUtil;

public class TrafficStatisticsActivity extends BaseActivity {
	private static final String TAG = "TrafficStatistics";
	private TextView mSendTrafficTv;
	private TextView mReceiveTrafficTv;
	private TrafficStatics mTrafficStatics;
	private LinearLayout mTrafficColorLayout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_traffic_statistics);
		
		initTitle(R.string.traffic_statistics, R.drawable.title_tiandi);
		
		mSendTrafficTv = (TextView) findViewById(R.id.tv_send_traffic);
		mReceiveTrafficTv = (TextView) findViewById(R.id.tv_receive_traffic);
		mTrafficColorLayout = (LinearLayout) findViewById(R.id.ll_traffic_color);
		
		mTrafficStatics = TrafficStatics.getInstance();
		
		long sendBytes = mTrafficStatics.getTotalTxBytes();
		long receiveBytes = mTrafficStatics.getTotalRxBytes();
		
		String sendTraffic = DreamUtil.getFormatSize(sendBytes);
		String receiveTraffic = DreamUtil.getFormatSize(receiveBytes);
		
		mSendTrafficTv.setText(getString(R.string.traffic_send, sendTraffic));
		mReceiveTrafficTv.setText(getString(R.string.traffic_receive, receiveTraffic));
		
		if (0 == sendBytes && 0== receiveBytes) {
			mTrafficColorLayout.setVisibility(View.GONE);
		}else {
			int count  = mTrafficColorLayout.getChildCount();
			mTrafficColorLayout.removeViews(0, count);
			
			addView((float) receiveBytes, R.color.traffic_send);
			addView((float) sendBytes, R.color.traffic_receive);
		}
	}
	
	public void addView(float weight, int color){
		View view = new View(this);
		view.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT, weight));
		view.setBackgroundResource(color);
		view.setId(color);
		
		mTrafficColorLayout.addView(view);
	}
}
