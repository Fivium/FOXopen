package net.foxopen.fox.enginestatus;

import net.foxopen.fox.banghandler.BangHandler;

import java.util.Map;

public interface StatusDestination {
  StatusTable addTable(String pTableName, String... pColumnNames);

  void addMessage(String pMessageTitle, String pMessageBody);

  void addMessage(String pMessageTitle, String pMessageBody, MessageLevel pLevel);

  void addDetailMessage(String pMessageTitle, StatusDetail.Provider pStatusDetailProvider);

  void addAction(String pPrompt, BangHandler pBangHandler);

  void addAction(String pPrompt, String pAbsoluteURI);

  void addAction(String pPrompt, BangHandler pBangHandler, Map<String, String> pParamMap);
}
