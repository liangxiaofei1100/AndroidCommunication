package com.dreamlink.communication.ui.common;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant;

import android.R.integer;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v4.widget.CursorAdapter;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;

public class BaseCursorAdapter extends CursorAdapter {
	protected int mMenuMode = DreamConstant.MENU_MODE_NORMAL;
	protected SparseBooleanArray mIsSelected = null;

	public BaseCursorAdapter(Context context, Cursor c, boolean autoRequery) {
		super(context, c, autoRequery);
		mIsSelected = new SparseBooleanArray();
		// TODO Auto-generated constructor stub
	}
	
	public void changeMode(int mode){
		mMenuMode = mode;
	}
	
	public int getMode(){
		return mMenuMode;
	}
	
	/**
	 * Select All or not
	 * @param isSelected true or false
	 */
	public void selectAll(boolean isSelected){
	}
	
	/**
	 * set the item is selected or not
	 * @param position the position that clicked
	 * @param isSelected selected or not
	 */
	public void setSelected(int position, boolean isSelected){
		mIsSelected.put(position, isSelected);
	}
	
	public void setSelected(int position){
		mIsSelected.put(position, !isSelected(position));
	}
	
	public boolean isSelected(int position){
		return mIsSelected.get(position);
	}
	
	public int getSelectedItemsCount(){
		return 0;
	};
	
	public void updateViewBackground(boolean selected, int position, View view){
		if (selected) {
			view.setBackgroundResource(R.color.bright_blue);
		}else {
			view.setBackgroundResource(Color.TRANSPARENT);
		}
	}

	@Override
	public void bindView(View arg0, Context arg1, Cursor arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
		// TODO Auto-generated method stub
		return null;
	}

}
