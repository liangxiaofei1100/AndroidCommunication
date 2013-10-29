package com.dreamlink.communication.ui.dialog;


import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

public class FileInfoDialog extends Dialog implements android.view.View.OnClickListener {

	private TextView mOkButton,mCancelButton;
	
	private TextView mTypeView,mLoacationView,mSizeView,mIncludeView,mDateView;
	private View mDividerView;
	
	private long mTotalSize;
	private int mFileNum;
	private int mFolderNum;
	
	private String mFileName;
	private String mFilePath;
	private long mModified;
	
	private Context mContext;
	
	private OnClickListener mClickListener;
	
	public static final int SINGLE_FILE = 0x01;
	public static final int SINGLE_FOLDER = 0x02;
	public static final int MULTI = 0x03;
	private int type;
	
	private static final int MSG_UPDATEUI_MULTI = 0x10;
	private static final int MSG_UPDATEUI_SINGLE = 0x11;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATEUI_MULTI:
				String sizeInfo = DreamUtil.getFormatSize(mTotalSize);
				mSizeView.setText(mContext.getResources().getString(R.string.size, sizeInfo));
				int folderNum = mFolderNum;
				if (0 != mFolderNum) {
					//remove self folder
					folderNum = mFolderNum - 1;
				}
				mIncludeView.setText(mContext.getResources().getString(R.string.include_files, mFileNum, folderNum));
				break;
			case MSG_UPDATEUI_SINGLE:
				String date = DreamUtil.getFormatDate(mModified);
				setTitle(mFileName + " 属性");
				mLoacationView.setText(mContext.getResources().getString(R.string.location, mFilePath));
				mDateView.setText(mContext.getResources().getString(R.string.modif_date, date));
				break;
			default:
				break;
			}
		};
	};
	
	
	public FileInfoDialog(Context context, int theme, String path) {
		super(context, theme);
		mFilePath = path;
		mContext = context;
	}
	
	public FileInfoDialog(Context context, int theme, int size){
		super(context, theme);
		mContext = context;
		mTotalSize = size;
	}
	
	public FileInfoDialog(Context context, int type) {
		super(context);
		mContext = context;
		this.type = type;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_file_info_dialog);
		
		mTypeView = (TextView) findViewById(R.id.tv_info_type);
		mLoacationView = (TextView) findViewById(R.id.tv_info_location);
		mSizeView = (TextView) findViewById(R.id.tv_info_size);
		mIncludeView = (TextView) findViewById(R.id.tv_info_include);
		mDateView = (TextView) findViewById(R.id.tv_info_date);
		mDividerView = findViewById(R.id.info_divider);
		
		if (MULTI == type) {
			mTypeView.setVisibility(View.GONE);
			mLoacationView.setVisibility(View.GONE);
			mDateView.setVisibility(View.GONE);
			mDividerView.setVisibility(View.GONE);
		}else if (SINGLE_FILE == type) {
			mIncludeView.setVisibility(View.GONE);
			mTypeView.setText(R.string.type_file);
		}else {
			mTypeView.setText(R.string.type_folder);
		}
		
		setTitle("属性");
		mOkButton = (TextView) findViewById(R.id.btn_info_ok);
		mOkButton.setOnClickListener(this);
	}
	
	
	public void updateUI(long size, int fileNum, int folderNum){
		this.mTotalSize = size;
		this.mFileNum = fileNum;
		this.mFolderNum = folderNum;
		mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATEUI_MULTI));
	}
	
	public void updateUI(String fileName, String filePath, long date){
		mFileName = fileName;
		mFilePath = filePath;
		mModified = date;
		mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATEUI_SINGLE));
	}
	
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_info_ok:
			cancel();
			break;
		case R.id.right_button:
			dismiss();
			break;

		default:
			break;
		}
	}
	
	public interface OnClickListener{
		public void onClick(View view);
	}
	
	public void setOnClickListener(OnClickListener listener){
		mClickListener = listener;
	}

}
