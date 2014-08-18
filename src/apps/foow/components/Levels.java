package apps.foow.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import apps.foow.activity.LevelActivity;
import apps.foow.activity.R;
import apps.foow.data.AccelerometerData;
import apps.foow.data.GlobalState;
import apps.acceltest.activity.IAccelRemoteService;

public abstract class Levels extends BaseGameActivity implements SensorEventListener {

	static final float NS2S = 1.0f / 1000000000.0f;
	float[] last_values = null;
	float[] velocity = null;
	float[] position = null;
	long last_timestamp = 0;

	private long prevUpdate = 0;
	private float prev_x, prev_y, prev_z;
	private static final int SHAKE_THRESHOLD = 500;

	private long prevShakeTimestamp = 0;
	private double shake_threshold = 500;

	private List<Sensor> sensors;
	private Sensor sensor;
	private long lastUpdate = -1;
	private long currentTime = -1;

	private float last_x, last_y, last_z;
	private float current_x, current_y, current_z, currenForce;
	private static final int FORCE_THRESHOLD = 900;
	private final int DATA_X = SensorManager.DATA_X;
	private final int DATA_Y = SensorManager.DATA_Y;
	private final int DATA_Z = SensorManager.DATA_Z;

	private SoundPool pool = null;
	private MyTimer countDownTimer;

	private TextView stepsTextView;

	private TextView timerTextView;
	private TextView instrucTextView;

	private View currentView;
	private Button startButton;
	// Animation and Audio
	// private int drawableID = R.anim.goldfish_animation;
	private int beginSplash;
	private int endSplash;
	private int crowdCheer;
	private int loseBuzz;
	private int turnRight;

	int counter;
	// private int shake;
	private int turnLeft;
	private int turnAround;
	private int walkPrompt;
	// Level Characteristics
	private int pointsForLevel = 10;
	private int numOfObstacles;
	private int numBadges;
	private int numOfSteps;
	private int winningSteps;
	private boolean isLastLevel;
	private boolean endOfLevel;
	private int[] obstacleAtSteps;
	// Sensors
	// sensor manager objects
	private SensorManager sensorManager;
	private Sensor sensorAccelerometer;
	private Sensor sensorMagneticField;

	private float[] valuesAccelerometer;
	private float[] valuesMagneticField;
	private float[] matrixR;
	private float[] matrixI;
	private float[] matrixValues;

	private ArrayList<AccelerometerData> accelerationSamples;
	private AccelerometerData prev;
	private double azimuth;
	// private TextView stepView;
	private Intent currentIntent;
	private Intent previousIntent;
	private Intent introIntent;

	private TextView scoreLabel;

	private static TextView scoreboard = null;

	private static final int START_SCORE = 0;

	private boolean isObstacleCleared;
	private String preObstacleDir = "";
	private int obstacleSelected = 0;

	private String currDirection = "";

	private static final long COUNTDOWN_INTERVAL = 1000;
	private static final String NORTH = "North";
	private static final String SOUTH = "South";
	private static final String EAST = "East";
	private static final String WEST = "West";
	private static final String GAME_NAME = "Fish Out of Water";
	private static final String INSTRUCTIONS = "Walk to catch up to Goldie!";
	private static final int OB_TURN_LEFT = 1;
	private static final int OB_TURN_RIGHT = 2;
	private static final int OB_TURN_AROUND = 3;
	private static final int LAST_LEVEL = 15;

	private RemoteServiceConnection conn;
	private IAccelRemoteService remoteService;

	public void gameStart() {

		scoreboard = (TextView) findViewById(R.id.scoreLabel);

		pool.play(walkPrompt, 1, 1, 1, 0, 1);
		counter = 0;
		numOfSteps = 0;
		endOfLevel = false;
		initRunTextView();
		initObstacles();

		registerListeners();
		if (isAccelServiceRunning()) {
			bindService();
		}
		countDownTimer.start();
	}

	/******************** Remote Service ***************************/
	private void bindService() {
		if (conn == null) {
			conn = new RemoteServiceConnection();
			Intent i = new Intent();
			i.setClassName("apps.acceltest.activity",
					"apps.acceltest.activity.AccelerometerService");
			bindService(i, conn, Context.BIND_AUTO_CREATE);
		}
	}

