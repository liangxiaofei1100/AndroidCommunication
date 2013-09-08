package com.dreamlink.communication.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.AllowLoginDialog.AllowLoginCallBack;
import com.dreamlink.communication.CallBacks.ILoginRequestCallBack;
import com.dreamlink.communication.CallBacks.ILoginRespondCallback;
import com.dreamlink.communication.AllowLoginDialog;
import com.dreamlink.communication.MainActivity;
import com.dreamlink.communication.NetworkStatus;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.UserManager.OnUserChangedListener;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.notification.NotificationMgr;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.app.AppFragmentActivity;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.dialog.ExitActivity;
import com.dreamlink.communication.ui.file.FileFragmentActivity;
import com.dreamlink.communication.ui.file.RemoteShareActivity;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.ui.image.ImageFragmentActivity;
import com.dreamlink.communication.ui.invite.InviteMainActivity;
import com.dreamlink.communication.ui.media.MediaFragmentActivity;
import com.dreamlink.communication.ui.service.FileManagerService;
import com.dreamlink.communication.ui.service.FileManagerService.ServiceBinder;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;
import com.readystatesoftware.viewbadger.BadgeView;

import android.app.ActivityGroup;
import android.app.AlertDialog;
import android.app.LocalActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 虽然ActivityGroup已经过时了，由于Fragment不太好做嵌套，所以第一层仍然使用ActivityGroup，第二层才使用Fragment
 * 主界面框架 分三部分 最上面是标题栏 中间是MainTabContainer，内容存储器 最下面是导航栏，仿闪存，目前有 应用，图片，影音，文件，四个
 */
public class MainUIFrame extends ActivityGroup implements OnClickListener, ILoginRequestCallBack, ILoginRespondCallback, OnUserChangedListener {
	private static final String TAG = "MainUIFrame";

	// container layout
	private LinearLayout mContainerLayout = null;
	private LocalActivityManager mActivityManager = null;
	private Intent mTabIntent = null;

	//Tab button in buttom
	private ImageView mAppBtn, mPicBtn, mMediaBtn, mFileBtn, mHistoryBtn, mAnimImg;
	/**tab badge*/
	private BadgeView mAppBadge,mPicBadge,mMediaBadge,mFileBadge,mHistoryBadge;
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
	// title right,view invite & close button layout
	private RelativeLayout mTitleRightLayout;
	/** show invite,close icons */
	private ImageView mRightIconView;
	/** show like:invite,close */
	private TextView mRightTextView;
	/**connect button*/
	private TextView mConnectView;
	/**show connectiing,creating,create_ok msg and so on.*/
	private TextView mProgressTipView;
	/** load bg view */
	private LinearLayout loadView;
	/** main view */
	private RelativeLayout mainView;
	/**show user info view*/
	private SlowHorizontalScrollView mUserInfoView;
	/**show connect info/status view*/
	private LinearLayout mConnectInfoView;
	/**connect button view*/
	private FrameLayout mConnectLayout;
	
	private UserTabManager mUserTabManager;

	private Context mContext;
	public static MainUIFrame instance;

	private static final String APP = "APP";
	private static final String PICTURE = "PICTURE";
	private static final String MEDIA = "MEDIA";
	private static final String FILE = "FILE";
	private static final String SHARE = "SHARE";

	private static final int REQUEST_FOR_MODIFY_NAME = 0x128;
	private static final int REQUEST_FOR_CONNECT = 0x129;

	public static final String DB_PATH = "/data"
			+ Environment.getDataDirectory().getAbsolutePath()
			+ "/com.dreamlink.communication" + "/databases";

	// user info
	private UserHelper mUserHelper = null;
	private UserManager mUserManager = null;
	private User mUser = null;
	private ConcurrentHashMap<Integer, User> mUsers = new ConcurrentHashMap<Integer, User>();
	private SocketCommunicationManager mSocketComMgr;
	
	private Notice mNotice = null;
	
	private FileManagerService mService = null;
	private boolean isServiceStarted = false;
	private boolean isServiceBinded = false;
	
	private static final int INIT = 0x00;
	public static final int CONNECTING = 0x01;
	public static final int CREATING = 0x02;
	private static final int CREATE_OK = 0x03;
	private static final int CONNECT_OK = 0x04;
	
	private static final int MSG_REFRESH_USER_LIST = 0x05;
	
	private int mCurrentStatus = -1;
	
