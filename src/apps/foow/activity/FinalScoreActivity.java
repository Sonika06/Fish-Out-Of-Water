package apps.foow.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FinalScoreActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.final_score);
		setBackground();

		TextView scoreView = (TextView) findViewById(R.id.scoreList);

		SharedPreferences scorePrefs = getSharedPreferences(
				LevelActivity.GAME_PREFS, 0);
		// scores
		String[] savedScores = scorePrefs.getString("highScores", "").split(
				"\\|");

		StringBuilder scoreBuild = new StringBuilder("");
		for (String score : savedScores) {
			scoreBuild.append(score + "\n");
		}
		// display
		scoreView.setText(scoreBuild.toString());
	}

	private void setBackground() {
		RelativeLayout relLay = (RelativeLayout) findViewById(R.id.scoreLayout);
		Drawable bg = relLay.getBackground();
		bg.setAlpha(100);
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
}