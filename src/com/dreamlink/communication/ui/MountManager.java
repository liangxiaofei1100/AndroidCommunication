package com.dreamlink.communication.ui;

import com.dreamlink.communication.ui.DevMountInfo.DevInfo;
import com.dreamlink.communication.util.Log;

import android.os.Environment;

public class MountManager {
	private static final String TAG = MountManager.class.getName();
	public static  String SDCARD_PATH;
	public static  String INTERNAL_PATH;
	public static final String SEPERATOR = "/";
	public static final String NO_EXTERNAL_SDCARD = "no_external_sdcard | no_mounted";
	public static final String NO_INTERNAL_SDCARD = "no_internal_sdcard";
	
	public static final int INTERNAL = 0;
	public static final int SDCARD = 1;
	private DevMountInfo mDevMountInfo;
	private DevInfo devInfo;
	
	public MountManager() {
		mDevMountInfo = DevMountInfo.getInstance();
		
		if (mDevMountInfo.isExistExternal()) {
			Log.d(TAG, "isExistExternal");
			if (isSdcardMounted()) {
				Log.d(TAG, "there is sdcard");
				devInfo = mDevMountInfo.getExternalInfo();
				SDCARD_PATH = devInfo.getPath();
			}else {
				Log.e(TAG, "ther is no sdcard");
				SDCARD_PATH = NO_EXTERNAL_SDCARD;
			}
			devInfo = mDevMountInfo.getInternalInfo();
			if (null == devInfo) {
				INTERNAL_PATH = NO_INTERNAL_SDCARD;
			}else {
				INTERNAL_PATH = devInfo.getPath();
			}
		} else {
			INTERNAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
			SDCARD_PATH = NO_EXTERNAL_SDCARD;
		}
	}
	
	/***
	 * 是否存在外部sdcard
	 * @return
	 */
	private boolean isSdcardMounted(){
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}
	
	public String getShowPath(String path,int type){
		int len = -1;
		switch (type) {
		case INTERNAL:
			len = INTERNAL_PATH.length();
			break;
		case SDCARD:
			len = SDCARD_PATH.length();
			break;
		default:
			break;
		}
		String result = path.substring(len);
		Log.d(TAG, "getShowPath=" + result);
		return result;
	}
}
