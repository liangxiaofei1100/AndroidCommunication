package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.MenuTabManager.onMenuItemClickListener;
import com.dreamlink.communication.ui.app.AppFragment;
import com.dreamlink.communication.ui.app.GameFragment;
import com.dreamlink.communication.ui.app.RecommendActivity;
import com.dreamlink.communication.ui.app.TiandiFragment;
import com.dreamlink.communication.ui.file.FileBrowserFragment;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.ui.image.PictureFragment;
import com.dreamlink.communication.ui.media.AudioFragment;
import com.dreamlink.communication.ui.media.VideoFragment;
import com.dreamlink.communication.ui.network.NetworkActivity;
import com.dreamlink.communication.ui.settings.SettingsActivity;
import com.dreamlink.communication.util.Log;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.view.KeyEvent;
import android.view.Menu;
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
	
	private Notice mNotice;

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
	private PopupMenu mSelectPopupMenu;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.ui_main_fragment);

		getSupportActionBar().hide();
		
		mNotice = new Notice(this);

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
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// TODO Auto-generated method stub
//		Log.d(TAG, "onCreateOptionsMenu");
////		this.mMenu = menu;
//		return super.onCreateOptionsMenu(menu);
//	}
	
	private void initMenuBar(){
		mMenuBarTopView = findViewById(R.id.menubar_top);
		mMenuBarBottomView = findViewById(R.id.menubar_bottom);
		mMenuBarTopView.setVisibility(View.GONE);
		mMenuBarBottomView.setVisibility(View.GONE);
//		openOptionsMenu();
		
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
			BaseFragment baseFragment = getBaseFragment();
			baseFragment.onBackPressed();
			return false;
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
		int mode = getBaseFragment().getMode();
		if (DreamConstant.MENU_MODE_EDIT == mode) {
			updateActionMenuBar();
			showActionMenuBar(true);
		}else {
			showActionMenuBar(false);
		}
		
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
		case R.id.ll_menubar_done:
			dismissActionMenu();
			BaseFragment baseFragment = getBaseFragment();
			baseFragment.onActionMenuDone();
			break;
		case R.id.btn_select:
			if (null == mSelectPopupMenu) {
				mSelectPopupMenu = createSelectPopupMenu(mSelectBtn);
			}
			updateSelectPopupMenu();
			mSelectPopupMenu.show();
			break;
		default:
			break;
		}
	}
	
	/**
	 * create popup view for select button,select all/unselect all
	 * @param anchorView
	 * @return
	 */
	private PopupMenu createSelectPopupMenu(View anchorView) {
        final PopupMenu popupMenu = new PopupMenu(MainFragmentActivity.this, anchorView);
        popupMenu.inflate(R.menu.select_popup_menu);
        popupMenu.setOnMenuItemClickListener(this);
        return popupMenu;
    }
	
	private void updateSelectPopupMenu(){
        if (mSelectPopupMenu == null) {
            mSelectPopupMenu = createSelectPopupMenu(mSelectBtn);
            return;
        }
        final Menu menu = mSelectPopupMenu.getMenu();
        int selectedCount = getBaseFragment().getSelectItemsCount();
        if (getBaseFragment().getCount() == 0) {
            menu.findItem(R.id.menu_select).setEnabled(false);
        } else {
            menu.findItem(R.id.menu_select).setEnabled(true);
            if (getBaseFragment().getCount() != selectedCount) {
                menu.findItem(R.id.menu_select).setTitle(R.string.select_all);
                getBaseFragment().setSelectAll(true);
            } else {
                menu.findItem(R.id.menu_select).setTitle(R.string.unselect_all);
                getBaseFragment().setSelectAll(false);
            }
        }
	}
	
	/**
	 * get BaseFragment object
	 * @return BaseFragment objcet
	 */
	public BaseFragment getBaseFragment(){
		int position = viewPager.getCurrentItem();
		return (BaseFragment) mFragmentLists.get(position);
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_select:
			getBaseFragment().selectAll(getBaseFragment().isSelectAll());
			updateActionMenuTitle(getBaseFragment().getSelectItemsCount());
			updateSelectPopupMenu();
			break;

		default:
			setCurrentItem(item.getOrder());
			break;
		}
		return true;
	}
	
	public void startActionMenu(Menu menu, MenuTabManager manager){
		showActionMenuBar(true);
		if (null == manager) {
			manager = new MenuTabManager(this, mMenuBarBottomView);
			manager.setOnMenuItemClickListener(new onMenuItemClickListener() {
				@Override
				public void onMenuClick(MenuItem item) {
					BaseFragment baseFragment = getBaseFragment();
					baseFragment.onActionMenuItemClick(item);
				}
			});
		}
		manager.refreshMenus(menu);
	}
	
	public void dismissActionMenu(){
//		mMenu.clear();
		mSelectBtn.setText(null);
		showActionMenuBar(false);
	}
	
	public void updateActionMenuBar(){
		int selectCount = getBaseFragment().getSelectItemsCount();
		updateActionMenuTitle(selectCount);
		
		MenuTabManager manager = getBaseFragment().getMenuTabManager();
		if (null != manager) {
			manager.refreshMenus(getBaseFragment().getMenu());
		}else {
			Log.e(TAG, "updateActionMenuBar.MenuTabManage is null");
			manager = new MenuTabManager(this, mMenuBarBottomView);
			manager.setOnMenuItemClickListener(new onMenuItemClickListener() {
				@Override
				public void onMenuClick(MenuItem item) {
					BaseFragment baseFragment = getBaseFragment();
					baseFragment.onActionMenuItemClick(item);
				}
			});
			manager.refreshMenus(getBaseFragment().getMenu());
		}
	}
	
	public void showActionMenuBar(boolean show){
		if (show) {
			mCustomTitleView.setVisibility(View.GONE);
			mMenuBarTopView.setVisibility(View.VISIBLE);
			mMenuBarBottomView.setVisibility(View.VISIBLE);
		}else {
			mCustomTitleView.setVisibility(View.VISIBLE);
			mMenuBarTopView.setVisibility(View.GONE);
			mMenuBarBottomView.setVisibility(View.GONE);
		}
	}
	
	public void updateActionMenuTitle(int count){
		mSelectBtn.setText(getResources().getString(R.string.select_msg, count));
	}
}
