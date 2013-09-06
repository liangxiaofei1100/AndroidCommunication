package com.dreamlink.communication.ui.app;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.dialog.FileInfoDialog;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.Notice;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ProgressBar;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

/**
 * use this to load app
 */
public class AppNormalFragment extends Fragment implements OnItemClickListener, OnItemLongClickListener {
	private static final String TAG = "AppNormalFragment";
	private GridView mGridView;
	private ProgressBar mProgressBar;

	private AppBrowserAdapter mAdapter = null;
	private AppManager mAppManager = null;
	private PackageManager pm = null;
	
	public static  List<AppEntry> mNormalAppLists = new ArrayList<AppEntry>();
	public static List<AppEntry> mGameAppList = new ArrayList<AppEntry>();
	public static List<AppEntry> mMyAppList = new ArrayList<AppEntry>();
	
	private static int mCurrentPosition = -1;
	private Context mContext;
	
	private AppReceiver mAppReceiver;
	private Notice mNotice = null;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		View rootView = inflater.inflate(R.layout.ui_app_normal, container, false);

		mContext = getActivity();
		
		mNotice = new Notice(mContext);
		
		mGridView = (GridView) rootView.findViewById(R.id.app_normal_gridview);
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.app_progressbar);
		
		//register broadcast
		mAppReceiver = new AppReceiver();
		IntentFilter filter = new IntentFilter(AppManager.ACTION_REFRESH_APP);
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
		getActivity().registerReceiver(mAppReceiver, filter);
		
		mAppManager = new AppManager(mContext);
		pm = mContext.getPackageManager();

		//get user app
		AppListTask appListTask = new AppListTask();
		appListTask.execute("");

		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
		
		Log.d(TAG, "onCreate end");
		return rootView;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final ApplicationInfo applicationInfo = (ApplicationInfo) mNormalAppLists.get(position).getApplicationInfo();
		String packageName = applicationInfo.packageName;
		if (DreamConstant.PACKAGE_NAME.equals(packageName)) {
			mNotice.showToast(R.string.app_has_started);
			return;
		}
		
		Intent intent = pm.getLaunchIntentForPackage(packageName);
		if (null != intent) {
			startActivity(intent);
		}else {
			mNotice.showToast(R.string.cannot_start_app);
			return;
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		final AppEntry appEntry = mNormalAppLists.get(position);
		int resId = R.array.app_menu_normal;
		if (DreamConstant.PACKAGE_NAME.equals(appEntry.getPackageName())) {
			//本身这个程序不允许卸载，不允许移动到游戏，已经打开了，所以没有打开选项
			//总之，菜单要不一样
			resId = R.array.app_menu_myself;
		}
		final String[] current_menus = getResources().getStringArray(resId);
		final String[] normal_menus = getResources().getStringArray(R.array.app_menu_normal);
		new AlertDialog.Builder(mContext)
			.setIcon(appEntry.getIcon())
			.setTitle(appEntry.getLabel())
			.setItems(resId, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String currentMenu = current_menus[which];
					if (normal_menus[0].equals(currentMenu)) {
						//open
						Intent intent = pm.getLaunchIntentForPackage(appEntry.getPackageName());
						if (null != intent) {
							startActivity(intent);
						}else {
							mNotice.showToast(R.string.cannot_start_app);
							return;
						}
					}else if (normal_menus[1].equals(currentMenu)) {
						//send
						
					}else if (normal_menus[2].equals(currentMenu)) {
						//uninstall
						mAppManager.uninstallApp(appEntry.getPackageName());
					}else if (normal_menus[3].equals(currentMenu)) {
						//app info
						mAppManager.showInfoDialog(appEntry);
					}else if (normal_menus[4].equals(currentMenu)) {
						//move to game
						mNormalAppLists.remove(position);
						
						int index = DreamUtil.getInsertIndex(mGameAppList, appEntry);
						if (mGameAppList.size() == index) {
							mGameAppList.add(appEntry);
						} else {
							mGameAppList.add(index, appEntry);
						}
						
						//insert to db
						ContentValues values = new ContentValues();
						values.put(MetaData.Game.PKG_NAME, appEntry.getPackageName());
						mContext.getContentResolver().insert(MetaData.Game.CONTENT_URI, values);
						
						//update myself
						mAdapter.notifyDataSetChanged();
						//and then update others
						mAppManager.updateAppUI();
					}
				}
			}).create().show();
		return true;
	}
	
	//recevier that can update ui
    private  class AppReceiver extends BroadcastReceiver {
        @Override 
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "get receiver:" + action);
			if (AppManager.ACTION_REFRESH_APP.equals(action)) {
				mAdapter.notifyDataSetChanged();
			}else {
				//get install or uninstall app package name
				String packageName = intent.getData().getSchemeSpecificPart();
				
				if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
					//get installed app
					AppEntry entry = null;
					List<AppEntry> appList = new ArrayList<AppEntry>();
					try {
						ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
						entry = new AppEntry(mContext, info);
						entry.setPackageName(packageName);
						entry.loadLabel();
						entry.loadVersion();
						if (mAppManager.isMyApp(packageName)) {
							appList = mMyAppList;
						}else {
							boolean is_game_app = mAppManager.isGameApp(packageName);
							entry.setIsGameApp(is_game_app);
							if (is_game_app) {
								appList = mGameAppList;
							}else {
								appList = mNormalAppLists;
							}
						}
						
						int index = DreamUtil.getInsertIndex(appList, entry);
						if (appList.size() == index) {
							appList.add(entry);
						} else {
							appList.add(index, entry);
						}
						
					} catch (NameNotFoundException e) {
						e.printStackTrace();
					}
				}  else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
					int[] result = mAppManager.getAppEntry(packageName, mNormalAppLists, mGameAppList, mMyAppList);
					int position = result[1];
					if (AppManager.NORMAL_APP == result[0]) {
						mNormalAppLists.remove(position);
					}else if (AppManager.GAME_APP == result[0]) {
						mGameAppList.remove(position);
					}else if (AppManager.MY_APP == result[0]) {
						mMyAppList.remove(position);
					}
				}
			}
			
			mAdapter.notifyDataSetChanged();
			mAppManager.updateAppUI();
		}
    }
    
    public class AppListTask extends AsyncTask<String, String, List<AppEntry>>{

		@Override
		protected List<AppEntry> doInBackground(String... params) {
			mNormalAppLists.clear();
			mGameAppList.clear();
			mMyAppList.clear();
			
			// Retrieve all known applications.
            List<ApplicationInfo> apps = mAppManager.getAllApps();
            if (apps == null) {
                apps = new ArrayList<ApplicationInfo>();
            }
            for (int i=0; i<apps.size(); i++) {
            	ApplicationInfo appInfo = apps.get(i);
            	//获取非系统应用
            	int flag1 = appInfo.flags & ApplicationInfo.FLAG_SYSTEM;
            	//本来是系统程序，被用户手动更新后，该系统程序也成为第三方应用程序了  
            	int flag2 = appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
            	if ((flag1 <= 0) || flag2 != 0 ) {
    				AppEntry entry = new AppEntry(mContext, apps.get(i));
                    entry.setPackageName(appInfo.packageName);
                    entry.loadLabel();
                    entry.loadVersion();
                    if (mAppManager.isMyApp(appInfo.packageName)) {
						mMyAppList.add(entry);
					}else {
						boolean is_game_app = mAppManager.isGameApp(appInfo.packageName);
	                    entry.setIsGameApp(is_game_app);
	                    if (!isNeedIngore(entry.getPackageName())) {
	                    	if (is_game_app) {
		                    	mGameAppList.add(entry);
							}else {
								mNormalAppLists.add(entry);
							}
						}
					}
                    if (DreamConstant.PACKAGE_NAME.equals(appInfo.packageName)) {
						DreamUtil.package_source_dir = appInfo.sourceDir;
					}
				}else {
					//system app
				}
            }

            // Sort the list.
            Collections.sort(mMyAppList, ALPHA_COMPARATOR);
            Collections.sort(mNormalAppLists, ALPHA_COMPARATOR);
            Collections.sort(mGameAppList, ALPHA_COMPARATOR);

            // Done!
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressBar.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected void onPostExecute(List<AppEntry> result) {
			super.onPostExecute(result);
			mProgressBar.setVisibility(View.GONE);
			mAdapter = new AppBrowserAdapter(mContext, mNormalAppLists);
			mGridView.setAdapter(mAdapter);
			
			mAppManager.updateAppUI();
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
		}
    	
    }
    
    private boolean isNeedIngore(String pkgName){
    	for (AppEntry appEntry : mMyAppList) {
			if (pkgName.equals(appEntry.getPackageName())) {
				return true;
			}
		}
    	return false;
    }
    
    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            return sCollator.compare(object1.getLabel(), object2.getLabel());
        }
    };
    
	@Override
	public void onDestroy() {
		super.onDestroy();
		mContext.unregisterReceiver(mAppReceiver);
	}
}
