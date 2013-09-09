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
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mUserManager = UserManager.getInstance();
		mNotice = new Notice(getActivity());
	}
	
	public void onDestroy() {
		super.onDestroy();
		//clear cache here
		imageLoader.stop();
		imageLoader.clearDiscCache();
		imageLoader.clearMemoryCache();
	};
}
