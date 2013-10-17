package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MainFragmentPagerAdapter extends FragmentPagerAdapter{
	List<Fragment> fragments = new ArrayList<Fragment>();
	
	public MainFragmentPagerAdapter(FragmentManager fm, List<Fragment> list) {
		super(fm);
		this.fragments = list;
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
