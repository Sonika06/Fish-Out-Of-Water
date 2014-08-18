package apps.foow.activity;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Help extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
	}

	@Override
	public void onStart() {
		super.onStart();
		// The rest of your onStart() code.
		EasyTracker.getInstance(this).activityStart(this); // Add this method.

	}

	@Override
	public void onStop() {
		super.onStop();
		// The rest of your onStop() code.
		EasyTracker.getInstance(this).activityStop(this); // Add this method.
	}

	@Override
	public void onBackPressed() {
		Intent startmenu = new Intent(this, StartGameActivity.class);
		startActivity(startmenu);
	}

	public void start(View view) {
		Intent menu = new Intent(this, StartGameActivity.class);
		startActivity(menu);
	}

	public void onLoad(long loadTime) {

		// May return null if EasyTracker has not been initialized with a
		// property
		// ID.
		Tracker easyTracker = EasyTracker.getInstance(this);

		easyTracker.send(MapBuilder.createTiming("resources", // Timing category
				// (required)
				loadTime, // Timing interval in milliseconds (required)
				"Help", // Timing name
				null) // Timing label
				.build());
	}

}