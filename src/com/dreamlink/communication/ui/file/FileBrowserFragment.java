package com.dreamlink.communication.ui.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.ui.MountManager;
import com.dreamlink.communication.ui.SlowHorizontalScrollView;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.PopupView.PopupViewClickListener;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager.NavigationRecord;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.util.Log;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FileBrowserFragment extends BaseFragment implements
		OnClickListener, OnItemClickListener,PopupViewClickListener, OnScrollListener, OnItemLongClickListener {
	private static final String TAG = "FileBrowserFragment";

	// 文件路径导航栏
	private SlowHorizontalScrollView mNavigationBar = null;
	// 显示所有文件
	private ListView mFileListView = null;
	private TextView mNoSDcardView;
	private LinearLayout mNavBarLayout;

	// sdcard & 手机存储 切换按钮
	private ImageView mSwitchImageView;
	// 快速回到根目录
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

	// 用来保存ListView中每个Item的图片，以便释放
	public static Map<String, Bitmap> bitmapCaches = new HashMap<String, Bitmap>();

	private Context mContext;

	private File mCurrentFile = null;
	private String mCurrentPath;

	// context menu
	private int mCurrentPosition = -1;
	// save current sdcard type
	private static int storge_type = -1;
	// save current sdcard type path
	private String mCurrent_root_path;

	//title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	
	private int mAppId = -1;
	public static FileBrowserFragment mInstance = null;
	private SharedPreferences sp = null;
	
	//two status
	private static final int STATUS_FILE = 0;
	private static final int STATUS_HOME = 1;
	private int mStatus = STATUS_HOME;
	
	private String sdcard_path;
	private String internal_path;
	
	/**
	 * Create a new instance of AppFragment, providing "appid" as an
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
		mInstance = this;
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.ui_file, container, false);
		Log.d(TAG, "onCreateView");
		mContext = getActivity();
		initTitleVIews(rootView);

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
		mSwitchImageView = (ImageView) rootView
				.findViewById(R.id.iv_home);
		mSwitchImageView.setOnClickListener(this);

		Log.d(TAG, "onCreate end");
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
			mHomeList.add(MountManager.INTERNAL);
		}
		
		if (!MountManager.NO_EXTERNAL_SDCARD.equals(sdcard_path)) {
			mHomeList.add(MountManager.SDCARD);
		} 
		
		if (mHomeList.size() <= 0) {
			mNavBarLayout.setVisibility(View.GONE);
			mNoSDcardView.setVisibility(View.VISIBLE);
		}else {
			goToHome();
		}
		
	}

	private void initTitleVIews(View view){
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.icon_transfer_history);
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText("批量传输");
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText(getResources().getString(R.string.num_format, 0));
		mRefreshView.setOnClickListener(this)	;
		mHistoryView.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_home:
			goToHome();
			mTabManager.refreshTab(mCurrent_root_path, storge_type);
			break;
		case R.id.iv_refresh:
			break;
		case R.id.iv_history:
			Intent intent = new Intent();
			intent.setClass(mContext, HistoryActivity.class);
			startActivity(intent);
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
			mNavBarLayout.setVisibility(View.VISIBLE);
			mFileInfoAdapter = new FileInfoAdapter(mContext, mAllLists);
			mFileListView.setAdapter(mFileInfoAdapter);
			mStatus = STATUS_FILE;
			switch (mHomeList.get(position)) {
			case MountManager.INTERNAL:
				doInternal();
				break;
			case MountManager.SDCARD:
				doSdcard();
				break;
			default:
				break;
			}
			return;
		}
		
		FileInfo fileInfo = mAllLists.get(position);
		int top = view.getTop();
		if (fileInfo.isDir) {
			addToNavigationList(mCurrentPath, top, fileInfo);
			browserTo(new File(mAllLists.get(position).filePath));
		} else {
			// file set file checked
			boolean checked = mFileInfoAdapter.isChecked(position);
			mFileInfoAdapter.setChecked(position, !checked);
			mFileInfoAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int position,
			long arg3) {
		if (mFileInfoAdapter.isHome) {
			return false;
		}
		
		final FileInfo fileInfo = mAllLists.get(position);
		int resId = R.array.file_menu;
		if (fileInfo.isDir) {
			resId = R.array.folder_menu;
		}
		new AlertDialog.Builder(mContext)
		.setTitle(fileInfo.fileName)
		.setItems(resId, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//暂时没想到什么好方法，实现文件和文件夹弹出不同菜单，先就这样
				if (fileInfo.isDir) {
					switch (which) {
					case 0:
						//info
						mFileInfoManager.showInfoDialog(fileInfo);
						break;
					case 1:
						//rename
						showRenameDialog(fileInfo, position);
						break;
					}
					return;
				}
				
				switch (which) {
				case 0:
					//open
					mFileInfoManager.openFile(fileInfo.filePath);
					break;
				case 1:
					//send
					FileTransferUtil fileSendUtil = new FileTransferUtil(getActivity());
					fileSendUtil.sendFile(fileInfo.filePath);
					break;
				case 2:
					//delete
					showDeleteDialog(fileInfo);
					break;
				case 3:
					//info
					mFileInfoManager.showInfoDialog(fileInfo);
					break;
				case 4:
					//rename
					showRenameDialog(fileInfo, position);
					break;

				default:
					break;
				}
			}
		}).create().show();
		return true;
	}

	public void browserTo(File file) {
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
	 * show delete dialog
	 * @param path  file path
	 */
	public void showDeleteDialog(FileInfo fileInfo) {
		String path = fileInfo.filePath;
		final int type = fileInfo.type;
		final FileDeleteDialog deleteDialog = new FileDeleteDialog(mContext, R.style.TransferDialog, path);
		deleteDialog.setOnClickListener(new OnDelClickListener() {
			@Override
			public void onClick(View view, String path) {
				switch (view.getId()) {
				case R.id.left_button:
					doDelete(path, type);
					break;

				default:
					break;
				}
			}
		});
		deleteDialog.show();
	}
	
	/**
	 * do delete file</br>
	 * if file is media file(image,audio,video),need delete in db
	 * @param path
	 * @param type
	 */
	public void doDelete(String path, int type){
		File file = new File(path);
		if (!file.exists()) {
			Log.e(TAG, path + " is not exist");
		} else {
			boolean ret = true;
			switch (type) {
			case FileInfoManager.TYPE_IMAGE:
				ret =  mFileInfoManager.deleteFileInMediaStore(DreamConstant.IMAGE_URI, path);
				break;
			case FileInfoManager.TYPE_AUDIO:
				ret =  mFileInfoManager.deleteFileInMediaStore(DreamConstant.AUDIO_URI, path);
				break;
			case FileInfoManager.TYPE_VIDEO:
				ret =  mFileInfoManager.deleteFileInMediaStore(DreamConstant.VIDEO_URI, path);
				break;
			default:
				//普通文件直接删除，不删除数据库，因为在3.0以前，还没有普通文件的数据哭
				ret = file.delete();
				break;
			}
			if (!ret) {
				Log.e(TAG, path + " delete failed");
			} else {
				mAllLists.remove(mCurrentPosition);
				mFileInfoAdapter.notifyDataSetChanged();
			}
		}
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

		protected void updateHomeButton() {
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
			if (storge_type == MountManager.INTERNAL) {
				homeBtn.setText("手机存储");
			} else if (storge_type == MountManager.SDCARD) {
				homeBtn.setText("SD卡");
			}
		}

		public void refreshTab(String initFileInfo, int type) {
			int count = mTabsHolder.getChildCount();
			mTabsHolder.removeViews(0, count);
			mTabNameList.clear();

			curFilePath = initFileInfo;
			if (curFilePath != null) {
				String[] result = mountManager.getShowPath(mCurrent_root_path, curFilePath, type)
						.split(MountManager.SEPERATOR);
				for (String string : result) {
					// add string to tab
					addTab(string);
				}
				startActionBarScroll();
			}

			updateHomeButton();
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
				updateHomeButton();
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
			// TODO Why MountManager.SDCARD_PATH is null?
			Log.e(TAG, "MountManager.SDCARD_PATH = null.");
			return;
		}
		browserTo(new File(mCurrent_root_path));
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
		case OnScrollListener.SCROLL_STATE_FLING:
			mFileInfoAdapter.setFlag(false);
			break;
		case OnScrollListener.SCROLL_STATE_IDLE:
			mFileInfoAdapter.setFlag(true);
			break;
		case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
			mFileInfoAdapter.setFlag(false);
			break;

		default:
			break;
		}
		mFileInfoAdapter.notifyDataSetChanged();
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// 注释：firstVisibleItem为第一个可见的Item的position，从0开始，随着拖动会改变
		// visibleItemCount为当前页面总共可见的Item的项数
		// totalItemCount为当前总共已经出现的Item的项数
		recycleBitmapCaches(0, firstVisibleItem);
		recycleBitmapCaches(firstVisibleItem + visibleItemCount, totalItemCount);
	}

	// 释放图片
	private void recycleBitmapCaches(int fromPosition, int toPosition) {
		Bitmap delBitmap = null;
		for (int del = fromPosition; del < toPosition; del++) {
			delBitmap = bitmapCaches.get(mAllLists.get(del));
			if (delBitmap != null) {
				// 如果非空则表示有缓存的bitmap，需要清理
				Log.d(TAG, "release position:" + del);
				// 从缓存中移除该del->bitmap的映射
				bitmapCaches.remove(mAllLists.get(del));
				delBitmap.recycle();
				delBitmap = null;
			}
		}
	}
	
	public void updateUI(int num){
		Message message = mHandler.obtainMessage();
		message.arg1 = num;
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}
	
	public void goToHome(){
		mStatus = STATUS_HOME;
		mNavBarLayout.setVisibility(View.GONE);
		mFileInfoAdapter = new FileInfoAdapter(mContext, true, mHomeList);
		mFileListView.setAdapter(mFileInfoAdapter);
		updateUI(mHomeList.size());
	}
	
	public void onBackPressed(){
		switch (mStatus) {
		case STATUS_HOME:
			getActivity().finish();
			break;
		case STATUS_FILE:
			if (mCurrent_root_path.equals(mCurrentPath)) {
				goToHome();
				return;
			}
			
			File parentFile = mCurrentFile.getParentFile();
			browserTo(parentFile.getAbsoluteFile());
			break;

		default:
			break;
		}
	}
	
}
