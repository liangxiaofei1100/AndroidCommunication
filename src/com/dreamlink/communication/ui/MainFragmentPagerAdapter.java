package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.ui.app.AppFragment;
import com.dreamlink.communication.ui.app.GameFragment;
import com.dreamlink.communication.ui.app.TiandiFragment;
import com.dreamlink.communication.ui.file.FileBrowserFragment;
import com.dreamlink.communication.ui.image.PictureFragment;
import com.dreamlink.communication.ui.media.AudioFragment;
import com.dreamlink.communication.ui.media.VideoFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MainFragmentPagerAdapter extends FragmentPagerAdapter{
	List<Fragment> fragments = new ArrayList<Fragment>();
	int appId = -1;
	
	public MainFragmentPagerAdapter(FragmentManager fm, List<Fragment> list) {
		super(fm);
		this.fragments = list;
	}
	
	public MainFragmentPagerAdapter(FragmentManager fm, int appid){
		super(fm);
		this.appId = appid;
		fragments.add(TiandiFragment.newInstance(appId));
		fragments.add(PictureFragment.newInstance(appId));
		fragments.add(AudioFragment.newInstance(appId));
		fragments.add(VideoFragment.newInstance(appId));
		fragments.add(AppFragment.newInstance(appId));
		fragments.add(GameFragment.newInstance(appId));
		fragments.add(FileBrowserFragment.newInstance(appId));
	}

	@Override
	public Fragment getItem(int position) {
		return fragments.get(position);
	}
	
	public void addItem(Fragment fragment){
		fragments.add(fragment);
	}

	@Override
	public int getCount() {
		return fragments.size();
	}
}
