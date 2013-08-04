package com.dreamlink.communication.ui.app;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;

public class AppManager {
	private static final String TAG = "AppManager";
	private final PackageManager pm;
	private Context mContext;
	
	public static final int NORMAL_APP = 0;
	public static final int GAME_APP = 1;
	public static final int ERROR_APP = -1;
	
	public static final String ACTION_REFRESH_APP = "intent.aciton.refresh.app";
	public static final String NORMAL_APP_SIZE = "normal_app_size";
	public static final String GAME_APP_SIZE = "game_app_size";
	
	public static final int NORMAL_APP_MENU = 0x00;
	public static final int GAME_APP_MENU_XX = 0x01;
	public static final int GAME_APP_MENU_MY = 0x02;
	public static int menu_type = -1;
	public static final int MENU_INFO = 0x10;
	public static final int MENU_SHARE = 0x11;
	public static final int MENU_UNINSTALL = 0x12;
	public static final int MENU_MOVE = 0x13;

	public AppManager(Context context) {
		mContext = context;
		pm = mContext.getPackageManager();
	}

	public List<ApplicationInfo> getAllApps() {
		List<ApplicationInfo> allApps = pm.getInstalledApplications(0);
		return allApps;
	}
	
	 /**
     * accord package name to get is game app from game db
     * @param pkgName package name
     * @return true,is game app </br>false, is normal app
     */
    public boolean  isGameApp(String pkgName){
    	Cursor cursor = mContext.getContentResolver().query(MetaData.Game.CONTENT_URI, 
    			new String[]{MetaData.Game.PKG_NAME}, null, null, null);
    	if (cursor.moveToFirst()) {
    		do {
    			String pkg_name = cursor.getString(cursor.getColumnIndex(MetaData.Game.PKG_NAME));
    			if (pkgName.equals(pkg_name)) {
    				return true;
    			}
			} while (cursor.moveToNext());
		}else {
			Log.e(TAG, "no db????????????");
		}
    	return false;
    }
    
    /**
     * accord package name to get the app entry and position
     * @param packageName 
     * @return int[0]:(0,normal app;1:game app)
     * </br>
     * 	int[1]:(the position in the list)
     */
    public int[] getAppEntry(String packageName, List<AppEntry> normalAppList, List<AppEntry> gameAppList){
    	int[] result = new int[2];
    	
    	for (int i = 0; i < normalAppList.size(); i++) {
			AppEntry appEntry = normalAppList.get(i);
			if (packageName.equals(appEntry.getPackageName())) {
				//is normal app
				result[0] = AppManager.NORMAL_APP;
				result[1] = i;
				return result;
			}
		}
    	
    	for (int i = 0; i < gameAppList.size(); i++) {
			AppEntry appEntry = gameAppList.get(i);
			if (packageName.equals(appEntry.getPackageName())) {
				//is game app
				result[0] = AppManager.GAME_APP;
				result[1] = i;
				return result;
			}
		}
    	
    	return null;
    }
    
    /**
     * send broad cast to update app ui
     * @param normalAppList
     * @param gameAppList
     */
    public void updateAppUI(){
    	//send broadcast & update ui
		Intent intent = new Intent(ACTION_REFRESH_APP);
		intent.putExtra(NORMAL_APP_SIZE, AppNormalFragment.mNormalAppLists.size());
		intent.putExtra(GAME_APP_SIZE, AppNormalFragment.mGameAppList.size());
		mContext.sendBroadcast(intent);
    }

	public void installApk(String apkFilePath) {
		if (apkFilePath.endsWith(".apk")) {
			installApk(new File(apkFilePath));
		}
	}

	public void installApk(File file) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		Uri uri = Uri.fromFile(file);
		intent.setDataAndType(uri, "application/vnd.android.package-archive");
		mContext.startActivity(intent);
	}
	
	public void uninstallApp(String packageName){
		Uri packageUri = Uri.parse("package:" + packageName);
		Intent deleteIntent = new Intent();
		deleteIntent.setAction(Intent.ACTION_DELETE);
		deleteIntent.setData(packageUri);
		mContext.startActivity(deleteIntent);
	}

	public void copyApp(ApplicationInfo appInfo, String directory) {
		File file = new File(directory); /* 创建临时文件 */
		if (!file.exists())// 如果文件夹不存在创建
		{
			file.mkdirs();
		}
		String src = appInfo.sourceDir;
		String name = appInfo.loadLabel(pm).toString() + ".apk";
		Log.d(TAG, "backuping..." + name);
		fileStreamCopy(src, directory + name);
	}

	/**
	 * io拷贝
	 * 
	 * @param inFile
	 *            源文件
	 * @param outFile
	 *            目标文件
	 * @return
	 * @throws Exception
	 */
	public void fileStreamCopy(String inFile, String outFile) {

		try {
			File files = new File(outFile);// 创建文件
			/* 将文件写入暂存盘 */
			FileOutputStream fos = new FileOutputStream(files);
			byte buf[] = new byte[128];
			InputStream fis = new BufferedInputStream(new FileInputStream(
					inFile), 8192 * 4);
			do {
				int numread = fis.read(buf);
				if (numread <= 0) {
					break;
				}
				fos.write(buf, 0, numread);
			} while (true);
			fis.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
