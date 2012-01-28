package apollo.gui;

import java.io.IOException;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/** Refactored abstract class for score calculators that store the scores in
 *  memory.
 */

public abstract class StoredScoreCalculator extends WindowScoreCalculator {

  //class variables
  protected final static Logger logger = LogManager.getLogger(StoredScoreCalculator.class);

  //instance variables
  protected TreeMap<Integer, Double> scoreMap;
  protected double minScore;
  protected double maxScore;

  /** Builds a StoredScoreCalculator object
   * 
   * @param winSize - size of display window
   */
  public StoredScoreCalculator(int winSize)
  {
    super(winSize);
    scoreMap = new TreeMap<Integer, Double>();
    minScore = Double.POSITIVE_INFINITY;
    maxScore = Double.NEGATIVE_INFINITY;
    factor = 1;
  }
  
  /** Gets the score range for all scores [minScore, maxScore]
   * 
   * @return the score range for all scores
   */
  @Override
  public int[] getYRange()
  {
    return new int[] { (int)(minScore * factor), (int)(maxScore * factor)};
  }
  
  public double getScoreForPosition(int position)
  {
    Double score = scoreMap.get(position);
    return score != null ? score.doubleValue() * factor : 0;
  }
  
  /** Get the scores for an array of positions
   * 
   * @param positions - positions to get the scores for
   * @return an array of scores for each position
   */
  @Override
  public double[] getScoresForPositions(int[] positions) {
    double []scores = new double[positions.length];
    for (int i = 0; i < positions.length; ++i) {
      Double score = scoreMap.get(positions[i]);
      if (score != null) {
        scores[i] = score.doubleValue() * factor;
      }
    }
    return scores;
  }

  /** Get the position range covered by the scores [lowestPosition, highestPosition]
   * 
   * @return the position range
   */
  @Override
  public int[] getXRange() {
    if (scoreMap.size() > 0) {
      return new int [] { scoreMap.firstKey(), scoreMap.lastKey() };
    }
    else {
      return new int [] {1,1};
    }
  }

  /** Get the minimum score for this set
   * 
   * @return mininum score for this set
   */
  public double getMinScore()
  {
    return minScore * factor;
  }
  
  /** Get the maximum score for this set
   * 
   * @return maximum score for this set
   */
  public double getMaxScore()
  {
    return maxScore * factor;
  }

}
