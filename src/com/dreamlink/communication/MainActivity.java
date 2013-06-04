package com.dreamlink.communication;

import com.dreamlink.communication.client.ClientActivity;
import com.dreamlink.communication.server.ServerActivity;

import android.R.anim;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity implements View.OnClickListener {
	@SuppressWarnings("unused")
	private static final String TAG = "MainActivity";
	private Button mStartServerButton;
	private Button mStartClientButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
	}

	private void initView() {
		mStartServerButton = (Button) findViewById(R.id.btn_start_server);
		mStartServerButton.setOnClickListener(this);
		mStartClientButton = (Button) findViewById(R.id.btn_start_client);
		mStartClientButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_start_server:
			startServer();
			break;
		case R.id.btn_start_client:
			startClient();
			break;

		default:
			break;
		}
	}

	private void startServer() {
		Intent intent = new Intent();
		intent.setClass(this, ServerActivity.class);
		startActivity(intent);
	}

	private void startClient() {
		Intent intent = new Intent();
		intent.setClass(this, ClientActivity.class);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}