package com.dreamlink.communication;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dreamlink.communication.wifip2p.WifiDirectManager;
import com.dreamlink.communication.wifip2p.WifiDirectReciver.WifiDirectDeviceNotify;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

/**
 * Get all app.
 * 
 */
public class AppListActivity extends Activity implements OnItemClickListener {
	private static final String TAG = "AppListActivity";
	private Context mContext;
	/**
	 * Map structure: </br>
	 * 
	 * KEY_TITLE - activity label</br>
	 * 
	 * KEY_INTENT - activity intent</br>
	 */
	private ArrayList<Map<String, Object>> mApps;
	private static final String KEY_TITLE = "title";
	private static final String KEY_INTENT = "intent";

	private ListView mListView;
	private SimpleAdapter mAdapter;
	private boolean mIsServer = false;
	/** Flag for server or client. True means server, False means client. */
	public static final String EXTRA_IS_SERVER = "isServer";
	private boolean WifiP2p = false;
	/**
	 * Action for find apps. Add a intent-filter including the action in
	 * manifest. Example as below: <intent-filter> <action
	 * android:name="com.dreamlink.communication.action.app" />
	 * 
	 * <category android:name="android.intent.category.DEFAULT" />
	 * </intent-filter>
	 */
	public static final String ACTION_APP = "com.dreamlink.communication.action.app";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.activity_app_list);

		initView();

		Intent intent = getIntent();
		if (intent != null) {
			mIsServer = intent.getBooleanExtra(EXTRA_IS_SERVER, false);
		}
	}

	private void initView() {
		initData();
		mAdapter = new SimpleAdapter(this, mApps,
				android.R.layout.simple_list_item_1, new String[] { "title" },
				new int[] { android.R.id.text1 });
		mListView = (ListView) findViewById(R.id.lstSupportedFunction);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
	}

	/**
	 * Get all apps.
	 */
	private void initData() {
		mApps = new ArrayList<Map<String, Object>>();

		Intent appIntent = new Intent(AppListActivity.ACTION_APP, null);
		PackageManager pm = getPackageManager();
		List<ResolveInfo> list = pm.queryIntentActivities(appIntent, 0);
		if (list != null) {
			int len = list.size();
			for (int i = 0; i < len; i++) {
				ResolveInfo info = list.get(i);
				String activityLabel = info.activityInfo.loadLabel(pm)
						.toString();
				addItem(mApps,
						activityLabel,
						activityIntent(
								info.activityInfo.applicationInfo.packageName,
								info.activityInfo.name));
			}
			Collections.sort(mApps, sDisplayNameComparator);
		} else {
			Log.d(TAG, "App not found.");
		}
	}

	private final static Comparator<Map<String, Object>> sDisplayNameComparator = new Comparator<Map<String, Object>>() {
		private final Collator collator = Collator.getInstance();

		public int compare(Map<String, Object> map1, Map<String, Object> map2) {
			return collator.compare(map1.get("title"), map2.get("title"));
		}
	};

	/**
	 * Create intent with package and class name.
	 * 
	 * @param pkg
	 * @param componentName
	 * @return
	 */
	private Intent activityIntent(String pkg, String componentName) {
		Intent result = new Intent();
		result.setClassName(pkg, componentName);
		return result;
	}

	/**
	 * Add title and intent to Map.
	 * 
	 * @param data
	 * @param name
	 * @param intent
	 */
	private void addItem(List<Map<String, Object>> data, String name,
			Intent intent) {
		Map<String, Object> temp = new HashMap<String, Object>();
		temp.put(KEY_TITLE, name);
		temp.put(KEY_INTENT, intent);
		data.add(temp);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long arg3) {
		Intent intent = (Intent) mApps.get(position).get(KEY_INTENT);
		intent.putExtra(EXTRA_IS_SERVER, mIsServer);
		startActivity(intent);
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		if (Build.VERSION.SDK_INT >= 14) {
			WifiDirectManager manager = new WifiDirectManager(this);
			SocketCommunicationManager.getInstance(this).closeCommunication();
			manager.stopConnect();
		}
	}

}
