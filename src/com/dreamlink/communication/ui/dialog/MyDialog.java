package com.dreamlink.communication.ui.dialog;

import com.dreamlink.communication.R;
import com.dreamlink.communication.util.Log;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MyDialog extends Dialog implements android.view.View.OnClickListener {
	
	private TextView mNameView;
	private TextView mNumView;
	private ProgressBar mProgressBar;
	
	private Button mCancelButton;
	private Context mContext;
	
	private int progress;
	private String mName;
	private int max;
	private static final int MSG_UPDATE_PROGRESS = 0x10;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_PROGRESS:
				mProgressBar.setProgress(progress);
				mNameView.setText(mName);
				mNumView.setText(progress + "/" + max);
				break;

			default:
				break;
			}
		};
	};
	
	public MyDialog(Context context, int size){
		super(context);
		mContext = context;
		max = size;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_my_dialog);
		
		mProgressBar = (ProgressBar) findViewById(R.id.bar_move);
		mProgressBar.setMax(max);
		
		mNameView = (TextView) findViewById(R.id.name_view);
		mNumView = (TextView) findViewById(R.id.num_view);
		
		mCancelButton = (Button) findViewById(R.id.button);
		mCancelButton.setText(android.R.string.cancel);
		mCancelButton.setOnClickListener(this);
		
		setCancelable(false);
	}
	
	public void updateName(String name){
		this.mName = name;
		mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PROGRESS));
	}
	
	public void updateProgress(int progress){
		this.progress = progress;
		mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PROGRESS));
	}
	
	public void updateUI(int progress, String fileName){
		this.progress = progress;
		this.mName = fileName;
		mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PROGRESS));
	}
	
	public int getMax(){
		return max;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button:
			cancel();
			break;

		default:
			break;
		}
	}

}
