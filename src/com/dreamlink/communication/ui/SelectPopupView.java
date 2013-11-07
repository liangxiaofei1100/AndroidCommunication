package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.HashMap;

import com.dreamlink.communication.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.LinearLayout.LayoutParams;

public class SelectPopupView implements OnItemClickListener {
	private static final String TAG = "SelectPopupView";
	private ArrayList<HashMap<String, Object>> itemList = new ArrayList<HashMap<String,Object>>();
    private Context context;
    private PopupWindow popupWindow ;
    private ListView listView;
    private LayoutInflater inflater;
    private SelectItemClickListener mListener;
    private SimpleAdapter mAdapter = null;
    
    private static final String[] TITLES = {"朝颜天地","图片","音频","视频","应用","游戏","批量传输","退出"};
    
    public interface SelectItemClickListener{
    	void onItemClick(int position);
    }
    
	public SelectPopupView(Context context){
		inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.ui_select_popupview, null);
		listView = (ListView) view.findViewById(R.id.popup_view_listView);
		listView.setOnItemClickListener(this);
		
		HashMap<String, Object> map = null;
		for (int i = 0; i < TITLES.length; i++) {
			map = new HashMap<String, Object>();
			map.put("name", TITLES[i]);
			itemList.add(map);
		}
		
		mAdapter = new SimpleAdapter(context, itemList, R.layout.ui_select_popupview_item, 
				new String[]{"name"}, 
				new int[]{R.id.select_menu_text});
		listView.setAdapter(mAdapter);
		popupWindow = new PopupWindow(view, 250, LayoutParams.WRAP_CONTENT);
		popupWindow.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.quickaction_vertical_btn_normal));
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
	
	public void setOnSelectedItemClickListener(SelectItemClickListener listener){
		mListener = listener;
	}
	
	
	public Object getItem(int position){
		return itemList.get(position);
	}
	
	//下拉式 弹出 pop菜单 parent 
	public void showAsDropDown(View parent) {
		// 保证尺寸是根据屏幕像素密度来的
		popupWindow.showAsDropDown(parent, 0, 0);
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
		mListener.onItemClick(position);
		popupWindow.dismiss();
	}

}
