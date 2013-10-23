package com.dreamlink.communication.ui.app;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.MainFragmentActivity;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.app.AppCursorAdapter.ViewHolder;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.common.FileTransferUtil.TransportCallback;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.util.Log;

public class TiandiFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener {
	private static final String TAG = "TiandiFragment";
	private GridView mGridView;
	private ProgressBar mLoadingBar;
	private Button mRechargeBtn;

	private AppCursorAdapter mAdapter = null;
	private AppManager mAppManager = null;
	private PackageManager pm = null;
	
	public static List<AppInfo> mMyAppList = new ArrayList<AppInfo>();
	
	private Context mContext;
	
	private Notice mNotice = null;
	private QueryHandler mQueryHandler;
	
	private int mAppId = -1;
	private Cursor mCursor;
	
	/**
	 * Create a new instance of TiandiFragment, providing "appid" as an
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
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		mFragmentActivity.addObject(MainFragmentActivity.ZY_TIANDI, (BaseFragment)this);
		mFragmentActivity.setTitleName(MainFragmentActivity.ZY_TIANDI);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.ui_zytiandi, container, false);
		mContext = getActivity();
		
		mGridView = (GridView) rootView.findViewById(R.id.gv_game);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.bar_progress);
		mRechargeBtn = (Button) rootView.findViewById(R.id.btn_recharge);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();
		
		mNotice = new Notice(mContext);
		mAppManager = new AppManager(mContext);
		pm = mContext.getPackageManager();
		
		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
		
		mQueryHandler = new QueryHandler(getActivity().getContentResolver());
		query();
	}
	
	private static final String[] PROJECTION = {
		AppData.App._ID,AppData.App.PKG_NAME,AppData.App.LABEL,
		AppData.App.APP_SIZE,AppData.App.VERSION,AppData.App.DATE,
		AppData.App.TYPE,AppData.App.ICON,AppData.App.PATH
	};
	
	public void query(){
		mLoadingBar.setVisibility(View.VISIBLE);
		//查询类型为游戏的所有数据
		String selectionString = AppData.App.TYPE + "=?" ;
    	String args[] = {"" + AppManager.ZHAOYAN_APP};
		mQueryHandler.startQuery(11, null, AppData.App.CONTENT_URI, PROJECTION, selectionString, args, AppData.App.SORT_ORDER_LABEL);
	}
	
	//query db
	private class QueryHandler extends AsyncQueryHandler {

		public QueryHandler(ContentResolver cr) {
			super(cr);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			// super.onQueryComplete(token, cookie, cursor);
			mLoadingBar.setVisibility(View.INVISIBLE);
			if (null != cursor && cursor.getCount() > 0) {
				mCursor = cursor;
				Log.d(TAG, "onQueryComplete.count=" + cursor.getCount());
				mAdapter = new AppCursorAdapter(mContext);
				mAdapter.changeCursor(cursor);
				mGridView.setAdapter(mAdapter);
			}

		}

	}
	 
	/**
	 * Perform alphabetical comparison of application entry objects.
	 */
	public static final Comparator<AppInfo> LABEL_COMPARATOR = new Comparator<AppInfo>() {
		private final Collator sCollator = Collator.getInstance();

		@Override
		public int compare(AppInfo object1, AppInfo object2) {
			return sCollator.compare(object1.getLabel(), object2.getLabel());
		}
	};

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		mCursor.moveToPosition(position);
		String packagename = mCursor.getString(mCursor.getColumnIndex(AppData.App.PKG_NAME));
		ApplicationInfo applicationInfo = null;
		AppInfo appInfo = null;
		try {
			applicationInfo = pm.getApplicationInfo(packagename, 0);
			appInfo = new AppInfo(getActivity(), applicationInfo);
			appInfo.setPackageName(packagename);
			appInfo.setAppIcon(applicationInfo.loadIcon(pm));
			appInfo.loadLabel();
			appInfo.loadVersion();

			showMenuDialog(appInfo, arg1);
		} catch (NameNotFoundException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
		mCursor.moveToPosition(position);
		String packagename = mCursor.getString(mCursor.getColumnIndex(AppData.App.PKG_NAME));
		Intent intent = pm.getLaunchIntentForPackage(packagename);
		if (null == intent) {
			mNotice.showToast(R.string.cannot_start_app);
			return;
		}
		startActivity(intent);
	}
	
	public void showMenuDialog(final AppInfo appInfo, final View view) {
		new AlertDialog.Builder(mContext)
				.setIcon(appInfo.getAppIcon())
				.setTitle(appInfo.getLabel())
				.setItems(R.array.zy_game_menu,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case 0:
									// send
									FileTransferUtil fileSendUtil = new FileTransferUtil(
											getActivity());
									fileSendUtil.sendFile(
											appInfo.getInstallPath(),
											new TransportCallback() {

												@Override
												public void onTransportSuccess() {
													ViewHolder viewHolder = (ViewHolder) view.getTag();
													showTransportAnimation(viewHolder.iconView);
												}

												@Override
												public void onTransportFail() {

												}
											});
									break;
								case 1:
									// app info
									mAppManager.showInfoDialog(appInfo);
									break;

								default:
									break;
								}
							}
						}).create().show();
	}
	
}
