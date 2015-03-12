package net.foxopen.fox.database.sql.bind;

public enum BindDirection {
  IN,
  OUT,
  IN_OUT;
  
  public boolean isInBind() {
    return this != OUT;
  }
  
  public boolean isOutBind() {
    return this != IN;
  }
}
