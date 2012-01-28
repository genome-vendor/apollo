package jalview.io;

import java.io.*;

public interface OutputGenerator {

  public PostscriptProperties getPostscriptProperties();
  public MailProperties getMailProperties();
  //    public FileProperties getFileProperties();

  public void setPostscriptProperties(PostscriptProperties pp);
  //    public void setFileProperties(FileProperties fp);
  //public void setMailProperties(MailProperties mp);

  public String getText(String format);
  public void getPostscript(PrintWriter bw);
  public void getPostscript(PrintStream ps);
  public StringBuffer getPostscript();

}
