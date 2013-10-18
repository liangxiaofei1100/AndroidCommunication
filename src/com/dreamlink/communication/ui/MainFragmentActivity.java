package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.ui.app.AppFragment;
import com.dreamlink.communication.ui.app.GameFragment;
import com.dreamlink.communication.ui.app.RecommendActivity;
import com.dreamlink.communication.ui.app.RecommendFragment;
import com.dreamlink.communication.ui.app.TiandiFragment;
import com.dreamlink.communication.ui.file.FileBrowserFragment;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.ui.image.PictureFragment;
import com.dreamlink.communication.ui.media.AudioFragment;
import com.dreamlink.communication.ui.media.VideoFragment;
import com.dreamlink.communication.ui.network.NetworkActivity;
import com.dreamlink.communication.ui.network.NetworkFragment;
import com.dreamlink.communication.ui.settings.SettingsActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

//各个Fragment的位置必须固定
public class MainFragmentActivity extends ActionBarActivity implements
		OnPageChangeListener, OnClickListener, OnMenuItemClickListener {
	private static final String TAG = "MainFragmentActivity";
	private ViewPager viewPager;
	private MainFragmentPagerAdapter mPagerAdapter;
	private int mLastPosition = 0;

	// define fragment position
	public static final int ZY_TIANDI = 0;
	public static final int IMAGE = 1;
	public static final int AUDIO = 2;
	public static final int VIDEO = 3;
	public static final int APP = 4;
	public static final int GAME = 5;
	public static final int FILE_BROWSER = 6;

	private List<Fragment> mFragmentLists = new ArrayList<Fragment>();
	private TiandiFragment mTiandiFragment;
	private PictureFragment mPictureFragment;
	private AudioFragment mAudioFragment;
	private VideoFragment mVideoFragment;
	private AppFragment mAppFragment;
	private GameFragment mGameFragment;
	private FileBrowserFragment mBrowserFragment;

	/**
	 * must inline
	 */
	private static final int[] TITLE_ICON_IDs = { R.drawable.title_tiandi,
			R.drawable.title_image, R.drawable.title_audio,
			R.drawable.title_video, R.drawable.title_app,
			R.drawable.title_game, R.drawable.icon_transfer_normal };

	private static final String[] TITLEs = { "朝颜天地", "图片",
			"音频", "视频", "应用", "游戏", "批量传输" };

	// title view
	private View mCustomTitleView;
	private View mSelectView;
	private ImageView mTitleIconView;
	private TextView mTitleNameView;
	private TextView mTitleNumView;
	private View mHistroyView;
	private View mSettingView;
	private View mNetworkView;
	private View mRecommendView;

	private RelativeLayout mContainLayout;
	
	//menubar view
	private View mMenuBarTopView;
	private View mDoneView;
	private Button mSelectBtn;
	private View mMenuBarBottomView;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.ui_main_fragment);

		getSupportActionBar().hide();

		mContainLayout = (RelativeLayout) findViewById(R.id.rl_main_fragment);
		initMenuBar();
		initTitle();

		int position = getIntent().getIntExtra("position", 0);
		viewPager = (ViewPager) findViewById(R.id.vp_main_frame);
		// 考虑到内存消耗问题，缓存页面不应该设置这么大
		viewPager.setOffscreenPageLimit(6);

		addFragments();
		setCurrentItem(position);
	}

	private void addFragments() {
		int appid = AppUtil.getAppID(this);
		mTiandiFragment = TiandiFragment.newInstance(appid);
		mPictureFragment = PictureFragment.newInstance(appid);
		mAudioFragment = AudioFragment.newInstance(appid);
		mVideoFragment = VideoFragment.newInstance(appid);
		mAppFragment = AppFragment.newInstance(appid);
		mGameFragment = GameFragment.newInstance(appid);
		mBrowserFragment = FileBrowserFragment.newInstance(appid);

		mFragmentLists.add(mTiandiFragment);
		mFragmentLists.add(mPictureFragment);
		mFragmentLists.add(mAudioFragment);
		mFragmentLists.add(mVideoFragment);
		mFragmentLists.add(mAppFragment);
		mFragmentLists.add(mGameFragment);
		mFragmentLists.add(mBrowserFragment);// 批量传输
		mPagerAdapter = new MainFragmentPagerAdapter(
				getSupportFragmentManager(), mFragmentLists);
		viewPager.setAdapter(mPagerAdapter);
		viewPager.setOnPageChangeListener(this);
	}
	
	private void initMenuBar(){
		mMenuBarTopView = findViewById(R.id.menubar_top);
		mMenuBarBottomView = findViewById(R.id.menubar_bottom);
		mMenuBarTopView.setVisibility(View.GONE);
		mMenuBarBottomView.setVisibility(View.GONE);
		
		mDoneView = findViewById(R.id.ll_menubar_done);
		mSelectBtn = (Button) findViewById(R.id.btn_select);
		mDoneView.setOnClickListener(this);
		mSelectBtn.setOnClickListener(this);
	}

	private void initTitle() {
		mCustomTitleView = findViewById(R.id.rl_title);
		mCustomTitleView.setVisibility(View.VISIBLE);
		// select view
		mSelectView = mCustomTitleView.findViewById(R.id.ll_menu_select);
		mSelectView.setOnClickListener(this);

		// title icon view
		mTitleIconView = (ImageView) mCustomTitleView
				.findViewById(R.id.iv_title_icon);

		// title name view
		mTitleNameView = (TextView) mCustomTitleView
				.findViewById(R.id.tv_title_name);
		mTitleNumView = (TextView) mCustomTitleView
				.findViewById(R.id.tv_title_num);

		// history button
		mHistroyView = mCustomTitleView.findViewById(R.id.ll_history);
		mHistroyView.setOnClickListener(this);

		// setting button
		mSettingView = mCustomTitleView.findViewById(R.id.ll_setting);
		mSettingView.setOnClickListener(this);
		
		//recommmend button
		mRecommendView = mCustomTitleView.findViewById(R.id.ll_recommend);
		mRecommendView.setVisibility(View.VISIBLE);
		mRecommendView.setOnClickListener(this);
		
		//network button
		mNetworkView = mCustomTitleView.findViewById(R.id.ll_network);
		mNetworkView.setVisibility(View.VISIBLE);
		mNetworkView.setOnClickListener(this);
	}

	public void setCurrentItem(int position) {
		mLastPosition = position;
		viewPager.setCurrentItem(position, false);
		updateTilte(position);
	}

	/**
	 * Show transport animation.
	 * 
	 * @param startViews The transport item image view.
	 */
	public void showTransportAnimation(ImageView... startViews) {
		TransportAnimationView transportAnimationView = new TransportAnimationView(
				this);
		transportAnimationView.startTransportAnimation(mContainLayout,
				mHistroyView, startViews);
	}

	public void setTitleNum(int position, int num) {
		if (position == viewPager.getCurrentItem()) {
			mTitleNumView.setText(getString(R.string.num_format, num));
		}
	}

	/**
	 * update title icon & name accrod to the position
	 * 
	 * @param position
	 */
	private void updateTilte(int position) {
		mTitleIconView.setImageResource(TITLE_ICON_IDs[position]);
		mTitleNameView.setText(TITLEs[position]);
		BaseFragment baseFragment = (BaseFragment) mFragmentLists.get(position);
		switch (position) {
		case IMAGE:
		case AUDIO:
		case VIDEO:
		case APP:
		case GAME:
		case FILE_BROWSER:
			mTitleNumView.setText(getString(R.string.num_format,
					baseFragment.getCount()));
			break;
		default:
			mTitleNumView.setText(null);
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			int position = viewPager.getCurrentItem();
			switch (position) {
			case IMAGE:
				// Picture
				mPictureFragment.onBackPressed();
				return false;
			case FILE_BROWSER:
				// FileBrowser
				mBrowserFragment.onBackPressed();
				return false;
			default:
				break;
			}
			break;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPageSelected(int position) {
		// TODO Auto-generated method stub
		// when scroll out of PictureFragment,set PictureFragment status to
		// Folder View
		if (IMAGE == mLastPosition) {
			mPictureFragment.scrollToHomeView();
		}
		mLastPosition = position;
		updateTilte(position);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ll_menu_select:
			PopupMenu popupMenu = new PopupMenu(this, mSelectView);
			popupMenu.setOnMenuItemClickListener(this);
			MenuInflater inflater = popupMenu.getMenuInflater();
			inflater.inflate(R.menu.main_menu_item, popupMenu.getMenu());
			popupMenu.show();
			break;
		case R.id.ll_history:
			MainUIFrame.startActivity(this, HistoryActivity.class);
			break;
		case R.id.ll_setting:
			MainUIFrame.startActivity(this, SettingsActivity.class);
			break;
		case R.id.ll_network:
			MainUIFrame.startActivity(this, NetworkActivity.class);
			break;
		case R.id.ll_recommend:
			MainUIFrame.startActivity(this, RecommendActivity.class);
			break;
		default:
			break;
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		setCurrentItem(item.getOrder());
		return true;
	}
}
