package net.foxopen.fox.entrypoint;

import net.foxopen.fox.XFUtil;

import java.util.jar.Manifest;

public class EngineVersionInfo {

  private static final String UNKNOWN = "unknown";
  private static final String UNSPECIFIED = "unspecified";

  private final String mVersionNumber;
  private final String mBuildTag;
  private final String mBuildTime;

  static EngineVersionInfo unknownInstance() {
    return new EngineVersionInfo(UNKNOWN, UNKNOWN, UNKNOWN);
  }

  static EngineVersionInfo fromManifest(Manifest pManifest) {
    String lVersion = XFUtil.nvl(pManifest.getMainAttributes().getValue("FOX-Version"), UNSPECIFIED);
    String lBuildTag = XFUtil.nvl(pManifest.getMainAttributes().getValue("Build-Tag"), UNSPECIFIED);
    String lTime = XFUtil.nvl(pManifest.getMainAttributes().getValue("Build-Time"), UNSPECIFIED);

    return new EngineVersionInfo(lVersion, lBuildTag, lTime);
  }

  private EngineVersionInfo(String pVersionNumber, String pBuildTag, String pBuildTime) {
    mVersionNumber = pVersionNumber;
    mBuildTag = pBuildTag;
    mBuildTime = pBuildTime;
  }

  public String getVersionNumber() {
    return mVersionNumber;
  }

  public String getBuildTag() {
    return mBuildTag;
  }

  public String getBuildTime() {
    return mBuildTime;
  }

  public boolean isVersionKnown() {
    return !(UNKNOWN.equals(mVersionNumber) || UNSPECIFIED.equals(mVersionNumber));
  }
}
