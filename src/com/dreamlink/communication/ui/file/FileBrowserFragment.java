package com.dreamlink.communication.ui.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.MainFragmentActivity;
import com.dreamlink.communication.ui.MenuTabManager;
import com.dreamlink.communication.ui.MenuTabManager.onMenuItemClickListener;
import com.dreamlink.communication.ui.MountManager;
import com.dreamlink.communication.ui.SlowHorizontalScrollView;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.PopupView.PopupViewClickListener;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.common.FileTransferUtil.TransportCallback;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoAdapter.ViewHolder;
import com.dreamlink.communication.ui.file.FileInfoManager.NavigationRecord;
import com.dreamlink.communication.ui.media.ActionMenu;
import com.dreamlink.communication.ui.media.ActionMenu.ActionMenuItem;
import com.dreamlink.communication.util.Log;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FileBrowserFragment extends BaseFragment implements OnClickListener,
		OnItemClickListener, PopupViewClickListener, OnScrollListener,
		OnItemLongClickListener, onMenuItemClickListener {
	private static final String TAG = "FileBrowserFragment";

	// 文件路径导航栏
	private SlowHorizontalScrollView mNavigationBar = null;
	// 显示所有文件
	private ListView mFileListView = null;
	private TextView mListViewTip;
	private ProgressBar mLoadingBar;
	private LinearLayout mNavBarLayout;

	// 快速回到根目录
	private ImageView mHomeBtn;
	
	private TabManager mTabManager;
	private View rootView = null;
	private MountManager mountManager;
	private FileInfo mSelectedFileInfo = null;
	private int mTop = -1;

	private FileInfoAdapter mItemAdapter = null;
	private FileHomeAdapter mHomeAdapter = null;
	private FileInfoManager mFileInfoManager = null;

	// save all files
	private List<FileInfo> mAllLists = new ArrayList<FileInfo>();
	// save folders
	private List<FileInfo> mFolderLists = new ArrayList<FileInfo>();
	// save files
	private List<FileInfo> mFileLists = new ArrayList<FileInfo>();
	private List<Integer> mHomeList = new ArrayList<Integer>();
	
	public static final int INTERNAL = MountManager.INTERNAL;
	public static final int SDCARD = MountManager.SDCARD;
	public static final int DOC = FileInfoManager.TYPE_DOC;
	public static final int EBOOK = FileInfoManager.TYPE_EBOOK;
	public static final int APK = FileInfoManager.TYPE_APK;
	public static final int ARCHIVE = FileInfoManager.TYPE_ARCHIVE;
	private static final int STATUS_FILE = 0;
	private static final int STATUS_HOME = 1;
	private int mStatus = STATUS_HOME;

	private Context mContext;

	private String mCurrentPath;

	// context menu
	// save current sdcard type
	private static int storge_type = -1;
	// save current sdcard type path
	private String mCurrent_root_path;

	private int mAppId = -1;
	private SharedPreferences sp = null;
	
	private String sdcard_path;
	private String internal_path;
	
	private ActionMenu mActionMenu;
	private MenuTabManager mMenuTabManager;
	private LinearLayout mMenuHolder;
	private View mMenuBarView;
	
	private FileDeleteDialog mDeleteDialog;
	
	/**
	 * Create a new instance of FileBrowserFragment, providing "appid" as an
	 * argument.
	 */
	public static FileBrowserFragment newInstance(int appid) {
		FileBrowserFragment f = new FileBrowserFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}
	
	private static final int MSG_UPDATE_UI = 0;
	private static final int MSG_UPDATE_CLASSIFY = 1;
	private static final int MSG_UPDATE_LIST = 2;
	private static final int MSG_UPDATE_HOME = 3;
	private static final int MSG_UPDATE_FILE = 4;
	private static final int MSG_START_LOADING_CLASSIFY = 5;
	private static final int MSG_LOADED_CLASSIFY = 6;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				count = size;
				if (isAdded()) {
					mFragmentActivity.setTitleNum(MainFragmentActivity.FILE_BROWSER, size);
				}
				break;
			case MSG_UPDATE_FILE:
				mItemAdapter.notifyDataSetChanged();
				break;
			case MSG_UPDATE_LIST:
				List<FileInfo> fileList = mItemAdapter.getList();
				fileList.remove(msg.arg1);
				mItemAdapter.notifyDataSetChanged();
				updateUI(fileList.size());
				break;
			case MSG_UPDATE_HOME:
				mHomeAdapter.notifyDataSetChanged();
				break;
			case MSG_START_LOADING_CLASSIFY:
//				new GetFileTask(true).execute(mStatus);
				break;
			case MSG_LOADED_CLASSIFY:
				int type = msg.arg1;
				if (APK == type) {
					Collections.sort(mAllLists);
				}else {
					Collections.sort(mAllLists, DATE_COMPARATOR);
				}
				
				mItemAdapter.setList(mAllLists);
				mItemAdapter.selectAll(false);
				mItemAdapter.notifyDataSetChanged();
				updateUI(mAllLists.size());
				break;
			default:
				break;
			}
		};
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
		Log.d(TAG, "onCreate.mStatus=" + mStatus);
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
		mFragmentActivity.addObject(MainFragmentActivity.FILE_BROWSER, (BaseFragment)this);
		mFragmentActivity.setTitleName(MainFragmentActivity.FILE_BROWSER);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.ui_file, container, false);
		mContext = getActivity();
		
		mFileListView = (ListView) rootView.findViewById(R.id.lv_file);
		mFileListView.setOnItemClickListener(this);
		mFileListView.setOnScrollListener(this);
		mFileListView.setOnItemLongClickListener(this);
		
		mListViewTip = (TextView) rootView.findViewById(R.id.tv_file_listview_tip);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.bar_loading_file);
		mNavBarLayout = (LinearLayout) rootView.findViewById(R.id.navigation_bar);
		mNavigationBar = (SlowHorizontalScrollView) rootView
				.findViewById(R.id.navigation_bar_view);
		if (mNavigationBar != null) {
			mNavigationBar.setVerticalScrollBarEnabled(false);
			mNavigationBar.setHorizontalScrollBarEnabled(false);
			mTabManager = new TabManager();
		}
		mHomeBtn = (ImageView) rootView
				.findViewById(R.id.iv_home);
		mHomeBtn.setOnClickListener(this);

		mMenuHolder = (LinearLayout) rootView.findViewById(R.id.ll_menutabs_holder);
		mMenuBarView = rootView.findViewById(R.id.menubar_bottom);
		mMenuBarView.setVisibility(View.GONE);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		sp = mContext.getSharedPreferences(Extra.SHARED_PERFERENCE_NAME, Context.MODE_PRIVATE);
		
		mFileInfoManager = new FileInfoManager(mContext);
		mountManager = new MountManager(getActivity());

		sdcard_path = sp.getString(Extra.SDCARD_PATH, MountManager.NO_EXTERNAL_SDCARD);
		internal_path = sp.getString(Extra.INTERNAL_PATH, MountManager.NO_INTERNAL_SDCARD);
		Log.d(TAG, "sdcard_path:" + sdcard_path + "\n," + "internal_path:" + internal_path);
		
		mHomeList.clear();
		// init
		if (!MountManager.NO_INTERNAL_SDCARD.equals(internal_path)) {
			mHomeList.add(INTERNAL);
		}
		
		if (!MountManager.NO_EXTERNAL_SDCARD.equals(sdcard_path)) {
			mHomeList.add(SDCARD);
		} 
		
		//为了在onItemClick处理方便，这里将4中type一起加到HomeList中
		mHomeList.add(DOC);
		mHomeList.add(EBOOK);
		mHomeList.add(APK);
		mHomeList.add(ARCHIVE);
		
		mHomeAdapter = new FileHomeAdapter(mContext, mHomeList);
		mItemAdapter = new FileInfoAdapter(mContext, mAllLists);
		
		if (mHomeList.size() <= 0) {
			mNavBarLayout.setVisibility(View.GONE);
			mListViewTip.setVisibility(View.VISIBLE);
			mListViewTip.setText(R.string.no_sdcard);
		}else {
			goToHome();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_home:
			showMenuBar(false);
			goToHome();
			break;
		default:
			mTabManager.updateNavigationBar(v.getId(), storge_type);
			break;
		}
	}
	
	private int restoreSelectedPosition() {
		if (mSelectedFileInfo == null) {
			Log.d(TAG, "restoreSelectedPosition.mSelectedFileInfo is null");
			return -1;
		} else {
			int curSelectedItemPosition = mItemAdapter
					.getPosition(mSelectedFileInfo);
			Log.d(TAG, "restoreSelectedPosition.curSelectedItemPosition=" + curSelectedItemPosition);
			mSelectedFileInfo = null;
			return curSelectedItemPosition;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (STATUS_HOME == mStatus) {
			int type = mHomeList.get(position);
			mNavBarLayout.setVisibility(View.VISIBLE);
			mStatus = STATUS_FILE;
			switch (type) {
			case INTERNAL:
				setAdapter(INTERNAL, mAllLists);
				doInternal();
				break;
			case SDCARD:
				setAdapter(SDCARD, mAllLists);
				doSdcard();
				break;
			default:
				mStatus = type;
				mTabManager.refreshTab(null, type);
				setAdapter(type, mAllLists);
				break;
			}
		}else {
			if (mItemAdapter.isMode(DreamConstant.MENU_MODE_EDIT)) {
				mItemAdapter.setSelected(position);
				mItemAdapter.notifyDataSetChanged();
				
				int selectedCount = mItemAdapter.getSelectedItems();
				updateActionMenuTitle(selectedCount);
				updateMenuBar();
				mMenuTabManager.refreshMenus(mActionMenu);
			}else {
				FileInfo selectedFileInfo = mItemAdapter.getItem(position);
				if (selectedFileInfo.isDir) {
					int top = view.getTop();
					Log.d(TAG, "onItemClick.fromtop:" + top);
					addToNavigationList(mCurrentPath, top, selectedFileInfo);
					browserTo(new File(selectedFileInfo.filePath));
				} else {
					//open file
					mFileInfoManager.openFile(selectedFileInfo.filePath);
				}
			}
		}
	}
	
	private void setAdapter(int type, List<FileInfo> list){
		updateUI(list.size());
		
		mItemAdapter.setList(list);
		mFileListView.setAdapter(mItemAdapter);
		
		if (INTERNAL != type && SDCARD != type) {
			new GetFileTask().execute(type);
		}
	}
	
	/**get sdcard classify files*/
	class GetFileTask extends AsyncTask<Integer, Integer, Object>{
		List<FileInfo> fileList = new ArrayList<FileInfo>();
		String[]  filterType = null;
		int type = -1;
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mLoadingBar.setVisibility(View.VISIBLE);
			mListViewTip.setVisibility(View.VISIBLE);
			mListViewTip.setText("正在加载...");
		}
		
		@Override
		protected Object doInBackground(Integer... params) {
			type = params[0];
			Log.d(TAG, "GetFileTask.doInBackground>type:" + type);
			if (DOC == type) {
				filterType = getResources().getStringArray(R.array.doc_file);
			}else if (EBOOK == type) {
				filterType = getResources().getStringArray(R.array.ebook_file);
			}else if (APK == type) {
				filterType = getResources().getStringArray(R.array.apk_file);
			}else if (ARCHIVE == type) {
				filterType = getResources().getStringArray(R.array.archive_file);
			}else {
				Log.e(TAG, "doInBackground.error.type:" + type);
			}
			File[] files = Environment.getExternalStorageDirectory().getAbsoluteFile().listFiles();
			listFile(files);
			return null;
		}
		
		@Override
		protected void onPostExecute(Object result) {
			super.onPostExecute(result);
			Log.d(TAG, "GetFileTask.onPostExecute");
			mLoadingBar.setVisibility(View.INVISIBLE);
			mListViewTip.setVisibility(View.INVISIBLE);
			
			mAllLists = fileList;
			Message message = mHandler.obtainMessage();
			message.arg1 = type;
			message.what = MSG_LOADED_CLASSIFY;
			message.sendToTarget();
			
			saveSharedPreference();
		}
		
		protected void listFile(final File[] files){
			if (null != files && files.length > 0) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						final int tag = i;
						new Thread(new Runnable() {
							@Override
							public void run() {
								listFile(files[tag].listFiles());
							}
						}).start();
					}else {
						String name = files[i].getName();
						FileInfo fileInfo = null;
						if (isSpeicFile(name)) {
							fileInfo = mFileInfoManager.getFileInfo(files[i]);
							fileList.add(fileInfo);
						}
					}
				}
			}
		}
		
		public boolean isSpeicFile(String name){
			for(String str : filterType){
				if (name.endsWith(str)) {
					return true;
				}
			}
			return false;
		}
		
		private void saveSharedPreference() {
			Editor editor = sp.edit();
			switch (type) {
			case DOC:
				editor.putInt(FileInfoManager.DOC_NUM, mAllLists.size());
				break;
			case EBOOK:
				editor.putInt(FileInfoManager.EBOOK_NUM, mAllLists.size());
				break;
			case APK:
				editor.putInt(FileInfoManager.APK_NUM, mAllLists.size());
				break;
			case ARCHIVE:
				editor.putInt(FileInfoManager.ARCHIVE_NUM, mAllLists.size());
				break;
			default:
				Log.e(TAG, "saveSharedPreference.type=" + type);
				break;
			}
			editor.commit();
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, final View view, final int position,
			long arg3) {
		if (STATUS_HOME == mStatus) {
			return false;
		}
		
		int mode = mItemAdapter.getMode();
		if (DreamConstant.MENU_MODE_EDIT == mode) {
			doSelectAll();
			return true;
		}else {
			mItemAdapter.changeMode(DreamConstant.MENU_MODE_EDIT);
			updateActionMenuTitle(1);
		}
		boolean isSelected = mItemAdapter.isSelected(position);
		mItemAdapter.setSelected(position, !isSelected);
		mItemAdapter.notifyDataSetChanged();
		
		mActionMenu = new ActionMenu(mContext);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_SEND, R.drawable.ic_action_send, R.string.menu_send);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_DELETE,R.drawable.ic_action_delete_enable,R.string.menu_delete);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_INFO,R.drawable.ic_action_info,R.string.menu_info);
