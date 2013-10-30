package com.dreamlink.communication.ui.dialog;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FileTransferDialog extends Dialog implements android.view.View.OnClickListener{
	/**view progress bar*/
	private ProgressBar mProgressBar;
	/**title view*/
	private TextView mTitleView;
	/**view extra message*/
	private TextView mTimeView;
	/**view percent*/
	private TextView mPercentView;
	/**view progress num*/
	private TextView mNumberView;
	/**view transfer speed*/
	private TextView mSpeedView;
	/**button*/
	private Button mLeftButton, mRightButton;
	
	private double dMax;
	private double dProgress;
	private double mSpeed = 0;
	private long mTime;
	private String mTitle = "";
	private String mFileName = "";
	
	private String dProgressStr;
	private String dMaxStr;
	
	private double prev = 0;
	
	//update msg
	public static final int MSG_UPDATE_PROGRESS = 0x00;
	public static final int MSG_UPDATE_TIME_SPEED = 0x01;
	public static final int MSG_UPDATE_STATE = 0x02;
	
	
	//copy state
	private int state = -1;
	public static final int STATE_COPYING = 0x10;
	public static final int STATE_COPY_OK = 0x11;
	public static final int STATE_COPY_FAIL = 0x12;
	
	private Context mContext;
	public FileTransferDialog(Context context, int theme) {
		super(context, theme);
		mContext = context;
	}
	
	public FileTransferDialog(Context context){
		super(context);
		mContext = context;
	}
	
	private FileTransferOnClickListener mListener = null;
	public interface FileTransferOnClickListener{
		public void onClick(View view, int state);
	}
	
	public void setFileTransferOnClickListener(FileTransferOnClickListener listener){
		mListener = listener;
	}
	
private static final NumberFormat nf = NumberFormat.getPercentInstance();
	
	private Handler mViewUpdateHandler  = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_PROGRESS:
				if (dMax == 0) {
					mProgressBar.setProgress(mProgressBar.getMax());
					mPercentView.setText("100%");
					mNumberView.setText("0Bytes/0Bytes");
				}else {
					double percent = dProgress / dMax;
					if (prev != percent * 100) {
						mProgressBar.setProgress((int)(percent * 100));
						mPercentView.setText(nf.format(percent));
						mNumberView.setText(dProgressStr + "/" + dMaxStr);
						prev = percent * 100;
					}
				}
				break;
			case MSG_UPDATE_TIME_SPEED:
				mSpeedView.setText(sizeFormat(mSpeed) + "/s");
				mTimeView.setText(DreamUtil.mediaTimeFormat(mTime));
				break;
			case MSG_UPDATE_STATE:
				if (STATE_COPY_OK == state) {
					mTitleView.setText(mContext.getResources().getString(R.string.copy_ok, mFileName));
					mTitleView.setTextColor(mContext.getResources().getColor(R.color.bright_blue));
					mLeftButton.setText(android.R.string.ok);
					mLeftButton.setText("Open");
					mRightButton.setText("OK");
				}else if (STATE_COPY_FAIL == state) {
					mTitleView.setText(mContext.getResources().getString(R.string.copy_fail, mFileName));
					mTitleView.setTextColor(Color.RED);
					mLeftButton.setText("Retry");
					mRightButton.setText("Cancel");
				}else if (STATE_COPYING == state) {
					mTitleView.setText(mContext.getResources().getString(R.string.copying, mFileName));
					mLeftButton.setText("Stop");
					mRightButton.setText("Hide");
				}
				break;
			default:
				break;
			}
			
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_filetransfer_dialog);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
		mProgressBar.setMax(100);
		
		mTitleView = (TextView) findViewById(R.id.title_veiw);
		mTimeView = (TextView) findViewById(R.id.time_view);
		mPercentView = (TextView) findViewById(R.id.percent_view);
		mNumberView = (TextView) findViewById(R.id.number_view);
		mSpeedView = (TextView) findViewById(R.id.speed_view);
		mLeftButton = (Button) findViewById(R.id.left_button);
		mRightButton = (Button) findViewById(R.id.right_button);
		mLeftButton.setOnClickListener(this);
		mRightButton.setOnClickListener(this);
		
		mViewUpdateHandler.sendMessage(mViewUpdateHandler.obtainMessage(MSG_UPDATE_PROGRESS));
	}
	
	/**set max progress(double),and convert to MB or KB*/
	public void setDMax(double max){
		dMax = max;
		dMaxStr = sizeFormat(max);
	}
	
	/**set progress to update*/
	public void setDProgress(double progress){
		dProgress = progress;
		dProgressStr = sizeFormat(progress);
		mViewUpdateHandler.sendMessage(mViewUpdateHandler.obtainMessage(MSG_UPDATE_PROGRESS));
	}
	
	public void setTP(double speed, long duration){
		mSpeed = speed;
		mTime = duration;
		mViewUpdateHandler.sendMessage(mViewUpdateHandler.obtainMessage(MSG_UPDATE_TIME_SPEED));
	}
	
	public void setFileName(String name){
		mFileName = name;
	}
	
	public void setState(int state){
		this.state = state;
		mViewUpdateHandler.sendMessage(mViewUpdateHandler.obtainMessage(MSG_UPDATE_STATE));
	}
	
	public static String sizeFormat(double size){
		if (size > 1024 * 1024) {
			Double dsize = (double) (size / (1024 * 1024));
			return new DecimalFormat("#.00").format(dsize) + "MB";
		}else if (size > 1024) {
			Double dsize = (double)size / (1024);
			return new DecimalFormat("#.00").format(dsize) + "KB";
		}else {
			return String.valueOf((int)size) + " Bytes";
		}
	}

	@Override
	public void onClick(View v) {
		if (R.id.left_button == v.getId()) {
			switch (state) {
			case STATE_COPYING:
				//stop
				mListener.onClick(v, STATE_COPYING);
				dismiss();
				break;
			case STATE_COPY_OK:
				//open
				mListener.onClick(v, STATE_COPY_OK);
				dismiss();
				break;
			case STATE_COPY_FAIL:
				//retry
				mListener.onClick(v, STATE_COPY_FAIL);
				break;

			default:
				break;
			}
		}else if (R.id.right_button == v.getId()) {
			switch (state) {
			case STATE_COPYING:
				//Hide
				mListener.onClick(v, STATE_COPYING);
				break;
			case STATE_COPY_OK:
				mListener.onClick(v, STATE_COPY_OK);
				dismiss();
				break;
			case STATE_COPY_FAIL:
				//Cancel
				mListener.onClick(v, STATE_COPY_FAIL);
				break;

			default:
				break;
			}
		}
	}

}
