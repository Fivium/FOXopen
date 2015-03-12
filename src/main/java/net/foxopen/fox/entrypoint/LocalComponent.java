package net.foxopen.fox.entrypoint;

import java.io.File;

public class LocalComponent {
  private final File mFile; // Path to component on dis
  private final String mType; // Mime type
  private final String mFormat; // BIN/CHAR
  private final boolean mOverloadable; // Should this component be found before or after looking at regular components

  public LocalComponent(File pFile, String pType, String pFormat, boolean pOverloadable) {
    mFile = pFile;
    mType = pType;
    mFormat = pFormat;
    mOverloadable = pOverloadable;
  }

  public File getFile() {
    return mFile;
  }

  public String getType() {
    return mType;
  }

  public String getFormat() {
    return mFormat;
  }

  public boolean isOverloadable() {
    return mOverloadable;
  }
}
