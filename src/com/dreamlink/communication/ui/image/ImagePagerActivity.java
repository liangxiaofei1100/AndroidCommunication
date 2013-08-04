package com.dreamlink.communication.ui.image;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

public class ImagePagerActivity extends Activity {

	private static final String STATE_POSITION = "STATE_POSITION";
	private static ImageLoader imageLoader = ImageLoader.getInstance();
	DisplayImageOptions options;
	ViewPager pager;
	private List<ImageInfo> imageList = new ArrayList<ImageInfo>();

	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_image_pager);

		Bundle bundle = getIntent().getExtras();
		imageList = bundle.getParcelableArrayList(Extra.IMAGE_INFO);
		int pagerPosition = bundle.getInt(Extra.IMAGE_POSITION, 0);

		// if (savedInstanceState != null) {
		// pagerPosition = savedInstanceState.getInt(STATE_POSITION);
		// }

		options = new DisplayImageOptions.Builder().showImageForEmptyUri(R.drawable.zapya_data_photo_l)
				.showImageOnFail(R.drawable.ic_picture_error).resetViewBeforeLoading(true).cacheOnDisc(true)
				.imageScaleType(ImageScaleType.EXACTLY).bitmapConfig(Bitmap.Config.RGB_565).displayer(new FadeInBitmapDisplayer(300))
				.build();

		pager = (ViewPager) findViewById(R.id.image_viewpager);
		pager.setAdapter(new ImagePagerAdapter(imageList));
		pager.setCurrentItem(pagerPosition);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_POSITION, pager.getCurrentItem());
	}

	private class ImagePagerAdapter extends PagerAdapter {

		private LayoutInflater inflater;
		private List<ImageInfo> list;

		ImagePagerAdapter(List<ImageInfo> data) {
			this.list = data;
			inflater = getLayoutInflater();
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager) container).removeView((View) object);
		}

		@Override
		public void finishUpdate(View container) {
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object instantiateItem(ViewGroup view, int position) {
			View imageLayout = inflater.inflate(R.layout.ui_image_pager_item, view, false);
			ImageView imageView = (ImageView) imageLayout.findViewById(R.id.image_pager);
			final ProgressBar spinner = (ProgressBar) imageLayout.findViewById(R.id.loading_image);

			String path = "file://" + list.get(position).getPath();
			imageLoader.displayImage(path, imageView, options, new SimpleImageLoadingListener() {
				@Override
				public void onLoadingStarted(String imageUri, View view) {
					spinner.setVisibility(View.VISIBLE);
				}

				@Override
				public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
					String message = null;
					switch (failReason.getType()) {
					case IO_ERROR:
						message = "Input/Output error";
						break;
					case DECODING_ERROR:
						message = "Image can't be decoded";
						break;
					case NETWORK_DENIED:
						message = "Downloads are denied";
						break;
					case OUT_OF_MEMORY:
						message = "Out Of Memory error";
						break;
					case UNKNOWN:
						message = "Unknown error";
						break;
					}
					Toast.makeText(ImagePagerActivity.this, message, Toast.LENGTH_SHORT).show();

					spinner.setVisibility(View.GONE);
				}

				@Override
				public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
					spinner.setVisibility(View.GONE);
				}
			});

			((ViewPager) view).addView(imageLayout, 0);
			return imageLayout;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view.equals(object);
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View container) {
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		imageLoader.stop();
		imageLoader.clearMemoryCache();
		imageLoader.clearDiscCache();
	}
}