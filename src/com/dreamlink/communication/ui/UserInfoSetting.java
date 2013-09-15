package com.dreamlink.communication.ui;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.DreamConstant.Extra;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * set user infos
 *include:user name,user icon and so on.
 */
public class UserInfoSetting extends Activity implements OnClickListener {
	private static final String TAG = "UserInfoSetting";
	
	//name edit text
	private EditText mUserName_Edit;
	private Button mSaveButton;
	private TextView mIpVersionView;
	
	private String mUserName;
	
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
		
		mUserName_Edit = (EditText) findViewById(R.id.name_editview);
		mSaveButton = (Button) findViewById(R.id.save_button);
		mIpVersionView = (TextView) findViewById(R.id.ip_version_view);
		
		mUserName = android.os.Build.MANUFACTURER;
		
		mSaveButton.setOnClickListener(this);
		
		mUserHelper = new UserHelper(this);
		mUser = mUserHelper.loadUser();
		String name = mUser.getUserName();
		if (!TextUtils.isEmpty(name)
				&& !UserHelper.KEY_NAME_DEFAULT.equals(name)) {
			mUserName_Edit.setText(name);
		}else {
			mUserName_Edit.setText(mUserName);
		}
		
		mNotice = new Notice(this);

		mIpVersionView.setText(getString(R.string.userinfo_ip,
				NetWorkUtil.getLocalIpAddress()) + "\n"
				+ getString(R.string.userinfo_android_version,
						mUser.getSystemInfo().mAndroidVersionCode));
		
		mIsFirstStart = getIntent().getBooleanExtra(Extra.IS_FIRST_START, false);
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
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.save_button:
		case R.id.title_right_layout:
			String name = mUserName_Edit.getText().toString();
			if (TextUtils.isEmpty(name)) {
				name = mUserName_Edit.getHint().toString();
			} 
			
			mUser.setUserName(name);
			mUserHelper.saveUser(mUser);
			mNotice.showToast("Name saved");
			Intent intent = new Intent();
			if (mIsFirstStart) {
				intent.setClass(this, MainUIFrame2.class);
				startActivity(intent);
			}else {
				intent.putExtra("user", name);
				setResult(RESULT_OK,intent);
			}
			UserInfoSetting.this.finish();
			
			break;

		default:
			break;
		}
	}
}
