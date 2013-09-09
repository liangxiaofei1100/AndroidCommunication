package com.dreamlink.communication.ui.app;


import java.io.File;
import java.util.Iterator;
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;

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
	private Notice mNotice = null;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		mContext = getActivity();
		
		View rootView = inflater.inflate(R.layout.ui_app_game, container, false);
		gridView1 = (GridView) rootView.findViewById(R.id.game_gridview_1);
		gridView1.setEmptyView(rootView.findViewById(R.id.game_empty_textview_1));
		gridView1.setOnItemClickListener(this);
		gridView1.setOnItemLongClickListener(this);
		gridView2 = (GridView) rootView.findViewById(R.id.game_gridview_2);
		gridView2.setEmptyView(rootView.findViewById(R.id.game_empty_textview_2));
		gridView2.setOnItemClickListener(this);
		gridView2.setOnItemLongClickListener(this);
		
		mNotice = new Notice(mContext);
		
		pm = mContext.getPackageManager();
		mAppManager = new AppManager(mContext);

		mAppReceiver = new AppReceiver();
		IntentFilter filter = new IntentFilter(AppManager.ACTION_REFRESH_APP);
		mContext.registerReceiver(mAppReceiver, filter);
		
		mAdapter1 = new AppBrowserAdapter(mContext, AppNormalFragment.mMyAppList);
		gridView1.setAdapter(mAdapter1);
		
		mAdapter2 = new AppBrowserAdapter(mContext, AppNormalFragment.mGameAppList);
		gridView2.setAdapter(mAdapter2);
		
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ApplicationInfo applicationInfo = null;
		switch (parent.getId()) {
		case R.id.game_gridview_1:
			applicationInfo = (ApplicationInfo) AppNormalFragment.mMyAppList.get(position).getApplicationInfo();
			break;
		case R.id.game_gridview_2:
			applicationInfo = (ApplicationInfo) AppNormalFragment.mGameAppList.get(position).getApplicationInfo();
			break;
		default:
			break;
		}
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
		switch (parent.getId()) {
		case R.id.game_gridview_1:
			final List<AppEntry> appList1 = AppNormalFragment.mMyAppList;
			final AppEntry appEntry1 = appList1.get(position);
			new AlertDialog.Builder(mContext)
			.setIcon(appEntry1.getIcon())
			.setTitle(appEntry1.getLabel())
			.setItems(R.array.app_menu_game1, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
						switch (which) {
						case 0:
							//open
							Intent intent = pm.getLaunchIntentForPackage(appEntry1.getPackageName());
							if (null != intent) {
								startActivity(intent);
							}else {
								mNotice.showToast(R.string.cannot_start_app);
								return;
							}
							break;
						case 1:
							//app info
							mAppManager.showInfoDialog(appEntry1);
							break;

						default:
							break;
						}
				}
			}).create().show();
			break;
			
		case R.id.game_gridview_2:
			final List<AppEntry> appList2 = AppNormalFragment.mGameAppList;
			final AppEntry appEntry2 = appList2.get(position);
			new AlertDialog.Builder(mContext)
			.setIcon(appEntry2.getIcon())
			.setTitle(appEntry2.getLabel())
			.setItems(R.array.app_menu_game, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							//open
							Intent intent = pm.getLaunchIntentForPackage(appEntry2.getPackageName());
							if (null != intent) {
								startActivity(intent);
							}else {
								mNotice.showToast(R.string.cannot_start_app);
								return;
							}
							break;
						case 1:
							//send
							FileTransferInfo fileTransferInfo = new FileTransferInfo(new File(appEntry2.getInstallPath()));

							FileSendUtil fileSendUtil = new FileSendUtil(getActivity());
							fileSendUtil.sendFile(fileTransferInfo);
							break;
						case 2:
							//uninstall 
							mAppManager.uninstallApp(appEntry2.getPackageName());
							break;
						case 3:
							//app info
							mAppManager.showInfoDialog(appEntry2);
							break;
						case 4:
							//move to app
							appList2.remove(position);
							
							int index = DreamUtil.getInsertIndex(AppNormalFragment.mNormalAppLists, appEntry2);
							if (AppNormalFragment.mNormalAppLists.size() == index) {
								AppNormalFragment.mNormalAppLists.add(appEntry2);
							} else {
								AppNormalFragment.mNormalAppLists.add(index, appEntry2);
							}
							
							//delete from db
							Uri uri = Uri.parse(MetaData.Game.CONTENT_URI + "/" + appEntry2.getPackageName());
							mContext.getContentResolver().delete(uri, null, null);
							
							mAdapter2.notifyDataSetChanged();
							mAppManager.updateAppUI();
							break;

						default:
							break;
						}
				}
			}).create().show();
			break;
		default:
			break;
		}
		return true;
	}

	
	//recevier that can update ui
    private  class AppReceiver extends BroadcastReceiver {
        @Override 
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "get receiver:" + action);
			if (AppManager.ACTION_REFRESH_APP.equals(action)) {
				mAdapter1.notifyDataSetChanged();
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
