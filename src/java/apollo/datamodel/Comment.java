package apollo.datamodel;

import java.util.Date;

import org.apache.log4j.*;

public class Comment implements java.io.Serializable, java.lang.Cloneable {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  protected final static Logger logger = LogManager.getLogger(Comment.class);

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  String id;
  String text;
  String person;
  long   timestamp;
  boolean isInternal;

  public Comment () {}

  public Comment(String id, String text, String person, long timestamp) {
    setId       (id);
    setText     (text);
    setPerson   (person);
    setTimeStamp(timestamp);
    isInternal = false;
  }

  public void setIsInternal(boolean in) {
    isInternal = in;
  }

  public boolean isInternal() {
    return isInternal;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getId() {
    return this.id;
  }
  public void setText(String text) {
    this.text = text;
  }
  public String getText() {
    return this.text;
  }
  public void setPerson(String person) {
    this.person = person;
  }
  public String getPerson() {
    return this.person;
  }
  public void setTimeStamp(long timestamp) {
    this.timestamp = timestamp;
  }
  public long getTimeStamp() {
    return this.timestamp;
  }

  /** to get a field-by-field replica of this feature */
  public Object clone() {
    try {
      Comment clone = (Comment)super.clone();
      clone.text = new String(text); // cloning is shallow - need to go deep
      if (person == null)
        logger.error("Comment.clone: person is null for comment \"" + text + "\"");
      else
        clone.person = new String(person);
      //clone.id = new String(id);
      clone.id = id;
      return clone;
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }

  public Comment cloneComment() {
    Comment clone = (Comment)clone();
    return clone;
  }

  public String toString() {
    return person + "-" + (new Date(timestamp)).toString();
  }
}
