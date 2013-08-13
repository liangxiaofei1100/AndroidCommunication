package com.dreamlink.communication.ui;

import com.dreamlink.communication.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ExitActivity extends Activity implements OnClickListener {
	
	private Button okBtn,cancelBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_exit_dialog);
		
		okBtn = (Button) findViewById(R.id.ok_button);
		cancelBtn = (Button) findViewById(R.id.cancel_button);
		
		okBtn.setText(android.R.string.ok);
		cancelBtn.setText(android.R.string.cancel);
		
		okBtn.setOnClickListener(this);
		cancelBtn.setOnClickListener(this);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		this.finish();
		return true;
	}

	@Override
	public void onClick(View v) {	
		switch (v.getId()) {
		case R.id.ok_button:
			this.finish();
			MainUIFrame.instance.finish();
			break;
		case R.id.cancel_button:
			this.finish();
			break;

		default:
			break;
		}
	}
}
