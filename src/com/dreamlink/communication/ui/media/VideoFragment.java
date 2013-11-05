package com.dreamlink.communication.ui.media;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.MenuTabManager;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.MainFragmentActivity;
import com.dreamlink.communication.ui.MenuTabManager.onMenuItemClickListener;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.common.FileTransferUtil.TransportCallback;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileInfoDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.media.ActionMenu.ActionMenuItem;
import com.dreamlink.communication.ui.media.VideoCursorAdapter.ViewHolder;
import com.dreamlink.communication.util.Log;

public class VideoFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, onMenuItemClickListener, OnScrollListener {
	private static final String TAG = "VideoFragment";
	private GridView mGridView;
	private ProgressBar mLoadingBar;
	
	private VideoCursorAdapter mAdapter;
	private QueryHandler mQueryHandler = null;
	
	private MenuTabManager mMenuManager;
	private View mMenuBottomView;
	private LinearLayout mMenuHolder;
	private ActionMenu mActionMenu;
	
	private FileDeleteDialog mDeleteDialog;
	
	private FileInfoManager mFileInfoManager;
	private Context mContext;
	
	private int mAppId = -1;
	
	private static final String[] PROJECTION = new String[] {MediaStore.Video.Media._ID, 
		MediaStore.Video.Media.DURATION, MediaStore.Video.Media.SIZE,
		MediaColumns.DATA, MediaStore.Video.Media.DISPLAY_NAME};
		
	//video contentObserver listener
	class VideoContent extends ContentObserver{
		public VideoContent(Handler handler) {
			super(handler);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			int count = mAdapter.getCount();
			updateUI(count);
		}
	}
	
