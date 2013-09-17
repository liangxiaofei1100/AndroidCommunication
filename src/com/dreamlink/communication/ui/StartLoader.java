package com.dreamlink.communication.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.app.AppInfo;
import com.dreamlink.communication.ui.app.AppManager;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.ui.service.FileTransferService;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public class StartLoader extends Activity {
	private static final String TAG = "StartLoader";
	/** The minimum time(ms) of loading page. */
	private static final int MIN_LOADING_TIME = 1500;
	private PackageManager pm = null;
	private AppManager appManager = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_loader);

		appManager = new AppManager(StartLoader.this);
		pm = getPackageManager();
		
		LoadAsyncTask loadAsyncTask = new LoadAsyncTask();
		loadAsyncTask.execute();
	}

	/**
	 * Do application initialize and load resources. This does not run in ui
	 * thread.
	 */
	private void load() {
		Log.d(TAG, "Load start");
		createFileSaveFolder();
		startService();
		List<AppInfo> zyAppInfos = loadZYAppToDb();
		loadAppToDb(zyAppInfos);
		Log.d(TAG, "Load end");
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

	public void launchLogin() {
		Intent intent = new Intent();
		intent.setClass(this, LoginActivity.class);
		startActivity(intent);
	}

	public void startService() {
		Intent intent = new Intent();
		intent.setClass(this, FileTransferService.class);
		startService(intent);
	}
	
	public List<AppInfo> loadZYAppToDb(){
		List<AppInfo> zyList = new ArrayList<AppInfo>();
		// get zhaoyan app list
		Intent appIntent = new Intent(DreamConstant.APP_ACTION);
		appIntent.addCategory(Intent.CATEGORY_DEFAULT);
		// 通过查询，获得所有ResolveInfo对象.
		List<ResolveInfo> resolveInfos = pm.queryIntentActivities(appIntent, 0);
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
		}
		// get zhaoyan app list end
		getContentResolver().bulkInsert(AppData.App.CONTENT_URI, values);
		return zyList;
	}
	
	public void loadAppToDb(List<AppInfo> zylist) {
		List<AppInfo> appList = new ArrayList<AppInfo>();
		// Retrieve all known applications.
		List<ApplicationInfo> apps = appManager.getAllApps();
		if (apps == null) {
			apps = new ArrayList<ApplicationInfo>();
		}
		//最多也就这么多吧，就多给他分配一点空间
		ContentValues[] values = new ContentValues[apps.size()];
		System.out.println("values.siz=" + values.length);
		for (int i = 0; i < apps.size(); i++) {
			ApplicationInfo info = apps.get(i);
			// 获取非系统应用
			int flag1 = info.flags & ApplicationInfo.FLAG_SYSTEM;
			// 本来是系统程序，被用户手动更新后，该系统程序也成为第三方应用程序了
			int flag2 = info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
			if ((flag1 <= 0) || flag2 != 0) {
				String pkgName = info.packageName;
				if (!isZyApp(pkgName, zylist)) {// 这里就不处理朝颜对战里的应用
					AppInfo entry = new AppInfo(StartLoader.this, apps.get(i));
					entry.setPackageName(info.packageName);
					entry.loadLabel();
					entry.setAppIcon(info.loadIcon(pm));
					entry.loadVersion();
					// 查询一下，该包名是否存在游戏数据表中
					// 每一个应用都要查询一下是否是游戏，会不会很慢呢？
					//经过验证，查询速度还是可以的
					//一般每个的查询速度是20ms。那么100个就是2s。可接受
					boolean is_game_app = appManager.isGameApp(info.packageName);
					if (is_game_app) {
						entry.setType(AppManager.GAME_APP);
					} else {
						entry.setType(AppManager.NORMAL_APP);
					}
					//这里每个又要花掉30ms
					values[i] = appManager.getValuesByAppInfo(entry);
//					Log.d(TAG, "begin query:" + System.currentTimeMillis());
//					Log.d(TAG, "end query:" + System.currentTimeMillis());
//					 appList.add(entry);

					if (DreamConstant.PACKAGE_NAME.equals(info.packageName)) {
						DreamUtil.package_source_dir = info.sourceDir;
					}
				}
			} else {
				// system app
			}
		}
		getContentResolver().bulkInsert(AppData.App.CONTENT_URI, values);
	}
	
	public void insertToDb(AppInfo entry){
		ContentValues values = appManager.getValuesByAppInfo(entry);
		getContentResolver().insert(AppData.App.CONTENT_URI, values);
	}
	
	public boolean isZyApp(String pkgname, List<AppInfo> zyAppInfos) {
		for (AppInfo appInfo : zyAppInfos) {
			if (pkgname.equals(appInfo.getPackageName())) {
				return true;
			}
		}
		return false;
	}
}
