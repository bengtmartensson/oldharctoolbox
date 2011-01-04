package harc;

import java.io.*;

/**
 *
 */
public class extension_filter implements FilenameFilter {

    protected String extension;

    public extension_filter(String extension) {
    this.extension = extension;
  }

  public boolean accept (File directory, String name) {
    return name.toLowerCase().endsWith(extension.toLowerCase());
  }

  public static void main (String args[]) {
    if (args.length != 1) {
       System.err.println("usage: java Filter <pattern list>");
       return;
    }

    extension_filter nf = new extension_filter(args[0]);
    File dir = new File (".");
    String[] strs = dir.list(nf);
    for (int i = 0; i < strs.length; i++) {
      System.out.println (strs[i]);
    }
  }
}
