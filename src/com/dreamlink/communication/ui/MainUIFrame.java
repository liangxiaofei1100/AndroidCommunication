package com.dreamlink.communication.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.AllowLoginDialog.AllowLoginCallBack;
import com.dreamlink.communication.CallBacks.ILoginRequestCallBack;
import com.dreamlink.communication.CallBacks.ILoginRespondCallback;
import com.dreamlink.communication.AllowLoginDialog;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.ui.app.AppFragmentActivity;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.FileFragmentActivity;
import com.dreamlink.communication.ui.file.RemoteShareActivity;
import com.dreamlink.communication.ui.image.ImageFragmentActivity;
import com.dreamlink.communication.ui.media.MediaFragmentActivity;
import com.dreamlink.communication.ui.service.FileManagerService;
import com.dreamlink.communication.ui.service.FileManagerService.ServiceBinder;
import com.dreamlink.communication.util.Log;

import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 虽然ActivityGroup已经过时了，由于Fragment不太好做嵌套，所以第一层仍然使用ActivityGroup，第二层才使用Fragment
 * 主界面框架 分三部分 最上面是标题栏 中间是MainTabContainer，内容存储器 最下面是导航栏，仿闪存，目前有 应用，图片，影音，文件，四个
 */
public class MainUIFrame extends ActivityGroup implements OnClickListener, ILoginRequestCallBack, ILoginRespondCallback {
	private static final String TAG = "MainUIFrame";

	// container layout
	private LinearLayout mContainerLayout = null;
	private LocalActivityManager mActivityManager = null;
	private Intent mTabIntent = null;

	private ImageView mAppBtn, mPicBtn, mMediaBtn, mFileBtn, mShareBtn, mAnimImg;
	/**navgation linearlayout*/
	private LinearLayout mAppLayout,mPictureLayout,mMediaLayout,mFileLayout,mShareLayout;

	private int zero = 0;// 动画图片偏移量
	private int currIndex = 0;// 当前页卡编号
	private int one;// 单个水平动画位移
	private int two;
	private int three;
	private int four;

	private ViewPager mTabPager;
	private MyPageAdapter mPagerAdapter;

	// title views
	// title left
	private RelativeLayout mTitleLeftLayout;
	private ImageView mUserIconView;
	private TextView mUserNameView;
	// title right
	private RelativeLayout mTitleRightLayout;
	/** show invite,close icons */
	private ImageView mRightIconView;
	/** show like:invite,close */
	private TextView mRightTextView;
	// title center，显示正在创建，或已经创建完成
	private TextView mConnectView;
	private ProgressBar mProgressBar;
	private TextView mProgressTipView;
	/**load bg view*/
	private LinearLayout loadView;
	/**main view*/
	private RelativeLayout mainView;
	/**show user info view*/
	private SlowHorizontalScrollView mUserInfoView;
	/**show connect info/status view*/
	private LinearLayout mConnectInfoView;
	/**connect button view*/
	private FrameLayout mConnectLayout;
	
	private UserTabManager mUserTabManager;

	private Context mContext;

	private static final String APP = "APP";
	private static final String PICTURE = "PICTURE";
	private static final String MEDIA = "MEDIA";
	private static final String FILE = "FILE";
	private static final String SHARE = "SHARE";

	private static final int REQUEST_FOR_MODIFY_NAME = 0x128;
	private static final int REQUEST_FOR_CONNECT = 0x129;

	public static final String DB_PATH = "/data" + Environment.getDataDirectory().getAbsolutePath() + "/com.dreamlink.communication"
			+ "/databases";

	// user info
	private UserHelper mUserHelper = null;
	private UserManager mUserManager = null;
	private User mUser = null;
	private ConcurrentHashMap<Integer, User> mUsers = new ConcurrentHashMap<Integer, User>();
	private SocketCommunicationManager mSocketComMgr;
	
	public static final String EXIT_ACTION = "intent.exit.aciton";
	
	private FileManagerService mService = null;
	private boolean isServiceStarted = false;
	private boolean isServiceBinded = false;
	
