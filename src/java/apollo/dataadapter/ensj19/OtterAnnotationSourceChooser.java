package apollo.dataadapter.ensj19;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * Uses otter database to fetch the datasets for a server URL.
**/
public final class 
  OtterAnnotationSourceChooser
extends 
  AnnotationSourceChooser
{
  
  public OtterAnnotationSourceChooser(){
    super();
  }
  
  public OtterAnnotationSourceChooser(
    Vector inputFileHistory, 
    Vector inputServerHistory, 
    Vector outputFileHistory, 
    Vector outputServerHistory
  ){
    super(
      inputFileHistory, 
      inputServerHistory, 
      outputFileHistory
    );
  }//end OtterAnnotationSourceChooser
  
  /**
   * I expect the following response from the otter server:
   * (1) A line which says "Datasets".
   * (2) One or more lines listing the annotation datasets you can edit.
  **/
  protected final List getDataSetsForServer(String serverURLString, String portString){
    ArrayList returnList = new ArrayList();
    URL uRL = null;
    String serverQueryString = 
      "http://"+
      serverURLString+":"+
      portString+
      "/perl/get_datasets";
    
    DataInputStream inputStream;
    String inputLine;
    
    try{
      uRL = new URL(serverQueryString);
      inputStream = new DataInputStream(uRL.openStream());
      
      inputLine = inputStream.readLine();
      if(
        inputLine != null &&
        inputLine.trim().indexOf("Datasets")>=0
      ){

        inputLine = inputStream.readLine();
        while(inputLine != null){
          if(
            inputLine.trim().length() > 0 &&
            !inputLine.equals("Datasets")
          ){
            returnList.add(inputLine.trim());
            inputLine = inputStream.readLine();
          }else{
            inputLine = inputStream.readLine();
          }//end if
        }//end while

      }else{
        javax.swing.JOptionPane.showMessageDialog(
          this, 
          "Unable to find otter annotation datasets for server at "+serverQueryString+"\n"
        );

        return returnList;
      }//end if
    }catch(IOException exception){
      javax.swing.JOptionPane.showMessageDialog(
        this, 
        "Unable to open URL: "+serverQueryString+" for annotations\n"+
        exception.getMessage()
      );
    }//end try

    return returnList;
  }//end getDataSetsForServer
}//end AnnotationSourceChooser
