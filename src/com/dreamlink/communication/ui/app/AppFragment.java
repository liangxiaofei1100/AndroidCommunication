package com.dreamlink.communication.ui.app;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.ui.history.HistoryCursorAdapter;
import com.dreamlink.communication.ui.history.HistoryManager;
import com.dreamlink.communication.util.Log;

import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
public class AppFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, OnClickListener {
	private static final String TAG = "AppFragment";
	private GridView mGridView;
	private ProgressBar mLoadingBar;

	private AppCursorAdapter mAdapter = null;
	private AppManager mAppManager = null;
	private PackageManager pm = null;
	
	public static List<AppInfo> mAppLists = new ArrayList<AppInfo>();
	
	private Context mContext;
	
	private AppReceiver mAppReceiver;
	private Notice mNotice = null;
	private QueryHandler mQueryHandler;
	
	//title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	private int mAppId = -1;
	private Cursor mCursor;
	
	/**
	 * Create a new instance of AppFragment, providing "appid" as an
	 * argument.
	 */
	public static AppFragment newInstance(int appid) {
		AppFragment f = new AppFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}
	
	private static final int MSG_UPDATE_UI = 0;
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				mTitleNum.setText(getResources().getString(R.string.num_format, size));
				break;

			default:
				break;
			}
		};
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		View rootView = inflater.inflate(R.layout.ui_app_normal, container, false);

		mContext = getActivity();
		
		mNotice = new Notice(mContext);
		
		mGridView = (GridView) rootView.findViewById(R.id.app_normal_gridview);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.app_progressbar);
		
		initTitleVIews(rootView);
		
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
		mQueryHandler = new QueryHandler(getActivity().getContentResolver());

		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
		
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		query();
		super.onActivityCreated(savedInstanceState);
	}
	
	private void initTitleVIews(View view){
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		//title icon
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_app);
		// refresh button
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		// go to history button
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		// title name
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.app);
		// show current page's item num
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText(getResources().getString(R.string.num_format, 0));
		mRefreshView.setOnClickListener(this);
		mHistoryView.setOnClickListener(this);
	}
	
	private static final String[] PROJECTION = {
		AppData.App._ID,AppData.App.PKG_NAME,AppData.App.LABEL,
		AppData.App.APP_SIZE,AppData.App.VERSION,AppData.App.DATE,
		AppData.App.TYPE,AppData.App.ICON,AppData.App.PATH
	};
	
	public void query(){
		mLoadingBar.setVisibility(View.VISIBLE);
		//查询类型为应用的所有数据
		String selectionString = AppData.App.TYPE + "=?" ;
    	String args[] = {"" + AppManager.NORMAL_APP};
		mQueryHandler.startQuery(11, null, AppData.App.CONTENT_URI, PROJECTION, selectionString, args, AppData.App.SORT_ORDER_LABEL);
	}
	
	//query db
	public class QueryHandler extends AsyncQueryHandler {

		public QueryHandler(ContentResolver cr) {
			super(cr);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			// super.onQueryComplete(token, cookie, cursor);
			Log.d(TAG, "onQueryComplete");
			mLoadingBar.setVisibility(View.INVISIBLE);
			Message message = mHandler.obtainMessage();
			if (null != cursor && cursor.getCount() > 0) {
				mCursor = cursor;
				Log.d(TAG, "onQueryComplete.count=" + cursor.getCount());
				mAdapter = new AppCursorAdapter(mContext);
				mAdapter.changeCursor(cursor);
				mGridView.setAdapter(mAdapter);
				message.arg1 = cursor.getCount();
			} else {
				message.arg1 = 0;
			}

			message.what = MSG_UPDATE_UI;
			message.sendToTarget();
		}

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mCursor.moveToPosition(position);
		String packagename = mCursor.getString(mCursor.getColumnIndex(AppData.App.PKG_NAME));
		if (DreamConstant.PACKAGE_NAME.equals(packagename)) {
			mNotice.showToast(R.string.app_has_started);
			return;
		}
		
		Intent intent = pm.getLaunchIntentForPackage(packagename);
		if (null != intent) {
			startActivity(intent);
		}else {
			mNotice.showToast(R.string.cannot_start_app);
			return;
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			final int position, long id) {
		mCursor.moveToPosition(position);
		final String packagename = mCursor.getString(mCursor
				.getColumnIndex(AppData.App.PKG_NAME));
		ApplicationInfo applicationInfo = null;
		AppInfo appInfo = null;
		try {
			applicationInfo = pm.getApplicationInfo(packagename, 0);
			appInfo = new AppInfo(getActivity(), applicationInfo);
			appInfo.setPackageName(packagename);
			appInfo.setAppIcon(applicationInfo.loadIcon(pm));
			appInfo.loadLabel();
			appInfo.loadVersion();

			showMenuDialog(appInfo);
		} catch (NameNotFoundException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}
		return true;
	}
	
	public void showMenuDialog(final AppInfo appInfo){
		int resId = R.array.app_menu_normal;
		if (DreamConstant.PACKAGE_NAME.equals(appInfo.getPackageName())) {
			//本身这个程序不允许卸载，不允许移动到游戏，已经打开了，所以没有打开选项
			//总之，菜单要不一样
			resId = R.array.app_menu_myself;
		}
		final String[] current_menus = getResources().getStringArray(resId);
		final String[] normal_menus = getResources().getStringArray(R.array.app_menu_normal);
		new AlertDialog.Builder(mContext)
		.setIcon(appInfo.getAppIcon())
		.setTitle(appInfo.getLabel())
		.setItems(resId, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String currentMenu = current_menus[which];
				if (normal_menus[0].equals(currentMenu)) {
					//open
					Intent intent = pm.getLaunchIntentForPackage(appInfo.getPackageName());
					if (null != intent) {
						startActivity(intent);
					}else {
						mNotice.showToast(R.string.cannot_start_app);
						return;
					}
				}else if (normal_menus[1].equals(currentMenu)) {
					//send
					FileTransferUtil fileSendUtil = new FileTransferUtil(getActivity());
					fileSendUtil.sendFile(appInfo.getInstallPath());
				}else if (normal_menus[2].equals(currentMenu)) {
					//uninstall
					mAppManager.uninstallApp(appInfo.getPackageName());
				}else if (normal_menus[3].equals(currentMenu)) {
					//app info
					mAppManager.showInfoDialog(appInfo);
				}else if (normal_menus[4].equals(currentMenu)) {
					//move to game
					//1，将该记录的type设置为game
					//2，将数据插入到game表中
					//3，通知GameFragment
					//4，重新查询数据库
					ContentResolver contentResolver = getActivity().getContentResolver();
					ContentValues values = null;
					
					values = new ContentValues();
					values.put(AppData.App.TYPE, AppManager.GAME_APP);
					contentResolver.update(AppData.App.CONTENT_URI, values, 
							AppData.App.PKG_NAME + "='" + appInfo.getPackageName() + "'", null);
					
					//insert to db
					values = new ContentValues();
					values.put(AppData.App.PKG_NAME, appInfo.getPackageName());
					contentResolver.insert(AppData.AppGame.CONTENT_URI, values);
					
//					
					Intent intent = new Intent(AppManager.ACTION_REFRESH_APP);
					mContext.sendBroadcast(intent);
					
					mCursor.requery();
				}
			}
		}).create().show();
	}
	
	//recevier that can update ui
	private class AppReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "get receiver:" + action);
			if (AppManager.ACTION_REFRESH_APP.equals(action)) {
				if (null != mCursor) {
					mCursor.requery();
				}
			}
		}
	}
    
    public void notifyUpdateUI(int num){
		Message message = mHandler.obtainMessage();
		message.arg1 = num;
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
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