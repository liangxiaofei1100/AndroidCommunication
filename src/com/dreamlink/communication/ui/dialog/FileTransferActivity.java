package com.dreamlink.communication.ui.dialog;

import com.dreamlink.communication.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FileTransferActivity extends Activity{
	/**view progress bar*/
	private ProgressBar mProgressBar;
	/**title view*/
	private TextView mTitleView;
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
	private long mTime;
	private String mMessage = "";
	private String mTitle = "";
	
	private String dProgressStr;
	private String dMaxStr;
	
	private double prev = 0;
	
	private Context mContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.progress_dialog);
	}
}
