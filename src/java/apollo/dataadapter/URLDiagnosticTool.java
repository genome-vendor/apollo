package apollo.dataadapter;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;

/**
 * I allow the user to check (and tweak) the contents of a URL-fetch
 * before allowing the fetched data to be returned to the application.
 * The best tactic is to build this tool and pass it the input query string.
 * The tool will block the current thread and wait for the user to hit the
 * "return" button. After the thread unblocks, you should query the 
 * return-text in your code for the URL response.
**/
public class 
  URLDiagnosticTool
extends 
  JDialog 
{
  
  private JTextField queryField;  
  private JTextArea responseField;
  private JButton executeButton;
  private JButton returnButton;

  /**
   * Responds to the "execute" button being pressed by sending the query string
   * as a GET to the specified URL.
  **/
  private ActionListener executeListener = 
    new ActionListener() {
      public void actionPerformed(ActionEvent event){
        System.out.println("STARTING on"+getQueryField().getText());
        try{

          URL url = new URL(getQueryField().getText());
          getResponseField().setText("opening URL");
          DataInputStream inputStream = new DataInputStream(url.openStream());
          getResponseField().setText("collating response");
          String line = inputStream.readLine();
          String totalString="";
          
          while(line != null){
            totalString += line+"\n";
            line = inputStream.readLine();
          }//end while
          
          System.out.println("FINISHING");
          getResponseField().setText(totalString);
          System.out.println("FINISHED");
        }catch(Exception exception){
          getResponseField().setText(exception.getMessage()+"\n");
        }//end try
      }
    };

  /**
   * Responds to the "Return" button by disposing of this dialogue
  **/
  private ActionListener returnListener = 
    new ActionListener(){
      public void actionPerformed(ActionEvent event){
        dispose();
      }
    };
  

  /**
   * Shut this dialogue down when the user hits the "x"
  **/
  public class MyWindowListener extends WindowAdapter {
    public void windowClosed(WindowEvent e) {
      Window window = e.getWindow();
      window.dispose();
    }
  }
    
  public URLDiagnosticTool(String query){
    super((Frame)null,"URL Diagnostic", true);
    setup(query);
  }
  
  public URLDiagnosticTool(String query, JDialog parent){
    super(parent,"URL Diagnostic", true);
    setup(query);
  }

  private void setup(String query){
    JScrollPane responsePane;
    queryField = new JTextField(30);
    queryField.setText(query);
    executeButton = new JButton("execute query");
    executeButton.addActionListener(executeListener);
    responseField = new JTextArea(20,30);
    responsePane = new JScrollPane(responseField);
    responseField.setText("Waiting for response");
    returnButton = new JButton("return");
    returnButton.addActionListener(returnListener);
    getContentPane().setLayout(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 1;
    getContentPane().add(queryField, constraints);
    constraints.gridy++;
    getContentPane().add(executeButton, constraints);
    constraints.gridy++;
    getContentPane().add(responsePane, constraints);
    constraints.gridy++;
    getContentPane().add(returnButton, constraints);
    addWindowListener(new MyWindowListener());
    pack();
    show();
  }
  
  public JTextField getQueryField() {
    return this.queryField;
  }
  
  public void setQueryField(JTextField queryField) {
    this.queryField = queryField;
  }

  public JTextArea getResponseField() {
    return this.responseField;
  }
  
  public void setResponseField(JTextArea responseField) {
    this.responseField = responseField;
  }
  
  public JButton getExecuteButton() {
    return this.executeButton;
  }
  
  public void setExecuteButton(JButton executeButton) {
    this.executeButton = executeButton;
  }
  
  public JButton getReturnButton() {
    return this.returnButton;
  }
  
  public void setReturnButton(JButton returnButton) {
    this.returnButton = returnButton;
  }
  
  public static void main(String[] args){
    final JDialog firstDialog = new JDialog((JFrame)null, true);
    firstDialog.getContentPane().setLayout(new BorderLayout());
    firstDialog.getContentPane().add(new JLabel("Hello"), BorderLayout.CENTER);
    JButton myButton = new JButton("Launch");
    myButton.addActionListener(
      new ActionListener(){
        public void actionPerformed(ActionEvent e){
          System.out.println("Before launching diagnostic");
          URLDiagnosticTool tool = new URLDiagnosticTool("http://www.google.com");
          System.out.println("After launching diagnostic");
        }
      }
    );
    firstDialog.getContentPane().add(myButton, BorderLayout.SOUTH);
    firstDialog.pack();
    firstDialog.show();
  }
  
  
}
