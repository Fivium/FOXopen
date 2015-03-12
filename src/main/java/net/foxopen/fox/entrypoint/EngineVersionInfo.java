package net.foxopen.fox.entrypoint;

import net.foxopen.fox.XFUtil;

import java.util.jar.Manifest;

public class EngineVersionInfo {

  private final String mVersionNumber;
  private final String mBuildTag;
  private final String mBuildTime;

  static EngineVersionInfo unknownInstance() {
    return new EngineVersionInfo("unknown", "unknown", "unknown");
  }

  static EngineVersionInfo fromManifest(Manifest pManifest) {
    String lVersion = XFUtil.nvl(pManifest.getMainAttributes().getValue("FOX-Version"), "unknown");
    String lBuildTag = XFUtil.nvl(pManifest.getMainAttributes().getValue("Build-Tag"), "unknown");
    String lTime = XFUtil.nvl(pManifest.getMainAttributes().getValue("Build-Time"), "unknown");

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
}
