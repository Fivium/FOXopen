package net.foxopen.fox.database.parser;

import net.foxopen.fox.database.sql.ExecutableAPI;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.plugin.api.database.parser.FxpParsedStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * An individual SQL statement which has been parsed for bind variables, with Oracle style bind syntax replaced with
 * JDBC compatible syntax. A list of the bind variable names, ordered according to their positions in the statement,
 * is established when the statement is parsed. Note: the statement string is not guaranteed to be syntactically valid SQL.
 */
public class ParsedStatement implements FxpParsedStatement {

  public static final String BIND_REPLACE_STRING = " ? "; //Oracle Bind string

  //Bind variable names must start with an alphanumeric and subsequently contain only alphanumerics, $, _ or # symbols
  private static final Pattern BIND_VARIABLE_PATTERN = Pattern.compile(":[A-Za-z0-9][\\w$#]*");

  private final String mStatementPurpose;

  private final boolean mBindNamesReplaced;

  private final String mOriginalStatement;
  private final String mParsedStatement;
  private final List<String> mBindNameList;

  /**
   * Constructs a new ParsedStatement from the segments provided in the list.
   * @param pSegmentList List of segments which will comprise the new ParsedStatement.
   */
  ParsedStatement(String pOriginalStatement, List<StatementSegment> pSegmentList, String pStatementPurpose, boolean pReplaceBindNames){
    mOriginalStatement = pOriginalStatement;
    mStatementPurpose = pStatementPurpose;

    StringBuilder lParsedStatementBuilder = new StringBuilder();

    //Loop through the statement segments, replacing binds in unescaped segments only
    List<String> lBindList = new ArrayList<String>();
    for(StatementSegment lSegment : pSegmentList){
      if(lSegment instanceof UnescapedTextSegment) {

        //Search for binds in the contents of this unescaped segment
        Matcher lMatcher = BIND_VARIABLE_PATTERN.matcher(lSegment.getContents());

        StringBuffer lNewContents = new StringBuffer();
        while (lMatcher.find()) {
          //Make a record of the string we replaced
          lBindList.add(lMatcher.group());
          //Replace with JDBC bind syntax
          if(pReplaceBindNames) {
            lMatcher.appendReplacement(lNewContents, BIND_REPLACE_STRING);
          }
        }
        lMatcher.appendTail(lNewContents);

        //Add the processed contents of this segment to the final parsed statement string
        lParsedStatementBuilder.append(lNewContents.toString());
      }
      else {
        //This is an escaped sequence; do not parse it for bind variables
        lSegment.serialiseTo(lParsedStatementBuilder);
      }
    }

    mBindNamesReplaced = pReplaceBindNames;

    mParsedStatement = lParsedStatementBuilder.toString();
    mBindNameList = Collections.unmodifiableList(lBindList);
  }

  public String getOriginalStatement() {
    return mOriginalStatement;
  }

  public String getParsedStatementString() {
    return mParsedStatement;
  }

  /**
   * Gets a list of the bind names used in this statement, in the order they are encountered in the statement. May
   * contain duplicates if the same bind name is used multiple times in the statement. The name includes the ":" prefix character.
   * @return List of bind names.
   */
  public List<String> getBindNameList() {
    return mBindNameList;
  }

  public ExecutableQuery createExecutableQuery(BindObjectProvider pBindProvider){
    return new ExecutableQuery(this, pBindProvider);
  }

  public ExecutableAPI createExecutableAPI(BindObjectProvider pBindProvider){
    return new ExecutableAPI(this, pBindProvider);
  }

  public String getStatementPurpose() {
    return mStatementPurpose;
  }

  @Override
  public String toString() {
    return "ParsedStatement " + mStatementPurpose;
  }

  /**
   * Applies template markup to this ParsedStatement, provided that it a) is marked as containing template markup and
   * b) the BindObjectProvider is a {@link net.foxopen.fox.database.sql.bind.template.TemplateVariableObjectProvider}.
   * @param pBindProvider BindProvider to use to resolve template variables.
   * @return A new ParsedStatement with templates applied, or self if templates were not applied.
   */
  public ParsedStatement applyTemplates(BindObjectProvider pBindProvider) {
    //Default implementation is to do nothing - allow TemplatedParsedStatement subclass to specialise
    return this;
  }

  protected boolean isBindNamesReplaced() {
    return mBindNamesReplaced;
  }
}
