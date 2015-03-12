package net.foxopen.fox.module;

public enum HelpDisplayOption {
  INLINE,
  ICON,
  NONE;
  
  public static HelpDisplayOption fromExternalString(String pExternalString) {
    if(pExternalString == null) {
      return null;
    }
    
    return valueOf(pExternalString.toUpperCase());
  }
  
  public String getExternalString() {
    return this.toString().toLowerCase();
  }
}
