package com.dreamlink.communication;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

import com.dreamlink.communication.lib.SystemInfo;
import com.dreamlink.communication.lib.util.ArrayUtil;
import com.dreamlink.communication.aidl.User;

public class UserHelper {
	private static final String TAG = "UserHelper";
	public static final String SHAREPREFERENCE_NAME = "user_info";
	public static final String KEY_NAME = "name";
	public static final String KEY_NAME_DEFAULT = "Unkown name";
	private Context mContext;

	public UserHelper(Context context) {
		mContext = context.getApplicationContext();
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
	
	public static byte[] encodeUser(User user) {
		byte[] data = ArrayUtil.objectToByteArray(user);
		return data;
	}

	public static User decodeUser(byte[] data) {
		User user = (User) ArrayUtil.byteArrayToObject(data);
		return user;
	}
}
