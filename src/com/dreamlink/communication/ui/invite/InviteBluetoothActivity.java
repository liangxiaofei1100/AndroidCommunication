package com.dreamlink.communication.ui.invite;

import java.io.File;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;

public class InviteBluetoothActivity extends Activity implements OnClickListener {
	
	private RelativeLayout mBackLayout;
	private Button mSendBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.ui_invite_bluetooth);
		
		mBackLayout = (RelativeLayout) findViewById(R.id.title_back_layout);
		mSendBtn = (Button) findViewById(R.id.bluetooth_send_btn);
		
		mBackLayout.setOnClickListener(this);
		mSendBtn.setOnClickListener(this);
		
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back_layout:
			exitActivity();
			break;
		case R.id.bluetooth_send_btn:
			//tranfer file by bluetooth
			Intent intent = new Intent();
			intent.setType("*/*"); 
			//set send by bluetooth ,only bluetooth
			intent.setClassName("com.android.bluetooth",
					"com.android.bluetooth.opp.BluetoothOppLauncherActivity");
			intent.setAction(Intent.ACTION_SEND);
			Uri uri = Uri.fromFile(new File(DreamUtil.package_source_dir));
			intent.putExtra(Intent.EXTRA_STREAM, uri);
			startActivity(intent);
			break;

		default:
			break;
		}
	}
	
	public void exitActivity(){
		Intent intent = new Intent(InviteBluetoothActivity.this, InviteMainActivity.class);
		startActivity(intent);
		overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
		finish();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		exitActivity();
	}
}
