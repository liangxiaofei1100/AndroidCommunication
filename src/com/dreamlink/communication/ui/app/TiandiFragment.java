package com.dreamlink.communication.ui.app;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.common.FileSendUtil;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.ui.media.MediaAudioFragment;

public class TiandiFragment extends BaseFragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener {
	private GridView mGridView;
	private ProgressBar mProgressBar;
	private Button mRechargeBtn;

	private AppAdapter mAdapter = null;
	private AppManager mAppManager = null;
	private PackageManager pm = null;
	
	public static List<AppInfo> mMyAppList = new ArrayList<AppInfo>();
	
	private Context mContext;
	
	private Notice mNotice = null;
	
	//title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	
	private int mAppId = -1;
	
	/**
	 * Create a new instance of AppFragment, providing "appid" as an
	 * argument.
	 */
	public static TiandiFragment newInstance(int appid) {
		TiandiFragment f = new TiandiFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
	};
	
	// recevier that can update ui
	private BroadcastReceiver myReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (AppManager.ACTION_ADD_MYGAME.equals(action)) {
				ApplicationInfo applicationInfo = intent
						.getParcelableExtra(AppManager.EXTRA_INFO);
				AppInfo appInfo = new AppInfo(mContext, applicationInfo);
				appInfo.setPackageName(applicationInfo.packageName);
				appInfo.setAppIcon(applicationInfo.loadIcon(pm));
				appInfo.setLaunchIntent(pm.getLaunchIntentForPackage(appInfo
						.getPackageName()));
				appInfo.loadLabel();

				mMyAppList.add(appInfo);
				mAdapter.notifyDataSetChanged();
			} else if (AppManager.ACTION_REMOVE_MYGAME.equals(action)) {
				ApplicationInfo applicationInfo = intent
						.getParcelableExtra(AppManager.EXTRA_INFO);
				int position = mAppManager.getAppPosition(
						applicationInfo.packageName, mMyAppList);
				mMyAppList.remove(position);
				mAdapter.notifyDataSetChanged();
			}
		}
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.ui_zytiandi, container, false);
		mContext = getActivity();
		
		mGridView = (GridView) rootView.findViewById(R.id.gv_game);
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.bar_progress);
		mRechargeBtn = (Button) rootView.findViewById(R.id.btn_recharge);
		mRechargeBtn.setOnClickListener(this);
		
		initTitleVIews(rootView);
		
		IntentFilter filter = new IntentFilter(AppManager.ACTION_ADD_MYGAME);
		filter.addAction(AppManager.ACTION_REMOVE_MYGAME);
		mContext.registerReceiver(myReceiver, filter);
		
		return rootView;
	}
	
	private void initTitleVIews(View view){
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_tiandi);
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText("朝颜天地");
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText("");
		mRefreshView.setOnClickListener(this);
		mHistoryView.setOnClickListener(this);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();
		
		mNotice = new Notice(mContext);
		mAppManager = new AppManager(mContext);
		pm = mContext.getPackageManager();

		//get user app
		AppListTask appListTask = new AppListTask();
		appListTask.execute("");

		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
	}
	
	 public class AppListTask extends AsyncTask<String, String, List<AppInfo>>{
			@Override
			protected List<AppInfo> doInBackground(String... params) {
				
				queryAppInfo();
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
				mAdapter = new AppAdapter(mContext, mMyAppList);
				mGridView.setAdapter(mAdapter);
			}
			
			@Override
			protected void onProgressUpdate(String... values) {
				// TODO Auto-generated method stub
				super.onProgressUpdate(values);
			}
	    	
	    }
	 
	// 获得所有启动Activity的信息，类似于 Launch界面
	public void queryAppInfo() {
		Intent mainIntent = new Intent();
		mainIntent.setAction(DreamConstant.APP_ACTION);
		mainIntent.addCategory(Intent.CATEGORY_DEFAULT);
		// 通过查询，获得所有ResolveInfo对象.
		List<ResolveInfo> resolveInfos = pm
				.queryIntentActivities(mainIntent, 0);
		// 调用系统排序,根据name排序
		Collections.sort(resolveInfos,
				new ResolveInfo.DisplayNameComparator(pm));
		mMyAppList.clear();
		for (ResolveInfo reInfo : resolveInfos) {
			String activityName = reInfo.activityInfo.name; // 获得该应用程序的启动Activity的name
			String pkgName = reInfo.activityInfo.packageName; // 获得应用程序的包名
			String appLabel = (String) reInfo.loadLabel(pm); // 获得应用程序的Label
			Drawable icon = reInfo.loadIcon(pm); // 获得应用程序图标
			// 为应用程序的启动 Activity 准备Intent
			Intent launchIntent = new Intent();
			launchIntent.setComponent(new ComponentName(pkgName, activityName));
			// 创建一个 AppInfo对象，并赋值
			AppInfo appInfo = new AppInfo(mContext,
					reInfo.activityInfo.applicationInfo);
			appInfo.setLable(appLabel);
			appInfo.setPackageName(pkgName);
			appInfo.setAppIcon(icon);
			appInfo.setLaunchIntent(launchIntent);

			mMyAppList.add(appInfo); // 添加至列表中
		}
		// Sort the list.
		Collections.sort(mMyAppList, ALPHA_COMPARATOR);
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
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.iv_refresh:
			mNotice.showToast("refresh");
			//get user app
			AppListTask appListTask = new AppListTask();
			appListTask.execute("");
			break;
		case R.id.iv_history:
			Intent intent = new Intent();
			intent.putExtra(Extra.APP_ID, mAppId);
			intent.setClass(mContext, HistoryActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		final AppInfo appInfo = mMyAppList.get(arg2);
		new AlertDialog.Builder(mContext)
		.setIcon(appInfo.getAppIcon())
		.setTitle(appInfo.getLabel())
		.setItems(R.array.zy_game_menu, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						//send
						FileTransferInfo fileTransferInfo = new FileTransferInfo(new File(appInfo.getInstallPath()));
						FileSendUtil fileSendUtil = new FileSendUtil(getActivity());
						fileSendUtil.sendFile(fileTransferInfo);
						break;
					case 1:
						//app info
						mAppManager.showInfoDialog(appInfo);
						break;

					default:
						break;
					}
			}
		}).create().show();
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		Intent intent = mMyAppList.get(position).getLaunchIntent();
		if (null == intent) {
			mNotice.showToast(R.string.cannot_start_app);
			return;
		}
		startActivity(intent);
	}
}
