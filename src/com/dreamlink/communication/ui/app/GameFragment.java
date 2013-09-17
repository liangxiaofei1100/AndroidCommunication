package com.dreamlink.communication.ui.app;


import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.common.FileSendUtil;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.history.HistoryActivity;
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
	private TextView mEmptyView;
	private ProgressBar mProgressBar;
	private AppAdapter mGameAdapter;
	
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
	public static List<AppInfo> mGameAppList = new ArrayList<AppInfo>();
	
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
	
	private int mAppId = -1;

	/**
	 * Create a new instance of AppFragment, providing "appid" as an
	 * argument.
	 */
	public static GameFragment newInstance(int appid) {
		GameFragment f = new GameFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		mContext = getActivity();
		
		View rootView = inflater.inflate(R.layout.ui_game, container, false);
		mEmptyView = (TextView) rootView.findViewById(R.id.tv_no_game);
		mGridView = (GridView) rootView.findViewById(R.id.gv_game);
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
		mTitleNum.setText("");
		mRefreshView.setOnClickListener(this)	;
		mHistoryView.setOnClickListener(this);
	}
	
	public void setAdapter(List<AppInfo> list){
//		if (null == mGameAdapter) {
//			mGameAdapter = new AppBrowserAdapter(mContext, list);
//			mGridView.setAdapter(mGameAdapter);
//		}else {
//			mGameAdapter.notifyDataSetChanged();
//		}
		
		//使用上面的方法，再次刷新时无法显示数据
		mGameAdapter = new AppAdapter(mContext, list);
		mGridView.setAdapter(mGameAdapter);
	}
	
	public class AppListTask extends AsyncTask<String, String, List<AppInfo>> {

		@Override
		protected List<AppInfo> doInBackground(String... params) {
			mGameAppList.clear();

			// Retrieve all known applications.
			List<ApplicationInfo> apps = mAppManager.getAllApps();
			if (apps == null) {
				apps = new ArrayList<ApplicationInfo>();
			}
			for (int i = 0; i < apps.size(); i++) {
				ApplicationInfo applicationInfo = apps.get(i);
				// 获取非系统应用
				int flag1 = applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM;
				// 本来是系统程序，被用户手动更新后，该系统程序也成为第三方应用程序了
				int flag2 = applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
				if ((flag1 <= 0) || flag2 != 0) {
					AppInfo entry = new AppInfo(mContext, apps.get(i));
					entry.setPackageName(applicationInfo.packageName);
					entry.setAppIcon(applicationInfo.loadIcon(pm));
					entry.loadLabel();
					entry.loadVersion();
					boolean is_game_app = mAppManager.isGameApp(applicationInfo.packageName);
					entry.setIsGameApp(is_game_app);
					if (is_game_app) {
						mGameAppList.add(entry);
					}
					
					if (DreamConstant.PACKAGE_NAME.equals(applicationInfo.packageName)) {
						DreamUtil.package_source_dir = applicationInfo.sourceDir;
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
		protected void onPostExecute(List<AppInfo> result) {
			super.onPostExecute(result);
			mProgressBar.setVisibility(View.GONE);
			if (mGameAppList.size() <= 0) {
				mEmptyView.setVisibility(View.VISIBLE);
			}else {
				mEmptyView.setVisibility(View.GONE);
			}
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
    public static final Comparator<AppInfo> ALPHA_COMPARATOR = new Comparator<AppInfo>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppInfo object1, AppInfo object2) {
            return sCollator.compare(object1.getLabel(), object2.getLabel());
        }
    };
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ApplicationInfo applicationInfo = null;
		applicationInfo = (ApplicationInfo) mGameAppList.get(position).getApplicationInfo();
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
		final AppInfo appInfo = mGameAppList.get(position);
		new AlertDialog.Builder(mContext).setIcon(appInfo.getAppIcon()).setTitle(appInfo.getLabel())
				.setItems(R.array.app_menu_game, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							// open
							Intent intent = pm.getLaunchIntentForPackage(appInfo.getPackageName());
							if (null != intent) {
								startActivity(intent);
							} else {
								mNotice.showToast(R.string.cannot_start_app);
								return;
							}
							break;
						case 1:
							// send
//							FileTransferInfo fileTransferInfo = new FileTransferInfo(new File(appInfo.getInstallPath()));

							FileSendUtil fileSendUtil = new FileSendUtil(getActivity());
							fileSendUtil.sendFile(appInfo.getInstallPath());
							break;
						case 2:
							// uninstall
							mAppManager.uninstallApp(appInfo.getPackageName());
							break;
						case 3:
							// app info
							mAppManager.showInfoDialog(appInfo);
							break;
						case 4:
							// move to app
							mGameAppList.remove(position);
							int index = DreamUtil.getInsertIndex(AppFragment.mAppLists, appInfo);
							if (AppFragment.mAppLists.size() == index) {
								AppFragment.mAppLists.add(appInfo);
							} else {
								AppFragment.mAppLists.add(index, appInfo);
							}
//							// delete from db
							Uri uri = Uri.parse(MetaData.Game.CONTENT_URI + "/" + appInfo.getPackageName());
							mContext.getContentResolver().delete(uri, null, null);

							notifyUpdateUI();
							
							mAppManager.updateAppUI();
							break;

						default:
							break;
						}
					}
				}).create().show();
		return true;
	}
	
	public void notifyUpdateUI(){
		mGameAdapter.notifyDataSetChanged();
		Message message = mHandler.obtainMessage();
		message.arg1 = mGameAppList.size();
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}

	
	//recevier that can update ui
    private  class AppReceiver extends BroadcastReceiver {
        @Override 
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "get receiver:" + action);
			if (AppManager.ACTION_REFRESH_APP.equals(action)) {
				notifyUpdateUI();
			}else if (AppManager.ACTION_SEND_TO_GAME.equals(action)) {
				
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
		switch (v.getId()) {
		case R.id.iv_refresh:
			
			break;
			
		case R.id.iv_history:
			Intent intent = new Intent();
			intent.setClass(mContext, HistoryActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
	}
	
}