	private static final int INIT = 0x00;
	private static final int CONNECTING = 0x01;
	private static final int CREATING = 0x02;
	private static final int CREATE_OK = 0x03;
	private static final int CONNECT_OK = 0x04;
	
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			mService.disconnected(this.getClass().getName());
			Log.w(TAG, "onServiceDisconnected");
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onServiceConnected");
			mService = ((ServiceBinder)service).getServiceInstance();
			serviceConnected();
		}
	};
	
	protected void serviceConnected(){
		Log.i(TAG, "serviceConnected");
	}
	
	private ExitReceiver exitReceiver = new ExitReceiver();
	private class ExitReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			MainUIFrame.this.finish();
		}
	}
	
	private static final int LOADING = 0x111;
	private static final int LOADED  = 0x112;
	private Handler mHandler = new Handler(){
		Timer mTimer = new Timer();
		TimerTask mTask = new TimerTask() {
			@Override
			public void run() {
				sendEmptyMessageDelayed(LOADED, 1000);
			}
		};
		
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case LOADING:
				loadView.setVisibility(View.VISIBLE);
				mainView.setVisibility(View.INVISIBLE);
				mTimer.schedule(mTask, 1500);
				break;
			case LOADED:
				loadView.setVisibility(View.INVISIBLE);
				mainView.setVisibility(View.VISIBLE);
				break;
			default:
				break;
			}
		};
	};

	
	private View rooView = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		LayoutInflater inflater = LayoutInflater.from(this);
		rooView = inflater.inflate(R.layout.ui_main2, null);
		setContentView(rooView);
//		setContentView(R.layout.ui_main2);

		mContext = this;
		mActivityManager = getLocalActivityManager();
		
		loadView = (LinearLayout) findViewById(R.id.load_view);
		mainView = (RelativeLayout) findViewById(R.id.main_view);
		mHandler.sendEmptyMessage(LOADING);

		mUserHelper = new UserHelper(mContext);
		mUser = mUserHelper.loadUser();
		mUserManager = UserManager.getInstance();
		
		mSocketComMgr = SocketCommunicationManager.getInstance(mContext);
		mSocketComMgr.setLoginRequestCallBack(this);
		mSocketComMgr.setLoginRespondCallback(this);
		
		IntentFilter filter = new IntentFilter(EXIT_ACTION);
		registerReceiver(exitReceiver, filter);

		importDb();
		initTab();
		initView();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "MainUiFrame.onStart");
