package com.dreamlink.communication.debug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.dreamlink.communication.aidl.HostInfo;
import com.dreamlink.communication.platform.PlatformManager;
import com.dreamlink.communication.platform.PlatformManager.HostNumberInterface;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class GetAllHostNumber extends Activity implements HostNumberInterface {
	private PlatformManager mPlatformManager;
	/**
	 * key: package name value: the number of the key
	 * */
	private Map<String, Integer> mHostNumberMap;

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			mHostNumberMap = (Map<String, Integer>) msg.obj;
			for (Entry<String, Integer> entry : mHostNumberMap.entrySet()) {
				// TODO do something to update the number
				Log.e("ArbiterLiu",
						entry.getKey() + "  number is  " + entry.getValue());
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPlatformManager = PlatformManager.getInstance(getApplicationContext());
		mPlatformManager.registerHostNumberInterface(this);
		mPlatformManager.getAllHost(0);// get all host info ,need to parse
	}

	@Override
	public void returnHostInfo(List<HostInfo> hostList) {
		/**
		 * when host info change ,will invoke here
		 * 
		 * please do it in other thread
		 * */
		new ParseThread(hostList).start();
	}

	private class ParseThread extends Thread {
		private List<HostInfo> allHostList;

		public ParseThread(List<HostInfo> hostList) {
			allHostList = hostList;
		}

		@Override
		public void run() {
			super.run();
			HashMap<String, Integer> temMap = new HashMap<String, Integer>();
			for (HostInfo info : allHostList) {
				String packageName = info.packageName;
				int num = 1;
				if (temMap.containsKey(packageName)) {
					num = temMap.get(packageName) + 1;
				}
				temMap.put(packageName, num);
			}
			mHandler.obtainMessage(0, temMap).sendToTarget();
		}

	}

}