	private SharedPreferences sp = null;
	/**am i is server?*/
	private boolean mIsServer = false;
	
	/**save users*/
	private List<User> mUserLists = new ArrayList<User>();
	
	private NotificationMgr mNotificationMgr = null;
	/**is mainui exit*/
	private boolean mIsExit = false;
	
	/**@unuse*/
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
			mService = ((ServiceBinder) service).getServiceInstance();
			serviceConnected();
		}
	};

	protected void serviceConnected() {
		Log.i(TAG, "serviceConnected");
	}

	
	private BroadcastReceiver mainReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (DreamConstant.EXIT_ACTION.equals(action)) {
				showExitDialog();
			}else if (DreamConstant.SERVER_CREATED_ACTION.equals(action)) {
				mIsServer = true;
				mCurrentStatus = CREATE_OK;
				mHandler.sendMessage(mHandler.obtainMessage(MSG_REFRESH_USER_LIST));
			}
		}
	};

	/**update ui handler*/
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			/***/
			switch (mCurrentStatus) {
			case CONNECTING:
			case CREATING:
			case CREATE_OK:
			case INIT:
				updateUserListUI();
				break;
			case CONNECT_OK:
				mUsers = (ConcurrentHashMap<Integer, User>) mUserManager.getAllUser();
				Log.d(TAG, "is server = " + mIsServer + "  mUsers.size:" + mUsers.size());
				if (mIsServer && mUsers.size() <= 1) {
					mCurrentStatus = CREATE_OK;
				}else if (!mIsServer && mUsers.size() <= 0) {
					mCurrentStatus = INIT;
				}else {
					mCurrentStatus = CONNECT_OK;
					mUserLists.clear();
					//we do not add locat user into map
					User locaUser = mUserManager.getLocalUser();
					for (Map.Entry<Integer, User> entry : mUsers.entrySet()) {
						User user = entry.getValue();
						Log.i(TAG, "user.id=" + user.getUserID());
						Log.i(TAG, "user.name=" + user.getUserName());
						if (locaUser.getUserID() != user.getUserID()) {
							mUserLists.add(user);
						}
					}
					Log.d(TAG, "mUsers.size:" + mUsers.size());
					mUserTabManager.refreshTab(mUserLists);
				}
				Log.i(TAG, "mCurrentStatus=" + mCurrentStatus);
				updateUserListUI();
				break;
			default:
				updateUserListUI(mCurrentStatus);
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
		rooView = inflater.inflate(R.layout.ui_main, null);
		setContentView(rooView);
		
		//make the vertiual menu key visible
		try {
			getWindow().addFlags(WindowManager.LayoutParams.class.getField("FLAG_NEEDS_MENU_KEY").getInt(null));
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mContext = this;
		instance = this;
		mActivityManager = getLocalActivityManager();
		
		mNotificationMgr = new NotificationMgr(MainUIFrame.this);
		mNotificationMgr.showNotificaiton(NotificationMgr.STATUS_UNCONNECTED);
		
		//get sdcards
		MountManager mountManager = new MountManager();
		mountManager.init();
		
		
		mUserHelper = new UserHelper(mContext);
		mUser = mUserHelper.loadUser();
		mUserManager = UserManager.getInstance();
		mUserManager.registerOnUserChangedListener(this);
		
		mNotice = new Notice(mContext);
		
		mSocketComMgr = SocketCommunicationManager.getInstance(mContext);
		mSocketComMgr.setLoginRequestCallBack(this);
		mSocketComMgr.setLoginRespondCallback(this);
		
		IntentFilter filter = new IntentFilter(DreamConstant.EXIT_ACTION);
		filter.addAction(DreamConstant.SERVER_CREATED_ACTION);
		registerReceiver(mainReceiver, filter);

		importDb();
		initTab();
		initView();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "MainUiFrame.onStart");
		// Intent intent = new Intent(mContext, FileManagerService.class);
		// if (null == startService(intent)) {
		// Log.e(TAG, "Error:can not start FileManagerService");
		// return;
		// }
		//
		// isServiceStarted = true;
		// isServiceBinded = bindService(intent, mServiceConnection,
		// BIND_AUTO_CREATE);
		// if (!isServiceBinded) {
		// Log.e(TAG, "cannot bind FileManagerService");
		// return;
		// }
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");
		mHandler.sendMessage(mHandler.obtainMessage(MSG_REFRESH_USER_LIST));
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
		mAppBadge = new BadgeView(mContext, mAppBtn);
		mPicBtn = (ImageView) findViewById(R.id.img_pictures);
		mPicBadge = new BadgeView(mContext, mPicBtn);
		mMediaBtn = (ImageView) findViewById(R.id.img_media);
		mMediaBadge = new BadgeView(mContext, mMediaBtn);
		mFileBtn = (ImageView) findViewById(R.id.img_file);
		mFileBadge = new BadgeView(mContext, mFileBtn);
		mHistoryBtn = (ImageView) findViewById(R.id.img_history);
		mHistoryBadge = new BadgeView(mContext, mHistoryBtn);
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
		Log.d("info", "获取的屏幕分辨率为：" + "\n" + displayWidth + one + "\n" + two
				+ "\n" + three + "\n" + four + "\n" + "X" + displayHeight);

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
		Intent shareIntent = new Intent(mContext, HistoryActivity.class);
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
		mProgressTipView = (TextView) findViewById(R.id.connect_progress_tip);

		mTitleRightLayout = (RelativeLayout) findViewById(R.id.title_right_layout);
		mRightIconView = (ImageView) findViewById(R.id.invite_icon_view);
		mRightTextView = (TextView) findViewById(R.id.invite_name_view);
		mTitleRightLayout.setOnClickListener(this);

		mTitleLeftLayout = (RelativeLayout) findViewById(R.id.title_left_layout);
		mTitleLeftLayout.setOnClickListener(this);
		mTitleLeftLayout.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				NetworkStatus status = new NetworkStatus(mContext);
				status.show();
				return true;
			}
		});

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
		
		mCurrentStatus = INIT;
