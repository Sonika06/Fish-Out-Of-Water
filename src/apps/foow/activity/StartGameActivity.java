package apps.foow.activity;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import apps.foow.data.GlobalState;

import com.google.example.games.basegameutils.BaseGameActivity;

import com.google.android.gms.common.api.*;
import com.google.android.gms.games.Games;

public class StartGameActivity extends BaseGameActivity implements
		View.OnClickListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_game);
		GlobalState.level_number = 1;
		GlobalState.score = 0;

		findViewById(R.id.show_achievements).setOnClickListener(this);
		findViewById(R.id.show_leaderboard).setOnClickListener(this);

		findViewById(R.id.sign_in_button).setOnClickListener(this);
		findViewById(R.id.sign_out_button).setOnClickListener(this);

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

	public void goToOnePlayerGame(View view) {
		Intent firstLevel = new Intent(this, LevelActivity.class);
		startActivity(firstLevel);
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(true);
	}

	public void goToHelp(View view) {
		Intent help = new Intent(this, Help.class);
		startActivity(help);

	}

	public void goToHigh(View view) {

		Intent levelHigh = new Intent(this, FinalScoreActivity.class);
		this.startActivity(levelHigh);
	}

	public void onSignInSucceeded() {
		// show sign-out button, hide the sign-in button
		findViewById(R.id.sign_in_button).setVisibility(View.GONE);
		findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);

		// (your code here: update UI, enable functionality that depends on sign
		// in, etc)
	}

	public void onSignInFailed() {
		// Sign in has failed. So show the user the sign-in button.
		findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
		findViewById(R.id.sign_out_button).setVisibility(View.GONE);
	}

	public void onLoad(long loadTime) {

		// May return null if EasyTracker has not been initialized with a
		// property
		// ID.
		Tracker easyTracker = EasyTracker.getInstance(this);

		easyTracker.send(MapBuilder.createTiming("resources", // Timing category
				// (required)
				loadTime, // Timing interval in milliseconds (required)
				"Main", // Timing name
				null) // Timing label
				.build());
	}

	@Override
	public void onClick(View view) {

		if (view.getId() == R.id.sign_in_button) {
			// start the asynchronous sign in flow
			beginUserInitiatedSignIn();
		} else if (view.getId() == R.id.sign_out_button) {

			signOut();

			// show sign-in button, hide the sign-out button
			findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
			findViewById(R.id.sign_out_button).setVisibility(View.GONE);
		}

		else if (view.getId() == R.id.show_achievements) {

			if (getApiClient().isConnected()) {
				startActivityForResult(
						Games.Achievements
								.getAchievementsIntent(getApiClient()),
						1);
			} else {
				beginUserInitiatedSignIn();
			}
		} else if (view.getId() == R.id.show_leaderboard) {

			if (getApiClient().isConnected()) {
				startActivityForResult(Games.Leaderboards.getLeaderboardIntent(
						getApiClient(),
						getString(R.string.number_guesses_leaderboard)), 2);
			} else {
				beginUserInitiatedSignIn();
			}
		}

	}

}
