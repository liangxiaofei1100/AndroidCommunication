package com.dreamlink.communication.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.search.WifiNameSuffixLoader;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.app.AppInfo;
import com.dreamlink.communication.ui.app.AppManager;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.ui.file.FileInfo;
import com.dreamlink.communication.ui.file.FileInfoManager;

import com.dreamlink.communication.util.Log;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Window;

/**
 * This is the first ui to show logo, initialize application and load resource.
 * 
 */
public class StartLoader extends Activity {
	private static final String TAG = "StartLoader";
	/** The minimum time(ms) of loading page. */
	private static final int MIN_LOADING_TIME = 1500;
	private SharedPreferences sp = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_loader);
		
		sp = getSharedPreferences(Extra.SHARED_PERFERENCE_NAME, MODE_PRIVATE);
		//get classify num
		int doc = sp.getInt(FileInfoManager.DOC_NUM, -1);
		int ebook = sp.getInt(FileInfoManager.EBOOK_NUM, -1);
		int apk = sp.getInt(FileInfoManager.APK_NUM, -1);
		int archive = sp.getInt(FileInfoManager.ARCHIVE_NUM, -1);
		//if there is one not exist,start search
		if (doc == -1 || ebook == -1 || apk == -1 || archive == -1) {
			//only when the app is the first start, need loading 
			new LoadClassifyTask().execute();
		}
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
		File file = new File(DreamConstant.DREAMLINK_FOLDER);
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
	
	/**
	 * load classify file
	 */
	/**get sdcard classify files*/
	class LoadClassifyTask extends AsyncTask<Object, Integer, Object>{
		List<FileInfo> docList = new ArrayList<FileInfo>();
		List<FileInfo> ebookList = new ArrayList<FileInfo>();
		List<FileInfo> apkList = new ArrayList<FileInfo>();
		List<FileInfo> archiveList = new ArrayList<FileInfo>();
		String[] docTypes = null;
		String[] ebookTypes = null;
		String[] apkTypes = null;
		String[] archiveTypes = null;
		
		FileInfoManager mFileInfoManager = null;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			docTypes = getResources().getStringArray(R.array.doc_file);
			ebookTypes = getResources().getStringArray(R.array.ebook_file);
			apkTypes = getResources().getStringArray(R.array.apk_file);
			archiveTypes = getResources().getStringArray(R.array.archive_file);
			
			mFileInfoManager = new FileInfoManager(StartLoader.this);
		}
		
		@Override
		protected List<FileInfo> doInBackground(Object... params) {
			Log.d(TAG, "doInBackground");
			List<FileInfo> filterList = new ArrayList<FileInfo>(); 
			File[] files = Environment.getExternalStorageDirectory().getAbsoluteFile().listFiles();
			listFile(files);
			return filterList;
		}
		
		@Override
		protected void onPostExecute(Object result) {
			super.onPostExecute(result);
			Log.d(TAG, "onPostExecute");
			saveSharedPreference();
		}
		
		protected void listFile(final File[] files){
			if (null != files && files.length > 0) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						final int tag = i;
						new Thread(new Runnable() {
							@Override
							public void run() {
								listFile(files[tag].listFiles());
							}
						}).start();
					}else {
						String name = files[i].getName();
						FileInfo fileInfo = null;
						if (isDocFile(name)) {
							fileInfo = mFileInfoManager.getFileInfo(files[i]);
							docList.add(fileInfo);
						}else if (isEbookFile(name)) {
							fileInfo = mFileInfoManager.getFileInfo(files[i]);
							ebookList.add(fileInfo);
						}else if (isApkFile(name)) {
							fileInfo = mFileInfoManager.getFileInfo(files[i]);
							apkList.add(fileInfo);
						}else if (isArchiveFile(name)) {
							fileInfo = mFileInfoManager.getFileInfo(files[i]);
							archiveList.add(fileInfo);
						}
					}
				}
			}
		}
		
		public boolean isDocFile(String name){
			for(String str : docTypes){
				if (name.endsWith(str)) {
					return true;
				}
			}
			
			return false;
		}
		public boolean isEbookFile(String name){
			for(String str : ebookTypes){
				if (name.endsWith(str)) {
					return true;
				}
			}
			return false;
		}
		
		public boolean isApkFile(String name){
			for(String str : apkTypes){
				if (name.endsWith(str)) {
					return true;
				}
			}
			return false;
		}
		
		public boolean isArchiveFile(String name){
			for(String str : archiveTypes){
				if (name.endsWith(str)) {
					return true;
				}
			}
			return false;
		}
		
		private void saveSharedPreference() {
			Editor editor = sp.edit();
			editor.putInt(FileInfoManager.DOC_NUM, docList.size());
			editor.putInt(FileInfoManager.EBOOK_NUM, ebookList.size());
			editor.putInt(FileInfoManager.APK_NUM, apkList.size());
			editor.putInt(FileInfoManager.ARCHIVE_NUM, archiveList.size());
			editor.commit();
		}
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