//		updateConnectUI(mCurrentStatus);
		mHandler.sendMessage(mHandler.obtainMessage(MSG_REFRESH_USER_LIST));
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
				mAppBtn.setImageDrawable(getResources().getDrawable(
						R.drawable.main_tab_app_selected));
				if (currIndex == 1) {
					animation = new TranslateAnimation(one, 0, 0, 0);
					mPicBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_pic_normal));
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, 0, 0, 0);
					mMediaBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_media_normal));
				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, 0, 0, 0);
					mFileBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_file_normal));
				} else if (currIndex == 4) {
					animation = new TranslateAnimation(four, 0, 0, 0);
					mHistoryBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_share_normal));
				}
				break;
			case 1:
				mPicBtn.setImageDrawable(getResources().getDrawable(
						R.drawable.main_tab_pic_selected));
				if (currIndex == 0) {
					animation = new TranslateAnimation(zero, one, 0, 0);
					mAppBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_app_normal));
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, one, 0, 0);
					mMediaBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_media_normal));
				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, one, 0, 0);
					mFileBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_file_normal));
				} else if (currIndex == 4) {
					animation = new TranslateAnimation(four, one, 0, 0);
					mHistoryBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_share_normal));
				}
				break;
			case 2:
				mMediaBtn.setImageDrawable(getResources().getDrawable(
						R.drawable.main_tab_media_selected));
				if (currIndex == 0) {
					animation = new TranslateAnimation(zero, two, 0, 0);
					mAppBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_app_normal));
				} else if (currIndex == 1) {
					animation = new TranslateAnimation(one, two, 0, 0);
					mPicBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_pic_normal));
				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, two, 0, 0);
					mFileBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_file_normal));
				} else if (currIndex == 4) {
					animation = new TranslateAnimation(four, two, 0, 0);
					mHistoryBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_share_normal));
				}
				break;
			case 3:
				mFileBtn.setImageDrawable(getResources().getDrawable(
						R.drawable.main_tab_file_selected));
				if (currIndex == 0) {
					animation = new TranslateAnimation(zero, three, 0, 0);
					mAppBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_app_normal));
				} else if (currIndex == 1) {
					animation = new TranslateAnimation(one, three, 0, 0);
					mPicBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_pic_normal));
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, three, 0, 0);
					mMediaBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_media_normal));
				} else if (currIndex == 4) {
					animation = new TranslateAnimation(four, three, 0, 0);
					mHistoryBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_share_normal));
				}
				break;

			case 4:
				mHistoryBtn.setImageDrawable(getResources().getDrawable(
						R.drawable.main_tab_share_selected));
				if (currIndex == 0) {
					animation = new TranslateAnimation(zero, four, 0, 0);
					mAppBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_app_normal));
				} else if (currIndex == 1) {
					animation = new TranslateAnimation(one, four, 0, 0);
					mPicBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_pic_normal));
				} else if (currIndex == 2) {
					animation = new TranslateAnimation(two, four, 0, 0);
					mMediaBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_media_normal));
				} else if (currIndex == 3) {
					animation = new TranslateAnimation(three, four, 0, 0);
					mFileBtn.setImageDrawable(getResources().getDrawable(
							R.drawable.main_tab_file_normal));
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
		Intent intent = null;
		switch (v.getId()) {
		case R.id.connect_button:
			intent = new Intent();
			intent.setClass(MainUIFrame.this, ConnectFriendActivity.class);
			startActivityForResult(intent, REQUEST_FOR_CONNECT);
			break;

		case R.id.title_left_layout:
			intent = new Intent();
			intent.setClass(MainUIFrame.this, UserInfoSetting.class);
			startActivityForResult(intent, REQUEST_FOR_MODIFY_NAME);
			break;

		case R.id.title_right_layout:
			switch (mCurrentStatus) {
			case INIT:
				//invite
				intent = new Intent(mContext, InviteMainActivity.class);
				startActivity(intent);
				break;
			case CREATING:
			case CREATE_OK:
			case CONNECTING:
			case CONNECT_OK:
				new AlertDialog.Builder(mContext)
					.setMessage("确定关闭连接?")
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (mIsServer) {
								mIsServer = false;
							}
							mSocketComMgr.closeAllCommunication();
							mCurrentStatus = INIT;
//							updateConnectUI(mCurrentStatus);
							mHandler.sendMessage(mHandler.obtainMessage(MSG_REFRESH_USER_LIST));
						}
					})
					.setNegativeButton(android.R.string.cancel, null)
					.create().show();
				break;
			default:
				break;
			}
			break;

		default:
			break;
		}
	}
	
	private void updateUserListUI(){
		updateUserListUI(mCurrentStatus);
	}
	
	/**
	 * according to status to update user list ui
	 * @param status
	 */
	private void updateUserListUI(int status){
		switch (status) {
		case INIT:
			Log.i(TAG, "updateConnectUI.INIT");
			mConnectLayout.setVisibility(View.VISIBLE);
			mConnectInfoView.setVisibility(View.INVISIBLE);
			mUserInfoView.setVisibility(View.INVISIBLE);
			mRightIconView.setImageResource(R.drawable.btn_title_invite_pressed);
			mRightTextView.setText(R.string.invite);
			break;
		case CREATING:
			Log.i(TAG, "updateConnectUI.CREATING");
			mConnectLayout.setVisibility(View.INVISIBLE);
			mConnectInfoView.setVisibility(View.VISIBLE);
			mUserInfoView.setVisibility(View.INVISIBLE);
			mRightIconView.setImageResource(R.drawable.btn_title_help_close);
			mRightTextView.setText(R.string.close);
			mProgressTipView.setText(R.string.creating);
			break;
		case CREATE_OK:
			Log.i(TAG, "updateConnectUI.CREATE_OK");
			mConnectLayout.setVisibility(View.INVISIBLE);
			mConnectInfoView.setVisibility(View.VISIBLE);
			mUserInfoView.setVisibility(View.INVISIBLE);
			mRightIconView.setImageResource(R.drawable.btn_title_help_close);
			mRightTextView.setText(R.string.close);
			mProgressTipView.setText(R.string.create_ok);
			break;
		case CONNECTING:
			Log.i(TAG, "updateConnectUI.CONNECTING");
			mConnectLayout.setVisibility(View.INVISIBLE);
			mConnectInfoView.setVisibility(View.VISIBLE);
			mUserInfoView.setVisibility(View.INVISIBLE);
			mRightIconView.setImageResource(R.drawable.btn_title_help_close);
			mRightTextView.setText(R.string.close);
			mProgressTipView.setText(R.string.connecting);
			break;
		case CONNECT_OK:
			Log.i(TAG, "updateConnectUI.CONNECT_OK");
			mConnectLayout.setVisibility(View.INVISIBLE);
			mConnectInfoView.setVisibility(View.INVISIBLE);
			mUserInfoView.setVisibility(View.VISIBLE);
			mRightIconView.setImageResource(R.drawable.btn_title_help_close);
			mRightTextView.setText(R.string.close);
			break;

		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {// when create server ,set result ok
//			mSocketComMgr.startServer(getApplicationContext());
			if (REQUEST_FOR_MODIFY_NAME == requestCode) {
				String name = data.getStringExtra("user");
				mUserNameView.setText(name);
			} else if (REQUEST_FOR_CONNECT == requestCode) {
				int status = -1;
				if (null != data) {
					status = data.getIntExtra("status", -1);
				}
				// 更新UI
				mCurrentStatus = status;
				Log.i(TAG, "onActivityResult.currentstatus=" + mCurrentStatus);
//				updateConnectUI(mCurrentStatus);
//				mHandler.sendMessage(mHandler.obtainMessage(0));
			}
		}
	}
	
	public void showExitDialog(){
		new AlertDialog.Builder(mContext)
			.setTitle(R.string.exit_app)
			.setMessage(R.string.confirm_exit)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mNotificationMgr.cancelNotification();
					mIsExit = true;
					// Disable wifi AP.
					NetWorkUtil.setWifiAPEnabled(mContext, null, false);
					// Clear wifi connect history.
					NetWorkUtil.clearWifiConnectHistory(mContext);
					MainUIFrame.this.finish();
				}
			})
			.setNeutralButton(R.string.hide, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					moveTaskToBack(true);
					mNotificationMgr.updateNotification(NotificationMgr.STATUS_DEFAULT);
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create().show();
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
		unregisterReceiver(mainReceiver);
		//when finish，cloase all connect
		mSocketComMgr.closeAllCommunication();
		
		System.exit(0);
	}
	
	@Override
	public void onLoginSuccess(User localUser, SocketCommunication communication) {
		// TODO Auto-generated method stub
		String nameString = localUser.getUserName();
		Log.d(TAG, "onLoginSuccess");
//		mNotice.showToast("User Login Success!");
		mCurrentStatus = CONNECTING;
		mIsServer = false;
		mHandler.sendMessage(mHandler.obtainMessage(MSG_REFRESH_USER_LIST));
	}

	@Override
	public void onLoginFail(int failReason, SocketCommunication communication) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onLoginFail");
