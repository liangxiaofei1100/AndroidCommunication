package com.dreamlink.communication.ui.file;

import java.io.File;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
import com.dreamlink.communication.util.LogFile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.TextView;

public class FileBrowserFragment extends BaseFragment implements OnClickListener,
		OnItemClickListener, PopupViewClickListener, OnScrollListener,
		OnItemLongClickListener, onMenuItemClickListener {
	private static final String TAG = "FileBrowserFragment";

	// 文件路径导航栏
	private SlowHorizontalScrollView mNavigationBar = null;
	// 显示所有文件
	private ListView mFileListView = null;
	private TextView mNoSDcardView;
	private LinearLayout mNavBarLayout;

	// 快速回到根目录
	private ImageView mHomeBtn;
	
	private TabManager mTabManager;
	private View rootView = null;
	private MountManager mountManager;

	private FileInfoAdapter mFileInfoAdapter = null;
	private FileInfoManager mFileInfoManager = null;

	// save all files
	private List<FileInfo> mAllLists = new ArrayList<FileInfo>();
	// save folders
	private List<FileInfo> mFolderLists = new ArrayList<FileInfo>();
	// save files
	private List<FileInfo> mFileLists = new ArrayList<FileInfo>();
	private List<Integer> mHomeList = new ArrayList<Integer>();
	//save classify file list
	//办公文档
	private List<FileInfo> mDocList = new ArrayList<FileInfo>();
	//电子书籍
	private List<FileInfo> mEbookList = new ArrayList<FileInfo>();
	//安装包
	private List<FileInfo> mApkList = new ArrayList<FileInfo>();
	//压缩包
	private List<FileInfo> mArchiveList = new ArrayList<FileInfo>();
	private List<List<FileInfo>> mCLassifyList = new ArrayList<List<FileInfo>>();
	
	private GetFilesTask mGetFileTask = null;
	private String[] fileNameTypes = null;
	
	public static String[] file_types;
	public static String[] file_types_tips;
	public static final int INTERNAL = MountManager.INTERNAL;
	public static final int SDCARD = MountManager.SDCARD;
	public static final int DOC = FileInfoManager.TYPE_DOC;
	public static final int EBOOK = FileInfoManager.TYPE_EBOOK;
	public static final int APK = FileInfoManager.TYPE_APK;
	public static final int ARCHIVE = FileInfoManager.TYPE_ARCHIVE;
	
	/**
	 * save num in sharedPrefernce
	 */
	//work document
	private static final String DOC_NUM = "doc_num";
	//ebook file
	private static final String EBOOK_NUM = "ebook_num";
	//app install package
	private static final String APP_NUM = "app_num";
	//archive file
	private static final String ARCHIVE_NUM = "archive_num";
	
	private Timer mClassifyTimer;

	private Context mContext;

	private File mCurrentFile = null;
	private String mCurrentPath;

	// context menu
	private int mCurrentPosition = -1;
	// save current sdcard type
	private static int storge_type = -1;
	// save current sdcard type path
	private String mCurrent_root_path;

	private int mAppId = -1;
	private SharedPreferences sp = null;
	
	//two status
	private static final int STATUS_FILE = 0;
	private static final int STATUS_HOME = 1;
	private static final int STATUS_DOC = 2;
	private static final int STATUS_EBOOK =3;
	private static final int STATUS_APK = 4;
	private static final int STATUS_ARCHIVE = 5;
	private int mStatus = STATUS_HOME;
	
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
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				count = size;
				if (isAdded()) {
					mFragmentActivity.setTitleNum(MainFragmentActivity.FILE_BROWSER, size);
				}
				break;
			case MSG_UPDATE_CLASSIFY:
				mFileInfoAdapter.notifyDataSetChanged();
				break;
			case MSG_UPDATE_LIST:
				List<FileInfo> fileList = mFileInfoAdapter.getList();
				fileList.remove(msg.arg1);
				mFileInfoAdapter.notifyDataSetChanged();
				
				updateUI(fileList.size());
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
		mNoSDcardView = (TextView) rootView.findViewById(R.id.tv_no_sdcard);
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
		
		file_types = getResources().getStringArray(R.array.file_classify);
		file_types_tips = getResources().getStringArray(R.array.file_classify_tip);
		fileNameTypes = getResources().getStringArray(R.array.classify_files_ending);

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
		
		if (mHomeList.size() <= 0) {
			mNavBarLayout.setVisibility(View.GONE);
			mNoSDcardView.setVisibility(View.VISIBLE);
		}else {
			goToHome();
		}
		
		if (null != mGetFileTask && mGetFileTask.getStatus() == AsyncTask.Status.RUNNING) {
			mGetFileTask.cancel(true);
		}else {
			mGetFileTask = new GetFilesTask();
			mGetFileTask.execute(0);
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
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (mFileInfoAdapter.isHome) {
			int type = mHomeList.get(position);
			//when get files,the item cannot click,fix the update ui error bug
			if (INTERNAL != type && SDCARD != type) {
				if (null != mGetFileTask && mGetFileTask.getStatus() == AsyncTask.Status.RUNNING) {
					return;
				}
			}
			mNavBarLayout.setVisibility(View.VISIBLE);
			mStatus = STATUS_FILE;
			switch (type) {
			case INTERNAL:
				stopUpdateClassifyUI();
				setAdapter(INTERNAL, mAllLists);
				doInternal();
				break;
			case SDCARD:
				stopUpdateClassifyUI();
				setAdapter(SDCARD, mAllLists);
				doSdcard();
				break;
			case DOC:
				//get doc documents
				mStatus = STATUS_DOC;
				mTabManager.refreshTab(null, DOC);
				setAdapter(DOC, mDocList);
				break;
			case EBOOK:
				mStatus = STATUS_EBOOK;
				mTabManager.refreshTab(null, EBOOK);
				setAdapter(EBOOK, mEbookList);
				break;
			case APK:
				mStatus = STATUS_APK;
				mTabManager.refreshTab(null, APK);
				setAdapter(APK, mApkList);
				break;
			case ARCHIVE:
				mStatus = STATUS_ARCHIVE;
				mTabManager.refreshTab(null, ARCHIVE);
				setAdapter(ARCHIVE, mArchiveList);
				break;
			default:
				break;
			}
		}else {
			if (mFileInfoAdapter.isMode(DreamConstant.MENU_MODE_EDIT)) {
				mFileInfoAdapter.setChecked(position);
				mFileInfoAdapter.notifyDataSetChanged();
				
				int selectedCount = mFileInfoAdapter.getCheckedItems();
				updateActionMenuTitle(selectedCount);
				updateMenuBar();
				mMenuTabManager.refreshMenus(mActionMenu);
			}else {
				List<FileInfo> list = mFileInfoAdapter.getList();
				FileInfo fileInfo = list.get(position);
				int top = view.getTop();
				if (fileInfo.isDir) {
					addToNavigationList(mCurrentPath, top, fileInfo);
					browserTo(new File(list.get(position).filePath));
				} else {
					//open file
					mFileInfoManager.openFile(fileInfo.filePath);
				}
			}
		}
	}
	
	private void setAdapter(int type, List<FileInfo> list){
		updateUI(list.size());
		
		mFileInfoAdapter = new FileInfoAdapter(mContext, list);
		mFileListView.setAdapter(mFileInfoAdapter);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, final View view, final int position,
			long arg3) {
		mCurrentPosition = position;
		if (mFileInfoAdapter.isHome) {
			return false;
		}
		
		int mode = mFileInfoAdapter.getMode();
		if (DreamConstant.MENU_MODE_EDIT == mode) {
			doSelectAll();
			return true;
		}else {
			mFileInfoAdapter.changeMode(DreamConstant.MENU_MODE_EDIT);
			updateActionMenuTitle(1);
		}
		boolean isSelected = mFileInfoAdapter.isChecked(position);
		mFileInfoAdapter.setChecked(position, !isSelected);
		mFileInfoAdapter.notifyDataSetChanged();
		
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
			mCurrentFile = file;

			clearList();

			fillList(file.listFiles());

			// sort
			Collections.sort(mFolderLists);
			Collections.sort(mFileLists);

			mAllLists.addAll(mFolderLists);
			mAllLists.addAll(mFileLists);
			
			mFileInfoAdapter.notifyDataSetChanged();
			//back to the listview top,every time
			mFileListView.setSelection(0);
			
			mFileInfoAdapter.selectAll(false);
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
					mFileInfoAdapter.notifyDataSetChanged();
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
    	List<FileInfo> fileList = mFileInfoAdapter.getList();
    	for(int position : posList){
    		nameList.add(fileList.get(position).fileName);
    	}
    	
    	mDeleteDialog = new FileDeleteDialog(mContext,nameList);
    	mDeleteDialog.setOnClickListener(new OnDelClickListener() {
			@Override
			public void onClick(View view, String path) {
				switch (view.getId()) {
				case R.id.left_button:
					showMenuBar(false);
					DeleteTask deleteTask = new DeleteTask(posList);
					deleteTask.execute();
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
    	List<Integer> positionList = new ArrayList<Integer>();
    	
    	DeleteTask(List<Integer> list){
    		positionList = list;
    	}
    	
		@Override
		protected String doInBackground(Void... params) {
			List<FileInfo> fileList = mFileInfoAdapter.getList();
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
		ArrayList<String> checkedList = (ArrayList<String>) mFileInfoAdapter.getCheckedFiles();
		
		//send
		FileTransferUtil fileTransferUtil = new FileTransferUtil(getActivity());
		fileTransferUtil.sendFiles(checkedList, new TransportCallback() {
			
			@Override
			public void onTransportSuccess() {
				int first = mFileListView.getFirstVisiblePosition();
				int last = mFileListView.getLastVisiblePosition();
				List<Integer> checkedItems = mFileInfoAdapter.getCheckedItemIds();
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
		mFileInfoManager.addNavigationList(new NavigationRecord(currentPath,
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
			refreshTab(newPath, storge_type);
			// showDirectoryContent(newPath);
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
					Log.d(TAG, "updateNavigationBar,text is: " + tabItemText);
					((Button) btn).setText(tabItemText);
					// ((Button) btn).setHorizontalScrollBarEnabled(true);
					// ((Button) btn).setHorizontalFadingEdgeEnabled(true);
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
					selectedFileInfo = mFileInfoAdapter.getItem(mFileListView
							.getPositionForView(view));
					top = view.getTop();
				}
				browserTo(new File(curFilePath));
				// addToNavigationList(mCurrentPath, top, selectedFileInfo);
				updateHomeButton(type);
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
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_FLING:
			Log.d(TAG, "SCROLL_STATE_FLING");
			mFileInfoAdapter.setFlag(false);
			break;
		case OnScrollListener.SCROLL_STATE_IDLE:
			Log.d(TAG, "SCROLL_STATE_IDLE");
			mFileInfoAdapter.setFlag(true);
			mFileInfoAdapter.notifyDataSetChanged();
			break;
		case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
			Log.d(TAG, "SCROLL_STATE_TOUCH_SCROLL");
			mFileInfoAdapter.setFlag(false);
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
	
	public void updateClssifyUI(){
		Message message = mHandler.obtainMessage();
		message.what = MSG_UPDATE_CLASSIFY;
		message.sendToTarget();
	}
	
	public void goToHome(){
		Log.i(TAG, "goToHome");
		mNavBarLayout.setVisibility(View.GONE);
		
		if (null != mGetFileTask && mGetFileTask.getStatus() == AsyncTask.Status.RUNNING) {
			startUpdateClassifyUI();
		}
		
		mStatus = STATUS_HOME;
		updateUI(5);
		mFileInfoAdapter = new FileInfoAdapter(mContext, mHomeList, mCLassifyList);
		mFileListView.setAdapter(mFileInfoAdapter);
	}
	
	//get type files
	class GetFilesTask extends AsyncTask<Integer, Integer, String>{
		long start;
		long end;
		ProgressDialog progressDialog = null;
		@Override
		protected String doInBackground(Integer... params) {
			File file = new File(DreamConstant.DEFAULT_SDCARD);
			if (!file.exists()) {
				Log.e(TAG, DreamConstant.DEFAULT_SDCARD + " is not exist");
			}else {
				mCLassifyList.add(mDocList);
				mCLassifyList.add(mEbookList);
				mCLassifyList.add(mApkList);
				mCLassifyList.add(mArchiveList);
				
				ClassifyFilenameFileter filenameFileter = new ClassifyFilenameFileter(fileNameTypes);
				listFiles(filenameFileter, file, params[0]);
			}
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			start = System.currentTimeMillis();
			startUpdateClassifyUI();
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			end = System.currentTimeMillis();
			Log.d(TAG, "total cost " + (end - start) / 1000 + "s");
			//when loading finish,cancel timer
			updateClssifyUI();
			stopUpdateClassifyUI();
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
		}
	}
	
	private void listFiles(ClassifyFilenameFileter fileter,File file, int type){
		if (file.isDirectory() && file.getName().equals(LogFile.LOG_FOLDER_NAME)) {
			//do not show log folder
			Log.d(TAG, "listFiles.name:" + file.getName());
			return;
		}
		
		File[] files = file.listFiles(fileter);
		FileInfo fileInfo = null;
		if (null == files) {
			return;
		}

		for(File file2 : files){
			if (file2.isHidden()) {
				//do not handler hide file
			}else {
				if (file2.isDirectory()) {
					listFiles(fileter, file2, type);
				}else {
					fileInfo = mFileInfoManager.getFileInfo(file2);
					int fileType = fileInfo.type;
					switch (fileType) {
					case DOC:
						mDocList.add(fileInfo);
						break;
					case EBOOK:
						mEbookList.add(fileInfo);
						break;
					case APK:
						mApkList.add(fileInfo);
						break;
					case ARCHIVE:
						mArchiveList.add(fileInfo);
						break;
					default:
						break;
					}
				}
			}
		}
		
		Collections.sort(mDocList, DATE_COMPARATOR);
		Collections.sort(mEbookList, DATE_COMPARATOR);
		Collections.sort(mApkList);
		Collections.sort(mArchiveList, DATE_COMPARATOR);
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
	 * start update classify file ui
	 */
	public void startUpdateClassifyUI(){
		if (null == mClassifyTimer) {
			mClassifyTimer = new Timer();
		}
		
		mClassifyTimer.schedule(new ClassifyUpdateTask(), 1000, 1000);
	}
	
	/***
	 * stop update Classify ui timer
	 */
	public void stopUpdateClassifyUI(){
		if (null != mClassifyTimer) {
			mClassifyTimer.cancel();
			mClassifyTimer = null;
		}
	}
	
	///当加载各种类型文件数量的时候，需要一个定时器，定时更新数量显示
	/**
	 * update classify ui timertask
	 */
	class ClassifyUpdateTask extends TimerTask{
		@Override
		public void run() {
			updateClssifyUI();
		}
	}
	
	/**
	 * back key callback
	 */
	@Override
	public boolean onBackPressed() {
		Log.d(TAG, "onBackPressed.mStatus=" + mStatus);
		if (mFileInfoAdapter.isMode(DreamConstant.MENU_MODE_EDIT)) {
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
				//up to parent path
				File parentFile = mCurrentFile.getParentFile();
				browserTo(parentFile.getAbsoluteFile());
			}
			break;
		case STATUS_DOC:
		case STATUS_EBOOK:
		case STATUS_APK:
		case STATUS_ARCHIVE:
			goToHome();
			break;
		}
		return false;
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (null != mGetFileTask) {
			mGetFileTask.cancel(true);
		}
	}

	@Override
	public void onMenuClick(ActionMenuItem item) {
		switch (item.getItemId()) {
		case ActionMenu.ACTION_MENU_SEND:
			doTransfer();
			showMenuBar(false);
			break;
		case ActionMenu.ACTION_MENU_DELETE:
			List<Integer> posList = mFileInfoAdapter.getCheckedItemIds();
			showDeleteDialog(posList);
			break;
		case ActionMenu.ACTION_MENU_INFO:
			List<FileInfo> list = mFileInfoAdapter.getSelectedList();
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
		int selectCount = mFileInfoAdapter.getCheckedItems();
		updateActionMenuTitle(selectCount);
		
		if (mFileInfoAdapter.getCount() == selectCount) {
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
		mFileInfoAdapter.changeMode(DreamConstant.MENU_MODE_NORMAL);
		mFileInfoAdapter.selectAll(false);
		mFileInfoAdapter.notifyDataSetChanged();
	}
	
	/**
	 * do select all items or unselect all items
	 */
	public void doSelectAll(){
		int selectedCount = mFileInfoAdapter.getCheckedItems();
		if (mFileInfoAdapter.getCount() != selectedCount) {
			mFileInfoAdapter.selectAll(true);
		} else {
			mFileInfoAdapter.selectAll(false);
		}
		updateMenuBar();
		mMenuTabManager.refreshMenus(mActionMenu);
		mFileInfoAdapter.notifyDataSetChanged();
	}
	
	/**
	 * update main title 
	 * @param selectCount
	 */
	public void updateActionMenuTitle(int selectCount){
		mFragmentActivity.updateTitleSelectNum(selectCount, count);
	}
	
}