//		Intent intent = new Intent(mContext, FileManagerService.class);
//		if (null == startService(intent)) {
//			Log.e(TAG, "Error:can not start FileManagerService");
//			return;
//		}
//		
//		isServiceStarted = true;
//		isServiceBinded = bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
//		if (!isServiceBinded) {
//			Log.e(TAG, "cannot bind FileManagerService");
//			return;
//		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		/*********TEST************/
//		mConnectInfoView.setVisibility(View.INVISIBLE);
//		mConnectLayout.setVisibility(View.INVISIBLE);
//		mUserInfoView.setVisibility(View.VISIBLE);
//		
//		User user = null;
//		for (int i = 0; i < 10; i++) {
//			user = new User();
//			user.setUserID(i);
//			user.setUserName("测试用户" + i);
//			mUsers.put(i, user);
//		}
//		mUserTabManager.refreshTab(mUsers);
		/*********TEST************/
		
	}
	//import game key db
	private void importDb() {
		// copy game_app.db to database
		if (!new File(DB_PATH).exists()) {
			if (new File(DB_PATH).mkdirs()) {
			} else {
				Log.e(TAG, "can not create " + DB_PATH);
			}
		}

		String dbstr = DB_PATH + "/" + MetaData.DATABASE_NAME;
		File dbFile = new File(dbstr);
		if (dbFile.exists()) {
			Log.d(TAG, dbstr + " is exist");
			return;
		}

		// import
		InputStream is;
		try {
			is = getResources().openRawResource(R.raw.game_app);
			FileOutputStream fos = new FileOutputStream(dbFile);
			byte[] buffer = new byte[4 * 1024];
			int count = 0;
			while ((count = is.read(buffer)) > 0) {
				fos.write(buffer, 0, count);
			}
			fos.close();// 关闭输出流
			is.close();// 关闭输入流
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	// init tab item
	private void initTab() {
		// four modules,and a animation image
		mAppBtn = (ImageView) findViewById(R.id.img_app);
		mPicBtn = (ImageView) findViewById(R.id.img_pictures);
		mMediaBtn = (ImageView) findViewById(R.id.img_media);
		mFileBtn = (ImageView) findViewById(R.id.img_file);
		mShareBtn = (ImageView) findViewById(R.id.img_share);
		mAnimImg = (ImageView) findViewById(R.id.img_tab_now);
		
		mAppLayout = (LinearLayout) findViewById(R.id.app_layout);
		mPictureLayout = (LinearLayout) findViewById(R.id.picture_layout);
		mMediaLayout = (LinearLayout) findViewById(R.id.media_layout);
		mFileLayout = (LinearLayout) findViewById(R.id.file_layout);
		mShareLayout = (LinearLayout) findViewById(R.id.share_layout);
		
		mAppLayout.setOnClickListener(new MyOnClickListener(0));
		mPictureLayout.setOnClickListener(new MyOnClickListener(1));
		mMediaLayout.setOnClickListener(new MyOnClickListener(2));
		mFileLayout.setOnClickListener(new MyOnClickListener(3));
		mShareLayout.setOnClickListener(new MyOnClickListener(4));

		// 获取屏幕当前分辨率
		Display currDisplay = getWindowManager().getDefaultDisplay();
		int displayWidth = currDisplay.getWidth();
		int displayHeight = currDisplay.getHeight();
		// 将屏幕等分成5份
		one = displayWidth / 5; // 设置水平动画平移大小
		two = one * 2;
		three = one * 3;
		four = one * 4;
		Log.d("info", "获取的屏幕分辨率为：" + "\n" + displayWidth + one + "\n" + two + "\n" + three + "\n" + four + "\n" + "X" + displayHeight);

		// 每个页面的view数据
		final ArrayList<View> views = new ArrayList<View>();
		Intent appIntent = new Intent(mContext, AppFragmentActivity.class);
		views.add(getView(APP, appIntent));
		Intent pictureIntent = new Intent(mContext, ImageFragmentActivity.class);
		views.add(getView(PICTURE, pictureIntent));
		Intent mediaIntent = new Intent(mContext, MediaFragmentActivity.class);
		views.add(getView(MEDIA, mediaIntent));
		Intent fileIntent = new Intent(mContext, FileFragmentActivity.class);
		views.add(getView(FILE, fileIntent));
		Intent shareIntent = new Intent(mContext, RemoteShareActivity.class);
		views.add(getView(SHARE, shareIntent));

		mTabPager = (ViewPager) findViewById(R.id.tabpager);
		mTabPager.setOnPageChangeListener(new MyOnPageChangeListener());
		mPagerAdapter = new MyPageAdapter(views);
		mTabPager.setCurrentItem(0);
		mTabPager.setAdapter(mPagerAdapter);
	}

	private void initView() {
		mUserIconView = (ImageView) findViewById(R.id.user_icon_imageview);
		mUserNameView = (TextView) findViewById(R.id.user_name_textview);
		mUserNameView.setText(mUser.getUserName());

		mConnectView = (TextView) findViewById(R.id.connect_button);
		mProgressBar = (ProgressBar) findViewById(R.id.connect_progress);
		mProgressTipView = (TextView) findViewById(R.id.connect_progress_tip);

		mTitleRightLayout = (RelativeLayout) findViewById(R.id.title_right_layout);
		mRightIconView = (ImageView) findViewById(R.id.invite_icon_view);
		mRightTextView = (TextView) findViewById(R.id.invite_name_view);
		mTitleRightLayout.setOnClickListener(this);

		mTitleLeftLayout = (RelativeLayout) findViewById(R.id.title_left_layout);
		mTitleLeftLayout.setOnClickListener(this);

		mConnectView.setOnClickListener(this);
		
		/////////////
		mConnectInfoView = (LinearLayout) findViewById(R.id.show_connect_msg_layout);
		mConnectLayout = (FrameLayout) findViewById(R.id.connect_button_layout);
		mUserInfoView = (SlowHorizontalScrollView) findViewById(R.id.show_user_scrollview);
		
		if (mUserInfoView != null) {
			mUserInfoView.setVerticalScrollBarEnabled(false);
			mUserInfoView.setHorizontalScrollBarEnabled(false);
			mUserTabManager = new UserTabManager(mContext, rooView, mUserInfoView);
		}
		
		updateConnectUI(INIT);
		////////////////
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
			((ViewPager) container).addView(views.get(position));
			return views.get(position);
		}

	}

	/** page change listener */
	public class MyOnPageChangeListener implements OnPageChangeListener {
		@Override
		public void onPageSelected(int arg0) {
			Animation animation = null;
			switch (arg0) {
			case 0:
				mAppBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_app_selected));
				if (currIndex == 1) {
					animation = new TranslateAnimation(one, 0, 0, 0);
					mPicBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_pic_normal));
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, 0, 0, 0);
					mMediaBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_media_normal));
				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, 0, 0, 0);
					mFileBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_file_normal));
				} else if (currIndex == 4) {
					animation = new TranslateAnimation(four, 0, 0, 0);
					mShareBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_share_normal));
				}
				break;
			case 1:
				mPicBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_pic_selected));
				if (currIndex == 0) {
					animation = new TranslateAnimation(zero, one, 0, 0);
					mAppBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_app_normal));
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, one, 0, 0);
					mMediaBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_media_normal));
				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, one, 0, 0);
					mFileBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_file_normal));
				} else if (currIndex == 4) {
					animation = new TranslateAnimation(four, one, 0, 0);
					mShareBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_share_normal));
				}
				break;
			case 2:
				mMediaBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_media_selected));
				if (currIndex == 0) {
					animation = new TranslateAnimation(zero, two, 0, 0);
					mAppBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_app_normal));
				} else if (currIndex == 1) {
					animation = new TranslateAnimation(one, two, 0, 0);
					mPicBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_pic_normal));
				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, two, 0, 0);
					mFileBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_file_normal));
				} else if (currIndex == 4) {
					animation = new TranslateAnimation(four, two, 0, 0);
					mShareBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_share_normal));
				}
				break;
			case 3:
				mFileBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_file_selected));
				if (currIndex == 0) {
					animation = new TranslateAnimation(zero, three, 0, 0);
					mAppBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_app_normal));
				} else if (currIndex == 1) {
					animation = new TranslateAnimation(one, three, 0, 0);
					mPicBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_pic_normal));
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, three, 0, 0);
					mMediaBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_media_normal));
				} else if (currIndex == 4) {
					animation = new TranslateAnimation(four, three, 0, 0);
					mShareBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_share_normal));
				}
				break;

			case 4:
				mShareBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_share_selected));
				if (currIndex == 0) {
					animation = new TranslateAnimation(zero, four, 0, 0);
					mAppBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_app_normal));
				} else if (currIndex == 1) {
					animation = new TranslateAnimation(one, four, 0, 0);
					mPicBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_pic_normal));
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, four, 0, 0);
					mMediaBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_media_normal));
				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, four, 0, 0);
					mFileBtn.setImageDrawable(getResources().getDrawable(R.drawable.main_tab_file_normal));
				}
				break;
			}
			currIndex = arg0;
			animation.setFillAfter(true);// True:图片停在动画结束位置
			animation.setDuration(150);
			mAnimImg.startAnimation(animation);
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}

	/**
	 * Item click listener
	 */
	public class MyOnClickListener implements View.OnClickListener {
		private int index = 0;

		public MyOnClickListener(int i) {
			index = i;
		}

		@Override
		public void onClick(View v) {
			mTabPager.setCurrentItem(index);
		}
	};

	/** get tab item view */
	private View getView(String id, Intent intent) {
		return mActivityManager.startActivity(id, intent).getDecorView();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.connect_button:
			Intent intent = new Intent();
			intent.setClass(MainUIFrame.this, ConnectFriendActivity.class);
			startActivityForResult(intent, REQUEST_FOR_CONNECT);
			break;

		case R.id.title_left_layout:
			Intent intent2 = new Intent();
			intent2.setClass(MainUIFrame.this, UserInfoSetting.class);
			startActivityForResult(intent2, REQUEST_FOR_MODIFY_NAME);
			break;

		case R.id.title_right_layout:

			break;

		default:
			break;
		}
	}
	
	private void updateConnectUI(int status){
		switch (status) {
		case INIT:
			mConnectLayout.setVisibility(View.VISIBLE);
			mConnectInfoView.setVisibility(View.INVISIBLE);
			mUserIconView.setVisibility(View.INVISIBLE);
			mRightIconView.setImageResource(R.drawable.btn_title_invite_pressed);
			break;
		case CREATING:
		case CONNECTING:
		case CREATE_OK:
			mConnectLayout.setVisibility(View.INVISIBLE);
			mConnectInfoView.setVisibility(View.VISIBLE);
			mUserIconView.setVisibility(View.INVISIBLE);
			mRightIconView.setImageResource(R.drawable.btn_title_help_close);
			break;
		case CONNECT_OK:
			mConnectLayout.setVisibility(View.INVISIBLE);
			mConnectInfoView.setVisibility(View.INVISIBLE);
			mUserIconView.setVisibility(View.VISIBLE);
			mRightIconView.setImageResource(R.drawable.btn_title_help_close);
			break;

		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_FOR_MODIFY_NAME) {
				String name = data.getStringExtra("user");
				mUserNameView.setText(name);
			}else if (REQUEST_FOR_CONNECT == requestCode) {
				//connect to friend
				//判断server创建的状态，并显示在UI上
				//更新UI
				//mProgressBar，mProgressBarTip
			}
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (isServiceBinded) {
			unbindService(mServiceConnection);
			isServiceBinded = false;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(exitReceiver);
	}

	@Override
	public void onLoginRequest(User user, SocketCommunication communication) {
			AllowLoginDialog dialog = new AllowLoginDialog(mContext);
			AllowLoginCallBack callBack = new AllowLoginCallBack() {

				@Override
				public void onLoginComfirmed(User user,
						SocketCommunication communication, boolean isAllow) {
					mSocketComMgr.respondLoginRequest(user, communication,
							isAllow);
				}
			};
			dialog.show(user, communication, callBack);
	}

	@Override
	public void onLoginSuccess(User localUser, SocketCommunication communication) {
		// TODO Auto-generated method stub
		String nameString = localUser.getUserName();
		updateConnectUI(CONNECT_OK);
		mUsers = (ConcurrentHashMap<Integer, User>) mUserManager.getAllUser();
		mUserTabManager.refreshTab(mUsers);
	}

	@Override
	public void onLoginFail(int failReason, SocketCommunication communication) {
		// TODO Auto-generated method stub
		
	}
}
