package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.dreamlink.communication.R;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class PopupView implements OnItemClickListener, OnClickListener {
	private static final String TAG = "PopupView";
	private ArrayList<HashMap<String, Object>> itemList = new ArrayList<HashMap<String,Object>>();
    private Context context;
    private PopupWindow popupWindow ;
    private ListView listView;
    private LayoutInflater inflater;
    private PopupViewClickListener mListener;
    private SimpleAdapter mAdapter = null;
    
    private static final String[] CARDS = {"手机存储","SD卡"};
    private static final int[] CARDS_ICON = {
    	R.drawable.storage_internal_n,
    	R.drawable.storage_sd_card_n
    };
    
    public interface PopupViewClickListener{
    	void doInternal();
    	void doSdcard();
    }
    
	public PopupView(Context context){
		inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.ui_popupmenu, null);
		listView = (ListView) view.findViewById(R.id.popup_view_listView);
		listView.setOnItemClickListener(this);
		
		HashMap<String, Object> map = null;
		for (int i = 0; i < CARDS.length; i++) {
			map = new HashMap<String, Object>();
			map.put("name", CARDS[i]);
			map.put("icon", CARDS_ICON[i]);
			itemList.add(map);
		}
		
		mAdapter = new SimpleAdapter(context, itemList, R.layout.ui_popupview_item, 
				new String[]{"name","icon"}, 
				new int[]{R.id.popup_view_text,R.id.popup_view_icon});
		listView.setAdapter(mAdapter);
		
		popupWindow = new PopupWindow(view, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		popupWindow.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.bg));
		popupWindow.setTouchInterceptor(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
					popupWindow.dismiss();
					return true;
				}
				return false;
			}
		});
	}
	
	public void setOnPopupViewListener(PopupViewClickListener listener){
		mListener = listener;
	}
	
	
	public Object getItem(int position){
		return itemList.get(position);
	}
	
	//下拉式 弹出 pop菜单 parent 
	public void showAsDropDown(View parent) {
		// 保证尺寸是根据屏幕像素密度来的
		popupWindow.showAsDropDown(parent, 0, 2);
		// 使其聚集
		popupWindow.setFocusable(true);
		// 设置允许在外点击消失
		popupWindow.setOutsideTouchable(true);
		// 刷新状态
		popupWindow.update();
	}
    
    //隐藏菜单
    public void dismiss() {
            popupWindow.dismiss();
    }
	

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//menu item click 
		if (position == 0) {
			mListener.doInternal();
		}else if (position == 1) {
			mListener.doSdcard();
		}else {
			Log.e(TAG, "what's going on?");
		}
		popupWindow.dismiss();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
//		case R.id.menu_item_open:
//			mListener.onOpenClickListener();
//			break;
//		case R.id.menu_item_send:
//			mListener.onSendClickListener();
//			break;
//		case R.id.menu_item_delete:
//			mListener.onDeleteClickListener();
//			break;

		default:
			break;
		}
	}
}
