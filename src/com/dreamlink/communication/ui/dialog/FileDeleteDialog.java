package com.dreamlink.communication.ui.dialog;

import java.io.File;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;

import android.R.integer;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FileDeleteDialog extends Dialog implements android.view.View.OnClickListener {

	private TextView mFileNameView;
	private TextView mDialogTitle;
	private Button mOkButton,mCancelButton;
	private ProgressBar mProgressBar; 
	
	private String mFilePath;
	private int mSize;
	
	private Context mContext;
	
	private OnDelClickListener mClickListener;
	
	private int max;
	private int progress;
	private String fileName;
	private static final int MSG_UPDATE_PROGRESS = 0x10;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_PROGRESS:
				mProgressBar.setProgress(progress);
				mFileNameView.setText(fileName);
				break;

			default:
				break;
			}
		};
	};
	
	
	public FileDeleteDialog(Context context, int theme, String path) {
		super(context, theme);
		mFilePath = path;
		mContext = context;
	}
	
	public FileDeleteDialog(Context context, int theme, int size){
		super(context, theme);
		mContext = context;
		mSize = size;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_filedelete_dialog);
		
		mProgressBar = (ProgressBar) findViewById(R.id.bar_delete);
		mProgressBar.setMax(mSize);
		
		mFileNameView = (TextView) findViewById(R.id.name_view);
		mDialogTitle = (TextView) findViewById(R.id.title_veiw);
		
		mFileNameView.setText(mSize + "个文件将被删除");
//		mFileSizeView.setText(mContext.getResources().getString(R.string.size, DreamUtil.getFormatSize(file.length())));
		
		mOkButton = (Button) findViewById(R.id.left_button);
		mCancelButton = (Button) findViewById(R.id.right_button);
		mOkButton.setText(android.R.string.ok);
		mCancelButton.setText(android.R.string.cancel);
		mOkButton.setOnClickListener(this);
		mCancelButton.setOnClickListener(this);
	}
	
	public void setMax(int max){
		this.max = max;
	}
	
	public void setProgress(int progress, String fileName){
		this.progress = progress;
		this.fileName = fileName;
		mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_PROGRESS));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.left_button:
			mClickListener.onClick(v, mFilePath);
			mDialogTitle.setText("正在删除文件...");
			mProgressBar.setVisibility(View.VISIBLE);
			mOkButton.setVisibility(View.GONE);
			break;
		case R.id.right_button:
			mClickListener.onClick(v, mFilePath);
			dismiss();
			break;

		default:
			break;
		}
	}
	
	public interface OnDelClickListener{
		public void onClick(View view, String path);
	}
	
	public void setOnClickListener(OnDelClickListener listener){
		mClickListener = listener;
	}

}
