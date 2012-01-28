package jalview.analysis.blast;

import jalview.datamodel.*;
import java.util.*;
import apollo.datamodel.FeaturePair;
import apollo.datamodel.SeqFeature;
import apollo.datamodel.SeqFeatureI;

public class HSP extends FeaturePair {

  int  query_length;
  int  hit_length;

  String query_string;
  String hit_string;
  String align_string;

  double pvalue;
  double percent_id;

  int positives;
  int frame;
  double bits;

  int qtype;
  int htype;

  int qinc;
  int hinc;

  static final int PEP = 1;
  static final int DNA = 2;

  public HSP(SeqFeatureI f1,SeqFeatureI f2) {
      super(f1,f2);
  }

  public Vector getUngappedFeatures() {

      char[] qchars = getQueryString().toCharArray();
      char[] hchars   = getHitString()  .toCharArray();

      int qstrand = getStrand();
      int hstrand = getHstrand();

      int qstart = getStart();
      int hstart = getHstart();

      int qend   = getStart();
      int hend   = getHstart();

      if (qstrand == -1) {
          qstart = getEnd();
          qend   = getEnd();
      }
      if (hstrand == -1) {
          hstart = getHend();
          hend   = getHend();
      }
      int count = 0;
      int found = 0;

      Vector tmp = new Vector();

    //  System.out.println("Query " + getQueryString());
    //  System.out.println("Hit   " + getHitString());

      while (count < qchars.length) {

//        System.out.println("Qchar " + qchars[count]);
 //         System.out.println("Hchar " + hchars[count]);
          if (qchars[count] != '-' &&
              hchars[count] != '-') {

              qend += qinc;
              hend += hinc;

              found = 1;
          } else {

              if (found == 1) {

                  FeaturePair fp = makeFeaturePair(qstart,qend,qstrand,hstart,hend,hstrand,qinc,hinc);

                  tmp.addElement(fp);
              }

              if (qchars[count] != '-') {
                  qstart = qend + qinc;
              } else {
                  qstart = qend;
              }
              if (hchars[count] != '-') {
                  hstart = hend + hinc;
              } else {
                  hstart = hend;
              }

              qend = qstart;
              hend = hstart;

              found = 0;
          }
          count++;
      }

      if (found == 1) {

          FeaturePair fp = makeFeaturePair(qstart,qend,qstrand,hstart,hend,hstrand,qinc,hinc);

          tmp.addElement(fp);
      }
      return tmp;
  }
  private FeaturePair makeFeaturePair(int qstart, int qend, int qstrand, int hstart, int hend , int hstrand, int qinc, int hinc) {
      int tmpqend = qend;
      int tmphend = hend;

      tmpqend -= qinc;
      tmphend -= hinc;

      int tmpqstart = qstart;
      int tmphstart = hstart;

      if (Math.abs(qinc) > 1) {
          tmpqend += qstrand * 2;
      }
      if (Math.abs(hinc) > 1) {
          tmphend += hstrand * 2;
      }

      if (tmpqstart > tmpqend) {
          int tmp = tmpqstart;
          tmpqstart = tmpqend;
          tmpqend   = tmp;
      }
      if (tmphstart > tmphend) {
          int tmp = tmphstart;
          tmphstart = tmphend;
          tmphend = tmp;
      }

      FeaturePair fp = new FeaturePair(new SeqFeature(),new SeqFeature());

      fp.setId       (getId());
      fp.setHname     (getHname());
      fp.setStart    (tmpqstart);
      fp.setEnd      (tmpqend);
      fp.setHstart   (tmphstart);
      fp.setHend     (tmphend);
      fp.setStrand   (qstrand);
      fp.setHstrand  (hstrand);
      fp.addScore ("probability", getPValue());
      fp.addScore ("identity", getPercentId());

      return fp;
  }

  public int getQueryIncrement() {
      return qinc;
  }
  public void setQueryIncrement(int inc) {
      this.qinc = inc;
  }
  public int getHitIncrement() {
      return hinc;
  }
  public void setHitIncrement(int inc) {
      this.hinc = inc;
  }
  public int getQueryType() {
      return qtype;
  }
  public void setQueryType(int type) {
      this.qtype = type;
  }
  public int getHitType() {
      return htype;
  }
  public void setHitType(int type) {
      this.htype = type;
  }

  public int getQueryLength() {
      return query_length;
  }
  public void setQueryLength(int length) {
      this.query_length = length;
  }
  public int getHitLength() {
      return hit_length;
  }
  public void setHitLength(int length) {
      this.hit_length = length;
  }
  public int getPositives() {
      return positives;
  }
  public void setPositives(int pos) {
      this.positives = pos;
  }
  public int getFrame() {
      return frame;
  }
  public void setFrame(int frame) {
      this.frame = frame;
  }
  public String getAlignString() {
      return align_string;
  }
  public void setAlignString(String align) {
      this.align_string= align;
  }
  public String getQueryString() {
      return query_string;
  }
  public void setQueryString(String str) {
      this.query_string = str;
  }
  public String getHitString() {
      return hit_string;
  }
  public void setHitString(String str) {
      this.hit_string = str;
  }
  public double getPValue() {
      return pvalue;
  }
  public void setPValue(double value) {
      this.pvalue = value;
  }
  public double getPercentId() {
      return percent_id;
  }
  public void setPercentId(double pid) {
      this.percent_id = pid;
  }
  public double getBitScore() {
      return bits;
  }
  public void setBitScore(double bits) {
      this.bits = bits;
  }

  public String toString() {
      String out = "";

      out += getId() +
          "\tStart: "       + getStart()       +
          "\tEnd: "         + getEnd()         +
          "\tScore: "       + getScore()       +
          "\tStrand: "      + getStrand()      +
          "\tFrame: "       + getFrame()       +
          "\tHitId: "       + getHname()       +
          "\tHitStart: "    + getHstart()      +
          "\tHitEnd: "      + getHend()        +
          "\tPValue: "      + getPValue()      +
          "\tPID: "         + getPercentId()   +
          "\tBitScore: "    + getBitScore()    +
          "\tQueryLength: " + getQueryLength() +
          "\n\n\tQuery: "   + getQueryString() +
          "\n\t       "     + getAlignString() +
          "\n\tHit  : "     + getHitString();

      return out;
  }
}


