package com.dreamlink.communication.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.ui.app.AppFragmentActivity;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.FileFragmentActivity;
import com.dreamlink.communication.ui.file.RemoteShareActivity;
import com.dreamlink.communication.ui.image.ImageFragmentActivity;
import com.dreamlink.communication.ui.media.MediaFragmentActivity;
import com.dreamlink.communication.util.Log;

import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 虽然ActivityGroup已经过时了，由于Fragment不太好做嵌套，所以第一层仍然使用ActivityGroup，第二层才使用Fragment
 * 主界面框架 分三部分 最上面是标题栏 中间是MainTabContainer，内容存储器 最下面是导航栏，仿闪存，目前有 应用，图片，影音，文件，四个
 */
public class MainUIFrame extends ActivityGroup implements OnClickListener {
	private static final String TAG = "MainUIFrame";

	// container layout
	private LinearLayout mContainerLayout = null;
	private LocalActivityManager mActivityManager = null;
	private Intent mTabIntent = null;

	private ImageView mAppBtn, mPicBtn, mMediaBtn, mFileBtn, mShareBtn, mAnimImg;

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
	private User mUser = null;
	
	public static final String EXIT_ACTION = "intent.exit.aciton";
	private ExitReceiver exitReceiver = new ExitReceiver();
	private class ExitReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			MainUIFrame.this.finish();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_main);

		mContext = this;
		mActivityManager = getLocalActivityManager();

		mUserHelper = new UserHelper(mContext);
		mUser = mUserHelper.loadUser();
		
		IntentFilter filter = new IntentFilter(EXIT_ACTION);
		registerReceiver(exitReceiver, filter);

		importDb();
		initTab();
		initView();

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

		mAppBtn.setOnClickListener(new MyOnClickListener(0));
		mPicBtn.setOnClickListener(new MyOnClickListener(1));
		mMediaBtn.setOnClickListener(new MyOnClickListener(2));
		mFileBtn.setOnClickListener(new MyOnClickListener(3));
		mShareBtn.setOnClickListener(new MyOnClickListener(4));

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
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(exitReceiver);
	}
}
