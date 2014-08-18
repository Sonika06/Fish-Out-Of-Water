package apps.foow.activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import apps.foow.components.Levels;
import apps.foow.data.GlobalState;

public class LevelActivity extends Levels {

	private static TextView scoreboard = null;

	private long levelDurationMili;
	private int obstacles;
	private int stepsToWin;
	private int currentLevel;
	private int badges;

	private static SharedPreferences gamePrefs;
	public static final String GAME_PREFS = "Fish Out Of Water";

	private Intent levelIntent;
	private Intent introIntent;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.level_layout);
		setLevelView(findViewById(R.id.level_layout));

		gamePrefs = getSharedPreferences(GAME_PREFS, 0);
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

		currentLevel = getLevelNumber();
		initLevelFeatures(currentLevel);
		setNumOfObstacles(obstacles);
		setNumOfStepsToWin(stepsToWin);
		setBadges(badges);

		levelIntent = new Intent(this, LevelActivity.class);
		introIntent = new Intent(this, StartGameActivity.class);

		TextView timerTextView = (TextView) findViewById(R.id.timerText);
		TextView stepView = (TextView) findViewById(R.id.stepsTextView);
		TextView instrucView = (TextView) findViewById(R.id.instrucTextView);

		scoreboard = (TextView) findViewById(R.id.scoreLabel);
		Button startButton = (Button) findViewById(R.id.startAnimationBtn);
		startButton.setText("Start Level " + currentLevel);

		// initSound(); // Moved this to onResume in Levels abstract class
		initTracking();

		// setAnimationDrawable(R.anim.goldfish_animation);

		setTextViews(timerTextView, stepView, instrucView, scoreboard);

		setCurrentIntent(levelIntent);
		setIntroIntent(introIntent);

		if (currentLevel > 1) {
			setPreviousIntent(levelIntent);
		} else {

			setPreviousIntent(introIntent);
		}

		initGameTimer(levelDurationMili);

		// GAME STARTS AFTER ANIMATION
		initAnimation(startButton); // starts game after animation is done

		instrucView.setText("Steps To Win: " + stepsToWin);
		scoreboard.setText("Score: " + GlobalState.myScore);

		if (savedInstanceState != null) {
			// saved instance state data
			int oldScore = savedInstanceState.getInt("score");
			scoreboard.setText("Score: " + oldScore);
		}

	}

	public void initLevelFeatures(int level) {
		switch (level) {
		case 1:
			levelDurationMili = 16000;
			obstacles = 1;
			stepsToWin = 20;
			badges = 0;
			break;
		case 2:
			levelDurationMili = 31000;
			obstacles = 3;
			stepsToWin = 40;
			badges = 1;
			break;
		case 3:
			levelDurationMili = 46000;
			obstacles = 4;
			stepsToWin = 60;
			badges = 1;
			break;
		case 4:
			levelDurationMili = 61000;
			obstacles = 5;
			stepsToWin = 80;
			badges = 2;
			break;
		case 5:
			levelDurationMili = 76000;
			obstacles = 6;
			stepsToWin = 100;
			badges = 2;
			break;
		case 6:
			levelDurationMili = 91000;
			obstacles = 8;
			stepsToWin = 120;
			badges = 2;
			break;
		case 7:
			levelDurationMili = 106000;
			obstacles = 10;
			stepsToWin = 140;
			badges = 3;
			break;
		case 8:
			levelDurationMili = 121000;
			obstacles = 11;
			stepsToWin = 160;
			badges = 3;
			break;
		case 9:
			levelDurationMili = 136000;
			obstacles = 12;
			stepsToWin = 180;
			badges = 3;
			break;
		case 10:
			levelDurationMili = 151000;
			obstacles = 14;
			stepsToWin = 200;
			badges = 4;
			break;
		case 11:
			levelDurationMili = 166000;
			obstacles = 15;
			stepsToWin = 220;
			badges = 4;
			break;
		case 12:
			levelDurationMili = 176000;
			obstacles = 15;
			stepsToWin = 240;
			badges = 5;
			break;
		case 13:
			levelDurationMili = 191000;
			obstacles = 15;
			stepsToWin = 260;
			badges = 5;
			break;
		case 14:
			levelDurationMili = 206000;
			obstacles = 17;
			stepsToWin = 280;
			badges = 5;
			break;
		case 15:
			levelDurationMili = 221000;
			obstacles = 20;
			stepsToWin = 300;
			badges = 6;
			break;
		}

	}

	private static int getScoreBoard() {

		int fade = 0;
		String scoreStr = scoreboard.getText().toString();
		try {
			fade = Integer.parseInt(scoreStr.substring(scoreStr
					.lastIndexOf(" ") + 1));
		} catch (Exception e) { /* log if you want */
			// // fade = 1500;
			// catch (NumberFormatException e) {
			// Log.e("Exception", e.toString());
		}
		return fade;
	}

	public static void setHighScore() {
		int oldScore = getScoreBoard();

		if (oldScore > 0) {
			SharedPreferences.Editor scoreEdit = gamePrefs.edit();

			DateFormat dateView = new SimpleDateFormat("dd MMMM");
			String date = dateView.format(new Date());

			String scores = gamePrefs.getString("highScores", "");

			if (scores.length() > 0) {

				// Existing scores
				List<Score> scoreStrings = new ArrayList<Score>();

				String[] oldScores = scores.split("\\|");

				for (String eSc : oldScores) {
					String[] parts = eSc.split(" - ");
					scoreStrings.add(new Score(parts[0], Integer
							.parseInt(parts[1])));
				}

				Score newScore = new Score(date, oldScore);
				scoreStrings.add(newScore);

				Collections.sort(scoreStrings);
				// Top Ten
				StringBuilder scoreBuild = new StringBuilder("");
				for (int s = 0; s < scoreStrings.size(); s++) {
					if (s >= 10)
						break;
					if (s > 0)
						scoreBuild.append("|");
					scoreBuild.append(scoreStrings.get(s).getScoreText());
				}

				scoreEdit.putString("highScores", scoreBuild.toString());
				scoreEdit.commit();

			} else {

				scoreEdit.putString("highScores", "" + date + " - " + oldScore);
				scoreEdit.commit();
			}
		}
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
	protected void onDestroy() {
		setHighScore();
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {

		int oldScore = getScoreBoard();
		savedInstanceState.putInt("score", oldScore);

		super.onSaveInstanceState(savedInstanceState);
	}


	public void onLoad(long loadTime) {

		// May return null if EasyTracker has not been initialized with a
		// property
		// ID.
		Tracker easyTracker = EasyTracker.getInstance(this);

		easyTracker.send(MapBuilder.createTiming("resources", // Timing category
				// (required)
				loadTime, // Timing interval in milliseconds (required)
				"Levels", // Timing name
				null) // Timing label
				.build());
	}

	@Override
	public void onSignInFailed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSignInSucceeded() {
		// TODO Auto-generated method stub
		
	}

}
