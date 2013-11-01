package com.dreamlink.communication.ui.media;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class VideoGridItem extends RelativeLayout implements Checkable {
	private static final String TAG = "VideoGridItem";
	private Context mContext;
	private boolean mChecked;
	public ImageView mIconView;
	private ImageView mSelectView;
	private TextView mTimeView;

	public VideoGridItem(Context context) {
		this(context, null, 0);
		// TODO Auto-generated constructor stub
	}
	
	public VideoGridItem(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		// TODO Auto-generated constructor stub
	}

	public VideoGridItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		LayoutInflater.from(context).inflate(R.layout.ui_media_video_item, this);
		mIconView = (ImageView) findViewById(R.id.iv_video_icon);
		mSelectView = (ImageView) findViewById(R.id.iv_select);
		mTimeView = (TextView) findViewById(R.id.tv_video_time);
	}


	@Override
	public void setChecked(boolean checked) {
		mChecked = checked;
		setBackgroundDrawable(checked ? mContext.getResources().getDrawable(R.color.holo_blue2) : null);
		mSelectView.setVisibility(checked ? View.VISIBLE : View.GONE);
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}
	
	public void startEditMode(boolean start){
		mSelectView.setVisibility(start ? View.VISIBLE : View.GONE);
	}
	
	public void setIconResId(int resId){
		if (null != mIconView) {
			mIconView.setImageResource(resId);
		}
	}
	
	public void setIconBitmap(Bitmap bitmap){
		if (null != mIconView) {
			mIconView.setImageBitmap(bitmap);
		}
	}
	
	public void setVideoTime(long time){
		String timeStr = DreamUtil.mediaTimeFormat(time);
		mTimeView.setText(timeStr);
	}

}
