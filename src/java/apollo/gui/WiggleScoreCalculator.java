package apollo.gui;

import jalview.datamodel.ScoreSequence;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import java.awt.Color;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import apollo.datamodel.CurationSet;

public class WiggleScoreCalculator extends StoredScoreCalculator {

  //class variables
  protected final static Logger logger = LogManager.getLogger(WiggleScoreCalculator.class);

  //instance variables
  private Color color;
  
  public enum WiggleType
  {
    BED,
    VARIABLE_STEP,
    FIXED_STEP,
    NOT_SET
  }

  /** Builds a WiggleScoreCalculator object
   * 
   * @param data - name of file containing wiggle data
   * @param winSize - size of display window
   * @throws IOException - if the file cannot be accessed
   */
  public WiggleScoreCalculator(String data, int winSize, CurationSet curation)
  throws IOException
  {
    this(new BufferedReader(new FileReader(data)), winSize, curation);
  }
  
  /** Builds a WiggleScoreCalculator object
   * 
   * @param br - BufferedReader containing wiggle data
   * @param winSize - size of display window
   * @throws IOException - if the file cannot be accessed
   */
  public WiggleScoreCalculator(BufferedReader br, int winSize, CurationSet curation)
    throws IOException
  {
    super(winSize);
    String line;
    WiggleType type = WiggleType.NOT_SET;
    int span = 0;
    int step = 0;
    int pos = 0;
    double score = 0;
    double minAbsScore = Double.POSITIVE_INFINITY;
    br.mark(100);
    Set<String> unmatchedIds = new HashSet<String>();
    while ((line = br.readLine()) != null) {
      //skip comments
      if (line.charAt(0) == '#') {
        continue;
      }
      String []tokens = line.split("\\s+");
      if (tokens[0].equals("track")) {
        if (type != WiggleType.NOT_SET) {
          br.reset();
          break;
        }
        //assume to be BED unless a declaration line is found for variableStep/fixedStep
        type = WiggleType.BED;
        for (int i = 1; i < tokens.length; ++i) {
          String token = tokens[i];
          if (token.contains("\"")) {
            String nextToken = null;
            do {
              nextToken = tokens[++i];
              token = token + " " + nextToken;
            }
            while (nextToken.charAt(nextToken.length() - 1) != '"');
            token = token.replaceAll("\"", "");
          }
          String []keyValue = parseKeyValue(token);
          if (keyValue[0].equals("color")) {
            String []rgb = keyValue[1].split(",");
            if (rgb.length != 3) {
              logger.warn("Invalid color specification: " + token);
            }
            else {
              color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
            }
            break;
          }
        }
      }
      else if (tokens[0].equals("variableStep")) {
        type = checkId(tokens[1], curation.getRefSequence().getName()) ?
            WiggleType.VARIABLE_STEP : WiggleType.NOT_SET;
        if (type == WiggleType.VARIABLE_STEP) {
          if (tokens.length > 2) {
            span = parseSpan(tokens[2]);
          }
        }
      }
      else if (tokens[0].equals("fixedStep")) {
        type = checkId(tokens[1], curation.getRefSequence().getName()) ?
            WiggleType.FIXED_STEP : WiggleType.NOT_SET;
        if (type == WiggleType.FIXED_STEP) {
          pos = parsePosition(tokens[2]);
          step = parseStep(tokens[3]);
          if (tokens.length > 4) {
            span = parseSpan(tokens[4]);
          }
        }
      }
      else {
        if (type == WiggleType.VARIABLE_STEP) {
          pos = Integer.parseInt(tokens[0]);
          score = Double.parseDouble(tokens[1]);
        }
        else if (type == WiggleType.FIXED_STEP) {
          score = Double.parseDouble(tokens[0]);
        }
        else if (type == WiggleType.BED) {
          if (!tokens[0].equals(curation.getRefSequence().getName())) {
            if (!unmatchedIds.contains(tokens[0])) {
              logger.warn("Sequence id " + tokens[0] + " does not match main sequence id " +
                  curation.getRefSequence().getName());
              unmatchedIds.add(tokens[0]);
            }
            continue;
          }
          pos = Integer.parseInt(tokens[1]) + 1;
          span = Integer.parseInt(tokens[2]) - pos + 1;
          score = Double.parseDouble(tokens[3]);
        }
        else {
          continue;
        }
        if (Math.abs(score) < minAbsScore) {
          minAbsScore = Math.abs(score);
        }
        for (int i = pos; i <= pos + span; ++i) {
          // chuck out scores outside of the given range
          if (i >= curation.getLow() && i <= curation.getHigh()) {
            scoreMap.put(i, score);
          }
        }
        if (type == WiggleType.FIXED_STEP) {
          pos += step - span;
        }
        if (score < minScore) {
          minScore = score;
        }
        if (score > maxScore) {
          maxScore = score;
        }
      }
      br.mark(100);
    }
    while (minAbsScore * factor < 1) {
      factor *= 10;
    }
  }

  public Color getColor()
  {
    return color;
  }
  
  private boolean checkId(String data, String refId)
  {
    String []id = parseKeyValue(data);
    if (id == null) {
      logger.warn("Sequence id not set for wiggle track");
      return false;
    }
    if (!id[1].equals(refId)) {
      logger.warn("Sequence id " + id[1] + " does not match main sequence id " +
          refId);
      return false;
    }
    return true;
  }

  private int parseSpan(String data)
  {
    int span = 0;
    String []spanData = parseKeyValue(data);
    if (spanData != null) {
      if (spanData[0].equals("span")) {
        span = Integer.parseInt(spanData[1]);
      }
    }
    return span;
  }
  
  private int parseStep(String data)
  {
    int step = 0;
    String []stepData = parseKeyValue(data);
    if (stepData != null) {
      if (stepData[0].equals("step")) {
        step = Integer.parseInt(stepData[1]);
      }
    }
    return step;
  }

  private int parsePosition(String data)
  {
    int pos = 0;
    String []posData = parseKeyValue(data);
    if (posData != null) {
      if (posData[0].equals("start")) {
        pos = Integer.parseInt(posData[1]);
      }
    }
    return pos;
  }
  
  private String[] parseKeyValue(String keyValue)
  {
    String []tokens = keyValue.split("=", 2);
    if (tokens.length != 2) {
      return null;
    }
    return tokens;
  }
  
  private String parseKey(String keyValue)
  {
    String []tokens = parseKeyValue(keyValue);
    if (tokens == null) {
      return null;
    }
    return tokens[0];
  }
  
  private String parseValue(String keyValue)
  {
    String []tokens = parseKeyValue(keyValue);
    if (tokens == null) {
      return null;
    }
    return tokens[0];
  }

}
