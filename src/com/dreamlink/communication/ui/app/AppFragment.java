package com.dreamlink.communication.ui.app;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.common.FileSendUtil;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.util.Log;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ProgressBar;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

/**
 * use this to load app
 */
public class AppFragment extends Fragment implements OnItemClickListener, OnItemLongClickListener, OnClickListener {
	private static final String TAG = "AppFragment";
	private GridView mGridView;
	private ProgressBar mProgressBar;

	private AppBrowserAdapter mAdapter = null;
	private AppManager mAppManager = null;
	private PackageManager pm = null;
	
	public static  List<AppEntry> mAppLists = new ArrayList<AppEntry>();
	
	private static int mCurrentPosition = -1;
	private Context mContext;
	
	private AppReceiver mAppReceiver;
	private Notice mNotice = null;
	
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	
	private static final int MSG_UPDATE_UI = 0;
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				mTitleNum.setText("(" + size + ")");
				break;

			default:
				break;
			}
		};
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		View rootView = inflater.inflate(R.layout.ui_app_normal, container, false);

		mContext = getActivity();
		
		mNotice = new Notice(mContext);
		
		mGridView = (GridView) rootView.findViewById(R.id.app_normal_gridview);
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.app_progressbar);
		
		getTitleVIews(rootView);
		
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
	
	private void getTitleVIews(View view){
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_app);
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText("应用");
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText("(N)");
		mRefreshView.setOnClickListener(this)	;
		mHistoryView.setOnClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final ApplicationInfo applicationInfo = (ApplicationInfo) mAppLists.get(position).getApplicationInfo();
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
		final AppEntry appEntry = mAppLists.get(position);
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
						FileTransferInfo fileTransferInfo = new FileTransferInfo(new File(appEntry.getInstallPath()));

						FileSendUtil fileSendUtil = new FileSendUtil(getActivity());
						fileSendUtil.sendFile(fileTransferInfo);
					}else if (normal_menus[2].equals(currentMenu)) {
						//uninstall
						mAppManager.uninstallApp(appEntry.getPackageName());
					}else if (normal_menus[3].equals(currentMenu)) {
						//app info
						mAppManager.showInfoDialog(appEntry);
					}else if (normal_menus[4].equals(currentMenu)) {
						//move to game
						mAppLists.remove(position);
						
//						int index = DreamUtil.getInsertIndex(mGameAppList, appEntry);
//						if (mGameAppList.size() == index) {
//							mGameAppList.add(appEntry);
//						} else {
//							mGameAppList.add(index, appEntry);
//						}
						
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
						}else {
							boolean is_game_app = mAppManager.isGameApp(packageName);
							entry.setIsGameApp(is_game_app);
							if (is_game_app) {
							}else {
								appList = mAppLists;
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
//					int[] result = mAppManager.getAppEntry(packageName, mAppLists, mGameAppList, mMyAppList);
//					int position = result[1];
//					if (AppManager.NORMAL_APP == result[0]) {
//						mAppLists.remove(position);
//					}else if (AppManager.GAME_APP == result[0]) {
//					}else if (AppManager.MY_APP == result[0]) {
//					}
				}
			}
			
			mAdapter.notifyDataSetChanged();
			mAppManager.updateAppUI();
		}
    }
    
    public void setAdapter(List<AppEntry> list){
		if (null == mAdapter) {
			mAdapter = new AppBrowserAdapter(mContext, list);
			mGridView.setAdapter(mAdapter);
		}else {
			mAdapter.notifyDataSetChanged();
		}
	}
    
    public class AppListTask extends AsyncTask<String, String, List<AppEntry>>{

		@Override
		protected List<AppEntry> doInBackground(String... params) {
			mAppLists.clear();
			
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
						boolean is_game_app = mAppManager.isGameApp(appInfo.packageName);
	                    entry.setIsGameApp(is_game_app);
	                    	if (is_game_app) {
							}else {
								mAppLists.add(entry);
							}
                    if (DreamConstant.PACKAGE_NAME.equals(appInfo.packageName)) {
						DreamUtil.package_source_dir = appInfo.sourceDir;
					}
                    
                    publishProgress("");
				}else {
					//system app
				}
            }

            // Sort the list.
            Collections.sort(mAppLists, ALPHA_COMPARATOR);

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
//			mAdapter = new AppBrowserAdapter(mContext, mAppLists);
//			mGridView.setAdapter(mAdapter);
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
			setAdapter(mAppLists);
			
			Message message = mHandler.obtainMessage();
			message.arg1 = mAppLists.size();
			message.what  = MSG_UPDATE_UI;
			message.sendToTarget();
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

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
}
