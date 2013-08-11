package com.dreamlink.communication.ui.app;


import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.FileInfoDialog;
import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.util.Log;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.Toast;

public class AppGameFragment extends Fragment implements OnItemClickListener, OnItemLongClickListener {
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
		gridView2.setOnItemLongClickListener(this);
		
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
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		final List<AppEntry> appList = AppNormalFragment.mGameAppList;
		final AppEntry appEntry = appList.get(position);
		new AlertDialog.Builder(mContext)
			.setIcon(appEntry.getIcon())
			.setTitle(appEntry.getLabel())
			.setItems(R.array.app_menu_game, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
						switch (which) {
						case 0:
							//open
							break;
						case 1:
							//send
							break;
						case 2:
							//uninstall 
							mAppManager.uninstallApp(appEntry.getPackageName());
							break;
						case 3:
							//app info
							mAppManager.showInfoDialog(appEntry);
//							String info = mAppManager.getAppInfo(appEntry);
//							FileInfoDialog fragment = FileInfoDialog.newInstance(info);
//							fragment.show(getActivity().getSupportFragmentManager(), "Info");
							break;
						case 4:
							//move to app
							appList.remove(position);
							
							int index = DreamUtil.getInsertIndex(AppNormalFragment.mNormalAppLists, appEntry);
							if (AppNormalFragment.mNormalAppLists.size() == index) {
								AppNormalFragment.mNormalAppLists.add(appEntry);
							} else {
								AppNormalFragment.mNormalAppLists.add(index, appEntry);
							}
							
							//delete from db
							Uri uri = Uri.parse(MetaData.Game.CONTENT_URI + "/" + appEntry.getPackageName());
							mContext.getContentResolver().delete(uri, null, null);
							
							mAdapter2.notifyDataSetChanged();
							mAppManager.updateAppUI();
							break;

						default:
							break;
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