//		mNotice.showToast("User Login Fail.Reason:" + failReason);
	}

	@Override
	public void onLoginRequest(final User user,
			final SocketCommunication communication) {
		// TODO Auto-generated method stub
		//****************test auto allow*************
		if (DreamConstant.UREY_TEST) {
			mSocketComMgr.respondLoginRequest(user, communication, true);
		}
		//****************test auto allow*************
		else {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					showAllowLoginDialog(user, communication);
				}
			});
		}

	}

	private void showAllowLoginDialog(User user,
			SocketCommunication communication) {
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
	public void onUserConnected(User user) {
		mCurrentStatus = CONNECT_OK;
		Log.d(TAG, "onUserConnected:" + user.getUserName());
		Log.i(TAG, "onUserConnected.users.size:" + mUserManager.getAllUser().size());
		Message msg = mHandler.obtainMessage(MSG_REFRESH_USER_LIST);
		msg.obj = user;
		msg.sendToTarget();
		
		mNotificationMgr.updateNotification(NotificationMgr.STATUS_CONNECTED);
	}

	@Override
	public void onUserDisconnected(User user) {
		Log.d(TAG, "onUserDisconnected:" + user.getUserName());
		mHandler.sendMessage(mHandler.obtainMessage(MSG_REFRESH_USER_LIST));
		if (mSocketComMgr.getCommunications().isEmpty()) {
			if (!mIsExit) {
				mNotificationMgr.updateNotification(NotificationMgr.STATUS_UNCONNECTED);
			}
		}
	}
	
	
	/**options menu*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		menu.add(0, 0, 0, "旧入口");
//		menu.add(0, 1, 0, "Share");
		menu.add(0, 2, 0, "远程共享");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			Intent intent = new Intent(mContext, MainActivity.class);
			startActivity(intent);
			break;
		case 1:
			mTabPager.setCurrentItem(4);
			break;
		case 2:
			Intent shareIntent = new Intent(mContext, RemoteShareActivity.class);
			//如果这个activity已经启动了，就不产生新的activity，而只是把这个activity实例加到栈顶来就可以了。
			shareIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);  
			startActivity(shareIntent);
			break;
		default:
			break;
		}
		return true;
	}
}
