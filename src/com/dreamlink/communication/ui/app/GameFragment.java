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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;

public class GameFragment extends Fragment implements OnItemClickListener, OnItemLongClickListener, OnClickListener {
	private static final String TAG = "AppGameFragment";
	private GridView mGridView;
	private ProgressBar mProgressBar;
	private AppBrowserAdapter mGameAdapter;
	
	private AppReceiver mAppReceiver;
	private AppManager mAppManager = null;
	private PackageManager pm = null;
	
	private int mCurrentPosition = -1;
	
	private Context mContext;
	private Notice mNotice = null;
	
	//title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	//title views
	private static List<AppEntry> mGameAppList = new ArrayList<AppEntry>();
	
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
		Log.d(TAG, "onCreate begin");
		mContext = getActivity();
		
		View rootView = inflater.inflate(R.layout.ui_game, container, false);
		mGridView = (GridView) rootView.findViewById(R.id.gv_game);
		mGridView.setEmptyView(rootView.findViewById(R.id.game_empty_textview));
		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.app_progressbar);
		
		getTitleVIews(rootView);
		
		mNotice = new Notice(mContext);
		
		pm = mContext.getPackageManager();
		mAppManager = new AppManager(mContext);

		mAppReceiver = new AppReceiver();
		IntentFilter filter = new IntentFilter(AppManager.ACTION_REFRESH_APP);
		mContext.registerReceiver(mAppReceiver, filter);
		
		//get user app
		AppListTask appListTask = new AppListTask();
		appListTask.execute("");
		
		mGameAdapter = new AppBrowserAdapter(mContext, AppNormalFragment.mGameAppList);
		mGridView.setAdapter(mGameAdapter);
		
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	private void getTitleVIews(View view){
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_game);
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText("游戏");
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText("(N)");
		mRefreshView.setOnClickListener(this)	;
		mHistoryView.setOnClickListener(this);
	}
	
	public void setAdapter(List<AppEntry> list){
//		if (null == mGameAdapter) {
//			mGameAdapter = new AppBrowserAdapter(mContext, list);
//			mGridView.setAdapter(mGameAdapter);
//		}else {
//			mGameAdapter.notifyDataSetChanged();
//		}
		
		//使用上面的方法，再次刷新时无法显示数据
		mGameAdapter = new AppBrowserAdapter(mContext, list);
		mGridView.setAdapter(mGameAdapter);
	}
	
	public class AppListTask extends AsyncTask<String, String, List<AppEntry>> {

		@Override
		protected List<AppEntry> doInBackground(String... params) {
			mGameAppList.clear();

			// Retrieve all known applications.
			List<ApplicationInfo> apps = mAppManager.getAllApps();
			if (apps == null) {
				apps = new ArrayList<ApplicationInfo>();
			}
			for (int i = 0; i < apps.size(); i++) {
				ApplicationInfo appInfo = apps.get(i);
				// 获取非系统应用
				int flag1 = appInfo.flags & ApplicationInfo.FLAG_SYSTEM;
				// 本来是系统程序，被用户手动更新后，该系统程序也成为第三方应用程序了
				int flag2 = appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
				if ((flag1 <= 0) || flag2 != 0) {
					AppEntry entry = new AppEntry(mContext, apps.get(i));
					entry.setPackageName(appInfo.packageName);
					entry.loadLabel();
					entry.loadVersion();
					boolean is_game_app = mAppManager.isGameApp(appInfo.packageName);
					entry.setIsGameApp(is_game_app);
					if (is_game_app) {
						mGameAppList.add(entry);
					}
					
					if (DreamConstant.PACKAGE_NAME.equals(appInfo.packageName)) {
						DreamUtil.package_source_dir = appInfo.sourceDir;
					}
					publishProgress("");
				} else {
					// system app
				}
			}

			// Sort the list.
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
//			mAdapter1 = new AppBrowserAdapter(mContext, mGameAppList);
//			mGridView.setAdapter(mAdapter1);
			
		}

		@Override
		protected void onProgressUpdate(String... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
			setAdapter(mGameAppList);
			
			Message message = mHandler.obtainMessage();
			message.arg1 = mGameAppList.size();
			message.what = MSG_UPDATE_UI;
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
		final List<AppEntry> appList2 = AppNormalFragment.mGameAppList;
		final AppEntry appEntry2 = appList2.get(position);
		new AlertDialog.Builder(mContext).setIcon(appEntry2.getIcon()).setTitle(appEntry2.getLabel())
				.setItems(R.array.app_menu_game, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							// open
							Intent intent = pm.getLaunchIntentForPackage(appEntry2.getPackageName());
							if (null != intent) {
								startActivity(intent);
							} else {
								mNotice.showToast(R.string.cannot_start_app);
								return;
							}
							break;
						case 1:
							// send
							FileTransferInfo fileTransferInfo = new FileTransferInfo(new File(appEntry2.getInstallPath()));

							FileSendUtil fileSendUtil = new FileSendUtil(getActivity());
							fileSendUtil.sendFile(fileTransferInfo);
							break;
						case 2:
							// uninstall
							mAppManager.uninstallApp(appEntry2.getPackageName());
							break;
						case 3:
							// app info
							mAppManager.showInfoDialog(appEntry2);
							break;
						case 4:
							// move to app
							appList2.remove(position);

							int index = DreamUtil.getInsertIndex(AppNormalFragment.mNormalAppLists, appEntry2);
							if (AppNormalFragment.mNormalAppLists.size() == index) {
								AppNormalFragment.mNormalAppLists.add(appEntry2);
							} else {
								AppNormalFragment.mNormalAppLists.add(index, appEntry2);
							}

							// delete from db
							Uri uri = Uri.parse(MetaData.Game.CONTENT_URI + "/" + appEntry2.getPackageName());
							mContext.getContentResolver().delete(uri, null, null);

							mGameAdapter.notifyDataSetChanged();
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
				mGameAdapter.notifyDataSetChanged();
			}
		}
    }
	
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
