package com.dreamlink.communication.ui.settings;

import android.os.Bundle;
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_traffic_statistics);
		
		getSupportActionBar().hide();
		
		initTitle(R.string.traffic_statistics, R.drawable.title_tiandi);
		
		mSendTrafficTv = (TextView) findViewById(R.id.tv_send_traffic);
		mReceiveTrafficTv = (TextView) findViewById(R.id.tv_receive_traffic);
		
		mTrafficStatics = TrafficStatics.getInstance();
		
		long sendBytes = mTrafficStatics.getTotalTxBytes();
		long receiveBytes = mTrafficStatics.getTotalRxBytes();
		
		String sendTraffic = DreamUtil.getFormatSize(sendBytes);
		String receiveTraffic = DreamUtil.getFormatSize(receiveBytes);
		
		mSendTrafficTv.setText(getString(R.string.traffic_send, sendTraffic));
		mReceiveTrafficTv.setText(getString(R.string.traffic_receive, receiveTraffic));
	}
}
