package com.dreamlink.communication.ui;

import com.dreamlink.communication.R;
import com.dreamlink.communication.UserHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class LoginActivity extends Activity implements View.OnClickListener {
	private Button mLoginButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_login);

		initView();
	}

	private void initView() {
		mLoginButton = (Button) findViewById(R.id.btn_login);
		mLoginButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_login:
			if (UserHelper.getUserName(getApplicationContext()) == null) {
				launchInfo();
			} else {
				launchMain();
			}
			finish();
			break;

		default:
			break;
		}
	}

	private void launchInfo() {
		Intent intent = new Intent();
		intent.putExtra(UserInfoSetting.EXTRA_IS_FIRST_START, true);
		intent.setClass(this, UserInfoSetting.class);
		startActivity(intent);
	}

	private void launchMain() {
		Intent intent = new Intent();
		intent.setClass(this, MainUIFrame.class);
		startActivity(intent);
	}
}