//		mActionMenu.addItem(MyMenu.ACTION_MENU_RENAME, R.drawable.ic_action_rename, R.string.menu_rename);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_SELECT, R.drawable.ic_aciton_select, R.string.select_all);
		
		mMenuTabManager = new MenuTabManager(mContext, mMenuHolder);
		showMenuBar(true);
		mMenuTabManager.refreshMenus(mActionMenu);
		mMenuTabManager.setOnMenuItemClickListener(this);
		return true;
	}

	public void browserTo(File file) {
		Log.d(TAG, "browserTo.status=" + mStatus);
		if (file.isDirectory()) {
			mCurrentPath = file.getAbsolutePath();

			clearList();

			fillList(file.listFiles());

			// sort
			Collections.sort(mFolderLists);
			Collections.sort(mFileLists);

			mAllLists.addAll(mFolderLists);
			mAllLists.addAll(mFileLists);
			
			mItemAdapter.notifyDataSetChanged();
			int seletedItemPosition = restoreSelectedPosition();
			Log.d(TAG, "seletedItemPosition:" + seletedItemPosition + ",mTop=" + mTop);
			if (seletedItemPosition == -1) {
				mFileListView.setSelectionAfterHeaderView();
			} else if (seletedItemPosition >= 0
					&& seletedItemPosition < mItemAdapter.getCount()) {
				if (mTop == -1) {
					mFileListView.setSelection(seletedItemPosition);
				} else {
					mFileListView
							.setSelectionFromTop(seletedItemPosition, mTop);
					mTop = -1;
				}
			}
			
			mItemAdapter.selectAll(false);
			updateUI(mAllLists.size());
			mTabManager.refreshTab(mCurrentPath, storge_type);
		} else {
			Log.e(TAG, "It is a file");
		}
	}

	private void clearList() {
		mAllLists.clear();
		mFolderLists.clear();
		mFileLists.clear();
	}

	/**fill current folder's files into list*/
	private void fillList(File[] file) {
		for (File currentFile : file) {
			FileInfo fileInfo = null;

			if (currentFile.isDirectory()) {
				fileInfo = new FileInfo(currentFile.getName());
				fileInfo.fileDate = currentFile.lastModified();
				fileInfo.filePath = currentFile.getAbsolutePath();
				fileInfo.isDir = true;
				fileInfo.fileSize = 0;
				fileInfo.icon = getResources().getDrawable(
						R.drawable.icon_folder);
				fileInfo.type = FileInfoManager.TYPE_DEFAULT;
				if (currentFile.isHidden()) {
					// do nothing
				} else {
					mFolderLists.add(fileInfo);
				}
			} else {
				fileInfo = mFileInfoManager.getFileInfo(currentFile);
				if (currentFile.isHidden()) {
					// do nothing
				} else {
					mFileLists.add(fileInfo);
				}
			}
		}
	}
	
	/**
	 * show rename dialog
	 * @param fileInfo the file info
	 * @param position the click position
	 */
	public void showRenameDialog(final FileInfo fileInfo, final int position){
		LayoutInflater inflater = LayoutInflater.from(mContext);
		View view = inflater.inflate(R.layout.ui_rename_dialog, null);
		final EditText editText = (EditText) view.findViewById(R.id.et_rename);
		editText.setText(fileInfo.fileName);
		editText.selectAll();
		new AlertDialog.Builder(mContext)
			.setTitle("重命名")
			.setView(view)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String newName = editText.getText().toString().trim();
					fileInfo.fileName = newName;
					fileInfo.filePath = mFileInfoManager.rename(new File(fileInfo.filePath), newName);
					mItemAdapter.notifyDataSetChanged();
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create().show();
	}
	
	/**
     * show delete confrim dialog
     */
    public void showDeleteDialog(final List<Integer> posList) {
    	//get name list
    	List<String> nameList = new ArrayList<String>();
    	List<FileInfo> fileList = mItemAdapter.getList();
    	for(int position : posList){
    		nameList.add(fileList.get(position).fileName);
    	}
    	
    	mDeleteDialog = new FileDeleteDialog(mContext,nameList);
    	mDeleteDialog.setButton(AlertDialog.BUTTON_POSITIVE, R.string.menu_delete, new OnDelClickListener() {
			@Override
			public void onClick(View view, String path) {
				showMenuBar(false);
				DeleteTask deleteTask = new DeleteTask(posList);
				deleteTask.execute();
			}
		});
    	mDeleteDialog.setButton(AlertDialog.BUTTON_NEGATIVE, R.string.cancel, null);
		mDeleteDialog.show();
    }
    
    /**
     * Delete file task
     */
    private class DeleteTask extends AsyncTask<Void, String, String>{
    	List<Integer> positionList = new ArrayList<Integer>();
    	
    	DeleteTask(List<Integer> list){
    		positionList = list;
    	}
    	
		@Override
		protected String doInBackground(Void... params) {
			List<FileInfo> fileList = mItemAdapter.getList();
			List<File> deleteList = new ArrayList<File>();
			//get delete path list
			File file = null;
			FileInfo fileInfo = null;
			for (int i = 0; i < positionList.size(); i++) {
				int position = positionList.get(i);
				fileInfo = fileList.get(position);
				file = new File(fileInfo.filePath);
				Log.d(TAG, "doInBackground.pos=" + position + ",path:" + file.getAbsolutePath());
				deleteList.add(file);
			}
			
			for (int i = 0; i < deleteList.size(); i++) {
				mDeleteDialog.setProgress(i + 1, deleteList.get(i).getName());
				doDelete(deleteList.get(i));
				int position = positionList.get(i) - i;		
				Message message = mHandler.obtainMessage();
				message.arg1 = position;
				message.what = MSG_UPDATE_LIST;
				message.sendToTarget();
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
	
	/**
	 * do delete file</br>
	 * if file is media file(image,audio,video),need delete in db
	 * @param path
	 * @param type
	 */
	public void doDelete(File file){
		if (file.isFile()) {
			int type = mFileInfoManager.fileFilter(file.getAbsolutePath());
			Log.d(TAG, "doDelete.type:" + type);
			switch (type) {
			case FileInfoManager.TYPE_IMAGE:
				mFileInfoManager.deleteFileInMediaStore(DreamConstant.IMAGE_URI, file.getAbsolutePath());
				break;
			case FileInfoManager.TYPE_AUDIO:
				mFileInfoManager.deleteFileInMediaStore(DreamConstant.AUDIO_URI, file.getAbsolutePath());
				break;
			case FileInfoManager.TYPE_VIDEO:
				mFileInfoManager.deleteFileInMediaStore(DreamConstant.VIDEO_URI, file.getAbsolutePath());
				break;
			default:
				//普通文件直接删除，不删除数据库，因为在3.0以前，还没有普通文件的数据哭
				file.delete();
				break;
			}
			return;
		}
		
		if (file.isDirectory()) {
			File[] childFiles = file.listFiles();
			if (null == childFiles || 0 == childFiles.length) {
				file.delete();
				return;
			}
			for(File childFile : childFiles){
				doDelete(childFile);
			}
			
			file.delete();
		}
	}
	
	/**
	 * do Tranfer files
	 */
	public void doTransfer(){
		ArrayList<String> checkedList = (ArrayList<String>) mItemAdapter.getSelectedFiles();
		
		//send
		FileTransferUtil fileTransferUtil = new FileTransferUtil(getActivity());
		fileTransferUtil.sendFiles(checkedList, new TransportCallback() {
			
			@Override
			public void onTransportSuccess() {
				int first = mFileListView.getFirstVisiblePosition();
				int last = mFileListView.getLastVisiblePosition();
				List<Integer> checkedItems = mItemAdapter.getCheckedItemIds();
				ArrayList<ImageView> icons = new ArrayList<ImageView>();
				for(int id : checkedItems) {
					if (id >= first && id <= last) {
						View view = mFileListView.getChildAt(id - first);
						if (view != null) {
							ViewHolder viewHolder = (ViewHolder) view.getTag();
							icons.add(viewHolder.iconView);
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
	}

	public void addToNavigationList(String currentPath, int top,
			FileInfo selectFile) {
		mFileInfoManager.addToNavigationList(new NavigationRecord(currentPath,
				top, selectFile));
	}

	/**file path tab manager*/
	protected class TabManager {
		private List<String> mTabNameList = new ArrayList<String>();
		protected LinearLayout mTabsHolder = null;
		private String curFilePath = null;
		private Button mBlankTab;

		public TabManager() {
			mTabsHolder = (LinearLayout) rootView
					.findViewById(R.id.tabs_holder);
			// 添加一个空的button，为了UI更美观
			mBlankTab = new Button(mContext);
			mBlankTab.setBackgroundResource(R.drawable.fm_blank_tab);
			LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
					new ViewGroup.MarginLayoutParams(
							LinearLayout.LayoutParams.MATCH_PARENT,
							LinearLayout.LayoutParams.MATCH_PARENT));

			mlp.setMargins(
					(int) getResources().getDimension(R.dimen.tab_margin_left),
					0,
					(int) getResources().getDimension(R.dimen.tab_margin_right),
					0);
			mBlankTab.setLayoutParams(mlp);
			mTabsHolder.addView(mBlankTab);
		}

		protected void updateHomeButton(int type) {
			Button homeBtn = (Button) mTabsHolder.getChildAt(0);
			if (homeBtn == null) {
				Log.e(TAG, "HomeBtm is null,return.");
				return;
			}
			Resources resources = getResources();
			homeBtn.setBackgroundResource(R.drawable.custom_home_ninepatch_tab);
			homeBtn.setPadding(
					(int) resources.getDimension(R.dimen.home_btn_padding), 0,
					(int) resources.getDimension(R.dimen.home_btn_padding), 0);
			switch (type) {
			case INTERNAL:
				homeBtn.setText(R.string.internal_sdcard);
				break;
			case SDCARD:
				homeBtn.setText(R.string.sdcard);
				break;
			case DOC:
				homeBtn.setText(R.string.doc_type);
				break;
			case EBOOK:
				homeBtn.setText(R.string.ebook_type);
				break;
			case APK:
				homeBtn.setText(R.string.apk_type);
				break;
			case ARCHIVE:
				homeBtn.setText(R.string.archive_type);
				break;
			default:
				break;
			}
		}

		public void refreshTab(String initFileInfo, int type) {
			Log.d(TAG, "refreshTab.initFileInfo:" + initFileInfo);
			int count = mTabsHolder.getChildCount();
			mTabsHolder.removeViews(0, count);
			mTabNameList.clear();

			curFilePath = initFileInfo;
			
			switch (type) {
			case DOC:
			case EBOOK:
			case APK:
			case ARCHIVE:
				addTab("");
				break;
			case INTERNAL:
			case SDCARD:
				if (curFilePath != null) {
					String[] result = mountManager.getShowPath(mCurrent_root_path, curFilePath, type)
							.split(MountManager.SEPERATOR);
					for (String string : result) {
						// add string to tab
						addTab(string);
					}
					startActionBarScroll();
				}
				break;
			}

			updateHomeButton(type);
		}

		private void startActionBarScroll() {
			// scroll to right with slow-slide animation
			// To pass the Launch performance test, avoid the scroll
			// animation when launch.
			int tabHostCount = mTabsHolder.getChildCount();
			int navigationBarCount = mNavigationBar.getChildCount();
			if ((tabHostCount > 2) && (navigationBarCount >= 1)) {
				int width = mNavigationBar.getChildAt(navigationBarCount - 1)
						.getRight();
				mNavigationBar.startHorizontalScroll(
						mNavigationBar.getScrollX(),
						width - mNavigationBar.getScrollX());
			}
		}

		/**
		 * This method updates the navigation view to the previous view when
		 * back button is pressed
		 * 
		 * @param newPath
		 *            the previous showed directory in the navigation history
		 */
		private void showPrevNavigationView(String newPath) {
//			refreshTab(newPath, storge_type);
			browserTo(new File(newPath));
		}

		/**
		 * This method creates tabs on the navigation bar
		 * 
		 * @param text
		 *            the name of the tab
		 */
		protected void addTab(String text) {
			LinearLayout.LayoutParams mlp = null;

			mTabsHolder.removeView(mBlankTab);
			View btn = null;
			if (mTabNameList.isEmpty()) {
				btn = new Button(mContext);
				mlp = new LinearLayout.LayoutParams(
						new ViewGroup.MarginLayoutParams(
								LinearLayout.LayoutParams.WRAP_CONTENT,
								LinearLayout.LayoutParams.MATCH_PARENT));
				mlp.setMargins(0, 0, 0, 0);
				btn.setLayoutParams(mlp);
			} else {
				btn = new Button(mContext);

				((Button) btn).setTextColor(getResources().getColor(
						R.drawable.path_selector2));
				btn.setBackgroundResource(R.drawable.custom_tab);
				if (text.length() <= 10) {
					((Button) btn).setText(text);
				} else {
					String tabItemText = text.substring(0, 10 - 3) + "...";
					((Button) btn).setText(tabItemText);
				}
				mlp = new LinearLayout.LayoutParams(
						new ViewGroup.MarginLayoutParams(
								LinearLayout.LayoutParams.WRAP_CONTENT,
								LinearLayout.LayoutParams.MATCH_PARENT));
				mlp.setMargins(
						(int) getResources().getDimension(
								R.dimen.tab_margin_left), 0, 0, 0);
				btn.setLayoutParams(mlp);
			}
			btn.setOnClickListener(FileBrowserFragment.this);
			btn.setId(mTabNameList.size());
			mTabsHolder.addView(btn);
			mTabNameList.add(text);

			// add blank tab to the tab holder
			mTabsHolder.addView(mBlankTab);
		}

		/**
		 * The method updates the navigation bar
		 * 
		 * @param id
		 *            the tab id that was clicked
		 */
		protected void updateNavigationBar(int id, int type) {
			Log.d(TAG, "updateNavigationBar,id = " + id);
			// click current button do not response
			if (id < mTabNameList.size() - 1) {
				showMenuBar(false);
				int count = mTabNameList.size() - id;
				mTabsHolder.removeViews(id, count);

				for (int i = 1; i < count; i++) {
					// update mTabNameList
					mTabNameList.remove(mTabNameList.size() - 1);
				}
				// mTabsHolder.addView(mBlankTab);

				if (id == 0) {
					curFilePath = mCurrent_root_path;
				} else {
					String[] result = mountManager.getShowPath(mCurrent_root_path, curFilePath,
							type).split(MountManager.SEPERATOR);
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i <= id; i++) {
						sb.append(MountManager.SEPERATOR);
						sb.append(result[i]);
					}
					curFilePath = mCurrent_root_path + sb.toString();
				}

				int top = -1;
				FileInfo selectedFileInfo = null;
				if (mFileListView.getCount() > 0) {
					View view = mFileListView.getChildAt(0);
					selectedFileInfo = mItemAdapter.getItem(mFileListView
							.getPositionForView(view));
					top = view.getTop();
				}
				browserTo(new File(curFilePath));
				addToNavigationList(mCurrentPath, top, selectedFileInfo);
				updateHomeButton(type);
			}else {
				//TODO Refresh current page
				if (STATUS_FILE == mStatus) {
					browserTo(new File(mCurrentPath));
				}else {
					new GetFileTask().execute(mStatus);
				}
			}
		}
		// end tab manager
	}

	@Override
	public void doInternal() {
		storge_type = MountManager.INTERNAL;
		if (MountManager.NO_INTERNAL_SDCARD.equals(internal_path)) {
			// 没有外部&内部sdcard
			return;
		}
		mCurrent_root_path = internal_path;
		browserTo(new File(mCurrent_root_path));
	}

	@Override
	public void doSdcard() {
		storge_type = MountManager.SDCARD;
		mCurrent_root_path = sdcard_path;
		if (mCurrent_root_path == null) {
			Log.e(TAG, "MountManager.SDCARD_PATH = null.");
			return;
		}
		browserTo(new File(mCurrent_root_path));
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (STATUS_HOME == mStatus) {
			return;	
		}
		
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_FLING:
			Log.d(TAG, "SCROLL_STATE_FLING");
			mItemAdapter.setFlag(false);
			break;
		case OnScrollListener.SCROLL_STATE_IDLE:
			Log.d(TAG, "SCROLL_STATE_IDLE");
			mItemAdapter.setFlag(true);
			mItemAdapter.notifyDataSetChanged();
			break;
		case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
			Log.d(TAG, "SCROLL_STATE_TOUCH_SCROLL");
			mItemAdapter.setFlag(false);
			break;
		default:
			break;
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
	}
	
	public void updateUI(int num){
		Message message = mHandler.obtainMessage();
		message.arg1 = num;
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}
	
	public void updateClassifyUI(){
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_CLASSIFY;
		message.sendToTarget();
	}
	
	public void goToHome(){
		Log.d(TAG, "goToHome");
		mAllLists.clear();
		mNavBarLayout.setVisibility(View.GONE);
		
		mStatus = STATUS_HOME;
		updateUI(mHomeList.size());
		mFileListView.setAdapter(mHomeAdapter);
		mHomeAdapter.notifyDataSetChanged();
	}
	
	/**
	 * sort by modify date
	 */
	public static final Comparator<FileInfo> DATE_COMPARATOR = new Comparator<FileInfo>() {
		@Override
		public int compare(FileInfo object1, FileInfo object2) {
			long date1 = object1.fileDate;
			long date2 = object2.fileDate;
			if (date1 > date2) {
				return -1;
			} else if (date1 == date2) {
				return 0;
			} else {
				return 1;
			}
		}
	};
	
	/**
	 * back key callback
	 */
	@Override
	public boolean onBackPressed() {
		Log.d(TAG, "onBackPressed.mStatus=" + mStatus);
		if (mItemAdapter.isMode(DreamConstant.MENU_MODE_EDIT)) {
			showMenuBar(false);
			return false;
		}
		
		switch (mStatus) {
		case STATUS_HOME:
			return true;
		case STATUS_FILE:
			//if is root path,back to Home view
			if (mCurrent_root_path.equals(mCurrentPath)) {
				goToHome();
			}else {
				NavigationRecord navRecord = mFileInfoManager
						.getPrevNavigation();
				String prevPath = null;
				if (null != navRecord) {
					prevPath = navRecord.getRecordPath();
					mSelectedFileInfo = navRecord.getSelectedFile();
					mTop = navRecord.getTop();
					if (null != prevPath) {
						mTabManager.showPrevNavigationView(prevPath);
						Log.d(TAG, "onBackPressed.prevPath=" + prevPath);
					}
				}
			}
			break;
		default:
			goToHome();
			break;
		}
		return false;
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onMenuClick(ActionMenuItem item) {
		switch (item.getItemId()) {
		case ActionMenu.ACTION_MENU_SEND:
			doTransfer();
			showMenuBar(false);
			break;
		case ActionMenu.ACTION_MENU_DELETE:
			List<Integer> posList = mItemAdapter.getCheckedItemIds();
			showDeleteDialog(posList);
			break;
		case ActionMenu.ACTION_MENU_INFO:
			List<FileInfo> list = mItemAdapter.getSelectedList();
			mFileInfoManager.showInfoDialog(list);
			showMenuBar(false);
			break;
		case ActionMenu.ACTION_MENU_SELECT:
			doSelectAll();
			break;
		default:
			break;
		}
	}
	
	/**
	 * set menubar visible or gone
	 * @param show
	 */
	public void showMenuBar(boolean show){
		if (show) {
			mMenuBarView.setVisibility(View.VISIBLE);
		}else {
			mMenuBarView.setVisibility(View.GONE);
			onActionMenuDone();
			updateActionMenuTitle(-1);
		}
	}
	
	/**
	 * update menu bar item icon and text color,enable or disable
	 */
	public void updateMenuBar(){
		int selectCount = mItemAdapter.getSelectedItems();
		updateActionMenuTitle(selectCount);
		
		if (mItemAdapter.getCount() == selectCount) {
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
	
	//Cancle Action menu
	public void onActionMenuDone() {
		mItemAdapter.changeMode(DreamConstant.MENU_MODE_NORMAL);
		mItemAdapter.clearSelected();
		mItemAdapter.notifyDataSetChanged();
	}
	
	/**
	 * do select all items or unselect all items
	 */
	public void doSelectAll(){
		int selectedCount = mItemAdapter.getSelectedItems();
		if (mItemAdapter.getCount() != selectedCount) {
			mItemAdapter.selectAll(true);
		} else {
			mItemAdapter.selectAll(false);
		}
		updateMenuBar();
		mMenuTabManager.refreshMenus(mActionMenu);
		mItemAdapter.notifyDataSetChanged();
	}
	
	/**
	 * update main title 
	 * @param selectCount
	 */
	public void updateActionMenuTitle(int selectCount){
		mFragmentActivity.updateTitleSelectNum(selectCount, count);
	}
	
}
