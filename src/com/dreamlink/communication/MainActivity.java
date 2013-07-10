package com.dreamlink.communication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.client.ServerListActivity;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.server.ClientListActivity;

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

	@Override
	protected void onResume() {
		super.onResume();
		checkUserInfo();
	}

	private void checkUserInfo() {
		if (UserHelper.getUserName(getApplicationContext()) == null) {
			launchUserInfo();
		} else {
			// init local user.
			UserHelper userHelper = new UserHelper(getApplicationContext());
			User localUser = userHelper.loadUser();
			UserManager userManager = UserManager.getInstance();
			userManager.setLocalUser(localUser);
		}
	}

	private void launchUserInfo() {
		Intent intent = new Intent();
		intent.setClass(this, UserInformationActivity.class);
		startActivity(intent);
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
			searchClient();
			break;
		case R.id.btn_start_client:
			searchServer();
			break;

		default:
			break;
		}
	}

	private void searchServer() {
		Intent intent = new Intent();
		intent.setClass(this, ServerListActivity.class);
		startActivity(intent);
	}

	private void searchClient() {
		Intent intent = new Intent();
		intent.setClass(this, ClientListActivity.class);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_user_info:
			launchUserInfo();
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
