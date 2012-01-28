package apollo.dataadapter.das.simple;

import apollo.dataadapter.das.*;

/**
 * I am a lightweight implementation of the <code>DASFeature</code> interface.
 * I am a simple bag of attributes with no further internal functionality.
 * 
 * @see apollo.dataadapter.das.DASFeature
**/
public class
      SimpleDASFeature
      implements
  DASFeature {
  public String id;
  public String label;
  public String typeId;
  public String typeCategory;
  public String typeLabel;
  public String typeSubparts;
  public String typeReference;

  public String methodId;
  public String methodLabel;

  public String start;
  public String end;
  public String score;

  public String phase;
  public String orientation;
  public String note;
  public String targetId;
  public String targetStart;
  public String targetStop;

  public String groupId;
  public String groupTargetStart;
  public String groupTargetStop;

  public SimpleDASFeature(String theId) {
    id = theId;
  }

  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }
  public String getTypeId() {
    return typeId;
  }
  public String getTypeCategory() {
    return typeCategory;
  }

  public String getTypeLabel() {
    return typeLabel;
  }

  public String getTypeReference() {
    return typeReference;
  }//end getTypeReference

  public String getTypeSubparts() {
    return typeSubparts;
  }//end getTypeSubparts

  public void setTypeReference(String newValue) {
    typeReference = newValue;
  }//end setTypeReference

  public void setTypeSubparts(String newValue) {
    typeSubparts = newValue;
  }//end setTypeSubparts


  public String getMethodId() {
    return methodId;
  }//end getMethodId

  public String getMethodLabel() {
    return methodLabel;
  }//end getMethodLabel

  public String getStart() {
    return start;
  }//end getStart

  public String getEnd() {
    return end;
  }//end getEnd

  public String getScore() {
    return score;
  }//end getScore

  public String getPhase() {
    return phase;
  }//end getPhase

  public String getOrientation() {
    return orientation;
  }

  public String getNote() {
    return note;
  }

  public String getTargetId() {
    return targetId;
  }

  public String getTargetStart() {
    return groupTargetStart;
  }

  public String getTargetStop() {
    return groupTargetStop;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getGroupTargetStart() {
    return groupTargetStart;
  }

  public String getGroupTargetStop() {
    return groupTargetStop;
  }

  public void setId(String newValue) {
    id = newValue;
  }

  public void setLabel(String newValue) {
    label = newValue;
  }
  public void setTypeId(String newValue) {
    typeId = newValue;
  }
  public void setTypeCategory(String newValue) {
    typeCategory = newValue;
  }
  public void setTypeLabel(String newValue) {
    typeLabel = newValue;
  }

  public void setMethodId(String newValue) {
    methodId = newValue;
  }

  public void setMethodLabel(String newValue) {
    methodLabel = newValue;
  }

  public void setStart(String newValue) {
    start = newValue;
  }

  public void setEnd(String newValue) {
    end = newValue;
  }

  public void setScore(String newValue) {
    score = newValue;
  }

  public void setPhase(String newValue) {
    phase = newValue;
  }

  public void setOrientation(String newValue) {
    orientation = newValue;
  }

  public void setNote(String newValue) {
    note = newValue;
  }

  public void setTargetId(String newValue) {
    targetId = newValue;
  }

  public void setTargetStart(String newValue) {
    groupTargetStart = newValue;
  }

  public void setTargetStop(String newValue) {
    groupTargetStop = newValue;
  }

  public void setGroupId(String newValue) {
    groupId = newValue;
  }

  public void setGroupTargetStart(String newValue) {
    groupTargetStart = newValue;
  }

  public void setGroupTargetStop(String newValue) {
    groupTargetStop = newValue;
  }

  public String toString() {
    return
      "DASFeature["+
      getId()+":"+
      getTypeId()+","+
      getMethodId()+","+
      getStart()+","+
      getEnd()+","+
      getScore()+","+
      getOrientation()+","+
      getPhase()+":"+
      getGroupId()+","+
      getGroupTargetStart()+","+
      getGroupTargetStop()+"]";
  }
}
