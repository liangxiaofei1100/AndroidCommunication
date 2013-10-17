package com.dreamlink.communication.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.search.WifiNameSuffixLoader;
import com.dreamlink.communication.ui.app.AppInfo;
import com.dreamlink.communication.ui.app.AppManager;
import com.dreamlink.communication.ui.db.AppData;

import com.dreamlink.communication.util.Log;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;

/**
 * This is the first ui to show logo, initialize application and load resource.
 * 
 */
public class StartLoader extends Activity {
	private static final String TAG = "StartLoader";
	/** The minimum time(ms) of loading page. */
	private static final int MIN_LOADING_TIME = 1500;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_loader);

		LoadAsyncTask loadAsyncTask = new LoadAsyncTask();
		loadAsyncTask.execute();
	}

	/**
	 * Do application initialize and load resources. This does not run in ui
	 * thread.
	 */
	private void load() {
		Log.d(TAG, "Load start");
		// Start log to save log to file.
		Log.startSaveToFile();
		// Create folder for file transportation.
		createFileSaveFolder();
		// Init MountManager.
		initMountManager();
		// Create WiFi Name suffix
		createNewWifiSuffixName();
		// Delete old APP Database table and refresh it.
		getContentResolver().delete(AppData.App.CONTENT_URI, null, null);
		LoadAppThread thread = new LoadAppThread();
		thread.start();
		Log.d(TAG, "Load end");
	}

	/**
	 * Used by WiFi AP name.
	 */
	private void createNewWifiSuffixName() {
		WifiNameSuffixLoader.createNewWifiSuffixName(getApplicationContext());
	}

	/**
	 * Load is finished
	 */
	private void loadFinished() {
		launchLogin();
		finish();
		overridePendingTransition(R.anim.push_right_in, R.anim.push_left_out);
	}

	private void createFileSaveFolder() {
		File file = new File(DreamConstant.DEFAULT_SAVE_FOLDER);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	private void initMountManager() {
		// Get sdcards
		MountManager mountManager = new MountManager(this);
		mountManager.init();
	}

	class LoadAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			long start = System.currentTimeMillis();
			// Do load.
			load();
			long end = System.currentTimeMillis();
			if (end - start < MIN_LOADING_TIME) {
				try {
					Thread.sleep(MIN_LOADING_TIME - (end - start));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			loadFinished();
		}
	};

	private void launchLogin() {
		Intent intent = new Intent();
		intent.setClass(this, LoginActivity.class);
		startActivity(intent);
	}

	private class LoadAppThread extends Thread {
		@Override
		public void run() {
			AppManager appManager = new AppManager(StartLoader.this);
			List<String> zyPkgList = loadZYAppToDb(appManager);
			loadAppToDb(appManager, zyPkgList);
		}
	}

	private List<String> loadZYAppToDb(AppManager appManager) {
		long start = System.currentTimeMillis();

		Intent appIntent = new Intent(DreamConstant.APP_ACTION);
		appIntent.addCategory(Intent.CATEGORY_DEFAULT);
		// 通过查询，获得所有ResolveInfo对象.
		PackageManager pm = getPackageManager();
		List<ResolveInfo> resolveInfos = pm.queryIntentActivities(appIntent, 0);

		// get zhaoyan app list
		List<String> zyList = new ArrayList<String>();
		AppInfo appInfo = null;
		ContentValues[] values = new ContentValues[resolveInfos.size()];

		for (int i = 0; i < resolveInfos.size(); i++) {
			ApplicationInfo info = resolveInfos.get(i).activityInfo.applicationInfo;
			appInfo = new AppInfo(StartLoader.this, info);
			appInfo.setPackageName(resolveInfos.get(i).activityInfo.packageName); // 获得应用程序的包名
			appInfo.loadLabel();
			appInfo.setAppIcon(resolveInfos.get(i).loadIcon(pm));
			appInfo.loadVersion();
			appInfo.setType(AppManager.ZHAOYAN_APP);
			values[i] = appManager.getValuesByAppInfo(appInfo);
			zyList.add(resolveInfos.get(i).activityInfo.packageName);
		}
		// get zhaoyan app list end

		getContentResolver().bulkInsert(AppData.App.CONTENT_URI, values);
		Log.d(TAG, "loadZYAppToDb cost time = "
				+ (System.currentTimeMillis() - start));
		return zyList;
	}

	private void loadAppToDb(AppManager appManager, List<String> zylist) {
		long start = System.currentTimeMillis();

		List<ContentValues> valuesList = new ArrayList<ContentValues>();
		// Retrieve all known applications.
		PackageManager pm = getPackageManager();
		List<ApplicationInfo> apps = pm.getInstalledApplications(0);
		if (apps == null) {
			apps = new ArrayList<ApplicationInfo>();
		}
		ContentValues values = null;
		for (int i = 0; i < apps.size(); i++) {
			ApplicationInfo info = apps.get(i);
			// 获取非系统应用
			int flag1 = info.flags & ApplicationInfo.FLAG_SYSTEM;
			if (flag1 <= 0) {
				String pkgName = info.packageName;
				if (!zylist.contains(pkgName)) {// 这里就不处理朝颜对战里的应用
					AppInfo entry = new AppInfo(StartLoader.this, apps.get(i));
					entry.setPackageName(info.packageName);
					entry.loadLabel();
					entry.setAppIcon(info.loadIcon(pm));
					entry.loadVersion();
					// 查询一下，该包名是否存在游戏数据表中
					// 每一个应用都要查询一下是否是游戏，会不会很慢呢？
					// 经过验证，查询速度还是可以的
					// 一般每个的查询速度是20ms。那么100个就是2s。可接受
					boolean is_game_app = appManager
							.isGameApp(info.packageName);
					if (is_game_app) {
						entry.setType(AppManager.GAME_APP);
					} else {
						entry.setType(AppManager.NORMAL_APP);
					}
					values = appManager.getValuesByAppInfo(entry);
					valuesList.add(values);
					// 为了蓝牙邀请准备材料
					if (DreamConstant.PACKAGE_NAME.equals(info.packageName)) {
						DreamUtil.package_source_dir = info.sourceDir;
					}
				}
			} else {
				// system app
			}
		}

		// get values
		ContentValues[] contentValues = new ContentValues[0];
		contentValues = valuesList.toArray(contentValues);
		// 经验证插入60个应用，仅90ms左右，所以插入时间可以忽略不计了
		getContentResolver().bulkInsert(AppData.App.CONTENT_URI, contentValues);
		Log.d(TAG, "loadAppToDb cost time = "
				+ (System.currentTimeMillis() - start));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// Not allow exit when loading, just wait loading jobs to be done.
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
