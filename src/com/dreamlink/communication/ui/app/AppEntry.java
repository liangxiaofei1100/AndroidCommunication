package com.dreamlink.communication.ui.app;

import java.io.File;

import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.file.FileInfo;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

public class AppEntry implements Parcelable{
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
	private Intent startIntent;
	
	private AppEntry(Parcel in){
		readFromParcel(in);
	}
	
	public AppEntry(Context context){
		this.context = context;
		mApkFile = null;
		pm = context.getPackageManager();
	}
	
	public AppEntry(Context context, ApplicationInfo info) {
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

	public Drawable getIcon() {
		if (mIcon == null) {
			if (mApkFile.exists()) {
				mIcon = mInfo.loadIcon(pm);
				return mIcon;
			} else {
				mMounted = false;
			}
		} else if (!mMounted) {
			// If the app wasn't mounted but is now mounted, reload
			// its icon.
			if (mApkFile.exists()) {
				mMounted = true;
				mIcon = mInfo.loadIcon(pm);
				return mIcon;
			}
		} else {
			return mIcon;
		}

		return context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
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
	
	public void setIcon(Drawable icon){
		mIcon = icon;
	}
	
	public void setIntent(Intent intent){
		startIntent = intent;
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
	
	public static final Parcelable.Creator<AppEntry> CREATOR  = new Parcelable.Creator<AppEntry>() {

		@Override
		public AppEntry createFromParcel(Parcel source) {
			return new AppEntry(source);
		}

		@Override
		public AppEntry[] newArray(int size) {
			return new AppEntry[size];
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
//		isDir = in.readInt() == 1 ? true : false;
//		fileName = in.readString();
//		fileSize = in.readDouble();
//		fileDate = in.readLong();
	}
	
}
