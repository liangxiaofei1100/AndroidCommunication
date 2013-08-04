package com.dreamlink.communication.ui;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

/**
 * set user infos
 *include:user name,user icon and so on.
 */
public class UserInfoSetting extends Activity implements OnClickListener {
	private static final String TAG = "UserInfoSetting";
	
	//name edit text
	private EditText mUserName_Edit;
	private Button mSaveButton;
	//title save layout
	private RelativeLayout mRightLayout;
	private String mUserName;
	
	private UserHelper mUserHelper;
	private User mUser;
	
	private Notice mNotice;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_user_setting);
		
		mUserName_Edit = (EditText) findViewById(R.id.name_editview);
		mSaveButton = (Button) findViewById(R.id.save_button);
		
		mUserName = android.os.Build.MANUFACTURER;
		
		mRightLayout = (RelativeLayout) findViewById(R.id.title_right_layout);
		
		mRightLayout.setOnClickListener(this);
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
			intent.putExtra("user", name);
			setResult(RESULT_OK,intent);
			UserInfoSetting.this.finish();
			
			break;

		default:
			break;
		}
	}
}
