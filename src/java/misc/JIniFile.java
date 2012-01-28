package misc;

// Copyright (c) 2005 m. mundry - mundry-at-web-dot-de
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or (at
// your option) any later version.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
// USA
//
// (See gpl.txt for details of the GNU General Public License.)

import java.io.*;
import java.util.ArrayList;
/**
 * JIniFile is a convenience class for accessing data stored in .ini files
 * JIniFile handles sections and keys case-sensitive
 *
 * @author Marvin Mundry
 * @version 1.0
 * 
 * Modified by Steve Searle for Apollo use:
 *   Lowercased first character of all method names
 *   Reindented
 *   Added constructor which takes a File argument
 *   Added {read,write}{Long,Double}
 *   Added addBlankLinesBetweenSections to make output slightly easier to read
 *   DeDOSified file
 */
public class JIniFile {
  private ArrayList allIni;
  private File iniFile;

  /**
   * Constructs the JIniFile class
   * @param pFileName The .ini file to open
   */
  public JIniFile(String pFileName) {
    iniFile=new File(pFileName);
    init();
  }

  public JIniFile(File file) {
    iniFile = file;
    init();
  }

  private void init() {
    allIni=new ArrayList(10);						// instantiate allIni (this component stores all ini data)
    String line=null;
    if (iniFile.exists()) { 						// if file exists read it into allIni
      BufferedReader buf=null;
      try {
        buf = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(iniFile))));
        while ( (line=buf.readLine()) != null ) {
          allIni.add(line);
        }
      }catch (IOException e){
      }finally{
        if (buf != null) {
         try {buf.close();} catch (IOException ioe) {}
        }
      }
    }
  }

  /**
   * Write the changes that were made into the .ini file
   * @return True if saving succeded else false
   */
  public boolean updateFile() {
    try {
      BufferedWriter bw = new BufferedWriter(new FileWriter(iniFile, false));
      for (int i=0; i<allIni.size(); i++) {
        bw.write((String)allIni.get(i));
        bw.newLine();
      }
      bw.close();
    } catch (IOException e) {return false;}
    return true;
  }


  /**
   * Read a value from the INI-data
   * @param pSection the Section to read from
   * @param pKey the key in pSection to read from
   * @return the value or null if value can not be found
   */
  private String readValue(String pSection, String pKey) {
    boolean inSection=false;
    for (int i=0; i<allIni.size(); i++) {
      if (inSection) {
        if (((String)allIni.get(i)).startsWith(pKey+"=")) {
          return ((String)allIni.get(i)).substring(((String)allIni.get(i)).indexOf("=")+1);
        }
        if (((String)allIni.get(i)).startsWith("[")) { //if end of section is reached
          return null;
        }
      }else{
        if (((String)allIni.get(i)).equals("["+pSection+"]")) { //if section starts
          inSection=true;
        }
      }
    }
    return null;
  }

  /**
   * Writes a value to the INI-data
   * @param pSection the section to write in
   * @param pKey the key in pSection to write in
   * @param pValue the value to write
   * @return just nonsense
   */
  private void writeValue(String pSection, String pKey, String pValue) {
    boolean inSection=false;
    for (int i=0; i<allIni.size(); i++) {
      if (inSection) {
        if (((String)allIni.get(i)).startsWith(pKey+"=")) {
          allIni.set(i,pKey+"="+pValue);
          return;
        }
        if (((String)allIni.get(i)).startsWith("[") || i==allIni.size()-1) { //if end of section is reached
          allIni.add(i,pKey+"="+pValue);
          return;
        }
      }else{ //if not in section
        if (((String)allIni.get(i)).equals("["+pSection+"]")) { //check whether section starts
          inSection=true;
          if (i==allIni.size()-1) {
            allIni.add(pKey+"="+pValue);
            return;
          }
        }
      }
    }
    allIni.add("["+pSection+"]");
    allIni.add(pKey+"="+pValue);
    return;
  }

  
  public void addBlankLinesBetweenSections() {
    boolean firstSection = true;

    for (int i=0; i<allIni.size(); i++) {
      if (((String)allIni.get(i)).startsWith("[")) { 
        if (firstSection) {
          firstSection=false;
        } else {
          if (i>0 && !allIni.get(i-1).equals("")) {
            allIni.add(i,"");
          }
        }
      }
    }
    return;
  }
  /**
   * Deletes a key from the INI-data
   * @param pSection The section in that the key should be
   * @param pKey The key to delete
   * @return Returns true if the key was deleted otherwise (if the key could nod be found) false
   */
  public boolean deleteKey(String pSection, String pKey) {
    boolean inSection=false;
    for (int i=0; i<allIni.size(); i++) {
      if (inSection) {
        if (((String)allIni.get(i)).startsWith(pKey+"=")) {
          allIni.remove(i);
          return true;
        }
        if (((String)allIni.get(i)).startsWith("[")) { //if end of section is reached
          return false;
        }
      }else{
        if (((String)allIni.get(i)).equals("["+pSection+"]")) { //if section starts
          inSection=true;
        }
      }
    }
    return false;
  }

  /**
   * Deletes a section
   * @param pSection The section to delete
   * @return Indicates wether the section was deleted or not existing
   */
  public boolean deleteSection(String pSection) {
    for (int i=0; i<allIni.size(); i++) {
      if (((String)allIni.get(i)).equals("["+pSection+"]")) { //if section starts
        do {
          allIni.remove(i);
        } while ( !((i==allIni.size())  || ((String)allIni.get(i)).startsWith("[")) );
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether a section exists
   * @param pSection the section to look for
   * @return true if section exists else false
   */
  public boolean sectionExists(String pSection) {
    for (int i=0; i<allIni.size(); i++) {
      if (((String)allIni.get(i)).equals("["+pSection+"]")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks whether a key in a defined section exists.
   * @param pSection The section in that the key sould be searched
   * @param pKey The key to find
   * @return True if pKey was found in pSection, else false
   */
  public boolean keyExists(String pSection, String pKey) {
    boolean inSection=false;
    for (int i=0; i<allIni.size(); i++) {
      if (inSection) {
        if (((String)allIni.get(i)).startsWith(pKey+"=")) {
          return true;
        }
        if (((String)allIni.get(i)).startsWith("[")) { //if end of section is reached
          return false;
        }
      }else{
        if (((String)allIni.get(i)).equals("["+pSection+"]")) { //if section starts
          inSection=true;
        }
      }
    }
    return false;
  }

  /**
   * Reads a string value from the INI-data
   * @param pSection The Section to read from
   * @param pKey The key to read from
   * @param pDefault The default value for the key
   * @return Returns the value of pKey in pSection if available else returns pDefault
   */
  public String readString(String pSection, String pKey, String pDefault){
    String retStr = readValue(pSection, pKey);
    if (retStr==null) {
      return pDefault;
    }else{
      return retStr;
    }
  }


  /**
   * Writes a string value to the INI-data
   * @param pSection The section to write into
   * @param pKey The key to write into
   * @param pValue The string to write into pKey in pSection
   */
  public void writeString(String pSection, String pKey, String pValue) {
    writeValue(pSection, pKey, pValue);
  }

  /**
   * Reads a float value from the INI-data
   * @param pSection The Section to read from
   * @param pKey The key to read from
   * @param pDefault The default value for the key
   * @return Returns the value of pKey in pSection if available else returns pDefault
   */
  public float readFloat(String pSection, String pKey, float pDefault){
    String retStr = readValue(pSection, pKey);
    if (retStr==null) {
      return pDefault;
    }else{
      return new Float(retStr).floatValue();
    }
  }

  /**
   * Writes a float value to the INI-data
   * @param pSection The section to write into
   * @param pKey The key to write into
   * @param pValue The float to write into pKey in pSection
   */
  public void writeFloat(String pSection, String pKey, float pValue) {
    writeValue(pSection, pKey, Float.toString(pValue));
  }

  /**
   * Reads a double value from the INI-data
   * @param pSection The Section to read from
   * @param pKey The key to read from
   * @param pDefault The default value for the key
   * @return Returns the value of pKey in pSection if available else returns pDefault
   */
  public double readDouble(String pSection, String pKey, double pDefault){
    String retStr = readValue(pSection, pKey);
    if (retStr==null) {
      return pDefault;
    }else{
      return new Double(retStr).doubleValue();
    }
  }

  /**
   * Writes a double value to the INI-data
   * @param pSection The section to write into
   * @param pKey The key to write into
   * @param pValue The float to write into pKey in pSection
   */
  public void writeDouble(String pSection, String pKey, double pValue) {
    writeValue(pSection, pKey, Double.toString(pValue));
  }

  /**
   * Reads an integer value from the INI-data
   * @param pSection The Section to read from
   * @param pKey The key to read from
   * @param pDefault The default value for the key
   * @return Returns the value of pKey in pSection if available else returns pDefault
   */
  public int readInteger(String pSection, String pKey, int pDefault){
    String retStr = readValue(pSection, pKey);
    if (retStr==null) {
      return pDefault;
    }else{
      return Integer.parseInt(retStr);
    }
  }

  /**
   * Writes an integer value to the INI-data
   * @param pSection The section to write into
   * @param pKey The key to write into
   * @param pValue The integer to write into pKey in pSection
   */
  public void writeInteger(String pSection, String pKey, int pValue) {
    writeValue(pSection, pKey, Integer.toString(pValue));
  }

  /**
   * Reads an long value from the INI-data
   * @param pSection The Section to read from
   * @param pKey The key to read from
   * @param pDefault The default value for the key
   * @return Returns the value of pKey in pSection if available else returns pDefault
   */
  public long readLong(String pSection, String pKey, long pDefault){
    String retStr = readValue(pSection, pKey);
    if (retStr==null) {
      return pDefault;
    }else{
      return Long.parseLong(retStr);
    }
  }

  /**
   * Writes an long value to the INI-data
   * @param pSection The section to write into
   * @param pKey The key to write into
   * @param pValue The integer to write into pKey in pSection
   */
  public void writeLong(String pSection, String pKey, long pValue) {
    writeValue(pSection, pKey, Long.toString(pValue));
  }

  /**
   * Reads a boolean value from the INI-data
   * @param pSection The Section to read from
   * @param pKey The key to read from
   * @param pDefault The default value for the key
   * @return Returns the value of pKey in pSection if available else returns pDefault
   */
  public boolean readBoolean(String pSection, String pKey, boolean pDefault){
    String retStr = readValue(pSection, pKey);
    if (retStr==null) {
      return pDefault;
    }else{
      if (retStr.equals("true")) {
        return true;
      }else{
        return false;
      }
    }
  }

  /**
   * Writes a boolean value to the INI-data
   * @param pSection The section to write into
   * @param pKey The key to write into
   * @param pValue The boolean value to write into pKey in pSection
   */
  public void writeBoolean(String pSection, String pKey, boolean pValue) {
    writeValue(pSection, pKey, Boolean.toString(pValue));
  }

  public String toString() {
    return iniFile.toString();
  }
}
