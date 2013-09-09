package com.dreamlink.communication.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.file.FileBrowserFragment;
import com.dreamlink.communication.ui.file.FileInfo;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UserTabManager {
	private static final String TAG = "UserTabManager";
	private List<User> mUserList = new ArrayList<User>();
	protected LinearLayout mTabsHolder = null;
	
	private View view = null;
	private LayoutInflater inflater = null;
	
	private TextView mNameView;
	private SlowHorizontalScrollView scrollView;
	
	private Context mContext;
	
	public UserTabManager(Context context, View rooView, SlowHorizontalScrollView scrollView) {
		mContext = context;
		inflater = LayoutInflater.from(context);
//		view = inflater.inflate(R.layout.ui_user_item, null);
		
//		mNameView = (TextView) view.findViewById(R.id.user_name_textview);
		
		mTabsHolder = (LinearLayout) rooView.findViewById(R.id.tabs_holder);
		
		this.scrollView = scrollView;
//		mTabsHolder.addView(view);
	}
	
	public void refreshTab(List<User> users) {
		Log.d(TAG, "refreshTab:" + users.size());
		int count = mTabsHolder.getChildCount();
		mTabsHolder.removeViews(0, count);
//		mUserList.clear();

//		for (Map.Entry<Integer, User> entry : users.entrySet()) {
//			User user = entry.getValue();
//			addTab(user);
//		}
		for(User user: users){
			addTab(user);
		}
		
		startActionBarScroll();
	}
	
	/**
	 * This method creates tabs on the navigation bar
	 * @param text
	 *            the name of the tab
	 */
	public void addTab(User user) {
		Log.d(TAG, "addTab:" + user.getUserName());
		
		View view = null;
		view = inflater.inflate(R.layout.ui_user_item, null);
		TextView nameView = (TextView) view.findViewById(R.id.user_name_textview);
		nameView.setText(user.getUserName());
		view.setId(mUserList.size());
		mTabsHolder.addView(view);
		mUserList.add(user);
	}
	
	/**
	 * The method updates the navigation bar
	 * 
	 * @param id
	 *            the tab id that was clicked
	 */
	protected void updateNavigationBar(int id, int type) {
//		Log.d(TAG, "updateNavigationBar,id = " + id);
		// click current button do not response
//		if (id < mUserList.size() - 1) {
//			int count = mTabNameList.size() - id;
//			mTabsHolder.removeViews(id, count);
//
//			for (int i = 1; i < count; i++) {
//				// update mTabNameList
//				mTabNameList.remove(mTabNameList.size() - 1);
//			}
			// mTabsHolder.addView(mBlankTab);

//			if (id == 0) {
//				curFilePath = current_root_path;
//			} else {
//				String[] result = mountManager.getShowPath(curFilePath,
//						type).split(MountManager.SEPERATOR);
//				StringBuilder sb = new StringBuilder();
//				for (int i = 0; i <= id; i++) {
//					sb.append(MountManager.SEPERATOR);
//					sb.append(result[i]);
//				}
//				curFilePath = current_root_path + sb.toString();
//			}

//			int top = -1;
//			FileInfo selectedFileInfo = null;
//			if (mFileListView.getCount() > 0) {
//				View view = mFileListView.getChildAt(0);
//				selectedFileInfo = mFileInfoAdapter.getItem(mFileListView
//						.getPositionForView(view));
//				top = view.getTop();
//			}
//			browserTo(new File(curFilePath));
//			// addToNavigationList(mCurrentPath, top, selectedFileInfo);
//			updateHomeButton();
//		}
//	}
	// end tab manager
}
	
	private void startActionBarScroll() {
		// scroll to right with slow-slide animation
		// To pass the Launch performance test, avoid the scroll
		// animation when launch.
		int tabHostCount = mTabsHolder.getChildCount();
		int navigationBarCount = scrollView.getChildCount();
		if ((tabHostCount > 2) && (navigationBarCount >= 1)) {
			int width = scrollView.getChildAt(navigationBarCount - 1)
					.getRight();
			scrollView.startHorizontalScroll(scrollView.getScrollX(),width - scrollView.getScrollX());
		}
	}
}
