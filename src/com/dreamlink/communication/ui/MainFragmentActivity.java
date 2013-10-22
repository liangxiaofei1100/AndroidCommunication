package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.UserHelper;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.UserManager.OnUserChangedListener;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.MenuTabManager.onMenuItemClickListener;
import com.dreamlink.communication.notification.NotificationMgr;
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

import com.dreamlink.communication.util.NetWorkUtil;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

//各个Fragment的位置必须固定
public class MainFragmentActivity extends ActionBarActivity implements
		OnPageChangeListener, OnClickListener, OnMenuItemClickListener, OnItemClickListener, OnUserChangedListener {
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
	
	//保存当前View的状态，用于在主界面和各Item视图切换
	private static final int STATUS_MAIN = 0x100;
	private static final int STATUS_ITEM = 0x101;
	private int mStatus = STATUS_MAIN;
	//key for save
	private static final String STATUS = "status";

	/**
	 * must inline
	 */
	private static final int[] TITLE_ICON_resIDs = { R.drawable.title_tiandi,
			R.drawable.title_image, R.drawable.title_audio,
			R.drawable.title_video, R.drawable.title_app,
			R.drawable.title_game, R.drawable.icon_transfer_normal };

	private static final int[] TITLE_resIDs = {
		R.string.zy_tiandi, R.string.image, R.string.audio,
		R.string.video, R.string.app, R.string.game, R.string.file_browser
	};

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
	
	private View mMainFrameView;

	//main ui frame view
	private ImageView mTransferIV, mSettingIV;
	private ImageView mUserIconView;
	private TextView mUserNameView;
	private TextView mNetWorkStatusView;
	private GridView mGridView;
	
	private MainUIAdapter mAdapter;
	
	private UserManager mUserManager = null;
	private User mLocalUser;

	private static final int MSG_USER_CONNECTED = 1;
	private static final int MSG_USER_DISCONNECTED = 2;
	
	private NotificationMgr mNotificationMgr = null;
	private SocketCommunicationManager mSocketComMgr;
	//main ui frame view
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_USER_CONNECTED:
				updateNetworkStatus();
				updateNotification();
				break;
			case MSG_USER_DISCONNECTED:
				updateNetworkStatus();
				updateNotification();
				break;

			default:
				break;
			}
		}

	};

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		mMainFrameView = getLayoutInflater().inflate(R.layout.ui_mainframe, null);
		mContainLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.ui_main_fragment, null);
				
		if (null != savedInstanceState) {
			Log.d(TAG, "savedInstanceState is not null");
			mStatus = savedInstanceState.getInt(STATUS);
		}
		if (STATUS_MAIN == mStatus) {
			setContentView(mMainFrameView);
		}else {
			setContentView(mContainLayout);
		}
		
//		setContentView(mContainLayout);
		getSupportActionBar().hide();
		
		mNotice = new Notice(this);
		UserHelper userHelper = new UserHelper(this);
		mLocalUser = userHelper.loadUser();

//		mContainLayout = (RelativeLayout) findViewById(R.id.rl_main_fragment);
		initTitle(mContainLayout);
		initMainFrameView(mMainFrameView);

		int position = getIntent().getIntExtra("position", 0);
		viewPager = (ViewPager) mContainLayout.findViewById(R.id.vp_main_frame);
		// 考虑到内存消耗问题，缓存页面不应该设置这么大
		viewPager.setOffscreenPageLimit(6);

		addFragments();
