// This implementation is based directly on code developed by David Croft
// http://www.davidc.net/programming/java/java-preferences-using-file-backing-store

// package net.infotrek.util.prefs;

package org.motiflab.engine.util;

import java.io.File;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * PreferencesFactory implementation that stores the preferences in a user-defined file. To use it,
 * set the system property <tt>java.util.prefs.PreferencesFactory</tt> to
 * <tt>net.infotrek.util.prefs.FilePreferencesFactory</tt>
 * <p/>
 * The file defaults to [user.home]/.fileprefs, but may be overridden with the system property
 * <tt>net.infotrek.util.prefs.FilePreferencesFactory.file</tt>
 *
 * @author David Croft (<a href="http://www.davidc.net">www.davidc.net</a>)
 * @version $Id: FilePreferencesFactory.java 282 2009-06-18 17:05:18Z david $
 */
public class FilePreferencesFactory implements PreferencesFactory
{
  private static final Logger log = Logger.getLogger(FilePreferencesFactory.class.getName());
 
  Preferences rootPreferences;
  public static final String SYSTEM_PROPERTY_FILE = "org.motiflab.engine.util.FilePreferencesFactory.file";
 
  public Preferences systemRoot()
  {
    return userRoot();
  }
 
  public Preferences userRoot()
  {
    if (rootPreferences == null) {
      log.finer("Instantiating root preferences");
 
      rootPreferences = new FilePreferences(null, "");
    }
    return rootPreferences;
  }
 
  private static File preferencesFile;
 
  public static File getPreferencesFile()
  {
    if (preferencesFile == null) {
      String prefsFile = System.getProperty(SYSTEM_PROPERTY_FILE);
      if (prefsFile == null || prefsFile.length() == 0) {
        prefsFile = System.getProperty("user.home") + File.separator + ".motiflabprefs";
      }
      preferencesFile = new File(prefsFile).getAbsoluteFile();
      log.finer("Preferences file is " + preferencesFile);
    }
    return preferencesFile;
  }
 
//  public static void main(String[] args) throws BackingStoreException
//  {
//    System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());
//    System.setProperty(SYSTEM_PROPERTY_FILE, "myprefs.txt");
// 
//    Preferences p = Preferences.userNodeForPackage(my.class);
// 
//    for (String s : p.keys()) {
//      System.out.println("p[" + s + "]=" + p.get(s, null));
//    }
// 
//    p.putBoolean("hi", true);
//    p.put("Number", String.valueOf(System.currentTimeMillis()));
//  }


}