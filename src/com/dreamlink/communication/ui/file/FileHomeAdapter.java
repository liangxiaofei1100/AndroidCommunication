package com.dreamlink.communication.ui.file;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileHomeAdapter extends BaseAdapter {
	private static final String TAG = "FileHomeAdapter";
	private List<Integer> homeList = new ArrayList<Integer>();
	private LayoutInflater mInflater = null;

	private boolean mLoading = true;
	private List<Integer> mCountList = new ArrayList<Integer>();

	private static final int[] TYPE_ICONS = { R.drawable.default_doc_icon,
			R.drawable.default_ebook_icon, R.drawable.deafult_apk_icon,
			R.drawable.default_archive_icon };
	private String[] file_types;
	private String[] file_types_tips;

	public FileHomeAdapter(Context context, List<Integer> homeList) {
		mInflater = LayoutInflater.from(context);
		this.homeList = homeList;
		
		file_types = context.getResources().getStringArray(R.array.file_classify);
		file_types_tips = context.getResources().getStringArray(R.array.file_classify_tip);
	}

	public void setLoading(boolean loading) {
		mLoading = loading;
	}

	public void setCount(int doc, int ebook, int apk, int archive) {
		mCountList.clear();
		mCountList.add(doc);
		mCountList.add(ebook);
		mCountList.add(apk);
		mCountList.add(archive);
	}

	@Override
	public int getCount() {
		return homeList.size();
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	class ViewHolder {
		ImageView iconView;
		TextView nameView;
		TextView dateAndSizeView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		ViewHolder holder = null;

		if (null == convertView || null == convertView.getTag()) {
			holder = new ViewHolder();
			view = mInflater.inflate(R.layout.ui_file_item, parent, false);
			holder.iconView = (ImageView) view
					.findViewById(R.id.file_icon_imageview);
			holder.nameView = (TextView) view
					.findViewById(R.id.file_name_textview);
			holder.dateAndSizeView = (TextView) view
					.findViewById(R.id.file_info_textview);
			view.setTag(holder);
		} else {
			view = convertView;
			holder = (ViewHolder) view.getTag();
		}

		if (position < homeList.size() - 4) {
			switch (homeList.get(position)) {
			case FileBrowserFragment.INTERNAL:
				holder.iconView.setImageResource(R.drawable.storage_internal_n);
				holder.nameView.setText(R.string.internal_sdcard);
				holder.dateAndSizeView.setVisibility(View.GONE);
				break;
			case FileBrowserFragment.SDCARD:
				holder.iconView.setImageResource(R.drawable.storage_sd_card_n);
				holder.nameView.setText(R.string.sdcard);
				holder.dateAndSizeView.setVisibility(View.GONE);
				break;
			}

			return view;
		}

		int pos = position - (homeList.size() - 4);

		holder.iconView.setImageResource(TYPE_ICONS[pos]);
		holder.dateAndSizeView
				.setText(file_types_tips[pos]);
		if (mLoading) {
			holder.nameView.setText(file_types[pos] + "(加载中...)");
		}else {
			holder.nameView.setText(file_types[pos] + 
					"(" + mCountList.get(pos) +")");
		}

		return view;
	}

}
