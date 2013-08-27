package com.dreamlink.communication.ui.dialog;

import java.text.BreakIterator;
import java.text.NumberFormat;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.file.RemoteShareActivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * @unuse 20130822 by yuri
 * @author lianxi
 *
 */
public class FileTransferActivity extends Activity implements OnClickListener{
	/**view progress bar*/
	private ProgressBar mProgressBar;
	/**title view*/
	private TextView mTitleView;
	/**view time*/
	private TextView mTimeView;
	/**view percent*/
	private TextView mPercentView;
	/**view progress num*/
	private TextView mNumberView;
	/**view transfer speed*/
	private TextView mSpeedView;
	/**stop button*/
	private Button mStopButton;
	
	private double dMax;
	private double dProgress;
	private double mSpeed = 0;
	private long mTime;
	private String mTitle = "";
	private String mFileName = "";
	
	private String dProgressStr;
	private String dMaxStr;
	
	private double prev = 0;
	
	private Context mContext;
	
	public static FileTransferActivity mInstance;
	
	public static final int COPYING = 0x00;
	public static final int COPY_OK = 0x01;
	public static final int COPY_FAIL = 0x02;
	private int mCopyState = -1;
	
	public static final int MSG_UPDATE_STATE = 0x00;
	public static final int MSG_UPDATE_UI = 0x01;
	public static final int MSG_UPDATE_TIME = 0x02;
	public static final int MSG_COPYING = 0x03;
	public static final int MSG_COPY_OK = 0x04;
	public static final int MSG_COPY_FAIL = 0x05;
	
	private static final NumberFormat nf = NumberFormat.getPercentInstance();
	private Handler mViewUpdateHandler  = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				double progress = msg.arg1;
				String progressStr = DreamUtil.getFormatSize(progress);
				double percent = progress / dMax;
				if (prev != percent * 100) {
					mProgressBar.setProgress((int)(percent * 100));
					mPercentView.setText(nf.format(percent));
					mNumberView.setText(progressStr + "/" + dMaxStr);
					prev = percent * 100;
				}
				break;
			case MSG_UPDATE_TIME:
				Bundle bundle = msg.getData();
				double speed = bundle.getDouble("speed");
				long time = bundle.getLong("time");
				mSpeedView.setText(DreamUtil.getFormatSize(speed)+ "/s");
				mTimeView.setText(DreamUtil.mediaTimeFormat(time));
				break;
			case MSG_COPYING:
				mCopyState = COPYING;
				mTitleView.setText(getResources().getString(R.string.copying, mFileName));
				mTitleView.setTextColor(Color.BLACK);
				break;
			case MSG_COPY_OK:
				mCopyState = COPY_OK;
				mTitleView.setText(getResources().getString(R.string.copy_ok, mFileName));
				mTitleView.setTextColor(0xff32cd32);
				mStopButton.setText(android.R.string.ok);
				break;
			case MSG_COPY_FAIL:
				mCopyState = COPY_FAIL;
				mTitleView.setText(getResources().getString(R.string.copy_fail, mFileName));
				mTitleView.setTextColor(Color.RED);
				mStopButton.setText(android.R.string.ok);
				break;

			default:
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("oncreate start");
		setContentView(R.layout.ui_filetransfer_dialog);
		
		mInstance = this;
		
		if (null != getIntent()) {
			mFileName = getIntent().getStringExtra("name");
			setDMax(getIntent().getDoubleExtra("max", 0));
		}
		
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
		mProgressBar.setMax(100);
		
		mTitleView = (TextView) findViewById(R.id.title_veiw);
		mTimeView = (TextView) findViewById(R.id.time_view);
		mPercentView = (TextView) findViewById(R.id.percent_view);
		mNumberView = (TextView) findViewById(R.id.number_view);
		mSpeedView = (TextView) findViewById(R.id.speed_view);
		mStopButton = (Button) findViewById(R.id.left_button);
		mStopButton.setOnClickListener(this);
		
		mTitleView.setText(getResources().getString(R.string.copying, mFileName));
		mCopyState = COPYING;
		onProgressChanged();
		
//		setResult(RESULT_OK);
		System.out.println("oncreate end");
	}
	
	private void onProgressChanged(){
		mViewUpdateHandler.sendMessage(mViewUpdateHandler.obtainMessage(MSG_UPDATE_UI));
	}
	
	/**set max progress(double),and convert to MB or KB*/
	public void setDMax(double max){
		dMax = max;
		dMaxStr = DreamUtil.getFormatSize(max);
	}
	
	/**set progress to update*/
	public void setDProgress(double progress){
		dProgress = progress;
		dProgressStr = DreamUtil.getFormatSize(progress);
		onProgressChanged();
	}
	
	public void setSpeed(double speed){
		mSpeed = speed;
	}
	
	public void setTime(long duration){
		mTime = duration;
	}
	
	public void setMyTitle(String title){
		mTitle = title;
	}
	
	
	public void setCopyState(int state){
		mCopyState = state;
		mViewUpdateHandler.sendMessage(mViewUpdateHandler.obtainMessage(MSG_UPDATE_STATE));
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.left_button:
			if (COPY_OK == mCopyState) {
				this.finish();
			}
			break;

		default:
			break;
		}
	}
}
