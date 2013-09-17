package com.dreamlink.communication.ui.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.util.Log;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
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

public class HistoryActivity extends FragmentActivity implements OnScrollListener, OnItemClickListener {
	private static final String TAG = "HistoryActivityTest";
	private Context mContext;
	
	//view
	private TextView mStorageTV;
	private ListView mHistoryMsgLV;
	private ProgressBar mLoadingBar;
	
	//adapter
	private HistoryCursorAdapter mAdapter;
	
	/**the list that send or receive history info*/
	private List<HistoryInfo> mHistoryList = new ArrayList<HistoryInfo>();
	// 用来保存ListView中每个Item的图片，以便释放
	public static Map<String, Bitmap> bitmapCaches = new HashMap<String, Bitmap>();
	
	private Notice mNotice;
	private SocketCommunicationManager communicationManager;
	
	private FileInfoManager mFileInfoManager = null;
	private UserManager mUserManager = null;
	
	//msg
	private static final int MSG_SEND_FILE = 0;
	private static final int MSG_UPDATE_UI = 1;
	private static final int MSG_UPDATE_SEND_PROGRESS = 2;
	private static final int MSG_UPDATE_SEND_STATUS = 3;
	private static final int MSG_UPDATE_RECEIVE_STATUS = 4;
	private static final int MSG_UPDATE_RECEIVE_PROGRESS = 5;
	private static final int MSG_SEND_FINISHED = 6;
	private static final int MSG_RECEIVE_FINISHED = 7;
	
	private QueryHandler queryHandler = null;
	private HistoryManager mHistoryManager = null;
	
	//title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	// title views
	
	 Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEND_FILE:
				break;
			case MSG_UPDATE_UI:
				int num = msg.arg1;
				mTitleNum.setText(getResources().getString(R.string.num_format, num));
				break;
				
			case MSG_UPDATE_SEND_PROGRESS:
				Log.d(TAG, "MSG_UPDATE_SEND_PROGRESS");
				HistoryInfo historyInfo2 = (HistoryInfo) msg.obj;
				Collections.sort(mHistoryList, HistoryManager.DATE_COMPARATOR);
				int position2 = mHistoryList.indexOf(historyInfo2);
				Log.i(TAG, "position2=" + position2);
				mHistoryMsgLV.setSelection(position2);
				mAdapter.notifyDataSetChanged();
				break;
				
			case MSG_UPDATE_RECEIVE_PROGRESS:
				HistoryInfo historyInfo3 = (HistoryInfo) msg.obj;
				Collections.sort(mHistoryList, HistoryManager.DATE_COMPARATOR);
				int positior3 = mHistoryList.indexOf(historyInfo3);
				Log.i(TAG, "positior3=" + positior3);
				mHistoryMsgLV.setSelection(positior3);
				mAdapter.notifyDataSetChanged();
				break;
				
			case MSG_UPDATE_SEND_STATUS:
			case MSG_UPDATE_RECEIVE_STATUS:
				int status = msg.arg1;
				mAdapter.setStatus(status);
				mAdapter.notifyDataSetChanged();
				
				//tell mainui update ui
				break;
			case MSG_SEND_FINISHED:
				boolean success = (Boolean) msg.obj;
				break;
			case MSG_RECEIVE_FINISHED:
				break;

			default:
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_history);
		mContext = this;
		mNotice = new Notice(mContext);
		
		mFileInfoManager = new FileInfoManager(mContext);
		
		queryHandler = new QueryHandler(getContentResolver());
		mHistoryManager = new HistoryManager(mContext);
		mUserManager = UserManager.getInstance();
		
		initTitleVIews();
		initView();
	}
	
	private static final String[] PROJECTION = {
		MetaData.History._ID,MetaData.History.FILE_PATH,MetaData.History.FILE_NAME,
		MetaData.History.FILE_SIZE,MetaData.History.SEND_USERNAME,MetaData.History.RECEIVE_USERNAME,
		MetaData.History.PROGRESS,MetaData.History.DATE,
		MetaData.History.STATUS,MetaData.History.MSG_TYPE,MetaData.History.FILE_TYPE
	};
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		query();
	}
	
	public void query(){
		mLoadingBar.setVisibility(View.VISIBLE);
		queryHandler.startQuery(11, null, MetaData.History.CONTENT_URI, PROJECTION, null, null, MetaData.History.SORT_ORDER_DEFAULT);
	}
	
	private void initTitleVIews(){
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
	
	private void initView(){
		mStorageTV = (TextView) findViewById(R.id.tv_storage);
		String space = getResources().getString(R.string.storage_space, 
				DreamUtil.getFormatSize(Environment.getExternalStorageDirectory().getTotalSpace()),
				DreamUtil.getFormatSize(Environment.getExternalStorageDirectory().getFreeSpace()));
		mStorageTV.setText(space);
		mLoadingBar = (ProgressBar) findViewById(R.id.pb_history_loading);
		mHistoryMsgLV = (ListView) findViewById(R.id.lv_history_msg);
		mHistoryMsgLV.setOnScrollListener(this);
		mHistoryMsgLV.setOnItemClickListener(this);
		
		mHistoryMsgLV.setAdapter(mAdapter);
		mHistoryMsgLV.setSelection(0);
	}
	
	//query db
	public class QueryHandler extends AsyncQueryHandler{

		public QueryHandler(ContentResolver cr) {
			super(cr);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			// TODO Auto-generated method stub
//			super.onQueryComplete(token, cookie, cursor);
			Log.d(TAG, "onQueryComplete");
			mLoadingBar.setVisibility(View.INVISIBLE);
			Message message = mHandler.obtainMessage();
			if (null  != cursor && cursor.getCount() > 0) {
				Log.d(TAG, "onQueryComplete.count=" + cursor.getCount());
				mAdapter = new HistoryCursorAdapter(mContext, mHistoryList);
				mAdapter.changeCursor(cursor);
				mHistoryMsgLV.setAdapter(mAdapter);
				message.arg1 = cursor.getCount();
			}else {
				message.arg1 = 0;
			}
			
			message.what = MSG_UPDATE_UI;
			message.sendToTarget();
		}
		
	}


	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// 注释：firstVisibleItem为第一个可见的Item的position，从0开始，随着拖动会改变
		// visibleItemCount为当前页面总共可见的Item的项数
		// totalItemCount为当前总共已经出现的Item的项数
//		recycleBitmapCaches(0, firstVisibleItem);
//		recycleBitmapCaches(firstVisibleItem + visibleItemCount, totalItemCount);
	}

	// release bitmap
	private void recycleBitmapCaches(int fromPosition, int toPosition) {
		Bitmap delBitmap = null;
		for (int del = fromPosition; del < toPosition; del++) {
			delBitmap = bitmapCaches.get(mHistoryList.get(del));
			if (delBitmap != null) {
				// 如果非空则表示有缓存的bitmap，需要清理
				Log.d(TAG, "release position:" + del);
				// 从缓存中移除该del->bitmap的映射
				bitmapCaches.remove(mHistoryList.get(del));
				delBitmap.recycle();
				delBitmap = null;
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		mNotice.showToast(position+"");
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// TODO Auto-generated method stub
		
	}
}
