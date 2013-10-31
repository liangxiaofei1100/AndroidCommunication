package com.dreamlink.communication.ui.image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
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
import com.dreamlink.communication.ui.media.ActionMenu;
import com.dreamlink.communication.ui.media.ActionMenu.ActionMenuItem;
import com.dreamlink.communication.util.Log;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.view.LayoutInflater;
import android.view.View;
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

public class PictureFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, OnScrollListener, onMenuItemClickListener {
	private static final String TAG = "PictureFragment";
	protected GridView mItemGridView;
	private GridView mFolderGridView;
	private ProgressBar mLoadingBar;

	private Context mContext;

	private FileInfoManager mFileInfoManager;
	
	private QueryHandler mQueryHandler = null;
	private PictureCursorAdapter mAdapter = null;
	private PictureAdapter mFolderAdapter = null;

	private int mAppId;
	private static final int STATUS_FOLDER = 0;
	private static final int STATUS_ITEM = 1;
	private int mStatus = STATUS_FOLDER;
	private static final String STATUS = "status";
	
	private static final int QUERY_TOKEN_FOLDER = 0x11;
	private static final int QUERY_TOKEN_ITEM = 0x12;
	
	private static final String[] PROJECTION = new String[] {MediaColumns._ID, 
		MediaColumns.DATE_MODIFIED, MediaColumns.SIZE,MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
		MediaColumns.DATA, MediaColumns.DISPLAY_NAME};
	
	private static final String[] PROJECTION2 = new String[] {MediaColumns._ID, 
		MediaColumns.DATE_MODIFIED, MediaColumns.SIZE,MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
		MediaColumns.DATA, MediaColumns.DISPLAY_NAME, "width", "height"};
	
	/**order by date_modified DESC*/
	public static final String SORT_ORDER_DATE = MediaColumns.DATE_MODIFIED + " DESC"; 
	private static final String SORT_ORDER_BUCKET = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " ASC"; 
	private List<PictureFolderInfo> mFolderInfosList = new ArrayList<PictureFolderInfo>();
	
	private static final String CAMERA = "Camera";
	
	private ActionMenu mActionMenu;
	private MenuTabManager mMenuManager;
	
	private View mMenuBottomView;
	private LinearLayout mMenuHolder;
	
	private FileDeleteDialog mDeleteDialog;
	/**
	 * Create a new instance of PictureFragment, providing "w" as an
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
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				count = size;
				if (isAdded()) {
					mFragmentActivity.setTitleNum(MainFragmentActivity.IMAGE, size);
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
			count = count == 0 ? count : count -1;
			Log.i(TAG, "PictureContent.onChange.count=" + count);
			updateUI(count);
			
			queryFolder();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
		if (null != savedInstanceState) {
			mStatus = savedInstanceState.getInt(STATUS);
			Log.d(TAG, "onCreate.savedInstanceState is not null.mStatus=" + mStatus);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		Log.d(TAG, "onSaveInstanceState.status=" + mStatus);
		outState.putInt(STATUS, mStatus);
	}
	
	public void onResume() {
		super.onResume();
		mFragmentActivity.addObject(MainFragmentActivity.IMAGE, (BaseFragment)this);
		mFragmentActivity.setTitleName(MainFragmentActivity.IMAGE);
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.ui_picture, container, false);
		mItemGridView = (GridView) rootView.findViewById(R.id.gv_picture_item);
		mFolderGridView = (GridView) rootView.findViewById(R.id.gv_picture_folder);
		mItemGridView.setVisibility(View.INVISIBLE);
		mFolderGridView.setVisibility(View.VISIBLE);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.bar_loading_image);
		
		mMenuBottomView = rootView.findViewById(R.id.menubar_bottom);
		mMenuBottomView.setVisibility(View.GONE);
		mMenuHolder = (LinearLayout) rootView.findViewById(R.id.ll_menutabs_holder);
		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();

		mItemGridView.setOnItemClickListener(this);
		mItemGridView.setOnItemLongClickListener(this);
		mItemGridView.setOnScrollListener(this);
		
		mFolderGridView.setOnItemClickListener(this);

		mFileInfoManager = new FileInfoManager(mContext);
		mQueryHandler = new QueryHandler(mContext.getContentResolver());
		
		mAdapter = new PictureCursorAdapter(mContext);
		mItemGridView.setAdapter(mAdapter);
		
		mFolderAdapter = new PictureAdapter(mContext, mFolderInfosList,mFolderGridView);
		mFolderGridView.setAdapter(mFolderAdapter);
		
		queryFolder();
//		
		PictureContent pictureContent  = new PictureContent(new Handler());
		getActivity().getContentResolver().registerContentObserver(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, pictureContent);
	}
	
	/***
	 * get {@link PictureFolderInfo} from {@link mFolderInfosList} accord to the speciy bucketDisplayName}}
	 * @param bucketDisplayName
	 * @return {@link PictureFolderInfo}, null if not find
	 */
	public PictureFolderInfo getFolderInfo(String bucketDisplayName){
		for(PictureFolderInfo folderInfo : mFolderInfosList){
			if (bucketDisplayName.equals(folderInfo.getBucketDisplayName())) {
				return folderInfo;
			}
		}
		return null;
	}
	
