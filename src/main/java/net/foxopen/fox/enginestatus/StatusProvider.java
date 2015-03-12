package net.foxopen.fox.enginestatus;

public interface StatusProvider {

  void refreshStatus(StatusCategory pCategory);

  String getCategoryTitle();

  String getCategoryMnemonic();

  boolean isCategoryExpandedByDefault();

}
