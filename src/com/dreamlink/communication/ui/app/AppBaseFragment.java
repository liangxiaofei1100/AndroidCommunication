package com.dreamlink.communication.ui.app;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.BaseFragment;
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
	
	protected MyDialog mUninstallDialog = null;
	protected List<String> mUninstallList = null;
	protected AppManager mAppManager = null;
	protected PackageManager pm = null;
	protected Notice mNotice = null;
	private static final int REQUEST_CODE_UNINSTALL = 0x101;
	
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
			if (null != mUninstallDialog) {
				mUninstallDialog.cancel();
				mUninstallDialog = null;
			}
			return;
		}
		String uninstallPkg = mUninstallList.get(0);
		Log.d(TAG, "uninstallApp:" + uninstallPkg + ",mUninstallList.size=" + mUninstallList.size());
		mUninstallDialog.setProgress(mUninstallDialog.getMax() - mUninstallList.size() + 1, 
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
