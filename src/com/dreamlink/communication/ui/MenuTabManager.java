package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MenuTabManager implements OnClickListener {
	private static final String TAG  = "MenuTabManager";
	
	private Menu menu;
	private LinearLayout mMenuHolders;
	private LayoutInflater mInflater;
	private Context mContext;
	private List<MenuItem> itemList = new ArrayList<MenuItem>();
	private onMenuItemClickListener mListener; 
	
	public MenuTabManager(Context context, View rootView){
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mMenuHolders = (LinearLayout) rootView.findViewById(R.id.ll_menutabs_holder);
	}
	
	public void setOnMenuItemClickListener(onMenuItemClickListener listener){
		this.mListener = listener;
	}
	
	public void refreshMenus(Menu menu){
		Log.d(TAG, "refreshMenus:" + menu.size());
		int count = mMenuHolders.getChildCount();
		mMenuHolders.removeViews(0, count);
		
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			addMenuItem(item);
		}
	}
	
	public void addMenuItem(MenuItem item){
		Log.d(TAG, "addMenuItem.item:" + item.getTitle());
		View view = null;
		view = mInflater.inflate(R.layout.ui_menubar_bottom2_item, null);
		view.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
		ImageView imageView = (ImageView) view.findViewById(R.id.iv_menu_icon);
		TextView textView = (TextView) view.findViewById(R.id.tv_menu_name);
		Drawable icon = item.getIcon();
		String name = item.getTitle().toString();
		imageView.setImageDrawable(icon);
		textView.setText(name);
		view.setId(itemList.size());
		view.setOnClickListener(this);
		
		mMenuHolders.addView(view);
		itemList.add(item);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onClick.id=" + v.getId());
		MenuItem item = itemList.get(v.getId());
		mListener.onMenuClick(item);
	}
	
	public void clearMenus(){
		itemList.clear();
	}
	
	public interface onMenuItemClickListener{
		public void onMenuClick(MenuItem item);
	}
}
