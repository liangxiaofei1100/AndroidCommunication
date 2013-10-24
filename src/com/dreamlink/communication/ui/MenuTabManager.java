package com.dreamlink.communication.ui;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.media.MyMenu;
import com.dreamlink.communication.ui.media.MyMenu.MyMenuItem;
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
	
	private LinearLayout mMenuHolders;
	private LayoutInflater mInflater;
	private Context mContext;
	private List<MenuItem> itemList = new ArrayList<MenuItem>();
	private List<MyMenuItem> items = new ArrayList<MyMenu.MyMenuItem>();
	private onMenuItemClickListener mListener; 
	
	public MenuTabManager(Context context, View rootView){
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mMenuHolders = (LinearLayout) rootView.findViewById(R.id.ll_menutabs_holder);
	}
	
	public MenuTabManager(Context context, LinearLayout menuHodlers){
		mInflater = LayoutInflater.from(context);
		mMenuHolders = menuHodlers;
	}
	
	public void setOnMenuItemClickListener(onMenuItemClickListener listener){
		this.mListener = listener;
	}
	
	public void refreshMenus(MyMenu myMenu){
		Log.d(TAG, "refreshMenus:" + myMenu.size());
		int count = mMenuHolders.getChildCount();
		mMenuHolders.removeViews(0, count);
		
		for (int i = 0; i < myMenu.size(); i++) {
			MyMenuItem item = null;
			try {
				item = myMenu.getItem(i);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			addMenuItem(item);
		}
	}
	
	public void addMenuItem(MyMenuItem item){
		Log.d(TAG, "addMenuItem.item:" + item.getTitle());
		View view = null;
		view = mInflater.inflate(R.layout.ui_menubar_bottom2_item, null);
		view.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
		ImageView imageView = (ImageView) view.findViewById(R.id.iv_menu_icon);
		TextView textView = (TextView) view.findViewById(R.id.tv_menu_name);
		int icon = item.getIcon();
		String name = item.getTitle().toString();
		imageView.setImageResource(icon);
		textView.setText(name);
		view.setId(items.size());
		view.setOnClickListener(this);
		view.setEnabled(item.isEnable());
		mMenuHolders.addView(view);
		items.add(item);
	}
	
	public void refresh(int id){
		mMenuHolders.findViewById(id).setEnabled(false);
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onClick.id=" + v.getId());
//		MenuItem item = itemList.get(v.getId());
		MyMenuItem item = items.get(v.getId());
		mListener.onMenuClick(item);
	}
	
	public void clearMenus(){
		itemList.clear();
	}
	
	public interface onMenuItemClickListener{
		public void onMenuClick(MyMenuItem item);
	}
}
