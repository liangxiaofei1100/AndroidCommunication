package com.dreamlink.communication.ui.common;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.dreamlink.communication.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RefreshListView extends ListView {

	public static final int NORMAL     = 0;
	public static final int PULL       = 1;
	public static final int RELEASE    = 2;
	public static final int REFRESHING = 3;
	
	private int state = PULL; //record current status̬
	private int preState;//re״cord pre status
	
	private View 			mHeader;
	private ProgressBar 	mHeaderBar;
	private ImageView 		mHeaderArrow;
	private TextView 		mHeaderText;
	private TextView 		mHeaderDate;
	
	
	private Drawable mPullDraw;
	private Drawable mReleaseDraw;
	
	private Context 		mContext;
	private LayoutInflater mInflater;
	private OnRefreshListener mRefreshListener;
	
	private float mDownY;
	private float mMoveY;
	
	private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context);
	}

	private void initialize(Context context) {
		mContext 	 = context;
		mInflater 	 = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mHeader 	 = mInflater.inflate(R.layout.ui_refresh_header,null);
		mHeaderBar 	 = (ProgressBar) mHeader.findViewById(R.id.bar_header);
		mHeaderArrow = (ImageView) mHeader.findViewById(R.id.iv_arrow);
		mHeaderText  = (TextView) mHeader.findViewById(R.id.tv_tip);
		mHeaderDate  = (TextView) mHeader.findViewById(R.id.tv_date);
		
		
		mPullDraw = getResources().getDrawable(R.drawable.pull_refresh);
		mReleaseDraw = getResources().getDrawable(R.drawable.realease_refresh);
		
		//创建布局参数传递给对应的view
		ListView.LayoutParams params = new ListView.LayoutParams(
				ListView.LayoutParams.MATCH_PARENT,100);
		mHeader.setLayoutParams(params);
		
		addHeaderView(mHeader);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mDownY = ev.getY();
			mHeaderDate.setText(mDateFormat.format(new Date()));
			break;
		case MotionEvent.ACTION_MOVE:
			mMoveY = ev.getY();
			if(state != REFRESHING) {
				int instance = (int) ((mMoveY-mDownY)/3);
				if(getFirstVisiblePosition() > 0) {
					mDownY = mMoveY;
					state = NORMAL;
					break;
				}
				if(instance < 0) break;
				setPadding(0,-mHeader.getLayoutParams().height+instance, 0, 0);
				//这一步相当重要 当数据的高度大于屏幕时 向上拉动时 lisview的滚动速度比setpadding要快 所以必须设置这一句使listview始终在第一行
				if(state != NORMAL)setSelection(0); 
				if(instance > 80) {
					state = RELEASE;
					mHeaderText.setText("松手以刷新");
					mHeaderArrow.setImageDrawable(mReleaseDraw);
//					mHeaderBar.setVisibility(View.INVISIBLE);
				} else if((state == NORMAL || state == RELEASE) ){
					preState = state;    state = PULL;
					if(preState == NORMAL && state == PULL && instance >= 18) mDownY = mMoveY;
					else if(preState != RELEASE && instance < 20) state = NORMAL;
					mHeaderText.setText("向下拖动刷新");
					mHeaderArrow.setImageDrawable(mPullDraw);
//					mHeaderBar.setVisibility(View.INVISIBLE);
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			if(state == RELEASE) {
				if(mRefreshListener!=null) mRefreshListener.onRefresh();
				else {
					setPadding(0, -mHeader.getLayoutParams().height, 0, 0);
					state = PULL;
				}
			}
			if(state == PULL) setPadding(0, -mHeader.getLayoutParams().height, 0, 0);
			if(mMoveY == mDownY) state = NORMAL;
		default:
			break;
		}
		return super.onTouchEvent(ev);
	}
	
	@Override
	public void setAdapter(ListAdapter adapter) {
		super.setAdapter(adapter);
		setPadding(0,-mHeader.getLayoutParams().height, 0, 0);
	}
	
	public interface OnRefreshListener {
		 public void onRefresh();
	}
	
	public void setOnRefreshListener(OnRefreshListener listener) {
		this.mRefreshListener = listener;
	}
	
	//使用该工具类时  开启线程进行耗时操作前必须调用该方法
	public void onPrepareRefresh() {
		setPadding(0,0,0,0);
		state = REFRESHING;
		mHeaderArrow.setImageDrawable(mPullDraw);
		mHeaderText.setText("载入中......");
		mHeaderArrow.setVisibility(View.INVISIBLE);
		mHeaderBar.setVisibility(View.VISIBLE);
	}
	
	//使用该工具类时  开启线程进行耗时操作后必须调用该方法
	public void onCompleteRefresh() {
		setPadding(0,-mHeader.getLayoutParams().height, 0, 0);
		state = PULL;
		mHeaderText.setText("向下拖动刷新");
		mHeaderArrow.setVisibility(View.VISIBLE);
		mHeaderBar.setVisibility(View.INVISIBLE);
	}
}
