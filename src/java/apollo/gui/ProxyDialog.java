package apollo.gui;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.net.*;
import java.awt.event.*;
import java.util.*;
import org.w3c.tools.codec.*;
import java.io.*;

import apollo.dataadapter.CGI;
import apollo.editor.UserName;

public class ProxyDialog extends JDialog implements ActionListener {
  String proxyServer;
  String proxyPort;
  String userName;
  String userPassword;
  boolean hasAuthentication = false;
  boolean hasProxy = false;

  JTextField server;
  JTextField port;
  JTextField name;
  JPasswordField password;
  JCheckBox  authBox;
  JCheckBox  proxyBox;
  JPanel     authP;
  JButton    ok;
  JButton    cancel;

  /**
   * Use this construtor when you don't need to call back from
   * the dialog to its parent after the user has made a choice.
  **/
  public ProxyDialog(JFrame frame) {
    super(frame,"Proxy settings",true);
    String setStr = System.getProperty("http.proxySet");
    if (setStr == null)
      setStr = System.getProperty("proxySet");
    if (setStr != null)
      hasProxy = new Boolean(setStr).booleanValue();

    proxyPort = System.getProperty("http.proxyPort");
    if (proxyPort == null)
      proxyPort = System.getProperty("proxyPort");
    proxyServer = System.getProperty("http.proxyHost");
    if (proxyServer == null)
      proxyServer = System.getProperty("proxyHost");

    init();
    if (frame==null) {
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      setLocation((screenSize.width - getSize().width) / 2,
                  (screenSize.height - getSize().height) / 2);
    }
  }

  public void actionPerformed(ActionEvent evt) {
    if (evt.getSource() == authBox) {
      hasAuthentication = authBox.isSelected();
      authP.setVisible(authBox.isSelected());
      name.setEnabled(hasAuthentication && hasProxy);
      password.setEnabled(hasAuthentication && hasProxy);
      pack();
    } else if (evt.getSource() == proxyBox) {
      hasProxy = proxyBox.isSelected();
      server.setEnabled(hasProxy);
      port.setEnabled(hasProxy);
      name.setEnabled(hasAuthentication && hasProxy);
      password.setEnabled(hasAuthentication && hasProxy);
      authBox.setEnabled(hasProxy);
    } else if (evt.getSource() == ok) {
      commitIfComplete();
    } else if (evt.getSource() == cancel) {
      hide();
      dispose();
    }
  }

  private void commitIfComplete() {
    if (proxyBox.isSelected()) {
      if (!server.getText().equals("") && !port.getText().equals("")) {
        System.out.println("server = " + server.getText());
        System.out.println("port   = " + port.getText());
        try {
          Integer.parseInt(port.getText(),10);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(this,"Port is not an integer","Error",JOptionPane.ERROR_MESSAGE);
          return;
        }
        if (hasAuthentication) {
          if (name.getText().equals("") || password.getText().equals("")) {
            JOptionPane.showMessageDialog(this,
                                          "You need to specify both name and password for authentication",
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
            return;
          }
          String authStr = name.getText() + ":" + password.getText();
          Base64Encoder encoder = new Base64Encoder(authStr);
          URLConnection.setDefaultRequestProperty("Proxy-Authorization","Basic " + encoder.processString());
          System.out.println("Processed string = " + encoder.processString());
        }
        Properties props = System.getProperties();
        props.put("http.proxySet","true");
        props.put("http.proxyHost",server.getText());
        props.put("http.proxyPort",port.getText());
      } else {
        JOptionPane.showMessageDialog(this,"You need to specify both server and port","Error",JOptionPane.ERROR_MESSAGE);
        return;
      }
    } else {
      Properties props = System.getProperties();
      props.put("http.proxySet","false");
      props.put("proxySet","false");
      props.remove("http.proxyHost");
      props.remove("http.proxyPort");
      props.remove("proxyHost");
      props.remove("proxyPort");
    }
    hide();
    dispose();
  }

  public void init() {

    server   = new JTextField(30);
    server.setText(proxyServer);
    port     = new JTextField(6);
    port.setText(proxyPort);
    server.setEnabled(hasProxy);
    port.setEnabled(hasProxy);
    authBox  = new JCheckBox();
    authBox.setSelected(hasAuthentication);
    authBox.setEnabled(hasProxy);
    authBox.addActionListener(this);
    proxyBox  = new JCheckBox();
    proxyBox.setSelected(hasProxy);
    proxyBox.addActionListener(this);
    name     = new JTextField(30);
    name.setText(UserName.getUserName());
    password = new JPasswordField(30);
    name.setEnabled(hasAuthentication && hasProxy);
    password.setEnabled(hasAuthentication && hasProxy);

    JPanel mainP = new JPanel();
    mainP.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    //gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.gridwidth = 1;
    mainP.add(new JLabel("Use Proxy"),gbc);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    mainP.add(proxyBox,gbc);
    gbc.gridwidth = 1;
    mainP.add(new JLabel("Server"),gbc);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    mainP.add(server,gbc);
    gbc.gridwidth = 1;
    mainP.add(new JLabel("Port"),gbc);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    mainP.add(port,gbc);
    gbc.gridwidth = 1;
    mainP.add(new JLabel("Use Authentication"),gbc);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    mainP.add(authBox,gbc);
    authP = new JPanel();
    authP.setLayout(new GridBagLayout());
    gbc.gridwidth = 1;
    authP.add(new JLabel("Username"),gbc);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    authP.add(name,gbc);
    gbc.gridwidth = 1;
    authP.add(new JLabel("Password"),gbc);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    authP.add(password,gbc);


    JPanel enclosureP = new JPanel();
    enclosureP.setLayout(new BorderLayout());
    enclosureP.add(mainP,BorderLayout.CENTER);
    enclosureP.add(authP,BorderLayout.SOUTH);

    ok = new JButton("OK");
    ok.addActionListener(this);
    cancel = new JButton("Cancel");
    cancel.addActionListener(this);
    JPanel buttonP = new JPanel();
    buttonP.add(ok);
    buttonP.add(cancel);

    getContentPane().add(enclosureP,BorderLayout.CENTER);
    getContentPane().add(buttonP,BorderLayout.SOUTH);
    authP.setVisible(false);
    pack();
  }

  public static void main(String [] args) {
    final JFrame frame = new JFrame();
    JButton show = new JButton("Show");
    JButton get = new JButton("Get");

    show.addActionListener(new ActionListener() {
                             public void actionPerformed(ActionEvent evt) {
                               ProxyDialog pd = new ProxyDialog(frame);
                               pd.setVisible(true);
                             }
                           }
                          );

    get.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent evt) {
                              CGI cgi = new CGI("www.google.com",80,"index.html",new Hashtable(),System.out);
                              cgi.run();
                              try {

                                BufferedReader d = new BufferedReader(
                                    new InputStreamReader(cgi.getInput()));

                                String data = d.readLine();
                                System.out.println("first line = " + data);
                              } catch (Exception e) {
                                e.printStackTrace();
                              }
                            }
                          }
                         );

    frame.getContentPane().setLayout(new GridLayout(2,1));
    frame.getContentPane().add(show);
    frame.getContentPane().add(get);
    frame.pack();
    frame.setVisible(true);
  }
}
