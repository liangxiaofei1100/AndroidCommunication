package com.dreamlink.communication;

import com.dreamlink.communication.data.User;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

public class UserInformationActivity extends Activity {
	@SuppressWarnings("unused")
	private static final String TAG = "UserInformationActivity";
	private UserHelper mUserHelper;
	private User mUser;

	private EditText mUserNameEditText;
	private TextView mIPTextView;
	private TextView mAndroidVisionTextView;

	private Notice mNotice;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_information);
		mUserHelper = new UserHelper(this);
		mUser = mUserHelper.loadUser();
		mNotice = new Notice(this);
		initView();
	}

	private void initView() {
		mUserNameEditText = (EditText) findViewById(R.id.et_ui_name);
		String name = mUser.getUserName();
		if (!TextUtils.isEmpty(name)
				&& !UserHelper.KEY_NAME_DEFAULT.equals(name)) {
			mUserNameEditText.setText(name);
		}

		mIPTextView = (TextView) findViewById(R.id.tv_ui_ip);
		mIPTextView.setText(getString(R.string.userinfo_ip,
				NetWorkUtil.getLocalIpAddress()));

		mAndroidVisionTextView = (TextView) findViewById(R.id.tv_ui_android_verion);
		mAndroidVisionTextView.setText(getString(
				R.string.userinfo_android_version,
				mUser.getSystemInfo().mAndroidVersionCode));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.user_info, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_ui_save:
			save();
			return true;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void save() {
		String name = mUserNameEditText.getText().toString();
		if (!TextUtils.isEmpty(name)) {
			mUser.setUserName(name);
			mUserHelper.saveUser(mUser);
			mNotice.showToast("Name saved");
			finish();
		} else {
			mNotice.showToast("Please input name");
		}
	}
}