//		setCurrentItem(position);
		
		mNotificationMgr = new NotificationMgr(this);
		mNotificationMgr.showNotificaiton(NotificationMgr.STATUS_UNCONNECTED);

		mSocketComMgr = SocketCommunicationManager.getInstance(this);
		mUserManager = UserManager.getInstance();
		mUserManager.registerOnUserChangedListener(this);
		
		MainUIFrame.startFileTransferService(this);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		Log.d(TAG, "onSaveInstanceState.status=" + mStatus);
		outState.putInt(STATUS, mStatus);
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Log.d(TAG, "onResume");
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		Log.d(TAG, "onStart");
	}
	
	private void updateNetworkStatus() {
		if (mSocketComMgr.isConnected()) {
			mNetWorkStatusView.setText(R.string.connected);
		} else {
			mNetWorkStatusView.setText(R.string.unconnected);
		}
	}
	
	private void updateNotification() {
		if (mSocketComMgr.isConnected()) {
			mNotificationMgr.updateNotification(NotificationMgr.STATUS_CONNECTED);
		} else {
			mNotificationMgr.updateNotification(NotificationMgr.STATUS_UNCONNECTED);
		}
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

	private void initTitle(View rootView) {
		mCustomTitleView = rootView.findViewById(R.id.rl_title);
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
	
	public void initMainFrameView(View rootView) {
		mUserIconView = (ImageView) rootView.findViewById(R.id.iv_usericon);
		mTransferIV = (ImageView) rootView.findViewById(R.id.iv_filetransfer);
		mSettingIV = (ImageView) rootView.findViewById(R.id.iv_setting);
		mUserNameView = (TextView) rootView.findViewById(R.id.tv_username);
		mUserNameView.setText(mLocalUser.getUserName());
		mNetWorkStatusView = (TextView) rootView.findViewById(R.id.tv_network_status);
		mUserIconView.setOnClickListener(this);
		mTransferIV.setOnClickListener(this);
		mSettingIV.setOnClickListener(this);

		mGridView = (GridView) rootView.findViewById(R.id.gv_main_menu);
		mAdapter = new MainUIAdapter(this, mGridView);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);
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
		mTitleIconView.setImageResource(TITLE_ICON_resIDs[position]);
		mTitleNameView.setText(TITLE_resIDs[position]);
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
			Log.d(TAG, "KEYCODE_BACK.status=" + mStatus);
			if (STATUS_MAIN == mStatus) {
				showExitDialog();
				return true;
			}
			
			boolean ret = getBaseFragment().onBackPressed();
			if (ret) {
				setContentView(mMainFrameView);
				mStatus = STATUS_MAIN;
			}
			return true;
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
		case R.id.iv_usericon:
			Intent intent = new Intent();
			intent.setClass(this, UserInfoSetting.class);
			startActivityForResult(intent,
					DreamConstant.REQUEST_FOR_MODIFY_NAME);
			break;
		case R.id.iv_filetransfer:
			setCurrentItem(6);
			setContentView(mContainLayout);
			mStatus = STATUS_ITEM;
			break;
		case R.id.iv_setting:
			MainUIFrame.startActivity(this, SettingsActivity.class);
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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (1 == position) {
			MainUIFrame.startActivity(this, NetworkActivity.class);
			return;
		}else if (2 == position) {
			MainUIFrame.startActivity(this, RecommendActivity.class);
			return;
		}else if(position >= 3){
			position = position -2;
		}
		setCurrentItem(position);
		setContentView(mContainLayout);
		mStatus = STATUS_ITEM;
	}

	@Override
	public void onUserConnected(User user) {
		mHandler.sendEmptyMessage(MSG_USER_CONNECTED);
	}

	@Override
	public void onUserDisconnected(User user) {
		mHandler.sendEmptyMessage(MSG_USER_DISCONNECTED);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {// when create server ,set result ok
			if (DreamConstant.REQUEST_FOR_MODIFY_NAME == requestCode) {
				String name = data.getStringExtra("user");
				mUserNameView.setText(name);
			}
		}
	}
	
	private void showExitDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.exit_app)
				.setMessage(R.string.confirm_exit)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mNotificationMgr.cancelNotification();
								MainFragmentActivity.this.finish();
							}
						})
				.setNeutralButton(R.string.hide,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								moveTaskToBack(true);
								mNotificationMgr
										.updateNotification(NotificationMgr.STATUS_DEFAULT);
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.create().show();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Log.d(TAG, "onPause");
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Log.d(TAG, "onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		// unregister listener
		mUserManager.unregisterOnUserChangedListener(this);
		// stop file transfer service
		MainUIFrame.stopTransferService(this);
		// modify history db
		MainUIFrame.modifyHistoryDb(this);
		// when finish，cloase all connect
		User tem = UserManager.getInstance().getLocalUser();
		tem.setUserID(0);
		mSocketComMgr.closeAllCommunication();
		// Disable wifi AP.
		NetWorkUtil.setWifiAPEnabled(this, null, false);
		// Clear wifi connect history.
		NetWorkUtil.clearWifiConnectHistory(this);
		// Stop record log and close log file.
		Log.stopAndSave();
	}
}
