package com.dreamlink.communication.fileshare;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.dreamlink.communication.R;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 自定义文件传输进度条
 * @author yuri
 */
public class ProgressBarDialog extends AlertDialog {

	/**view progress bar*/
	private ProgressBar mProgressBar;
	/**view extra message*/
	private TextView mMessageView;
	/**view percent*/
	private TextView mPercentView;
	/**view progress num*/
	private TextView mNumberView;
	/**view transfer speed*/
	private TextView mSpeedView;
	
	private double dMax;
	private double dProgress;
	private long mSpeed = 0;
	private String mMessage = "";
	
	private String dProgressStr;
	private String dMaxStr;
	
	private int prev = 0;
	
	private Context mContext;
	protected ProgressBarDialog(Context context) {
		super(context);
		
		mContext = context;
	}
	
	private static final NumberFormat nf = NumberFormat.getPercentInstance();
	
	private Handler mViewUpdateHandler  = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			double percent = dProgress / dMax;
			if (prev != (int)(percent * 100)) {
				mProgressBar.setProgress((int)(percent * 100));
				mPercentView.setText(nf.format(percent));
				mNumberView.setText(dProgressStr + "/" + dMaxStr);
				
				///
				mSpeedView.setText(sizeFormat(mSpeed) + "/s");
				///
				prev = (int)(percent * 100);
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		LayoutInflater inflater = LayoutInflater.from(getContext());
		
		View view = inflater.inflate(R.layout.progress_dialog, null);
		mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		mProgressBar.setMax(100);
		
		mMessageView = (TextView) view.findViewById(R.id.message_view);
		mPercentView = (TextView) view.findViewById(R.id.percent_view);
		mNumberView = (TextView) view.findViewById(R.id.number_view);
		mSpeedView = (TextView) view.findViewById(R.id.speed_view);
		
		setView(view);
		onProgressChanged();
		mMessageView.setText(mMessage);
		super.onCreate(savedInstanceState);
	}
	
	private void onProgressChanged(){
		mViewUpdateHandler.sendMessage(mViewUpdateHandler.obtainMessage(0));
	}
	
	/**get the max progress*/
	public double getDMax(){
		return dMax;
	}
	
	/**set max progress(double),and convert to MB or KB*/
	public void setDMax(double max){
		dMax = max;
		dMaxStr = sizeFormat(max);
	}
	
	/**get current progress*/
	public double getDProgress(){
		return dProgress;
	}
	
	/**set progress to update*/
	public void setDProgress(double progress){
		dProgress = progress;
		dProgressStr = sizeFormat(progress);
		onProgressChanged();
	}
	
	public void setMessage(String message){
		mMessage = message;
	}
	
	public void setMessage(int resId){
		mMessage = mContext.getResources().getString(resId);
	}
	
	public void setSpeed(long speed){
		mSpeed = speed;
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
	
}
