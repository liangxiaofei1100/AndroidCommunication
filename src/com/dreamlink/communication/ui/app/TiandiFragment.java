package com.dreamlink.communication.ui.app;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
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
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.util.Log;

public class TiandiFragment extends BaseFragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener {
	private static final String TAG = "TiandiFragment2";
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
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.ui_zytiandi, container, false);
		mContext = getActivity();
		
		mGridView = (GridView) rootView.findViewById(R.id.gv_game);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.bar_progress);
		mRechargeBtn = (Button) rootView.findViewById(R.id.btn_recharge);
		mRechargeBtn.setOnClickListener(this);
		
		initTitleVIews(rootView);
		
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
		mQueryHandler.startQuery(11, null, AppData.App.CONTENT_URI, PROJECTION, selectionString, args, AppData.App.SORT_ORDER_DEFAULT);
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
				if (null != cursor && cursor.getCount() > 0) {
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
//						FileTransferInfo fileTransferInfo = new FileTransferInfo(new File(appInfo.getInstallPath()));
						FileTransferUtil fileSendUtil = new FileTransferUtil(getActivity());
						fileSendUtil.sendFile(appInfo.getInstallPath());
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
	
	public void onDestroy() {
		super.onDestroy();
	};
}
