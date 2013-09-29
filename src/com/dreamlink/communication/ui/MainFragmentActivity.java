package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.ui.app.AppFragment;
import com.dreamlink.communication.ui.app.GameFragment;
import com.dreamlink.communication.ui.app.RecommendFragment;
import com.dreamlink.communication.ui.app.TiandiFragment;
import com.dreamlink.communication.ui.file.FileBrowserFragment;
import com.dreamlink.communication.ui.help.HelpFragment;
import com.dreamlink.communication.ui.image.ImageFragment;
import com.dreamlink.communication.ui.image.PictureFragment;
import com.dreamlink.communication.ui.media.AudioFragment;
import com.dreamlink.communication.ui.media.MediaAudioFragment;
import com.dreamlink.communication.ui.media.MediaVideoFragment;
import com.dreamlink.communication.ui.media.VideoFragment;
import com.dreamlink.communication.ui.network.NetworkFragment;
import com.dreamlink.communication.ui.settings.SettingsFragment;
import com.dreamlink.communication.util.Log;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

public class MainFragmentActivity extends FragmentActivity {
	private static final String TAG = "MainFragmentActivity";
	ViewPager viewPager;
	MyFragmentPagerAdapter mAdapter;
	MyPageAdapter myPageAdapter;
	public static MainFragmentActivity instance;
	
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_main_fragment);
		instance = this;
		
		int position = getIntent().getIntExtra("position", 0);
		viewPager = (ViewPager) findViewById(R.id.vp_main_frame);
		//考虑到内存消耗问题，缓存页面不应该设置这么大
		viewPager.setOffscreenPageLimit(2);
		
		int appid = AppUtil.getAppID(this);
		List<Fragment> fragments = new ArrayList<Fragment>();
		
		fragments.add(TiandiFragment.newInstance(appid));//朝颜天地
		fragments.add(NetworkFragment.newInstance(appid));//网上邻居
		fragments.add(RecommendFragment.newInstance(appid));//精品推荐
		fragments.add(PictureFragment.newInstance(appid));//图库
		fragments.add(AudioFragment.newInstance(appid));//音频
		fragments.add(VideoFragment.newInstance(appid));//视频
		fragments.add(AppFragment.newInstance(appid));//应用
		fragments.add(GameFragment.newInstance(appid));//游戏
		fragments.add(FileBrowserFragment.newInstance(appid));//批量传输
		fragments.add(SettingsFragment.newInstance(appid));//设置
		fragments.add(HelpFragment.newInstance(appid));//帮助
		mAdapter = new MyFragmentPagerAdapter(getSupportFragmentManager(), fragments);
		viewPager.setAdapter(mAdapter);
		viewPager.setCurrentItem(position);
	}
	
	public class MyFragmentPagerAdapter extends FragmentPagerAdapter{
		List<Fragment> fragments = new ArrayList<Fragment>();
		public MyFragmentPagerAdapter(FragmentManager fm, List<Fragment> list) {
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
			return fragments.size()  ;
		}
		
	}
	
	public void goToHistory(){
		viewPager.setCurrentItem(9);
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
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			//8 is instead of FileBrowserFragment,fixed
			int position = viewPager.getCurrentItem();
			if (8 == position) {
				FileBrowserFragment.mInstance.onBackPressed();
				return false;
			}
			break;

		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}
}
