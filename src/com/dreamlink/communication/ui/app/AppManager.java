package com.dreamlink.communication.ui.app;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.util.Log;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public class AppManager {
	private static final String TAG = "AppManager";
	private final PackageManager pm;
	private Context mContext;
	
	public static final int NORMAL_APP = 0;
	public static final int GAME_APP = 1;
	public static final int ZHAOYAN_APP = 2;
	public static final int ERROR_APP = -1;
	
	public static final String ACTION_REFRESH_APP = "intent.aciton.refresh.app";
	public static final String ACTION_SEND_TO_APP = "intent.aciton.send.to.app";
	public static final String ACTION_ADD_MYGAME = "intent.cation.add.mygame";
	public static final String ACTION_REMOVE_MYGAME = "intent.action.remove.mygame";
	public static final String ACTION_SEND_TO_GAME = "intent.aciton.send.to.game";
	public static final String NORMAL_APP_SIZE = "normal_app_size";
	public static final String GAME_APP_SIZE = "game_app_size";
	
	public static final String EXTRA_INFO = "info";
	
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
		System.out.println("getallapps.size=" + allApps.size());
		return allApps;
	}
	
	 /**
     * accord package name to get is game app from game db
     * @param pkgName package name
     * @return true,is game app </br>false, is normal app
     */
    public boolean  isGameApp(String pkgName){
    	String selectionString = AppData.App.PKG_NAME + "=?" ;
    	String args[] = {pkgName};
    	boolean ret = false;
    	Cursor cursor = mContext.getContentResolver().query(AppData.AppGame.CONTENT_URI, 
    			null, selectionString, args, null);
    	if (cursor.getCount() <= 0) {
			ret = false;
		}else {
			ret = true;
		}
    	cursor.close();
    	return ret;
    }
    
    public boolean isMyApp(String packageName){
    	//get we app
		Intent appIntent = new Intent(DreamConstant.APP_ACTION);
		appIntent.addCategory(Intent.CATEGORY_DEFAULT);
		// 通过查询，获得所有ResolveInfo对象.
		List<ResolveInfo> resolveInfos = pm.queryIntentActivities(appIntent, 0);
		for (ResolveInfo resolveInfo : resolveInfos) {
			String pkgName = resolveInfo.activityInfo.packageName; // 获得应用程序的包名
			if (packageName.equals(pkgName)) {
				return true;
			}
		}
		return false;
    }
    
    public ContentValues getValuesByAppInfo(AppInfo appInfo){
    	ContentValues values = new ContentValues();
    	values.put(AppData.App.PKG_NAME, appInfo.getPackageName());
    	values.put(AppData.App.LABEL, appInfo.getLabel());
    	values.put(AppData.App.VERSION, appInfo.getVersion());
    	values.put(AppData.App.APP_SIZE, appInfo.getAppSize());
    	values.put(AppData.App.DATE, appInfo.getDate());
    	values.put(AppData.App.TYPE, appInfo.getType());
    	values.put(AppData.App.ICON, appInfo.getIconBlob());
    	values.put(AppData.App.PATH, appInfo.getInstallPath());
    	return values;
    }
    
    /**
     * accord package name to get the app entry and position
     * @param packageName 
     * @return int[0]:(0,normal app;1:game app)
     * </br>
     * 	int[1]:(the position in the list)
     */
    public int[] getAppInfo(String packageName, List<AppInfo> normalAppList, List<AppInfo> gameAppList, List<AppInfo> myAppList){
    	int[] result = new int[2];
    	
    	for (int i = 0; i < normalAppList.size(); i++) {
			AppInfo appInfo = normalAppList.get(i);
			if (packageName.equals(appInfo.getPackageName())) {
				//is normal app
				result[0] = AppManager.NORMAL_APP;
				result[1] = i;
				return result;
			}
		}
    	
    	for (int i = 0; i < gameAppList.size(); i++) {
			AppInfo appInfo = gameAppList.get(i);
			if (packageName.equals(appInfo.getPackageName())) {
				//is game app
				result[0] = AppManager.GAME_APP;
				result[1] = i;
				return result;
			}
		}
    	
    	for (int i = 0; i < myAppList.size(); i++) {
			AppInfo appInfo = myAppList.get(i);
			if (packageName.equals(appInfo.getPackageName())) {
				//is my app
				result[0] = AppManager.ZHAOYAN_APP;
				result[1] = i;
				return result;
			}
		}
    	
    	return null;
    }
    
    /**
     * send broad cast to update app ui
     */
    public void updateAppUI(){
    	//send broadcast & update ui
		Intent intent = new Intent(ACTION_REFRESH_APP);
		mContext.sendBroadcast(intent);
    }
    
    /**return the position according to packagename*/
    public int getAppPosition(String packageName, List<AppInfo> list){
    	for (int i = 0; i < list.size(); i++) {
			AppInfo appInfo = list.get(i);
			if (packageName.equals(appInfo.getPackageName())) {
				return i;
			}
		}
    	return -1;
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
	
	
	public void showInstalledAppDetails(String packageName){
		Intent intent = new Intent();
		final int apiLevel = Build.VERSION.SDK_INT;
		if (apiLevel >= Build.VERSION_CODES.GINGERBREAD) {
			intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			Uri uri = Uri.fromParts("package", packageName, null);
			intent.setData(uri);
		}else {
			intent.setAction(Intent.ACTION_VIEW);
			intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
			intent.putExtra("pkg", packageName);
		}
		mContext.startActivity(intent);
	}
	
	public void showInfoDialog(AppInfo appInfo){
		String info  = getAppInfo(appInfo);
		DreamUtil.showInfoDialog(mContext, appInfo.getLabel(), info);
	}
	
	private String getAppInfo(AppInfo appInfo){
		String result = "";
		result = "类型:" + (appInfo.getType() == GAME_APP ? "游戏" : "应用") + DreamConstant.ENTER
				+ "版本:" + appInfo.getVersion() + DreamConstant.ENTER
				+ "包名:" + appInfo.getPackageName() + DreamConstant.ENTER
				+ "位置:" + appInfo.getInstallPath() + DreamConstant.ENTER
				+ "大小:" + appInfo.getFormatSize() + DreamConstant.ENTER
				+ "修改日期:" + appInfo.getFormatDate();
		return result;
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
