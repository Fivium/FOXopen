package net.foxopen.fox.plugin;

import net.foxopen.fox.ex.ExInternal;

public class PluginVersion {
  private final int mMajorVersion;
  private final int mMinorVersion;

  public PluginVersion(String pPluginVersion) {
    String[] lVersionParts = pPluginVersion.split("\\.", 2);
    try {
      mMajorVersion = Integer.valueOf(lVersionParts[0]);
      mMinorVersion = Integer.valueOf(lVersionParts[1]);
    }
    catch (Throwable th) {
      throw new ExInternal("Plugin API version property string '" + pPluginVersion + "' is not comprised of two valid integers");
    }
  }

  public int getMajorVersion() {
    return mMajorVersion;
  }

  public int getMinorVersion() {
    return mMinorVersion;
  }

  /**
   * Check that this PluginVersion has a matching Major version and an equivalent or higher Minor version to pComparableVersion
   *
   * @param pComparableVersion a PluginVersion to test this PluginVersion against
   */
  public boolean isVersionCompatible(PluginVersion pComparableVersion) {
    if (getMajorVersion() != pComparableVersion.getMajorVersion() || getMinorVersion() >= pComparableVersion.getMinorVersion()) {
      return false;
    }
    return true;
  }

  public String getVersionString() {
    return getMajorVersion() + "." + getMinorVersion();
  }
}
