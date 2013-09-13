package com.dreamlink.communication.ui.network;

import com.dreamlink.communication.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.RelativeLayout;

public class InviteMainActivity extends Activity implements OnClickListener {
	
	private RelativeLayout mBackLayout;
	private RelativeLayout mBluetoothInvite;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.ui_invite_main);
		
		mBackLayout = (RelativeLayout) findViewById(R.id.title_back_layout);
		mBluetoothInvite  = (RelativeLayout) findViewById(R.id.bluetooth_invite_layout);
		
		mBackLayout.setOnClickListener(this);
		mBluetoothInvite.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back_layout:
			finish();
			break;
		case R.id.bluetooth_invite_layout:
			Intent intent = new Intent(InviteMainActivity.this, InviteBluetoothActivity.class);
			startActivity(intent);
			overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
			finish();
			break;

		default:
			break;
		}
	}
	
}
