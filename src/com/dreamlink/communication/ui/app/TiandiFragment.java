package com.dreamlink.communication.ui.app;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;

public class TiandiFragment extends BaseFragment implements OnClickListener {
	private GridView mGridView;
	private ProgressBar mProgressBar;

	private AppBrowserAdapter2 mAdapter = null;
	private AppManager mAppManager = null;
	private PackageManager pm = null;
	
	public static List<AppInfo> mMyAppList = new ArrayList<AppInfo>();
	
	private static int mCurrentPosition = -1;
	private Context mContext;
	
//	private AppReceiver mAppReceiver;
	private Notice mNotice = null;
	
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView = inflater.inflate(R.layout.ui_zhaoyantiandi, container, false);
		mGridView = (GridView) rootView.findViewById(R.id.gv_game);
		mProgressBar = (ProgressBar) rootView.findViewById(R.id.bar_progress);
		
		getTitleVIews(rootView);
		return rootView;
	}
	
	private void getTitleVIews(View view){
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_tiandi);
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText("朝颜天地");
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText("");
		mRefreshView.setOnClickListener(this)	;
		mHistoryView.setOnClickListener(this);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();
		
		mNotice = new Notice(mContext);
		mAppManager = new AppManager(mContext);
		pm = mContext.getPackageManager();

		//get user app
		AppListTask appListTask = new AppListTask();
		appListTask.execute("");

//		mGridView.setOnItemClickListener(this);
//		mGridView.setOnItemLongClickListener(this);
	}
	
	 public class AppListTask extends AsyncTask<String, String, List<AppEntry>>{

			@Override
			protected List<AppEntry> doInBackground(String... params) {
				
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
			protected void onPostExecute(List<AppEntry> result) {
				super.onPostExecute(result);
				mProgressBar.setVisibility(View.GONE);
				mAdapter = new AppBrowserAdapter2(mContext, mMyAppList);
				mGridView.setAdapter(mAdapter);
				
				mAppManager.updateAppUI();
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
			List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
			// 调用系统排序,根据name排序
			Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(pm));
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
					AppInfo appInfo = new AppInfo();

					appInfo.setAppLabel(appLabel);
					appInfo.setPkgName(pkgName);
					appInfo.setAppIcon(icon);
					appInfo.setIntent(launchIntent);
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
	            return sCollator.compare(object1.getAppLabel(), object2.getAppLabel());
	        }
	    };

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
}
