package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.util.Log;

import android.R.integer;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

public class MyMenu{
	private static final String TAG = "MyMenu";
	
	public static final int ACTION_MENU_OPEN = 0x01;
	public static final int ACTION_MENU_SEND = 0x02;
	public static final int ACTION_MENU_DELETE = 0x03;
	public static final int ACTION_MENU_INFO = 0x04;
	public static final int ACTION_MENU_MORE = 0x05;
	public static final int ACTION_MENU_PLAY = 0x06;
	public static final int ACTION_MENU_RENAME = 0x07;
	public static final int ACTION_MENU_SELECT = 0x08;
	
	private Menu menu;
	private boolean enable;
	private MenuItem item;
	private List<MenuItem> itemList = new ArrayList<MenuItem>();
	private List<MyMenuItem> items = new ArrayList<MyMenu.MyMenuItem>();
	
	private Context mContext;
	
	public MyMenu(Context context){
		mContext = context;
	}
	
	public void setMenu(Menu menu){
		this.menu = menu;
	}
	
	public Menu getMenu(){
		return menu;
	}
	
	public void setEnable(boolean enable){
		this.enable = enable;
	}
	
	public boolean isEnable(){
		return enable;
	}
	
	public void addItem(MenuItem item){
		Log.d(TAG, "addItem.name=" + item.getTitle());
		itemList.add(item);
	}
	
	public void addItem(int id, int iconId, String title){
		MyMenuItem item = new MyMenuItem(id, iconId, title);
		items.add(item);
	}
	
	public void addItem(int id, int iconId, int title_redId){
		String title = mContext.getResources().getString(title_redId);
		addItem(id, iconId, title);
	}
	
	public int size(){
		return items.size();
	}
	
	public MyMenuItem getItem(int index) throws Exception{
		if (index >= size()) {
			throw new Exception("Out of bound,the size is " + size() + ",index is " + index);
		}else {
			return items.get(index);
		}
	}
	
	public MyMenuItem findItem(int id){
		for(MyMenuItem item : items){
			if (id == item.getItemId()) {
				return item;
			}
		}
		
		return null;
	}
	
	public class MyMenuItem{
		int id;
		int iconId;
		String title;
		boolean enable = true;
		int text_color;
		
		MyMenuItem(int id, int iconid, String title){
			this.id = id;
			this.iconId = iconid;
			this.title = title;
			text_color = mContext.getResources().getColor(R.color.black);
		}
		
		public int getItemId(){
			return id;
		}
		
		public void setTitle(String title){
			this.title = title;
		}
		
		public void setTitle(int resId){
			String title = mContext.getResources().getString(resId);
			setTitle(title);
		}
		
		public String getTitle(){
			return title;
		}
		
		public void setTextColor(int color_id){
			this.text_color = color_id;
		}
		
		public int getTextColor(){
			return text_color;
		}
		
		public void setIcon(int iconId){
			this.iconId = iconId;
		}
		
		public int getIcon(){
			return iconId;
		}
		
		public void setEnable(boolean enable){
			this.enable = enable;
		}
		
		public boolean isEnable(){
			return enable;
		}
	}
	
}
