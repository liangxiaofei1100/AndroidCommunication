package com.dreamlink.communication.ui;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.app.AppFragment;
import com.dreamlink.communication.ui.app.AppFragmentActivity;
import com.dreamlink.communication.ui.app.AppNormalFragment;
import com.dreamlink.communication.ui.app.GameFragment;
import com.dreamlink.communication.ui.app.RecommendFragment;
import com.dreamlink.communication.ui.app.TiandiFragment;
import com.dreamlink.communication.ui.common.FragmentPagerSupport;
import com.dreamlink.communication.ui.file.FileBrowserFragment;
import com.dreamlink.communication.ui.file.FileClassifyFragment;
import com.dreamlink.communication.ui.file.FileFragmentActivity;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.ui.image.ImageFragment;
import com.dreamlink.communication.ui.image.ImageFragmentActivity;
import com.dreamlink.communication.ui.media.MediaAudioFragment;
import com.dreamlink.communication.ui.media.MediaFragmentActivity;
import com.dreamlink.communication.ui.media.MediaVideoFragment;
import com.dreamlink.communication.util.Log;

import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.Window;

public class MainFragmentActivity extends FragmentActivity {
	private static final String TAG = "MainFragmentActivity";
	ViewPager viewPager;
	MyFragmentPagerAdapter mAdapter;
	MyPageAdapter myPageAdapter;
	private static final String APP = "APP";
	private static final String PICTURE = "PICTURE";
	private static final String MEDIA = "MEDIA";
	private static final String FILE = "FILE";
	private static final String SHARE = "SHARE";
	
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_main_fragment);
		int position = getIntent().getIntExtra("position", 0);
		viewPager = (ViewPager) findViewById(R.id.vp_main_frame);
		List<Fragment> fragments = new ArrayList<Fragment>();
		
		fragments.add(new TiandiFragment());//朝颜天地
		//network
		fragments.add(new RecommendFragment());//精品推荐
		fragments.add(ImageFragment.newInstance(3));//图库
		fragments.add(new MediaAudioFragment());//音频
		fragments.add(new MediaVideoFragment());//视频
		fragments.add(new AppFragment());//应用
		fragments.add(new GameFragment());//游戏
		mAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager(), fragments);
		viewPager.setAdapter(mAdapter);
		viewPager.setCurrentItem(position);
	}
	
	public class MyFragmentPagerAdapter extends FragmentPagerAdapter{
		List<Fragment> fragments = new ArrayList<Fragment>();
		public MyFragmentPagerAdapter(FragmentManager fm, List<Fragment> list) {
			super(fm);
			this.fragments = list;
			// TODO Auto-generated constructor stub
		}

		@Override
		public Fragment getItem(int position) {
			// TODO Auto-generated method stub
//			Fragment fragment = null;
			return fragments.get(position);
//			switch (position) {
//			case 0:
////				fragment= new AppNormalFragment();
//				fragment = FragmentPagerSupport.ArrayListFragment.newInstance(position);
//				break;
//			case 1:
////				fragment = new FileBrowserFragment();
//				fragment = FragmentPagerSupport.ArrayListFragment.newInstance(position);
//				break;
//			case 2:
////				fragment = new FileClassifyFragment();
//				fragment = FragmentPagerSupport.ArrayListFragment.newInstance(position);
//				break;
//			case 3:
//				fragment = ImageFragment.newInstance(position);
////				fragment = FragmentPagerSupport.ArrayListFragment.newInstance(position);
//				break;
////			case 4:
//////				fragment = FragmentPagerSupport.ArrayListFragment.newInstance(position);
////				fragment = ImageFragment.newInstance(position);
////				break;
//			default:
//				break;
//			}
			
//			return fragment;
		}
		
		public void addItem(Fragment fragment){
			fragments.add(fragment);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return fragments.size();
		}
		
		
		
	}
	
	public class MyPageAdapter extends PagerAdapter {
		private ArrayList<View> views = new ArrayList<View>();

		public MyPageAdapter(ArrayList<View> views) {
			this.views = views;
		}

		@Override
		public int getCount() {
			return views.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			((ViewPager) container).removeView(views.get(position));
		}

		@Override
		public Object instantiateItem(View container, int position) {
			Log.d(TAG, "position" + position);
			((ViewPager) container).addView(views.get(position));
			return views.get(position);
		}

	}
}
