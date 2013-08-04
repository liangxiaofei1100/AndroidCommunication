package com.dreamlink.communication.ui.app;


import com.dreamlink.communication.R;
import com.dreamlink.communication.util.Log;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;

public class AppGameFragment extends Fragment implements OnItemClickListener {
	private static final String TAG = "AppGameFragment";
	private GridView gridView1,gridView2;
	private AppBrowserAdapter mAdapter1;
	private AppBrowserAdapter mAdapter2;
	
	private AppReceiver mAppReceiver;
	private AppManager mAppManager;
	
	private PackageManager pm;
	
	private int mCurrentPosition = -1;
	
	private Context mContext;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		mContext = getActivity();
		
		View rootView = inflater.inflate(R.layout.ui_app_game, container, false);
		gridView1 = (GridView) rootView.findViewById(R.id.game_gridview_1);
		gridView1.setEmptyView(rootView.findViewById(R.id.game_empty_textview_1));
		gridView2 = (GridView) rootView.findViewById(R.id.game_gridview_2);
		gridView2.setEmptyView(rootView.findViewById(R.id.game_empty_textview_2));
		gridView2.setOnItemClickListener(this);
		gridView2.setOnCreateContextMenuListener(new ListOnCreateContext());
		
		pm = mContext.getPackageManager();
		mAppManager = new AppManager(mContext);

		mAppReceiver = new AppReceiver();
		IntentFilter filter = new IntentFilter(AppManager.ACTION_REFRESH_APP);
		mContext.registerReceiver(mAppReceiver, filter);
		
		mAdapter2 = new AppBrowserAdapter(mContext, AppNormalFragment.mGameAppList);
		gridView2.setAdapter(mAdapter2);
		
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final ApplicationInfo applicationInfo = (ApplicationInfo) AppNormalFragment.mGameAppList.get(position).getApplicationInfo();
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
			Log.d(TAG, "onCreateContextMenu");
			AppManager.menu_type = AppManager.GAME_APP_MENU_MY;
			menu.setHeaderTitle(R.string.operitor_title);
			menu.add(0, AppManager.MENU_INFO, 0, "App Info");
			menu.add(0, AppManager.MENU_SHARE, 0, "Share");
			menu.add(0, AppManager.MENU_UNINSTALL, 1, "UNINSTALL");
			menu.add(0, AppManager.MENU_MOVE, 1, "Move to APP");
		}
	}

	
	//recevier that can update ui
    private  class AppReceiver extends BroadcastReceiver {
        @Override 
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "get receiver:" + action);
			if (AppManager.ACTION_REFRESH_APP.equals(action)) {
				mAdapter2.notifyDataSetChanged();
			}
		}
    }
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mContext.unregisterReceiver(mAppReceiver);
	}
	
}
