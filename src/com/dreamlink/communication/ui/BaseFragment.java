package com.dreamlink.communication.ui;

import android.R.integer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;
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
	
	/**
	 * when user pressed back key
	 */
	public boolean onBackPressed(){
		return true;
	}
	
	/**
	 * custom actionmode menu item click call back
	 * @param item menu item
	 */
	public void onActionMenuItemClick(MenuItem item){
	}

	/***
	 * cancle action menu
	 */
	public void onActionMenuDone(){};
	
	/**
	 * get current page menu mode,default is normal
	 * @return 
	 */
	public int getMode(){
		return DreamConstant.MENU_MODE_NORMAL;
	}
	
	/**
	 * get listview/gridview select items count
	 * @return count,default is 0
	 */
	public int getSelectItemsCount(){
		return 0;
	}
	
	/**
	 * select all or unselect all
	 * @param isSelectAll
	 */
	public void selectAll(boolean isSelectAll){};
	
	public void setSelectAll(boolean all){
		mIsSelectAll  = all;
	}
	
	public boolean isSelectAll(){
		return mIsSelectAll;
	}
	
	public void setMenuTabManager(MenuTabManager manager){
	}
	
	public MenuTabManager getMenuTabManager(){
		return null;
	}
	
	public Menu getMenu(){
		return null;
	}
	
	public void onDestroy() {
		super.onDestroy();
		//clear cache here
		imageLoader.stop();
		imageLoader.clearDiscCache();
		imageLoader.clearMemoryCache();
	};
}
