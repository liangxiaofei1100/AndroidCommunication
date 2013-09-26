package com.dreamlink.communication.ui.history;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.util.Log;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HistoryActivity extends FragmentActivity implements
		OnScrollListener, OnItemClickListener {
	private static final String TAG = "HistoryActivityTest";
	private Context mContext;

	// view
	private TextView mStorageTV;
	private ListView mHistoryMsgLV;
	private ProgressBar mLoadingBar;

	// adapter
	private HistoryCursorAdapter mAdapter;

	private Notice mNotice;

	// msg
	private static final int MSG_UPDATE_UI = 1;

	private QueryHandler queryHandler = null;

	// title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	// title views

	private static final String[] PROJECTION = { MetaData.History._ID,
			MetaData.History.FILE_PATH, MetaData.History.FILE_NAME,
			MetaData.History.FILE_SIZE, MetaData.History.SEND_USERNAME,
			MetaData.History.RECEIVE_USERNAME, MetaData.History.PROGRESS,
			MetaData.History.DATE, MetaData.History.STATUS,
			MetaData.History.MSG_TYPE, MetaData.History.FILE_TYPE };

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int num = msg.arg1;
				mTitleNum.setText(getResources().getString(R.string.num_format,
						num));
				break;

			default:
				break;
			}
		};
	};
	
	class HistoryContent extends ContentObserver{
		public HistoryContent(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			int count = mAdapter.getCount();
			updateUI(count);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_history);
		mContext = this;
		mNotice = new Notice(mContext);

		initTitleVIews();
		initView();
		queryHandler = new QueryHandler(getContentResolver());
		
		HistoryContent historyContent = new HistoryContent(new Handler());
		getContentResolver().registerContentObserver(MetaData.History.CONTENT_URI, true, historyContent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		query();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Cursor cursor = mAdapter.getCursor();
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	public void query() {
		queryHandler.startQuery(11, null, MetaData.History.CONTENT_URI,
				PROJECTION, null, null, MetaData.History.SORT_ORDER_DEFAULT);
	}

	private void initTitleVIews() {
		RelativeLayout titleLayout = (RelativeLayout) findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_tiandi);
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText("传输记录");
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText(getResources().getString(R.string.num_format, 0));
		mRefreshView.setVisibility(View.GONE);
		mHistoryView.setVisibility(View.GONE);
	}

	private void initView() {
		mStorageTV = (TextView) findViewById(R.id.tv_storage);
		String space = getResources().getString(
				R.string.storage_space,
				DreamUtil.getFormatSize(Environment
						.getExternalStorageDirectory().getTotalSpace()),
				DreamUtil.getFormatSize(Environment
						.getExternalStorageDirectory().getFreeSpace()));
		mStorageTV.setText(space);
		mLoadingBar = (ProgressBar) findViewById(R.id.pb_history_loading);
		mHistoryMsgLV = (ListView) findViewById(R.id.lv_history_msg);
		mHistoryMsgLV.setOnScrollListener(this);
		mHistoryMsgLV.setOnItemClickListener(this);

		mAdapter = new HistoryCursorAdapter(mContext);
		mHistoryMsgLV.setAdapter(mAdapter);
		mHistoryMsgLV.setSelection(0);
	}

	// query db
	public class QueryHandler extends AsyncQueryHandler {

		public QueryHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			Log.d(TAG, "onQueryComplete");
			mLoadingBar.setVisibility(View.INVISIBLE);
			int num = 0;
			if (null != cursor) {
				Log.d(TAG, "onQueryComplete.count=" + cursor.getCount());
				mAdapter.swapCursor(cursor);
				num = cursor.getCount();
			}

			updateUI(num);
		}

	}
	
	private void updateUI(int num){
		Message message = mHandler.obtainMessage();
		message.arg1 = num;
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// 注释：firstVisibleItem为第一个可见的Item的position，从0开始，随着拖动会改变
		// visibleItemCount为当前页面总共可见的Item的项数
		// totalItemCount为当前总共已经出现的Item的项数
		// recycleBitmapCaches(0, firstVisibleItem);
		// recycleBitmapCaches(firstVisibleItem + visibleItemCount,
		// totalItemCount);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		mNotice.showToast(position + "" + ",view = " + view);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {

	}
	
	public static void launch(Context context){
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		intent.setClass(context, HistoryActivity.class);
		context.startActivity(intent);
	}
}
