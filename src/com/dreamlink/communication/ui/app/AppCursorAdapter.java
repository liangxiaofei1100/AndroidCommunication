package com.dreamlink.communication.ui.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.common.BaseCursorAdapter;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class AppCursorAdapter extends BaseCursorAdapter {
	private static final String TAG = "AppCursorAdapter";
	private LayoutInflater inflater = null;
	private PackageManager pm = null;
	
	public AppCursorAdapter(Context context){
		super(context, null, true);
		
		this.mContext = context;
		inflater = LayoutInflater.from(context);
		
		pm = context.getPackageManager();
	}
	
	@Override
	public void selectAll(boolean isSelected) {
		int count = this.getCount();
		for (int i = 0; i < count; i++) {
			setSelected(i, isSelected);
		}
	}
	
	@Override
	public int getSelectedItemsCount() {
		int count = 0;
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				count ++;
			}
		}
		return count;
	}
	
	@Override
	public List<Integer> getSelectedItemPos() {
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				list.add(i);
			}
		}
		return list;
	}
	
	/**
	 * get Select item pakcageName list
	 * @return
	 */
	public List<String> getSelectedPkgList(){
		Log.d(TAG, "getSelectedPkgList");
		List<String> list = new ArrayList<String>();
		Cursor cursor = getCursor();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				cursor.moveToPosition(i);
				String url = cursor.getString(cursor
						.getColumnIndex(AppData.App.PKG_NAME));
				list.add(url);
			}
		}
		return list;
	}
	
	/**
	 * get Select item installed path list
	 * @return
	 */
	public List<String> getSelectItemPathList(){
		Log.d(TAG, "getSelectItemPathList");
		List<String> list = new ArrayList<String>();
		Cursor cursor = getCursor();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				cursor.moveToPosition(i);
				String packagename = cursor.getString(cursor
						.getColumnIndex(AppData.App.PKG_NAME));
				ApplicationInfo applicationInfo = null;
				try {
					applicationInfo = pm.getApplicationInfo(packagename, 0);
					list.add(applicationInfo.sourceDir);
				} catch (NameNotFoundException e) {
					Log.e(TAG, "getSelectItemPathList:" + packagename + " name not found.");
				}
			}
		}
		return list;
	}
	
	/**
	 * get Select item filename  list
	 * @return
	 */
	public List<String> getSelectItemNameList(){
		List<String> list = new ArrayList<String>();
		Cursor cursor = getCursor();
		for (int i = 0; i < mIsSelected.size(); i++) {
			if (mIsSelected.valueAt(i)) {
				cursor.moveToPosition(i);
				String name = cursor.getString(cursor
						.getColumnIndex(AppData.App.PKG_NAME));
				list.add(name);
			}
		}
		return list;
	}
	
	@Override
	public Object getItem(int position) {
		return super.getItem(position);
	}
	
	@Override
	public void bindView(View view, Context arg1, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		
		final String packagename = cursor.getString(cursor.getColumnIndex(AppData.App.PKG_NAME));
		ApplicationInfo applicationInfo = null;
		try {
			applicationInfo = pm.getApplicationInfo(packagename, 0);
		} catch (NameNotFoundException e) {
			Log.e(TAG, e.toString());
			return;
		}
		
		holder.iconView.setImageDrawable(applicationInfo.loadIcon(pm));
		holder.nameView.setText(applicationInfo.loadLabel(pm));
		long size = new File(applicationInfo.sourceDir).length();
		holder.sizeView.setText(DreamUtil.getFormatSize(size));
		
		boolean isSelected = isSelected(cursor.getPosition());
		updateViewBackground(isSelected, cursor.getPosition(), view);
	}
	
	@Override
	public View newView(Context arg0, Cursor cursor, ViewGroup arg2) {
		View view  = inflater.inflate(R.layout.ui_app_item, null);
		ViewHolder holder = new ViewHolder();
		
		holder.iconView = (ImageView) view.findViewById(R.id.app_icon_text_view);
		holder.nameView = (TextView) view.findViewById(R.id.app_name_textview);
		holder.sizeView = (TextView) view.findViewById(R.id.app_size_textview);
		view.setTag(holder);
		
		return view;
	}
	
	class ViewHolder{
		ImageView iconView;
		TextView nameView;
		TextView sizeView;
	}

}
