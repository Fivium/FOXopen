package net.foxopen.fox.module.serialiser;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;


public enum HtmlDoctype {
  HTML5("<!DOCTYPE html>\r\n")
, HTML4_TRANSITIONAL("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\r\n")
, HTML4_STRICT("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\r\n")
, XHTML1_TRANSITIONAL("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\r\n")
, XHTML1_STRICT("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\r\n")
, NONE("");

  private final String mDoctypeDeclaration;

  private HtmlDoctype(String pDoctypeString) {
    mDoctypeDeclaration = pDoctypeString;
  }

  public String getDoctypeDeclaration() {
    return mDoctypeDeclaration;
  }

  public static HtmlDoctype getByNameOrNull(String pName) {
    if (!XFUtil.isNull(pName)) {
      try {
        return valueOf(pName);
      }
      catch (IllegalArgumentException ex) {
        throw new ExInternal("document-type of '" + pName + "' not found in the HtmlDoctypes Enum", ex);
      }
    }
    else {
      return null;
    }
  }
}
