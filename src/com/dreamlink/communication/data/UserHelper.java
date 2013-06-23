package com.dreamlink.communication.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

public class UserHelper {
	private static final String TAG = "UserHelper";
	public static final String SHAREPREFERENCE_NAME = "user_info";
	public static final String KEY_NAME = "name";
	public static final String KEY_NAME_DEFAULT = "Unkown name";
	private Context mContext;

	public UserHelper(Context context) {
		mContext = context;
	}

	/**
	 * Get the set name, if name is not set, return null
	 * 
	 * @param context
	 * @return
	 */
	public static String getUserName(Context context) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				SHAREPREFERENCE_NAME, Context.MODE_PRIVATE);
		String name = sharedPreferences.getString(KEY_NAME, KEY_NAME_DEFAULT);
		if (KEY_NAME_DEFAULT.equals(name) || TextUtils.isEmpty(name)) {
			return null;
		}
		return name;
	}

	/**
	 * Load use info from shared preference.
	 * 
	 * @param user
	 */
	public User loadUser() {
		SharedPreferences sharedPreferences = mContext.getSharedPreferences(
				SHAREPREFERENCE_NAME, Context.MODE_PRIVATE);
		String name = sharedPreferences.getString(KEY_NAME, KEY_NAME_DEFAULT);

		User user = new User();
		user.setUserName(name);
		user.setSystemInfo(new SystemInfo().getLocalSystemInfo());
		return user;
	}

	/**
	 * Save use info to shared preference.
	 * 
	 * @param user
	 */
	public void saveUser(User user) {
		if (!TextUtils.isEmpty(user.getUserName())) {
			SharedPreferences sharedPreferences = mContext
					.getSharedPreferences(SHAREPREFERENCE_NAME,
							Context.MODE_PRIVATE);
			Editor editor = sharedPreferences.edit();
			editor.putString(KEY_NAME, user.getUserName());
			editor.commit();
		} else {
			Log.d(TAG, "saveUser: user name is empty, abort.");
		}
	}

	private static final String SEPERATOR = "|-|";

	public byte[] encodeUser(User user) {
		if (user == null) {
			Log.e(TAG, "encodeUser, user is null");
			return new byte[0];
		}
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(user.getUserName());
		stringBuffer.append(SEPERATOR);
		stringBuffer.append(user.getSystemInfo().mAndroidVersionCode);
		return stringBuffer.toString().getBytes();
	}

	public User parseUser(byte[] data) {
		String userInfo = new String(data);
		String[] infos = userInfo.split(SEPERATOR);
		if (infos.length != 2) {
			Log.d(TAG, "parseUser fail: " + userInfo);
			return null;
		}
		User user = new User();
		user.setUserName(infos[0]);
		SystemInfo systemInfo = new SystemInfo();
		systemInfo.mAndroidVersionCode = Integer.valueOf(infos[1]);
		user.setSystemInfo(systemInfo);
		return user;
	}
}
