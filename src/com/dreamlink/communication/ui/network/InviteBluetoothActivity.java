package com.dreamlink.communication.ui.network;

import java.io.File;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.util.Log;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class InviteBluetoothActivity extends Activity implements
		OnClickListener {
	private static final String TAG = "InviteBluetoothActivity";
	// Title
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	private ImageView mMoreView;
	
	private Button mSendBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_invite_bluetooth);
		
		initTitle();
		initView();
	}
	
	private void initTitle() {
		// Title icon
		mTitleIcon = (ImageView) findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.network_neighborhood_bluetooth);
		// Title text
		mTitleView = (TextView) findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.bluetooth_invite);
		// Title number
		mTitleNum = (TextView) findViewById(R.id.tv_title_num);
		mTitleNum.setVisibility(View.GONE);
		// Refresh icon
		mRefreshView = (ImageView) findViewById(R.id.iv_refresh);
		mRefreshView.setVisibility(View.GONE);
		// History icon
		mHistoryView = (ImageView) findViewById(R.id.iv_history);
		mHistoryView.setVisibility(View.GONE);
		// menu more icon
		mMoreView = (ImageView) findViewById(R.id.iv_more);
		mMoreView.setVisibility(View.GONE);
	}

	private void initView() {
		mSendBtn = (Button) findViewById(R.id.bluetooth_send_btn);
		mSendBtn.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bluetooth_send_btn:
			// tranfer file by bluetooth
			Intent intent = new Intent();
			intent.setType("*/*");
			intent.setAction(Intent.ACTION_SEND);
			Uri uri = Uri.fromFile(new File(DreamUtil.package_source_dir));
			intent.putExtra(Intent.EXTRA_STREAM, uri);
			try {
				// set send by bluetooth ,only bluetooth
				intent.setClassName("com.android.bluetooth",
						"com.android.bluetooth.opp.BluetoothOppLauncherActivity");
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Log.d(TAG,
						"Can not find BluetoothOppLauncherActivity. Try mediatek's BluetoothShareGatewayActivity"
								+ e.toString());
				try {
					intent.setClassName("com.mediatek.bluetooth",
							"com.mediatek.bluetooth.BluetoothShareGatewayActivity");
					startActivity(intent);
				} catch (ActivityNotFoundException e2) {
					Log.d(TAG,
							"Can not find BluetoothShareGatewayActivity. Do not set class name."
									+ e.toString());
					intent.setComponent(null);
					startActivity(intent);
				}
			}
			break;

		default:
			break;
		}
	}

	public void exitActivity() {
		finish();
	}
}