	public void query(int token, String selection, String[] selectionArgs, String orderBy) {
		String[] projection = PROJECTION;
		if (Build.VERSION.SDK_INT >=  Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			projection = PROJECTION2;
		}
		mQueryHandler.startQuery(token, null, DreamConstant.IMAGE_URI,
				projection, selection, selectionArgs, orderBy);		
	}
	
	/**query all*/
	public void queryFolder(){
		mFolderInfosList.clear();
		query(QUERY_TOKEN_FOLDER, null, null, SORT_ORDER_DATE);
	}
	
	public void queryFolderItem(String bucketName){
		String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "=?";
		String selectionArgs[] = {bucketName};
		query(QUERY_TOKEN_ITEM, selection, selectionArgs, SORT_ORDER_DATE);
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
				switch (token) {
				case QUERY_TOKEN_FOLDER:
					if (cursor.moveToFirst()) {
						PictureFolderInfo pictureFolderInfo = null;
						do {
							long id = cursor.getLong(cursor.getColumnIndex(MediaColumns._ID));
							String bucketDisplayName = cursor.getString(cursor.getColumnIndex(
									MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME));
							pictureFolderInfo = getFolderInfo(bucketDisplayName);
							if (null == pictureFolderInfo) {
								pictureFolderInfo = new PictureFolderInfo();
								pictureFolderInfo.setBucketDisplayName(bucketDisplayName);
								pictureFolderInfo.addIdToList(id);
								if (CAMERA.equals(bucketDisplayName)) {
									mFolderInfosList.add(0, pictureFolderInfo);
								}else {
									mFolderInfosList.add(pictureFolderInfo);
								}
								
							}else {
								pictureFolderInfo.addIdToList(id);
							}
						} while (cursor.moveToNext());
						cursor.close();
						if (STATUS_FOLDER == mStatus) {
							num = mFolderInfosList.size();
							mFolderAdapter.notifyDataSetChanged();
							updateUI(num);
						}
					}
					break;
				case QUERY_TOKEN_ITEM:
					mAdapter.changeCursor(cursor);
					mAdapter.selectAll(false);
					num = cursor.getCount();
					updateUI(num);
					break;
				default:
					Log.e(TAG, "Error token:" + token);
					break;
				}
			}
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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		switch (parent.getId()) {
		case R.id.gv_picture_folder:
			mStatus = STATUS_ITEM;
			String name = mFolderInfosList.get(position).getBucketDisplayName();
			queryFolderItem(name);
			mItemGridView.setVisibility(View.VISIBLE);
			mFolderGridView.setVisibility(View.INVISIBLE);
			break;
		case R.id.gv_picture_item:
			if (mAdapter.getMode() == DreamConstant.MENU_MODE_EDIT) {
				mAdapter.setSelected(position);
				mAdapter.notifyDataSetChanged();
				
				int selectedCount = mAdapter.getSelectedItemsCount();
				updateActionMenuTitle(selectedCount);
				updateMenuBar();
				mMenuManager.refreshMenus(mActionMenu);
			}else {
				Cursor cursor = mAdapter.getCursor();
				startPagerActivityByPosition(position, cursor);
			}
			break;
		}
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
    public void showDeleteDialog() {
    	List<String> deleteNameList = mAdapter.getSelectItemNameList();
    	//do not use dialogfragment
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
		}
    }
    
	private void doDelete(String path) {
		Log.d(TAG, "doDelete.path:" + path);
		boolean ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.IMAGE_URI, path);
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

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub
	}
	
	
	/**
	 * when out of PictureFragment,make the view to folder view
	 */
	public void scrollToHomeView() {
		Log.d(TAG, "scrollToHomeView");
		if (mStatus == STATUS_ITEM) {
			mStatus = STATUS_FOLDER;
			mAdapter.changeCursor(null);
			mFolderAdapter.notifyDataSetChanged();
			mItemGridView.setVisibility(View.INVISIBLE);
			mFolderGridView.setVisibility(View.VISIBLE);
		}
	}
	
	public boolean onBackPressed(){
		Log.d(TAG, "onBackPressed.status="+ mStatus);
		if (mAdapter.getMode() == DreamConstant.MENU_MODE_EDIT) {
			showMenuBar(false);
			return false;
		}
		
		switch (mStatus) {
		case STATUS_FOLDER:
			return true;
		case STATUS_ITEM:
			mStatus = STATUS_FOLDER;
			mAdapter.changeCursor(null);
			
			mFolderAdapter.notifyDataSetChanged();
			updateUI(mFolderInfosList.size());
			mItemGridView.setVisibility(View.INVISIBLE);
			mFolderGridView.setVisibility(View.VISIBLE);
			break;
		}
		return false;
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
					int first = mItemGridView.getFirstVisiblePosition();
					int last = mItemGridView.getLastVisiblePosition();
					List<Integer> checkedItems = mAdapter.getSelectedItemPos();
					ArrayList<ImageView> icons = new ArrayList<ImageView>();
					for(int id : checkedItems) {
						if (id >= first && id <= last) {
							View view = mItemGridView.getChildAt(id - first);
							if (view != null) {
								PictureGridItem item = (PictureGridItem) view;
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
						.getColumnIndex(MediaStore.Images.Media.SIZE)); // 文件大小
				String url = cursor.getString(cursor
						.getColumnIndex(MediaStore.Images.Media.DATA)); // 文件路径
				String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
				long date = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED));
				
				dialog.updateUI(size, 0, 0);
				dialog.updateUI(name, url, date);
			}else {
				dialog = new FileInfoDialog(mContext,FileInfoDialog.MULTI);
				int fileNum = list.size();
				long size = getTotalSize(list);
				dialog.updateUI(size, fileNum, 0);
			}
			dialog.show();
			
			showMenuBar(false);
			break;
		case ActionMenu.ACTION_MENU_SELECT:
			doSelectAll();
			break;

		default:
			break;
		}
	}
	
	public long getTotalSize(List<Integer> list){
		long totalSize = 0;
		Cursor cursor = mAdapter.getCursor();
		for(int pos : list){
			cursor.moveToPosition(pos);
			long size = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Images.Media.SIZE)); // 文件大小
			totalSize += size;
		}
		
		return totalSize;
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
	
	
	public void onActionMenuDone() {
		mAdapter.changeMode(DreamConstant.MENU_MODE_NORMAL);
		mAdapter.selectAll(false);
		mAdapter.notifyDataSetChanged();
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
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_SEND).setTextColor(getResources().getColor(R.color.disable_color));
        	
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_DELETE).setEnable(false);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_DELETE).setTextColor(getResources().getColor(R.color.disable_color));
        	
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_INFO).setEnable(false);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_INFO).setTextColor(getResources().getColor(R.color.disable_color));
		}else {
			mActionMenu.findItem(ActionMenu.ACTION_MENU_SEND).setEnable(true);
			mActionMenu.findItem(ActionMenu.ACTION_MENU_SEND).setTextColor(getResources().getColor(R.color.black));
			
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_DELETE).setEnable(true);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_DELETE).setTextColor(getResources().getColor(R.color.black));
        	
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_INFO).setEnable(true);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_INFO).setTextColor(getResources().getColor(R.color.black));
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
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d(TAG, "onDestroy");
	}
}
