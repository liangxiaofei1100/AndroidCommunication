package com.dreamlink.communication.ui.image;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.MainFragmentActivity;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.help.HelpActivity;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.ui.settings.SettingsActivity;
import com.dreamlink.communication.util.Log;

import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PictureFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, OnClickListener, OnScrollListener, OnMenuItemClickListener {
	private static final String TAG = "PictureFragment";
	protected GridView mGridview;
	private ProgressBar mLoadingBar;

	// title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private LinearLayout mRefreshLayout;
	private LinearLayout mHistoryLayout;
	private LinearLayout mMenuLayout;
	private LinearLayout mMoreLayout;

	private Context mContext;

	private FileInfoManager mFileInfoManager;
	
	private QueryHandler mQueryHandler = null;
	private PictureCursorAdapter mAdapter = null;

	private int mAppId;
	
	private static final String[] PROJECTION = new String[] {MediaColumns._ID, 
		MediaColumns.DATE_MODIFIED, MediaColumns.SIZE,
		MediaColumns.DATA, MediaColumns.DISPLAY_NAME};
	
	private static final String[] PROJECTION2 = new String[] {MediaColumns._ID, 
		MediaColumns.DATE_MODIFIED, MediaColumns.SIZE,
		MediaColumns.DATA, MediaColumns.DISPLAY_NAME, "width", "height"};
	
	/**order by date_modified DESC*/
	public static final String SORT_ORDER_DATE = MediaColumns.DATE_MODIFIED + " DESC"; 

	/**
	 * Create a new instance of ImageFragment, providing "appid" as an
	 * argument.
	 */
	public static PictureFragment newInstance(int appid) {
		PictureFragment f = new PictureFragment();

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
				Log.i(TAG, "handleMessage");
				int size = msg.arg1;
				if (isAdded()) {
					mTitleNum.setText(getString(R.string.num_format, size));
				}
				break;
			default:
				break;
			}
		};
	};
	
	// video contentObserver listener
	class PictureContent extends ContentObserver {
		public PictureContent(Handler handler) {
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
	}

	private void initTitleVIews(View view) {
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		//title icon
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_image);
		// refresh button
		mRefreshLayout = (LinearLayout) titleLayout.findViewById(R.id.ll_refresh);
		mRefreshLayout.setVisibility(View.GONE);
		// go to history button
		mHistoryLayout = (LinearLayout) titleLayout.findViewById(R.id.ll_history);
		mMenuLayout = (LinearLayout) titleLayout.findViewById(R.id.ll_menu_select);
		mMenuLayout.setOnClickListener(this);
		mRefreshLayout.setOnClickListener(this);
		mHistoryLayout.setOnClickListener(this);
		mMoreLayout = (LinearLayout) titleLayout.findViewById(R.id.ll_more);
		mMoreLayout.setOnClickListener(this);
		// title name
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.image);
		// show current page's item num
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText(getResources().getString(R.string.num_format, 0));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.ui_picture, container, false);
		mGridview = (GridView) rootView.findViewById(R.id.picture_gridview);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.bar_loading_image);
		initTitleVIews(rootView);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();

		mGridview.setOnItemClickListener(this);
		mGridview.setOnItemLongClickListener(this);
		mGridview.setOnScrollListener(this);

		mFileInfoManager = new FileInfoManager(mContext);
		mQueryHandler = new QueryHandler(mContext.getContentResolver());
		query();
		
		mAdapter = new PictureCursorAdapter(mContext);
		mGridview.setAdapter(mAdapter);
		
		PictureContent pictureContent  = new PictureContent(new Handler());
		getActivity().getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, pictureContent);
	}
	
	public void query() {
		String[] projection = PROJECTION;
		if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			projection = PROJECTION2;
		}
		mQueryHandler.startQuery(1, null, DreamConstant.IMAGE_URI,
				projection, null, null, SORT_ORDER_DATE);
	}
	
		// query db
	private class QueryHandler extends AsyncQueryHandler {

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

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_FLING:
			mAdapter.setIdleFlag(false);
			break;
		case OnScrollListener.SCROLL_STATE_IDLE:
			mAdapter.setIdleFlag(true);
			break;
		case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
			mAdapter.setIdleFlag(false);
			break;

		default:
			break;
		}
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		final Cursor cursor = mAdapter.getCursor();
		cursor.moveToPosition(position);
		final String url = cursor.getString(cursor.getColumnIndex(MediaColumns.DATA));
		final String name = cursor.getString(cursor.getColumnIndex(MediaColumns.DISPLAY_NAME));
		
		new AlertDialog.Builder(mContext)
			.setTitle(name)
			.setItems(R.array.picture_menu, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						//open
						startPagerActivityByPosition(position, cursor);
						break;
					case 1:
						//send
						FileTransferUtil fileSendUtil = new FileTransferUtil(getActivity());
						fileSendUtil.sendFile(url);
						break;
					case 2:
						//delete
						showDeleteDialog(position, url);
						break;
					case 3:
						//info
						String info = getImageInfo(cursor);
						DreamUtil.showInfoDialog(mContext, name, info);
						break;

					default:
						break;
					}
				}
			}).create().show();
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Cursor cursor = mAdapter.getCursor();
		startPagerActivityByPosition(position, cursor);
	}
	
	private String getImageInfo(Cursor cursor){
		String result = "";
		String url = cursor.getString(cursor
				.getColumnIndex(MediaStore.MediaColumns.DATA));
		long size = cursor.getLong(cursor.getColumnIndex(MediaColumns.SIZE));
		long date = cursor.getLong(cursor.getColumnIndex(MediaColumns.DATE_MODIFIED));
		long width = 0;
		long height = 0;
		if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			width = cursor.getLong(cursor.getColumnIndex("width"));
			height = cursor.getLong(cursor.getColumnIndex("height"));
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			result = "类型:" + "图片" + DreamConstant.ENTER
					+ "位置:" + DreamUtil.getParentPath(url) + DreamConstant.ENTER
					+ "大小:" + DreamUtil.getFormatSize(size) + DreamConstant.ENTER
					+ "宽度:" +  width + DreamConstant.ENTER
					+ "高度:" + height + DreamConstant.ENTER
					+ "修改日期:" + DreamUtil.getFormatDate(date);
		}else {
			result = "类型:" + "图片" + DreamConstant.ENTER
					+ "位置:" + DreamUtil.getParentPath(url) + DreamConstant.ENTER
					+ "大小:" + size + DreamConstant.ENTER
					+ "修改日期:" + date;
		}
		return result;
	}
	
	private void startPagerActivityByPosition(int position, Cursor cursor){
		List<String> urlList = new ArrayList<String>();
		if (cursor.moveToFirst()) {
			do {
				String url = cursor.getString(cursor
						.getColumnIndex(MediaStore.MediaColumns.DATA));
				urlList.add(url);
			} while (cursor.moveToNext());
		}
		Intent intent = new Intent(mContext, PicturePagerActivity.class);
		intent.putExtra(Extra.IMAGE_POSITION, position);
		intent.putStringArrayListExtra(Extra.IMAGE_INFO, (ArrayList<String>) urlList);
		startActivity(intent);
	}
	
	/**
     * show confrim dialog
     * @param path file path
     */
    public void showDeleteDialog(final int pos, final String path) {
    	//do not use dialogfragment
		final FileDeleteDialog deleteDialog = new FileDeleteDialog(mContext, R.style.TransferDialog, path);
		deleteDialog.setOnClickListener(new OnDelClickListener() {
			@Override
			public void onClick(View view, String path) {
				switch (view.getId()) {
				case R.id.left_button:
					doDelete(pos, path);
					break;
				default:
					break;
				}
			}
		});
		deleteDialog.show();
    }
    
	private void doDelete(int position, String path) {
		boolean ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.IMAGE_URI, path);
		if (!ret) {
			mNotice.showToast(R.string.delete_fail);
			Log.e(TAG, path + " delete failed");
		}else {
			int num = mAdapter.getCount();
			updateUI(num);
		}
	}
	
	public void updateUI(int num){
		Message message = mHandler.obtainMessage();
		message.arg1 = num;
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ll_refresh:
			mAdapter.getCursor().requery();
			break;
			
		case R.id.ll_history:
			Intent intent = new Intent();
			intent.setClass(mContext, HistoryActivity.class);
			startActivity(intent);
			break;
		case R.id.ll_menu_select:
			PopupMenu popupMenu = new PopupMenu(mContext, mMenuLayout);
			popupMenu.setOnMenuItemClickListener(this);
			MenuInflater inflater = popupMenu.getMenuInflater();
			inflater.inflate(R.menu.main_menu_item, popupMenu.getMenu());
			popupMenu.show();
			break;
		case R.id.ll_more:
			PopupMenu popupMenu2 = new PopupMenu(mContext, mMoreLayout);
			popupMenu2.setOnMenuItemClickListener(this);
			MenuInflater inflater2 = popupMenu2.getMenuInflater();
			inflater2.inflate(R.menu.more_menu_item, popupMenu2.getMenu());
			popupMenu2.show();
			break;

		default:
			break;
		}
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.setting:
			intent = new Intent(mContext, SettingsActivity.class);
			startActivity(intent);
			break;
		case R.id.help:
			intent = new Intent(mContext, HelpActivity.class);
			startActivity(intent);
			break;
		default:
			MainFragmentActivity.instance.setCurrentItem(item.getOrder());
			break;
		}
		return true;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
	}
}