	private void releaseService() {
		if (conn != null) {
			conn.serviceAppendEndGame();
			unbindService(conn);
			conn = null;
		}
	}

	private boolean isAccelServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("apps.acceltest.activity.AccelerometerService"
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	class RemoteServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			remoteService = IAccelRemoteService.Stub.asInterface(boundService);
			try {
				remoteService.setGameNameFromService(GAME_NAME + " Level: "
						+ getLevelNumber());
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d(getClass().getSimpleName(), "onServiceConnected()");
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {

			remoteService = null;
			Log.d(getClass().getSimpleName(), "onServiceDisconnected");
		}

		public void serviceAppendEndGame() {
			try {
				remoteService.setEndGameFlagFromService(true);
				remoteService.setGameNameFromService(GAME_NAME + " Level: "
						+ getLevelNumber());
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	/***************** End Remote Service ******************************/

	public void setLevelNumber(int levelNum) {
		GlobalState.level_number = levelNum;
	}

	public int getLevelNumber() {
		return GlobalState.level_number;
	}

	private void setDirection(String newDirection) {
		currDirection = newDirection;
	}

	private String getCurrDirection() {
		return currDirection;
	}

	private void updateDirection() {
		if (isFacingNorth()) {
			setDirection(NORTH);
		} else if (isFacingSouth()) {
			setDirection(SOUTH);
		} else if (isFacingEast()) {
			setDirection(EAST);
		} else if (isFacingWest()) {
			setDirection(WEST);
		}
	}

	// -------- ACCELERATION AND MAGENTIC FIELD CODE
	// ------------------------------------------------
	public void initTracking() {
		initAccelerationContainers();
		initSensorManagement();
		initValueArrays();
	}

	private void initAccelerationContainers() {
		accelerationSamples = new ArrayList<AccelerometerData>();
	}

	private void initSensorManagement() {
		// this.shake_threshold=this.shake_threshold
		// *SensorManager.GRAVITY_EARTH
		// *SensorManager.GRAVITY_EARTH;
		//
		// make sensor manager
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorAccelerometer = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorMagneticField = sensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}

	private void initValueArrays() {
		valuesAccelerometer = new float[3];
		valuesMagneticField = new float[3];

		matrixR = new float[9];
		matrixI = new float[9];
		matrixValues = new float[3];
	}

	private void registerListeners() {
		sensorManager.registerListener(this, sensorAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this, sensorMagneticField,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	private boolean isFacingNorth() {
		return ((azimuth > 0 && azimuth < 45) || (azimuth > -45 && azimuth <= 0));
	}

	private boolean isFacingEast() {
		return (azimuth > 45 && azimuth < 135);
	}

	private boolean isFacingSouth() {
		return ((azimuth > 135 && azimuth < 180) || (azimuth > -180 && azimuth < -135));
	}

	private boolean isFacingWest() {
		return (azimuth > -135 && azimuth < -45);
	}

	@Override
	protected void onResume() {

		super.onResume();

		initSound();
	}

	@Override
	protected void onPause() {

		super.onPause();
		if (pool != null) {
			pool.release();
			pool = null;
		}
	}

	@Override
	protected void onStop() {
		unregisterListeners();
		stopTimer();
		releaseService();

		super.onStop();
	}

	private void unregisterListeners() {
		try {
			sensorManager.unregisterListener(this);
		} catch (NullPointerException e) {
			Log.e("UNREGISTER LISTENER",
					"Null pointer during unregistering listener");
		}
	}

	private void stopTimer() {
		countDownTimer.cancel();
	}

	private void startTimer() {
		countDownTimer.start();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Must be implemented, but no functionality

	}

	/**
	 * Changing value in text view to value from accelerometer.
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (last_values != null) {
			float dt = (event.timestamp - last_timestamp) * NS2S;

			for (int index = 0; index < 3; ++index) {
				velocity[index] += (event.values[index] + last_values[index])
						/ 2 * dt;
				position[index] += velocity[index] * dt;
			}
		} else {
			last_values = new float[3];
			velocity = new float[3];
			position = new float[3];
			velocity[0] = velocity[1] = velocity[2] = 0f;
			position[0] = position[1] = position[2] = 0f;
		}
		System.arraycopy(event.values, 0, last_values, 0, 3);
		last_timestamp = event.timestamp;

		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			accelerometerHandler(event);
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			magnetometerHandler(event);
			break;
		}

		updateGameState();
	}

	private void accelerometerHandler(SensorEvent event) {
		for (int i = 0; i < 3; i++) {
			valuesAccelerometer[i] = event.values[i];
			addAccelerationSamples(event);

			if (prev != null) {
				AccelerometerData curr = new AccelerometerData(event.values[0],
						event.values[1], event.values[2]);
				if (isStep(prev, curr) && isObstacleCleared()) {

					updatePlayerSteps();
					updateStepView(numOfSteps);
					if (getNumOfObstacles() > 0) {
						checkObstacleEvent(numOfSteps);
					}
				}
				prev = curr;
			} else {
				prev = new AccelerometerData(event.values[0], event.values[1],
						event.values[2]);
			}

			currentTime = System.currentTimeMillis();

			if ((currentTime - lastUpdate) > 100) {
				long diffTime = (currentTime - lastUpdate);
				lastUpdate = currentTime;

				current_x = event.values[DATA_X];
				current_y = event.values[DATA_Y];
				current_z = event.values[DATA_Z];

				currenForce = Math.abs(current_x + current_y + current_z
						- last_x - last_y - last_z)
						/ diffTime * 10000;

				if (currenForce > FORCE_THRESHOLD) {

					counter += 1;
					instrucTextView.setText("SHAKE IT!");

					// if(counter > 3)
					// {
					//
					// updateStepView(numOfSteps--);
					// }
				}
				last_x = current_x;
				last_y = current_y;
				last_z = current_z;

			}

		}

	}

	private void addAccelerationSamples(SensorEvent event) {
		accelerationSamples.add(new AccelerometerData(event.values[0],
				event.values[1], event.values[2]));

	}

	private void updatePlayerSteps() {
		numOfSteps++;
		int halfway = winningSteps / 2;
		if ((countDownTimer.getSecondsRemaining() <= (countDownTimer
				.getSecondsStartedWith() / 2)) && numOfSteps < halfway) {
			instrucTextView.setText("Hurry Up! You are behind the timer.");
		} else if (numOfSteps >= halfway) {
			instrucTextView
					.setText("You're over halfway to Goldie! Keep walking!");
		} else {
			instrucTextView.setText(INSTRUCTIONS);
		}
	}

	private boolean isStep(AccelerometerData prev, AccelerometerData curr) {
		// if (curr.getX() != prev.getX() || (curr.getY() != prev.getY())) {
		if (curr.getZ() > 9.8 && prev.getZ() < 9)

			// // {
			// if (((curr.getX() - prev.getX()) > 2)
			// || ((curr.getX() - prev.getX()) < -2))
			//
			// {
			// if (((curr.getY() - prev.getY()) > 2)
			// || ((curr.getY() - prev.getY()) < -2))
			return true;
		// }
		//
		// }
		// }
		// return false;

		// return (curr.getZ() > 9 && prev.getZ() < 9);

		return false;

	}

	private void magnetometerHandler(SensorEvent event) {
		for (int i = 0; i < 3; i++) {
			valuesMagneticField[i] = event.values[i];
		}
	}

	private void updateAzimuth(double newAzimuth) {
		azimuth = newAzimuth;
	}

	private void updateGameState() {

		boolean success = SensorManager.getRotationMatrix(matrixR, matrixI,
				valuesAccelerometer, valuesMagneticField);

		if (success) {
			SensorManager.getOrientation(matrixR, matrixValues);
			updateAzimuth(Math.toDegrees(matrixValues[0]));
			updateDirection();
			obstacleListener();
		}

		if (!endOfLevel) {
			if (numOfSteps >= getNumOfStepsToWin()
					&& !countDownTimer.isFinished()) {
				levelCompleted();
			} else if (countDownTimer.isFinished()) {
				levelFailed();

			}
		}
	}

	// -----------END OF ACCELEROMETER AND MAGNETIC FIELD CODE
	// --------------------------------------------

	public void resetScore() {
		GlobalState.myScore = START_SCORE;
	}

	public int getScore() {
		return GlobalState.myScore;

	}

	private void vibrate() {
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(300);
	}

	public void initGameTimer(long numberOfMiliSec) {
		countDownTimer = new MyTimer(numberOfMiliSec, COUNTDOWN_INTERVAL,
				timerTextView);
	}

	public void initRunTextView() {
		instrucTextView.setText(INSTRUCTIONS);
	}

	public void setTextViews(TextView timerView, TextView stepsView,
			TextView instrucView, TextView scoreboard) {
		timerTextView = timerView;
		stepsTextView = stepsView;
		instrucTextView = instrucView;
		scoreLabel = scoreboard;
	}

	/**
	 * This method must be called before initAnimation in the child class.
	 * 
	 * @param newDrawableID
	 *            default value is goldfish_animation
	 */
	public void setAnimationDrawable(int newDrawableID) {
		// drawableID = newDrawableID;
	}

	public void initSound() {
		pool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		beginSplash = pool.load(this, R.raw.splash_out, 1);
		endSplash = pool.load(this, R.raw.splash_in, 1);
		crowdCheer = pool.load(this, R.raw.crowd_cheer, 1);
		loseBuzz = pool.load(this, R.raw.lose_buzz, 1);
		turnRight = pool.load(this, R.raw.turn_right, 1);
		turnLeft = pool.load(this, R.raw.turn_left, 1);
		turnAround = pool.load(this, R.raw.turn_around, 1);
		walkPrompt = pool.load(this, R.raw.walk_prompt, 1);
	}

	public void levelCompleted() {

		pool.play(endSplash, 1, 1, 1, 0, 1);
		pool.play(crowdCheer, 1, 1, 1, 0, 1);

		GlobalState.myScore += pointsForLevel + (getLevelNumber() * 10);

		if (getLevelNumber() == LAST_LEVEL) {
			isLastLevel = true;
		}
		endOfLevel = true;
		this.onStop();
		setLevelNumber(getLevelNumber() + 1);
		showEndOfLevelAlert(true, isLastLevel);
	}

	public void levelFailed() {
		currentView.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.lost));
		pool.play(loseBuzz, 1, 1, 1, 0, 1);
		vibrate();
		endOfLevel = true;
		this.onStop();
		showEndOfLevelAlert(false, false);
	}

	public void showEndOfLevelAlert(boolean completed, final boolean isLast) {
		scoreLabel.setText("Score: " + getScore());
		int b = getNumBadges();

		AlertDialog.Builder alertBox = new AlertDialog.Builder(this);
		alertBox.setTitle(" ");
		switch (b) {
		case 1:
			alertBox.setIcon(R.drawable.award);
			if (getApiClient().isConnected()) {
				
				Games.Achievements.increment(getApiClient(),
						getString(R.string.correct_guess_achievement), 2);
				
			}
			break;
		case 2:
			alertBox.setIcon(R.drawable.award2);
			if (getApiClient().isConnected()) {
				
				Games.Achievements.increment(getApiClient(),
						getString(R.string.achievement_4),3);
				}
			break;
		case 3:
			alertBox.setIcon(R.drawable.award3);
			if (getApiClient().isConnected()) {
			
				Games.Achievements.increment(getApiClient(),
						getString(R.string.achievement_7),6);
				}
			break;
		case 4:
			alertBox.setIcon(R.drawable.award4);
			
			if (getApiClient().isConnected()) {
				
				Games.Achievements.increment(getApiClient(),
						getString(R.string.achievement_10),9);
				}break;
		case 5:
			alertBox.setIcon(R.drawable.award5);
			if (getApiClient().isConnected()) {
			
				Games.Achievements.increment(getApiClient(),
						getString(R.string.achievement_12),11);
					}
			break;
		case 6:
			alertBox.setIcon(R.drawable.champ);
			if (getApiClient().isConnected()) {
			
				Games.Achievements.increment(getApiClient(),
						getString(R.string.achievement_15),13);
				}
			break;

		}

		if (completed && !isLast) {
			alertBox.setMessage("Hooray! You saved Goldie! You have "
					+ getScore() + " points! Time to move to Level "
					+ getLevelNumber() + ". "
					+ (LAST_LEVEL - getLevelNumber() + 1) + " more to go!!");
			// if(numBadges> 0)
			LevelActivity.setHighScore();
			if (getApiClient().isConnected()) {
				Games.Leaderboards.submitScore(getApiClient(),
						getString(R.string.number_guesses_leaderboard),
						getScore());
			}
		} else if (completed && isLast) {

			alertBox.setMessage("Congratulations! You have " + getScore()
					+ " points! You cleared every level!");
			LevelActivity.setHighScore();
			if (getApiClient().isConnected()) {
				Games.Leaderboards.submitScore(getApiClient(),
						getString(R.string.number_guesses_leaderboard),
						getScore());
			}

		} else {
			alertBox.setMessage("You Coudn't save Goldie! Try again!");
		}
		alertBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialogInterface, int arg1) {
				if (isLast) {

					startActivity(getIntroIntent());
					resetScore();

				} else {
					startActivity(getCurrentIntent());
				}

				dialogInterface.cancel();

			}

		});

		alertBox.setCancelable(false);
		alertBox.create().show();

	}

	public void initAnimation(Button startBtn) {

		startButton = startBtn;

		startButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View arg0) { /*
											 * currentView.setBackgroundDrawable(
											 * introCad);
											 * introCad.setOneShot(true); if
											 * (!introCad.isRunning()) { //
											 * Start the animation
											 * introCad.start();
											 * pool.play(beginSplash, 1, 1, 1,
											 * 0, 1);
											 * startButton.setVisibility(View
											 * .GONE); }
											 */
				currentView.setBackgroundDrawable(getResources().getDrawable(
						R.drawable.fish));

				// currentView.getBackground().setCallback(null);
				startButton.setVisibility(View.GONE);
				gameStart(); // starts game
			}
		});

	}

	private void initObstacles() {
		int numObstacles = getNumOfObstacles();

		isObstacleCleared = true;
		if (numObstacles > 0) {
			int stepsWin = getNumOfStepsToWin();
			int obstacleStepInterval = stepsWin / (numObstacles + 1);
			obstacleAtSteps = new int[numObstacles];

			for (int i = 0; i < obstacleAtSteps.length; i++) {
				int obstacleAtStep = obstacleStepInterval * (i + 1);
				if (obstacleAtStep > stepsWin) {
					obstacleAtStep = stepsWin;
				}
				obstacleAtSteps[i] = obstacleAtStep;
			}

		}
	}

	private void obstacleCleared() {

		isObstacleCleared = true;
		preObstacleDir = "";
		obstacleSelected = 0;
		instrucTextView.setText(INSTRUCTIONS);

	}

	private void obstacleListener() {
		if (!preObstacleDir.equals("") && obstacleSelected != 0) {
			switch (obstacleSelected) {
			case OB_TURN_LEFT:
				if (playerTurnLeft(preObstacleDir)) {
					obstacleCleared();
				}
				break;
			case OB_TURN_RIGHT:
				if (playerTurnRight(preObstacleDir)) {
					obstacleCleared();
				}
				break;
			case OB_TURN_AROUND:
				if (playerTurnAround(preObstacleDir)) {
					obstacleCleared();
				}
				break;
			}
		}
	}

	private boolean checkObstacleEvent(int currStep) {
		boolean obstacleFired = false;

		for (int i = 0; i < obstacleAtSteps.length; i++) {
			if (currStep == obstacleAtSteps[i]) {
				vibrate();
				preObstacleDir = getCurrDirection();
				isObstacleCleared = false;
				// POP UP STOP IMAGE AND INSTRUCTIONS
				instrucTextView.setText("STOP! YOU MUST TURN ");
				obstacleSelected = obstacleRandomizer();
				switch (obstacleSelected) {
				case OB_TURN_LEFT:
					// instrucTextView.setBackgroundResource(R.drawable.left);
					instrucTextView.append("LEFT");
					pool.play(turnLeft, 1, 1, 1, 0, 1);
					break;
				case OB_TURN_RIGHT:
					// instrucTextView.setBackgroundResource(R.drawable.right);
					instrucTextView.append("RIGHT");
					pool.play(turnRight, 1, 1, 1, 0, 1);
					break;
				case OB_TURN_AROUND:

					// instrucTextView.setBackgroundResource(R.drawable.around);
					instrucTextView.append("AROUND");
					pool.play(turnAround, 1, 1, 1, 0, 1);
					break;
				}
				instrucTextView.setVisibility(View.VISIBLE);
				obstacleFired = true;
				numOfObstacles--;
				break;
			}
		}
		return obstacleFired;
	}

	private boolean playerTurnLeft(String prevDirection) {
		boolean turnedLeft = false;
		if ((prevDirection.equals(NORTH) && getCurrDirection().equals(WEST))
				|| (prevDirection.equals(SOUTH) && getCurrDirection().equals(
						EAST))
				|| (prevDirection.equals(EAST) && getCurrDirection().equals(
						NORTH))
				|| (prevDirection.equals(WEST) && getCurrDirection().equals(
						SOUTH))) {
			turnedLeft = true;
		}
		return turnedLeft;
	}

	private boolean playerTurnRight(String prevDirection) {
		boolean turnedRight = false;
		if ((prevDirection.equals(NORTH) && getCurrDirection().equals(EAST))
				|| (prevDirection.equals(SOUTH) && getCurrDirection().equals(
						WEST))
				|| (prevDirection.equals(EAST) && getCurrDirection().equals(
						SOUTH))
				|| (prevDirection.equals(WEST) && getCurrDirection().equals(
						NORTH))) {
			turnedRight = true;
		}
		return turnedRight;
	}

	private boolean playerTurnAround(String prevDirection) {
		boolean turnedAround = false;
		if ((prevDirection.equals(NORTH) && getCurrDirection().equals(SOUTH))
				|| (prevDirection.equals(SOUTH) && getCurrDirection().equals(
						NORTH))
				|| (prevDirection.equals(EAST) && getCurrDirection().equals(
						WEST))
				|| (prevDirection.equals(WEST) && getCurrDirection().equals(
						EAST))) {
			turnedAround = true;
		}
		return turnedAround;
	}

	private int obstacleRandomizer() {
		Random gen = new Random();
		int pickedNumber = gen.nextInt(3) + 1;
		return pickedNumber;
	}

	public void updateStepView(int steps) {
		stepsTextView.setText("Steps: " + steps);
	}

	private boolean isObstacleCleared() {
		return isObstacleCleared;
	}

	/**
	 * 
	 * @param newNumOfObstacles
	 */
	public void setNumOfObstacles(int newNumOfObstacles) {
		numOfObstacles = newNumOfObstacles;
	}

	public int getNumOfObstacles() {
		return numOfObstacles;
	}

	public void setNumOfStepsToWin(int stepsToWin) {
		winningSteps = stepsToWin;
	}

	protected void setBadges(int badges2) {
		numBadges = badges2;
	}

	private int getNumBadges() {
		return numBadges;
	}

	private int getNumOfStepsToWin() {
		return winningSteps;
	}

	@Override
	public void onBackPressed() {

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					GlobalState.level_number = 1;
					resetScore();
					// startActivity(getPreviousIntent());
					startActivity(getIntroIntent());

					break;

				case DialogInterface.BUTTON_NEGATIVE:
					dialog.cancel();
					break;
				}
			}
		};
		if (getApiClient().isConnected()) {
			Games.Leaderboards.submitScore(getApiClient(),
					getString(R.string.number_guesses_leaderboard),
					getScore());
		}
		LevelActivity.setHighScore();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to quit?")
				.setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener).show();

	}

	public void setCurrentIntent(Intent currIntent) {
		currentIntent = currIntent;
	}

	private Intent getCurrentIntent() {
		return currentIntent;
	}

	public void setPreviousIntent(Intent prevIntent) {
		previousIntent = prevIntent;
	}

	public void setIntroIntent(Intent newIntent) {
		introIntent = newIntent;
	}

	private Intent getIntroIntent() {
		return introIntent;
	}

	public void setLevelView(View view) {
		currentView = view;
	}

}
