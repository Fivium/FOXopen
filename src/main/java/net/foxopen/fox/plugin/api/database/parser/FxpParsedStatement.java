package net.foxopen.fox.plugin.api.database.parser;

import java.util.List;

public interface FxpParsedStatement {
  String getOriginalStatement();

  String getParsedStatementString();

  /**
   * Gets a list of the bind names used in this statement, in the order they are encountered in the statement. May
   * contain duplicates if the same bind name is used multiple times in the statement. The name includes the ":" prefix character.
   * @return List of bind names.
   */
  List<String> getBindNameList();

  String getStatementPurpose();
}
