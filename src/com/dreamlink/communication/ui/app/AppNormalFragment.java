package com.dreamlink.communication.ui.app;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.FileInfoDialog;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.util.Log;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

/**
 * use this to load app
 */
public class AppNormalFragment extends Fragment implements OnItemClickListener {
	private static final String TAG = "AppNormalFragment";
	private GridView mGridView;
	private ProgressBar mProgressBar;

	private AppBrowserAdapter mAdapter = null;
	private AppManager mAppManager = null;
	private PackageManager pm = null;
	
	public static  List<AppEntry> mNormalAppLists = new ArrayList<AppEntry>();
	public static List<AppEntry> mGameAppList = new ArrayList<AppEntry>();
	
	private static int mCurrentPosition = -1;
	private Context mContext;
	
	private AppReceiver mAppReceiver;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		View rootView = inflater.inflate(R.layout.ui_app_normal, container, false);

		mContext = getActivity();
		
		mGridView = (GridView) rootView.findViewById(R.id.app_normal_gridview);
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.app_progressbar);
		
		//register broadcast
		mAppReceiver = new AppReceiver();
		IntentFilter filter = new IntentFilter(AppManager.ACTION_REFRESH_APP);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
		getActivity().registerReceiver(mAppReceiver, filter);
		
		mAppManager = new AppManager(mContext);

		//get user app
		AppListTask appListTask = new AppListTask();
		appListTask.execute("");

		mGridView.setOnItemClickListener(this);
		mGridView.setOnCreateContextMenuListener(new ListOnCreateContext());
	
		pm = mContext.getPackageManager();
		
		Log.d(TAG, "onCreate end");
		return rootView;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final ApplicationInfo applicationInfo = (ApplicationInfo) mNormalAppLists.get(position).getApplicationInfo();
		String packageName = applicationInfo.packageName;
		PackageInfo packageInfo = null;
		try {
			packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			// start app
			ActivityInfo activityInfo = null;
			activityInfo = packageInfo.activities[0];
			if (activityInfo == null) {
				Toast.makeText(mContext, "can not start this app!", Toast.LENGTH_SHORT).show();
				return;
			} else {
				String packagename = packageInfo.packageName;
				String activityName = activityInfo.name;
				Intent intent = new Intent();
				// start app by package name
				intent.setComponent(new ComponentName(packagename, activityName));
				startActivity(intent);
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	/** context menu */
	private class ListOnCreateContext implements OnCreateContextMenuListener {
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			AppManager.menu_type = AppManager.NORMAL_APP_MENU;
			menu.setHeaderTitle(R.string.operitor_title);
			menu.add(0, AppManager.MENU_INFO, 0, "App Info");
			menu.add(0, AppManager.MENU_SHARE, 0, "Share");
			menu.add(0, AppManager.MENU_UNINSTALL, 1, "UNINSTALL");
			menu.add(0, AppManager.MENU_MOVE, 1, "Move to game");
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
		mCurrentPosition = menuInfo.position;
		int position = menuInfo.position;
		AppEntry appEntry = null;
		Log.d(TAG, "mcurrentposition:" + position + "\n"
				+ "type:" + AppManager.menu_type);
		List<AppEntry> appList = new ArrayList<AppEntry>();
		if (AppManager.NORMAL_APP_MENU == AppManager.menu_type) {
			appList = mNormalAppLists;
		}else if (AppManager.GAME_APP_MENU_MY == AppManager.menu_type) {
			appList = mGameAppList;
		}
		appEntry = appList.get(position);
		switch (item.getItemId()) {
		case AppManager.MENU_INFO:
			 FileInfoDialog fragment = FileInfoDialog.newInstance(appEntry, FileInfoDialog.APP_INFO);
//			 if (null != fragment) {
//				fragment.dismissAllowingStateLoss();
//			}
		     fragment.show(getFragmentManager(), "Info");
			break;
		case AppManager.MENU_SHARE:
			break;
		case AppManager.MENU_UNINSTALL:
			// uninstall app
			Uri packageUri = Uri.parse("package:" + appEntry.getApplicationInfo().packageName);
			Intent deleteIntent = new Intent();
			deleteIntent.setAction(Intent.ACTION_DELETE);
			deleteIntent.setData(packageUri);
			startActivity(deleteIntent);
			break;
			
		case AppManager.MENU_MOVE:
			appList.remove(position);
			
			if (AppManager.NORMAL_APP_MENU == AppManager.menu_type) {
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
			}else if (AppManager.GAME_APP_MENU_MY == AppManager.menu_type) {
				int index = DreamUtil.getInsertIndex(mNormalAppLists, appEntry);
				if (mNormalAppLists.size() == index) {
					mNormalAppLists.add(appEntry);
				} else {
					mNormalAppLists.add(index, appEntry);
				}
				//delete from db
				Uri uri = Uri.parse(MetaData.Game.CONTENT_URI + "/" + appEntry.getPackageName());
				mContext.getContentResolver().delete(uri, null, null);
			}
			
			//update myself
			mAdapter.notifyDataSetChanged();
			//and then update others
			mAppManager.updateAppUI();
			break;

		default:
			break;
		}
		return super.onContextItemSelected(item);
	}
	
	//recevier that can update ui
    private  class AppReceiver extends BroadcastReceiver {
        @Override 
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "get receiver:" + action);
			if (AppManager.ACTION_REFRESH_APP.equals(action)) {
				//
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
						
						boolean is_game_app = mAppManager.isGameApp(packageName);
						entry.setIsGameApp(is_game_app);
						if (is_game_app) {
							appList = mGameAppList;
						}else {
							appList = mNormalAppLists;
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
					int[] result = mAppManager.getAppEntry(packageName, mNormalAppLists, mGameAppList);
					int position = result[1];
					if (AppManager.NORMAL_APP == result[0]) {
						mNormalAppLists.remove(position);
					}else if (AppManager.GAME_APP == result[0]) {
						mGameAppList.remove(position);
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
			// Retrieve all known applications.
            List<ApplicationInfo> apps = mAppManager.getAllApps();;
            if (apps == null) {
                apps = new ArrayList<ApplicationInfo>();
            }

            for (int i=0; i<apps.size(); i++) {
            	ApplicationInfo appInfo = apps.get(i);
    			if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {
    				//system app
    			}else {
    				AppEntry entry = new AppEntry(mContext, apps.get(i));
                    entry.setPackageName(appInfo.packageName);
                    entry.loadLabel();
                    entry.loadVersion();
                    boolean is_game_app = mAppManager.isGameApp(appInfo.packageName);
                    entry.setIsGameApp(is_game_app);
                    if (is_game_app) {
                    	mGameAppList.add(entry);
					}else {
						mNormalAppLists.add(entry);
					}
				}
            }

            // Sort the list.
            Collections.sort(mNormalAppLists, ALPHA_COMPARATOR);
            Collections.sort(mGameAppList, ALPHA_COMPARATOR);

            // Done!
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
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
