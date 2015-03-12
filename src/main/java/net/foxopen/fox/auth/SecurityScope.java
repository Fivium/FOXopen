package net.foxopen.fox.auth;

import net.foxopen.fox.XFUtil;

public class SecurityScope {

  private final String mCsvSystemPrivileges;
  private final String mUrefXpath;
  private final String mCsvURefList;
  private final String mCsvObjectPrivileges;
  private final String mCsvURefTypes;

  private static final SecurityScope DEFAULT_INSTANCE = new DefaultSecurityScope("*","","","","");

  public static SecurityScope defaultInstance(){
    return DEFAULT_INSTANCE;
  }

  public SecurityScope(String pCsvSysemPrivileges, String pUrefXpath, String pCsvURefList, String pCsvObjectPrivileges, String pCsvURefTypes) {
    mCsvSystemPrivileges = pCsvSysemPrivileges;
    mUrefXpath = pUrefXpath;
    mCsvURefList = pCsvURefList;
    mCsvObjectPrivileges = pCsvObjectPrivileges;
    mCsvURefTypes = pCsvURefTypes;
  }

  public String getUrefXpath() {
    return mUrefXpath;
  }

  public String getCsvURefList() {
    return mCsvURefList;
  }

  public String getCsvObjectPrivileges() {
    return mCsvObjectPrivileges;
  }

  public String getCsvURefTypes() {
    return mCsvURefTypes;
  }

  public boolean isDefault(){
    return false;
  }

  public String getCsvSystemPrivileges() {
    return mCsvSystemPrivileges;
  }

  private static class DefaultSecurityScope
  extends SecurityScope {
    private DefaultSecurityScope(String pCsvSysemPrivileges, String pUrefXpath, String pCsvURefList, String pCsvObjectPrivileges, String pCsvURefTypes){
      super(pCsvSysemPrivileges, pUrefXpath, pCsvURefList, pCsvObjectPrivileges, pCsvURefTypes);
    }

    public boolean isDefault(){
      return true;
    }
  }

  //IntelliJ generated equals method with additional check for isDefault
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SecurityScope)) {
      return false;
    }

    SecurityScope that = (SecurityScope) o;

    if(that.isDefault() && this.isDefault()) {
      return true;
    }
    if (!XFUtil.nvl(mCsvObjectPrivileges).equals(XFUtil.nvl(that.mCsvObjectPrivileges))) {
      return false;
    }
    if (!XFUtil.nvl(mCsvSystemPrivileges).equals(XFUtil.nvl(that.mCsvSystemPrivileges))) {
      return false;
    }
    if (!XFUtil.nvl(mCsvURefList).equals(XFUtil.nvl(that.mCsvURefList))) {
      return false;
    }
    if (!XFUtil.nvl(mCsvURefTypes).equals(XFUtil.nvl(that.mCsvURefTypes))) {
      return false;
    }
    if (!XFUtil.nvl(mUrefXpath).equals(XFUtil.nvl(that.mUrefXpath))) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int lresult = mCsvSystemPrivileges.hashCode();
    lresult = 31 * lresult + XFUtil.nvl(mUrefXpath).hashCode();
    lresult = 31 * lresult + XFUtil.nvl(mCsvURefList).hashCode();
    lresult = 31 * lresult + XFUtil.nvl(mCsvObjectPrivileges).hashCode();
    lresult = 31 * lresult + XFUtil.nvl(mCsvURefTypes).hashCode();
    lresult = 31 * lresult + ((Boolean) isDefault()).hashCode();
    return lresult;
  }
}
