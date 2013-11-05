package com.dreamlink.communication.ui.image;

import com.dreamlink.communication.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class PictureGridItem extends RelativeLayout implements Checkable {
	private static final String TAG = "PictureGridItem";
	private Context mContext;
	private boolean mChecked;
	public ImageView mIconView;
	private ImageView mSelectView;

	public PictureGridItem(Context context) {
		this(context, null, 0);
		// TODO Auto-generated constructor stub
	}
	
	public PictureGridItem(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		// TODO Auto-generated constructor stub
	}

	public PictureGridItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		mContext = context;
		LayoutInflater.from(context).inflate(R.layout.ui_picture_item, this);
		mIconView = (ImageView) findViewById(R.id.iv_picture_item);
		mSelectView = (ImageView) findViewById(R.id.iv_select);
	}


	@Override
	public void setChecked(boolean checked) {
		// TODO Auto-generated method stub
		mChecked = checked;
//		setBackgroundResource(checked ? R.color.bright_blue : null);
		setBackgroundDrawable(checked ? mContext.getResources().getDrawable(R.color.holo_blue2) : null);
//		mSelectView.setImageResource(checked ? R.drawable.item_checked : R.drawable.item_unchecked);
		mSelectView.setVisibility(checked ? View.VISIBLE : View.GONE);
	}

	@Override
	public boolean isChecked() {
		// TODO Auto-generated method stub
		return mChecked;
	}

	@Override
	public void toggle() {
		// TODO Auto-generated method stub
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

}
