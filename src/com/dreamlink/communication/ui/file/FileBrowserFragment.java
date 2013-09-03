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
import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.ui.MountManager;
import com.dreamlink.communication.ui.PopupView;
import com.dreamlink.communication.ui.SlowHorizontalScrollView;
import com.dreamlink.communication.ui.PopupView.PopupViewClickListener;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager.NavigationRecord;
import com.dreamlink.communication.util.Log;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class FileBrowserFragment extends BaseFragment implements
		OnClickListener, OnItemClickListener,PopupViewClickListener, OnScrollListener {
	private static final String TAG = "FileBrowserFragment";

	// 文件路径导航栏
	private SlowHorizontalScrollView mNavigationBar = null;
	// 显示所有文件
	private ListView mFileListView = null;

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

	// 用来保存ListView中每个Item的图片，以便释放
	public static Map<String, Bitmap> bitmapCaches = new HashMap<String, Bitmap>();

	private Context mContext;

	// get sdcard files flag
	private boolean isFirst = true;

	private File mCurrentFile = null;
	private String mCurrentPath;

	private DisplayImageOptions options;

	// context menu
	private int mCurrentPosition = -1;
	// //popup view
	// show select sdcard view
	private PopupView mPopupView;
	// save current sdcard type
	private static int storge_type = -1;
	// save current sdcard type path
	private String current_root_path;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.ui_file_all, container, false);
		Log.d(TAG, "onCreate begin");
		mContext = getActivity();

		mFileListView = (ListView) rootView.findViewById(R.id.file_listview);
		if (mFileListView != null) {
			mFileListView.setEmptyView(rootView
					.findViewById(R.id.empty_textview));
			mFileListView.setOnItemClickListener(this);
			mFileListView.setOnScrollListener(this);
			mFileListView.setOnCreateContextMenuListener(new ListContextMenu(
					ListContextMenu.MENU_TYPE_FILE));
		}

		mNavigationBar = (SlowHorizontalScrollView) rootView
				.findViewById(R.id.navigation_bar_view);
		if (mNavigationBar != null) {
			mNavigationBar.setVerticalScrollBarEnabled(false);
			mNavigationBar.setHorizontalScrollBarEnabled(false);
			mTabManager = new TabManager();
		}
		mSwitchImageView = (ImageView) rootView
				.findViewById(R.id.ram_select_imageview);
		mSwitchImageView.setOnClickListener(this);

		mFileInfoManager = new FileInfoManager(mContext);
		mountManager = new MountManager();

		// init
		if (MountManager.NO_EXTERNAL_SDCARD.equals(MountManager.SDCARD_PATH)) {
			mSwitchImageView.setVisibility(View.GONE);
			doInternal();
		} else {
			if (MountManager.NO_INTERNAL_SDCARD.equals(MountManager.INTERNAL_PATH)) {
				mSwitchImageView.setVisibility(View.GONE);
			}
			doSdcard();
		}

		mPopupView = new PopupView(mContext);
		mPopupView.setOnPopupViewListener(this);

		Log.d(TAG, "onCreate end");
		return rootView;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ram_select_imageview:
			mPopupView.showAsDropDown(v);
			break;
		default:
			mTabManager.updateNavigationBar(v.getId(), storge_type);
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		FileInfo fileInfo = mAllLists.get(position);
		System.out.println("fileino.type=" + fileInfo.type);
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

			if (isFirst) {
				isFirst = false;

				// mFileInfoAdapter = new FileInfoAdapter(mContext, mAllLists);
				mFileInfoAdapter = new FileInfoAdapter(mContext, mAllLists);
				mFileListView.setAdapter(mFileInfoAdapter);
			} else {
				mFileInfoAdapter.notifyDataSetChanged();
			}
			//back to the listview top,every time
			mFileListView.setSelection(0);
			
			mFileInfoAdapter.selectAll(false);
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

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		mCurrentPosition = menuInfo.position;
		int position = menuInfo.position;
		FileInfo fileInfo = mAllLists.get(position);
		switch (item.getItemId()) {
		case ListContextMenu.MENU_OPEN:
			mFileInfoManager.openFile(fileInfo.filePath);
			break;
		case ListContextMenu.MENU_SEND:
			break;
		case ListContextMenu.MENU_DELETE:
			showDeleteDialog(fileInfo);
			break;
		case ListContextMenu.MENU_INFO:
			mFileInfoManager.showInfoDialog(fileInfo);
			break;
		case ListContextMenu.MENU_RENAME:
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
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
				String[] result = mountManager.getShowPath(curFilePath, type)
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
					curFilePath = current_root_path;
				} else {
					String[] result = mountManager.getShowPath(curFilePath,
							type).split(MountManager.SEPERATOR);
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i <= id; i++) {
						sb.append(MountManager.SEPERATOR);
						sb.append(result[i]);
					}
					curFilePath = current_root_path + sb.toString();
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
		if (MountManager.NO_INTERNAL_SDCARD.equals(MountManager.INTERNAL_PATH)) {
			// 没有外部&内部sdcard
			return;
		}
		current_root_path = MountManager.INTERNAL_PATH;
		browserTo(new File(current_root_path));
	}

	@Override
	public void doSdcard() {
		storge_type = MountManager.SDCARD;
		current_root_path = MountManager.SDCARD_PATH;
		browserTo(new File(current_root_path));
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
		// TODO Auto-generated method stub
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

}