	private static final int MSG_UPDATE_UI = 0;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				count = size;
				if (isAdded()) {
					mFragmentActivity.setTitleNum(MainFragmentActivity.VIDEO, size);
				}
				break;
			default:
				break;
			}
		};
	};
	
	/**
	 * Create a new instance of AppFragment, providing "appid" as an
	 * argument.
	 */
	public static VideoFragment newInstance(int appid) {
		VideoFragment f = new VideoFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		mFragmentActivity.addObject(MainFragmentActivity.VIDEO, (BaseFragment)this);
		mFragmentActivity.setTitleName(MainFragmentActivity.VIDEO);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mContext = getActivity();
		View rootView = inflater.inflate(R.layout.ui_media_video, container, false);
		mGridView = (GridView) rootView.findViewById(R.id.video_gridview);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
		mGridView.setOnScrollListener(this);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.bar_video_loading);
		mAdapter = new VideoCursorAdapter(mContext);
		mGridView.setAdapter(mAdapter);
		
		mMenuBottomView = rootView.findViewById(R.id.menubar_bottom);
		mMenuBottomView.setVisibility(View.GONE);
		mMenuHolder = (LinearLayout) rootView.findViewById(R.id.ll_menutabs_holder);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mFileInfoManager = new FileInfoManager(mContext);
		mQueryHandler = new QueryHandler(getActivity().getContentResolver());
				
		query();
		VideoContent videoContent  = new VideoContent(new Handler());
		getActivity().getContentResolver().registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videoContent);
	}
	
	public void query() {
		mQueryHandler.startQuery(0, null, DreamConstant.VIDEO_URI,
				PROJECTION, null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
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
				mAdapter.selectAll(false);
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
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int mode = mAdapter.getMode();
		if (DreamConstant.MENU_MODE_NORMAL == mode) {
			Cursor cursor = mAdapter.getCursor();
			cursor.moveToPosition(position);
			String url = cursor.getString(cursor
					.getColumnIndex(MediaStore.Video.Media.DATA)); // 文件路径
			mFileInfoManager.openFile(url);
		}else {
			mAdapter.setSelected(position);
			mAdapter.notifyDataSetChanged();
			
			int selectedCount = mAdapter.getSelectedItemsCount();
			updateActionMenuTitle(selectedCount);
			updateMenuBar();
			mMenuManager.refreshMenus(mActionMenu);
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, final View view, final int position, long id) {
		int mode = mAdapter.getMode();
		if (DreamConstant.MENU_MODE_EDIT == mode) {
			doSelectAll();
			return true;
		}else {
			mAdapter.changeMode(DreamConstant.MENU_MODE_EDIT);
			updateActionMenuTitle(1);
		}
		boolean isSelected = mAdapter.isSelected(position);
		mAdapter.setSelected(position, !isSelected);
		mAdapter.notifyDataSetChanged();
		
		mActionMenu = new ActionMenu(mContext);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_SEND, R.drawable.ic_action_send, R.string.menu_send);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_DELETE,R.drawable.ic_action_delete_enable,R.string.menu_delete);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_INFO,R.drawable.ic_action_info,R.string.menu_info);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_SELECT, R.drawable.ic_aciton_select, R.string.select_all);
		
		mMenuManager = new MenuTabManager(mContext, mMenuHolder);
		showMenuBar(true);
		mMenuManager.refreshMenus(mActionMenu);
		mMenuManager.setOnMenuItemClickListener(this);
		return true;
	}
	
	/**
     * show confrim dialog
     * @param path file path
     */
    public void showDeleteDialog() {
    	List<String> deleteNameList = mAdapter.getSelectItemNameList();
    	mDeleteDialog = new FileDeleteDialog(mContext, deleteNameList);
    	mDeleteDialog.setOnClickListener(new OnDelClickListener() {
			@Override
			public void onClick(View view, String path) {
				switch (view.getId()) {
				case R.id.left_button:
					List<String> deleteList = mAdapter.getSelectItemList();
					DeleteTask deleteTask = new DeleteTask(deleteList);
					deleteTask.execute();
					showMenuBar(false);
					break;
				default:
					break;
				}
			}
		});
    	mDeleteDialog.show();
    }
    
    /**
     * Delete file task
     */
    private class DeleteTask extends AsyncTask<Void, String, String>{
    	private List<String> deleteList = new ArrayList<String>();
    	
    	DeleteTask(List<String> list){
    		deleteList = list;
    	}
    	
		@Override
		protected String doInBackground(Void... params) {
			Log.d(TAG, "doInBackground.size=" + deleteList.size());
			//start delete file from delete list
			for (int i = 0; i < deleteList.size(); i++) {
				File file = new File(deleteList.get(i));
				mDeleteDialog.setProgress(i + 1, file.getName());
				doDelete(deleteList.get(i));
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if (null != mDeleteDialog) {
				mDeleteDialog.cancel();
				mDeleteDialog = null;
			}
			mNotice.showToast("操作完成");
		}
    }
    
    private void doDelete(String path) {
		boolean ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.VIDEO_URI, path);
		if (!ret) {
			mNotice.showToast(R.string.delete_fail);
			Log.e(TAG, path + " delete failed");
		}
	}
    
    public void updateUI(int num){
		Message message = mHandler.obtainMessage();
		message.arg1 = num;
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}
    
    public long getTotalSize(List<Integer> list){
		long totalSize = 0;
		Cursor cursor = mAdapter.getCursor();
		for(int pos : list){
			cursor.moveToPosition(pos);
			long size = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Video.Media.SIZE)); // 文件大小
			totalSize += size;
		}
		
		return totalSize;
	}

    public void onActionMenuDone() {
		mAdapter.changeMode(DreamConstant.MENU_MODE_NORMAL);
		mAdapter.selectAll(false);
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onBackPressed() {
		int mode = mAdapter.getMode();
		Log.d(TAG, "onBackPressed.mode="+ mode);
		if (DreamConstant.MENU_MODE_EDIT == mode) {
			showMenuBar(false);
			return false;
		}else {
			return true;
		}
	}

	@Override
	public void onMenuClick(ActionMenuItem item) {
		switch (item.getItemId()) {
		case ActionMenu.ACTION_MENU_SEND:
			ArrayList<String> selectedList = (ArrayList<String>) mAdapter.getSelectItemList();
			//send
			FileTransferUtil fileTransferUtil = new FileTransferUtil(getActivity());
			fileTransferUtil.sendFiles(selectedList, new TransportCallback() {
				@Override
				public void onTransportSuccess() {
					int first = mGridView.getFirstVisiblePosition();
					int last = mGridView.getLastVisiblePosition();
					List<Integer> checkedItems = mAdapter.getSelectedItemPos();
					ArrayList<ImageView> icons = new ArrayList<ImageView>();
					for(int id : checkedItems) {
						if (id >= first && id <= last) {
							View view = mGridView.getChildAt(id - first);
							if (view != null) {
								VideoGridItem item = (VideoGridItem) view;
								icons.add(item.mIconView);
							}
						}
					}
					
					if (icons.size() > 0) {
						ImageView[] imageViews = new ImageView[0];
						showTransportAnimation(icons.toArray(imageViews));
					}
				}
				
				@Override
				public void onTransportFail() {
				}
			});
			showMenuBar(false);
			break;
		case ActionMenu.ACTION_MENU_DELETE:
			showDeleteDialog();
			break;
		case ActionMenu.ACTION_MENU_INFO:
			List<Integer> list = mAdapter.getSelectedItemPos();
			FileInfoDialog dialog = null;
			if (1 == list.size()) {
				dialog = new FileInfoDialog(mContext,FileInfoDialog.SINGLE_FILE);
				Cursor cursor = mAdapter.getCursor();
				cursor.moveToPosition(list.get(0));
				
				long size = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Video.Media.SIZE)); // 文件大小
				String url = cursor.getString(cursor
						.getColumnIndex(MediaStore.Video.Media.DATA)); // 文件路径
				String name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
//				long date = cursor.getLong(cursor
//						.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED));
//				Log.d(TAG, "date:" + date);
				dialog.updateUI(size, 0, 0);
				dialog.updateUI(name, url, 0);
			}else {
				dialog = new FileInfoDialog(mContext,FileInfoDialog.MULTI);
				int fileNum = list.size();
				long size = getTotalSize(list);
				dialog.updateUI(size, fileNum, 0);
			}
			dialog.show();
			
			showMenuBar(false);
			//info
			break;
		case ActionMenu.ACTION_MENU_SELECT:
			doSelectAll();
			break;

		default:
			break;
		}
	}
	
	/**
	 * do select all items or unselect all items
	 */
	public void doSelectAll(){
		int selectedCount1 = mAdapter.getSelectedItemsCount();
		if (mAdapter.getCount() != selectedCount1) {
			mAdapter.selectAll(true);
		} else {
			mAdapter.selectAll(false);
		}
		updateMenuBar();
		mMenuManager.refreshMenus(mActionMenu);
		mAdapter.notifyDataSetChanged();
	}
	
	/**
	 * set menubar visible or gone
	 * @param show
	 */
	public void showMenuBar(boolean show){
		if (show) {
			mMenuBottomView.setVisibility(View.VISIBLE);
		}else {
			mMenuBottomView.setVisibility(View.GONE);
			updateActionMenuTitle(-1);
			onActionMenuDone();
		}
	}
	
	/**
	 * update menu bar item icon and text color,enable or disable
	 */
	public void updateMenuBar(){
		int selectCount = mAdapter.getSelectedItemsCount();
		updateActionMenuTitle(selectCount);
		
		if (mAdapter.getCount() == selectCount) {
			mActionMenu.findItem(ActionMenu.ACTION_MENU_SELECT).setTitle(R.string.unselect_all);
		}else {
			mActionMenu.findItem(ActionMenu.ACTION_MENU_SELECT).setTitle(R.string.select_all);
		}
		
		if (0==selectCount) {
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_SEND).setEnable(false);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_DELETE).setEnable(false);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_INFO).setEnable(false);
		}else {
			mActionMenu.findItem(ActionMenu.ACTION_MENU_SEND).setEnable(true);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_DELETE).setEnable(true);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_INFO).setEnable(true);
		}
	}
	
	/**
	 * update main title 
	 * @param selectCount
	 */
	public void updateActionMenuTitle(int selectCount){
		mFragmentActivity.updateTitleSelectNum(selectCount, count);
	}
	
	@Override
	public int getSelectedCount() {
		return mAdapter.getSelectedItemsCount();
	}
	
	@Override
	public int getMenuMode() {
		return mAdapter.getMode();
	}

	@Override
	public void onDestroyView() {
		if (mAdapter != null) {
			Cursor cursor = mAdapter.getCursor();
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		super.onDestroyView();
	}
	
}
