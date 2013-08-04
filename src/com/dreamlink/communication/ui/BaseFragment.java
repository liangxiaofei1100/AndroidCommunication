package com.dreamlink.communication.ui;

import android.support.v4.app.Fragment;

import com.nostra13.universalimageloader.core.ImageLoader;

public class BaseFragment extends Fragment{
	protected ImageLoader imageLoader = ImageLoader.getInstance();
	
	public void onDestroy() {
		super.onDestroy();
		//clear cache here
		imageLoader.stop();
		imageLoader.clearDiscCache();
		imageLoader.clearMemoryCache();
	};
}
