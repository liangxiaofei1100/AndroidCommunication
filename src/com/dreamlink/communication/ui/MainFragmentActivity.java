package com.dreamlink.communication.ui;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.UserHelper;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.UserManager.OnUserChangedListener;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.notification.NotificationMgr;
import com.dreamlink.communication.ui.SelectPopupView.SelectItemClickListener;
import com.dreamlink.communication.ui.app.RecommendActivity;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.ui.network.NetworkActivity;
import com.dreamlink.communication.ui.settings.SettingsActivity;
import com.dreamlink.communication.util.Log;

import com.dreamlink.communication.util.NetWorkUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

//各个Fragment的位置必须固定
public class MainFragmentActivity extends FragmentActivity implements
		OnPageChangeListener, OnClickListener, OnItemClickListener, OnUserChangedListener {
	private static final String TAG = "MainFragmentActivity";
	private ViewPager viewPager;
	private MainFragmentPagerAdapter mPagerAdapter;
	private Notice mNotice;

	// define fragment position
	public static final int ZY_TIANDI = 0;
	public static final int IMAGE = 1;
	public static final int AUDIO = 2;
	public static final int VIDEO = 3;
	public static final int APP = 4;
	public static final int GAME = 5;
	public static final int FILE_BROWSER = 6;
	
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
	
	private SparseArray<BaseFragment> mFragmentArray = new SparseArray<BaseFragment>();
	//main ui frame view
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_USER_CONNECTED:
				updateNetworkStatus();
//				updateNotification();
				break;
			case MSG_USER_DISCONNECTED:
				updateNetworkStatus();
//				updateNotification();
				break;

			default:
				break;
			}
		}

	};

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
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
		
		mNotice = new Notice(this);
		UserHelper userHelper = new UserHelper(this);
		mLocalUser = userHelper.loadUser();

		initTitle(mContainLayout);
		initMainFrameView(mMainFrameView);

		viewPager = (ViewPager) mContainLayout.findViewById(R.id.vp_main_frame);
		viewPager.setOffscreenPageLimit(6);
		mPagerAdapter = new MainFragmentPagerAdapter(getSupportFragmentManager(), AppUtil.getAppID(this));
		viewPager.setAdapter(mPagerAdapter);
		viewPager.setOnPageChangeListener(this);
		
		//cancel show notification
//		mNotificationMgr = new NotificationMgr(this);
//		mNotificationMgr.showNotificaiton(NotificationMgr.STATUS_UNCONNECTED);

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
	
	public void addObject(int pos,BaseFragment fragment){
		Log.d(TAG, "pos="+ pos + ",fragment:" + fragment);
		mFragmentArray.put(pos, fragment);
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
		Log.d(TAG, "setCurrentItem.position=" + position);
		viewPager.setCurrentItem(position, false);
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
	
	public void updateTitleSelectNum(int selectCount, int totalCount){
		if (-1 == selectCount) {
			mTitleNumView.setText(getString(R.string.num_format, totalCount));
		}else {
			mTitleNumView.setText("(" + selectCount + "/" + totalCount + ")");
		}
	}
	
	public void setTitleName(int position){
		if (position == viewPager.getCurrentItem()) {
			mTitleIconView.setImageResource(TITLE_ICON_resIDs[position]);
			mTitleNameView.setText(TITLE_resIDs[position]);
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
		BaseFragment baseFragment = getBaseFragment();
		if (null == baseFragment) {
			return;
		}
		switch (position) {
		case ZY_TIANDI:
			mTitleNumView.setText(null);
			break;
		default:
			if (DreamConstant.MENU_MODE_NORMAL == baseFragment.getMenuMode()) {
				mTitleNumView.setText(getString(R.string.num_format,
						baseFragment.getCount()));
			}else {
				int selectCount = baseFragment.getSelectedCount();
				mTitleNumView.setText(getString(R.string.num_format2,selectCount,
						baseFragment.getCount()));
			}
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			Log.d(TAG, "KEYCODE_BACK.status=" + mStatus);
			if (STATUS_MAIN == mStatus) {
				moveTaskToBack(true);
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
		Log.d(TAG, "onPageSelected.position=" + position);
		// when scroll out of PictureFragment,set PictureFragment status to
		// Folder View
//		if (IMAGE != position && null != getBaseFragment(IMAGE)) {
//			((PictureFragment)getBaseFragment(IMAGE)).scrollToHomeView();
//		}
		updateTilte(position);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ll_menu_select:
			SelectPopupView selectPopupView = new SelectPopupView(this);
			selectPopupView.showAsDropDown(mSelectView);
			selectPopupView.setOnSelectedItemClickListener(new SelectItemClickListener() {
				@Override
				public void onItemClick(int position) {
					if (7 == position) {
						showExitDialog();
					}else {
						setCurrentItem(position);
					}
				}
			});
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
	
	/**
	 * get BaseFragment object
	 * @return BaseFragment objcet
	 */
	private BaseFragment getBaseFragment(){
		int position = viewPager.getCurrentItem();
		return getBaseFragment(position);
	}
	
	private BaseFragment getBaseFragment(int position){
		BaseFragment bFragment = mFragmentArray.get(position);
		Log.d(TAG, "getBaseFragment:" + bFragment);
		return bFragment;
	}
	
	private void showExitDialog() {
		new AlertDialog.Builder(this)
				.setMessage(R.string.confirm_exit)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								MainFragmentActivity.this.finish();
							}
						})
				.setNegativeButton(android.R.string.cancel, null)
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
