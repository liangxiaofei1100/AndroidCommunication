package com.dreamlink.communication.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.lib.util.Notice;
import com.nostra13.universalimageloader.core.ImageLoader;

public class BaseFragment extends Fragment{
	protected ImageLoader imageLoader = ImageLoader.getInstance();
	protected UserManager mUserManager = null;
	protected Notice mNotice = null;
	
	/**
	 * current fragment file size
	 */
	protected int count = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mUserManager = UserManager.getInstance();
		mNotice = new Notice(getActivity());
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
	public void onBackPressed(){
	}
	
	public void onDestroy() {
		super.onDestroy();
		//clear cache here
		imageLoader.stop();
		imageLoader.clearDiscCache();
		imageLoader.clearMemoryCache();
	};
}
