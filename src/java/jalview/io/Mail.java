/* Jalview - a java multiple alignment editor
 * Copyright (C) 1998  Michele Clamp
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package jalview.io;

import java.net.*;
import java.io.*;
import java.net.*;

public class Mail {
  BufferedReader in;
  PrintWriter    out;
  Socket         s;

  public static void main(String s[]) {
    try {
      Mail t = new Mail(s[0],s[1],s[2],"Test jalview mail");
      t.send("Test message");
      t.finish();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Mail(String mailServer, String recipient, String author ,String subject) throws Exception {
    _init(mailServer);
    sendHeader(author,recipient,subject);
  }


  private void _init(String mailServer) throws Exception {
    s = new Socket(mailServer, 25);

    in = new BufferedReader
         (new InputStreamReader(s.getInputStream(), "8859_1"));
    out = new PrintWriter
          (new OutputStreamWriter(s.getOutputStream(), "8859_1"));
  }

  private void sendHeader(String author, String recipient, String subject) {
    sendInOut("HELO theWorld");
    sendInOut("MAIL FROM: " + "<" + author + ">");
    sendInOut("RCPT TO: " + recipient);
    sendInOut("DATA");

    send("Subject: " + subject);
    send("From: " + "<" + author  + ">" );
    send("\n");
  }

  public void send(String mailServer, String recipient, String author ,String subject, String text) {
    try {
      _init(mailServer);

      sendHeader(author,recipient,subject);
      // message body
      send(text);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void finish() {
    try {
      send("\n.\n");
      sendInOut("QUIT");
      in  = null;
      out = null;
      s.close();
    } catch (Exception e) {
      System.out.println("Exception : " +  e);
    }
  }

  public void send(String mailServer, String recipient) {
    try {
      _init(mailServer);

      sendHeader("Elvis.Presley@jailhouse.rock",recipient,"In the ghetto");

      // message body
      send("I'm alive. Help me!");
      finish();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Not sure why this is needed but it is.
  public void sendInOut(String s) {
    try {
      out.write(s + "\n");
      out.flush();
      s = in.readLine();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void send(String s) {
    try {

      //       splitMessage(s);
      out.write(s + "\n");
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Not used
  private void splitMessage(String s) {
    try {
      int last = 0;
      int next = 0;
      while ((next = s.indexOf('\n',last)) != -1) {
        out.write(s.substring(last,next) + "\n");
        System.out.println("Split message: " + s.substring(last,next));
        out.flush();
        last = next+1;
      }
      out.write(s.substring(last) + "\n");
      System.out.println("End Split message: " + s.substring(last));
      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
