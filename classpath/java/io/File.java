/* Copyright (c) 2008, Avian Contributors

   Permission to use, copy, modify, and/or distribute this software
   for any purpose with or without fee is hereby granted, provided
   that the above copyright notice and this permission notice appear
   in all copies.

   There is NO WARRANTY for this software.  See license.txt for
   details. */

package java.io;

public class File {
  private static final String FileSeparator
    = System.getProperty("file.separator");

  //   static {
  //     System.loadLibrary("natives");
  //   }

  private final String path;

  public File(String path) {
    if (path == null) throw new NullPointerException();
    this.path = path;
  }

  public File(String parent, String child) {
    this(parent + FileSeparator + child);
  }

  public File(File parent, String child) {
    this(parent.getPath() + FileSeparator + child);
  }

  private static native boolean isDirectory(String path);

  public boolean isDirectory() {
    return isDirectory(path);
  }

  public String getName() {
    int index = path.lastIndexOf(FileSeparator);
    if (index >= 0) {
      return path.substring(index + 1);
    } else {
      return path;
    }
  }

  public String getPath() {
    return path;
  }

  public String getParent() {
    int index = path.lastIndexOf(FileSeparator);
    if (index >= 0) {
      return path.substring(0, index);
    } else {
      return null;
    }    
  }

  public File getParentFile() {
    String s = getParent();
    return (s == null ? null : new File(s));
  }

  private static native String toCanonicalPath(String path);

  public String getCanonicalPath() {
    return toCanonicalPath(path);
  }

  public File getCanonicalFile() {
    return new File(getCanonicalPath());
  }

  private static native String toAbsolutePath(String path);

  public String getAbsolutePath() {
    return toAbsolutePath(path);
  }

  private static native long length(String path);

  public long length() {
    return length(path);
  }

  private static native boolean exists(String path);

  public boolean exists() {
    return exists(path);
  }

  private static native void mkdir(String path) throws IOException;

  public boolean mkdir() {
    try {
      mkdir(path);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  private static native void createNewFile(String path) throws IOException;

  public boolean createNewFile() {
    try {
      createNewFile(path);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static native void delete(String path) throws IOException;

  public boolean delete() {
    try {
      delete(path);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public boolean mkdirs() {
    File parent = getParentFile();
    if (parent != null) {
      if (!parent.exists()) {
        if (!parent.mkdirs()) {
          return false;
        }
      }
    }
    return mkdir();
  }

  public String[] list() {
    return list(null);
  }

  public String[] list(FilenameFilter filter) {
    long handle = 0;
    try {
      handle = openDir(path);
      Pair list = null;
      int count = 0;
      for (String s = readDir(handle); s != null; s = readDir(handle)) {
        if (filter == null || filter.accept(this, s)) {
          list = new Pair(s, list);
          ++ count;
        }
      }

      String[] result = new String[count];
      for (int i = count; i >= 0; --i) {
        result[i] = list.value;
        list = list.next;
      }

      return result;
    } finally {
      if (handle != 0) {
        closeDir(handle);
      }
    }
  }

  private static native long openDir(String path);

  private static native String readDir(long handle);

  private static native long closeDir(long handle);

  private static class Pair {
    public final String value;
    public final Pair next;
    
    public Pair(String value, Pair next) {
      this.value = value;
      this.next = next;
    }
  }

}
