package com.dreamlink.communication.ui;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.UserHelper;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.util.NetWorkUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Set user information. include:user name,user icon and so on.
 */
public class UserInfoSetting extends Activity implements OnClickListener {
	private static final String TAG = "UserInfoSetting";

	public static final String EXTRA_IS_FIRST_START = "is_first_start";

	// name edit text
	private EditText mUserNameEditText;
	private Button mSaveButton;
	private TextView mIpAddressTextView;
	private TextView mAndroidVersionTextView;

	private UserHelper mUserHelper;
	private User mUser;

	private Notice mNotice;

	private boolean mIsFirstStart = false;

	// Title
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_user_setting);

		initTitle();
		initView();
		mUserHelper = new UserHelper(this);
		mUser = mUserHelper.loadUser();
		mNotice = new Notice(this);
		mIsFirstStart = getIntent()
				.getBooleanExtra(EXTRA_IS_FIRST_START, false);

		setUserInfo();
	}

	private void setUserInfo() {
		String defaultName = android.os.Build.MANUFACTURER;
		String name = mUser.getUserName();
		if (!TextUtils.isEmpty(name)
				&& !UserHelper.KEY_NAME_DEFAULT.equals(name)) {
			mUserNameEditText.setText(name);
		} else {
			mUserNameEditText.setText(defaultName);
		}

		mIpAddressTextView.setText(getString(R.string.userinfo_ip,
				NetWorkUtil.getLocalIpAddress()));

		mAndroidVersionTextView.setText(getString(
				R.string.userinfo_android_version,
				mUser.getSystemInfo().mAndroidVersionCode));
	}

	private void initView() {
		mUserNameEditText = (EditText) findViewById(R.id.name_editview);

		mSaveButton = (Button) findViewById(R.id.save_button);
		mSaveButton.setOnClickListener(this);

		mIpAddressTextView = (TextView) findViewById(R.id.ip_view);
		mAndroidVersionTextView = (TextView) findViewById(R.id.android_version_view);
	}

	private void initTitle() {
		// Title icon
		mTitleIcon = (ImageView) findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.user_icon_default);
		// Title text
		mTitleView = (TextView) findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.user_info_setting);
		// Title number
		mTitleNum = (TextView) findViewById(R.id.tv_title_num);
		mTitleNum.setVisibility(View.GONE);
		// Refresh icon
		mRefreshView = (ImageView) findViewById(R.id.iv_refresh);
		mRefreshView.setVisibility(View.GONE);
		// History icon
		mHistoryView = (ImageView) findViewById(R.id.iv_history);
		mHistoryView.setVisibility(View.GONE);
		
		//setting
		View view = findViewById(R.id.ll_setting);
		view.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.save_button:
			String name = mUserNameEditText.getText().toString();
			if (TextUtils.isEmpty(name)) {
				name = mUserNameEditText.getHint().toString();
			}

			mUser.setUserName(name);
			mUserHelper.saveUser(mUser);
			mNotice.showToast(R.string.userinfo_message_saved);
			Intent intent = new Intent();
			if (mIsFirstStart) {
				intent.setClass(this, MainUIFrame.class);
				startActivity(intent);
			} else {
				intent.putExtra("user", name);
				setResult(RESULT_OK, intent);
			}
			finish();

			break;
		default:
			break;
		}
	}
}
