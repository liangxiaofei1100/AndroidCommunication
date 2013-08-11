package com.dreamlink.communication.ui;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.app.AppManager;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;

public class ListContextMenu implements OnCreateContextMenuListener{
	private String title = "";
	private int menu_type = -1;
	
	public static final int MENU_TYPE_NORMAL_APP = 0x00;
	public static final int MENU_TYPE_GAME_APP = 0x01;
	public static final int MENU_TYPE_IMAGE = 0x02;
	public static final int MENU_TYPE_AUDIO = 0x03;
	public static final int MENU_TYPE_FILE = 0x04;
	
	//menu
	public static final int MENU_OPEN = 0x10;
	public static final int MENU_SEND = 0x11;
	public static final int MENU_DELETE = 0x12;
	public static final int MENU_INFO = 0x13;
	public static final int MENU_RENAME = 0x14;
	public static final int MENU_PLAY = 0x15;
	public static final int MENU_MORE = 0x16;
	public static final int MENU_UNINSTALL = 0x17;
	public static final int MENU_MOVE = 0x18;
	
	public ListContextMenu(String title, int type){
		this.title = title;
		this.menu_type = type;
	}
	
	public ListContextMenu(int type){
		this.title = null;
		this.menu_type = type;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (null == title) {
			menu.setHeaderTitle(R.string.menu);
		}else {
			menu.setHeaderTitle(title);
		}
		switch (menu_type) {
		case MENU_TYPE_NORMAL_APP:
			AppManager.menu_type = AppManager.NORMAL_APP_MENU;
			menu.add(0, MENU_INFO, 0, R.string.menu_app_info);
			menu.add(0, MENU_SEND, 0, R.string.menu_send);
			menu.add(0, MENU_UNINSTALL, 0, R.string.menu_uninstall);
			menu.add(0, MENU_MOVE, 0, R.string.menu_move_to_game);
			break;
		case MENU_TYPE_GAME_APP:
			AppManager.menu_type = AppManager.GAME_APP_MENU_MY;
			menu.add(0, MENU_INFO, 0, R.string.menu_app_info);
			menu.add(0, MENU_SEND, 0, R.string.menu_send);
			menu.add(0, MENU_UNINSTALL, 0, R.string.menu_uninstall);
			menu.add(0, MENU_MOVE, 0, R.string.menu_move_to_app);
			break;
		case MENU_TYPE_FILE:
			menu.add(0, MENU_OPEN, 0, R.string.menu_open);
			menu.add(0, MENU_SEND, 0, R.string.menu_send);
			menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
			menu.add(0, MENU_INFO, 0, R.string.menu_info);
			menu.add(0, MENU_RENAME, 0, R.string.menu_rename);
			break;
		case MENU_TYPE_IMAGE:
			menu.add(0, MENU_OPEN, 0, R.string.menu_open);
			menu.add(0, MENU_SEND, 0, R.string.menu_send);
			menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
			menu.add(0, MENU_INFO, 0, R.string.menu_info);
			break;

		default:
			break;
		}
		
	}
	
}
