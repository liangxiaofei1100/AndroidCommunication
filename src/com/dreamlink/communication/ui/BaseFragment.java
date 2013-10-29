package com.dreamlink.communication.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.ImageView;

import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.lib.util.Notice;
import com.nostra13.universalimageloader.core.ImageLoader;

public class BaseFragment extends Fragment{
	protected ImageLoader imageLoader = ImageLoader.getInstance();
	protected UserManager mUserManager = null;
	protected Notice mNotice = null;
	protected MainFragmentActivity mFragmentActivity;
	protected boolean mIsSelectAll = false;
	
	/**
	 * current fragment file size
	 */
	protected int count = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mUserManager = UserManager.getInstance();
		
		mFragmentActivity = (MainFragmentActivity) getActivity();
		mNotice = new Notice(mFragmentActivity);
		setHasOptionsMenu(true);
	}
	
	/**
	 * Show transport animation.
	 * 
	 * @param startViews The transport item image view.
	 */
	public void showTransportAnimation(ImageView... startViews) {
		if (mFragmentActivity != null) {
			mFragmentActivity.showTransportAnimation(startViews);
		}
	}
	
	/**
	 * get current fragment file count
	 */
	public int getCount(){
		return count;
	}
	
	public int getSelectedCount(){
		return -1;
	}
	
	public int getMenuMode(){
		return DreamConstant.MENU_MODE_NORMAL;
	}
	
//	/**
//	 * for picture use
//	 */
//	public void scrollToHomeView(){};
	
	/**
	 * when user pressed back key
	 */
	public boolean onBackPressed(){
		return true;
	}
	
	public void onDestroy() {
		super.onDestroy();
		//clear cache here
		imageLoader.stop();
		imageLoader.clearDiscCache();
		imageLoader.clearMemoryCache();
	};
}
