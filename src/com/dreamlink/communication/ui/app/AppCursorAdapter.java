package com.dreamlink.communication.ui.app;

import java.io.File;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class AppCursorAdapter extends CursorAdapter {
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
