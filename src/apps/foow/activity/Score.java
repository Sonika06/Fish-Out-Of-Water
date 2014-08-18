package apps.foow.activity;

public class Score implements Comparable<Score> {

	private String date;
	public int score;

	public Score(String Sdate, int num) {
		date = Sdate;
		score = num;
	}

	/**
	 * Sorting scores Sort in Descending order
	 */
	@Override
	public int compareTo(Score sc) {

		return sc.score > score ? 1 : sc.score < score ? -1 : 0;
	}

	public String getScoreText() {
		return date + " - " + score;
	}
}
