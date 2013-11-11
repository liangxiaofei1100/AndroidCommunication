package com.dreamlink.communication.ui.app;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.MenuTabManager;
import com.dreamlink.communication.ui.dialog.MyDialog;
import com.dreamlink.communication.ui.media.ActionMenu;
import com.dreamlink.communication.util.Log;

public class AppBaseFragment extends BaseFragment{
	private static final String TAG = "AppBaseFragment";
	
	protected GridView mGridView;
	protected ProgressBar mLoadingBar;

	protected AppCursorAdapter mAdapter = null;
	
	protected Context mContext;
	protected int mAppId = -1;
	
	protected ActionMenu mActionMenu;
	protected MenuTabManager mMenuManager;
	
	protected View mMenuBottomView;
	protected LinearLayout mMenuHolder;
	
	protected MyDialog mMyDialog = null;
	protected List<String> mUninstallList = null;
	protected AppManager mAppManager = null;
	protected PackageManager pm = null;
	protected Notice mNotice = null;
	private static final int REQUEST_CODE_UNINSTALL = 0x101;
	
	private static final int MSG_TOAST = 0;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_TOAST:
				String message = (String) msg.obj;
				mNotice.showToast(message);
				break;
			default:
				break;
			}
		};
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getActivity();
		mAppManager = new AppManager(mContext);
		mNotice = new Notice(mContext);
		pm = mContext.getPackageManager();
	}
	
	protected void uninstallApp(){
		if (mUninstallList.size() <= 0) {
			mUninstallList = null;
			if (null != mMyDialog) {
				mMyDialog.cancel();
				mMyDialog = null;
			}
			return;
		}
		String uninstallPkg = mUninstallList.get(0);
		mMyDialog.updateUI(mMyDialog.getMax() - mUninstallList.size() + 1, 
				mAppManager.getAppLabel(uninstallPkg));
		Uri packageUri = Uri.parse("package:" + uninstallPkg);
		Intent deleteIntent = new Intent();
		deleteIntent.setAction(Intent.ACTION_DELETE);
		deleteIntent.setData(packageUri);
		startActivityForResult(deleteIntent, REQUEST_CODE_UNINSTALL);
		mUninstallList.remove(0);
	}
	
	/**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppInfo> ALPHA_COMPARATOR = new Comparator<AppInfo>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppInfo object1, AppInfo object2) {
            return sCollator.compare(object1.getLabel(), object2.getLabel());
        }
    };
    
    @SuppressWarnings("unchecked")
	protected void showBackupDialog(List<String> packageList){
    	Log.d(TAG, "Environment.getExternalStorageState():" + Environment.getExternalStorageState());
    	if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			mNotice.showToast(R.string.no_sdcard);
			return;
		}
    	final BackupAsyncTask task = new BackupAsyncTask();
    	task.execute(packageList);
    	mMyDialog = new MyDialog(mContext, packageList.size());
		mMyDialog.setTitle(R.string.backuping);
		mMyDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				Log.d(TAG, "showBackupDialog.onCancel");
				if (null != task) {
					task.cancel(true);
				}
			}
		});
		mMyDialog.show();
    }
    
    private class BackupAsyncTask extends AsyncTask<List<String>, Integer, Void>{
    	
		@Override
		protected Void doInBackground(List<String>... params) {
			File file = new File(DreamConstant.BACKUP_FOLDER); 
			if (!file.exists()){
				boolean ret = file.mkdirs();
				if (!ret) {
					Log.e(TAG, "create file fail:" + file.getAbsolutePath());
					return null;
				}
			}
			
			String label = "";
			String version = "";
			String sourceDir = "";
			String packageName = "";
			for (int i = 0; i < params[0].size(); i++) {
				if (isCancelled()) {
					Log.d(TAG, "doInBackground.isCancelled");
					return null;
				}
				packageName = params[0].get(i);
				label = mAppManager.getAppLabel(packageName);
				version = mAppManager.getAppVersion(packageName);
				sourceDir = mAppManager.getAppSourceDir(packageName);
				mMyDialog.updateName(label);
				String desPath = DreamConstant.BACKUP_FOLDER + "/" + label + "_" + version + ".apk";
				if (!new File(desPath).exists()) {
					try {
						DreamUtil.fileStreamCopy(sourceDir, desPath);
					} catch (IOException e) {
						Log.e(TAG, "doInBackground.Error:" + e.toString());
						Message message = mHandler.obtainMessage();
						message.obj = getString(R.string.backup_fail, label);
						message.what = MSG_TOAST;
						message.sendToTarget();
					}
				}
				mMyDialog.updateProgress(i + 1); 
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			Log.d(TAG, "onPostExecute");
			if (null != mMyDialog) {
				mMyDialog.cancel();
				mMyDialog = null;
			}
			mNotice.showToast("备份完成");
		}
    }
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "onActivityResult.requestCode=" + requestCode);
		if (REQUEST_CODE_UNINSTALL == requestCode) {
			uninstallApp();
		}
	}
}
