package com.dreamlink.communication.ui.app;

import java.io.File;

import com.dreamlink.communication.ui.DreamUtil;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

public class AppInfo implements Parcelable{
	private  ApplicationInfo mInfo;
	private File mApkFile;
	/**App name*/
	private String mLabel;
	/**App Icon*/
	private Drawable mIcon;
	private boolean mMounted;
	/**
	 * if ture ,it is a game app
	 * </br>
	 * if flase ,it is a normal app
	 * */
	private boolean isGameApp;
	/**app package name*/
	private String packageName;
	/**app version*/
	private String version;
	private Context context;
	private PackageManager pm;
	private Intent launchIntent;
	
	private AppInfo(Parcel in){
		readFromParcel(in);
	}
	
	public AppInfo(Context context){
		this.context = context;
		mApkFile = null;
		pm = context.getPackageManager();
	}
	
	public AppInfo(Context context, ApplicationInfo info) {
		this.context = context;
		mInfo = info;
		mApkFile = new File(info.sourceDir);
		pm = context.getPackageManager();
	}

	public ApplicationInfo getApplicationInfo() {
		return mInfo;
	}

	public String getLabel() {
		return mLabel;
	}

	@Override
	public String toString() {
		return mLabel;
	}

	void loadLabel() {
		if (mLabel == null || !mMounted) {
			if (!mApkFile.exists()) {
				mMounted = false;
				mLabel = mInfo.packageName;
			} else {
				mMounted = true;
				CharSequence label = mInfo.loadLabel(pm);
				mLabel = label != null ? label.toString() : mInfo.packageName;
			}
		}
	}
	
	public void setLable(String label){
		mLabel = label;
	}
	
	public Drawable getAppIcon(){
		return mIcon;
	}
	
	public void setAppIcon(Drawable icon){
		mIcon = icon;
	}
	
	public Intent getLaunchIntent(){
		return launchIntent;
	}
	
	public void setLaunchIntent(Intent intent){
		this.launchIntent = intent;
	}
	
	public void loadVersion(){
		try {
			this.version = pm.getPackageInfo(packageName, 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public String getVersion(){
		return this.version;
	}
	
	public String getFormatSize(){
		long appSize = mApkFile.length();
		return DreamUtil.getFormatSize(appSize);
	}
	
	public void setPackageName(String packageName){
		this.packageName = packageName;
	}
	
	public String getPackageName(){
		return this.packageName;
	}
	
	public void setIsGameApp(boolean flag){
		this.isGameApp = flag;
	}
	
	public boolean isGameApp(){
		return isGameApp;
	}
	
	/**get app installed path
	 * </br>
	 * like: /data/app/xxx.apk
	 * */
	public String getInstallPath(){
		return mApkFile.getAbsolutePath();
	}
	
	/**get app install date*/
	public String getDate(){
		long date = mApkFile.lastModified();
		return DreamUtil.getFormatDate(date);
	}
	
	public static final Parcelable.Creator<AppInfo> CREATOR  = new Parcelable.Creator<AppInfo>() {

		@Override
		public AppInfo createFromParcel(Parcel source) {
			return new AppInfo(source);
		}

		@Override
		public AppInfo[] newArray(int size) {
			return new AppInfo[size];
		}
	};

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		
	}
	
	public void readFromParcel(Parcel in){
	}
	
}
