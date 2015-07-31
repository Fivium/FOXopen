package net.foxopen.fox.database.parser;


import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.List;


/**
 * A parser for converting SQL statement strings of into {@link ParsedStatement}s, which are in turn composed of
 * individual {@link StatementSegment}s. This class contains no state and does not need to be instantiated.<br/><br/>
 *
 * <b>Bind variable name replacement</b><br/><br/>
 *
 * When this flag is set to true, bind names such as ":bind" are replaced to the standard JDBC bind string "?". This is
 * usually acceptable behaviour ande means binds can be applied positionally and use standards compliant syntax.
 * An exception is in queries where the Oracle parser needs to assert that a bound value is the same, for instance in a
 * query where a bind is present in both the SELECT clause and the GROUP BY clause. In this case bind names should not be
 * replaced. Oracle allows positional binding in queries with names binds, so this is sufficient to solve the problem.
 * As such, arbitrary queries from external sources should not have bind names replaced as this potentially changes the
 * semantics of the statement. APIs must always have bind names replaced as Oracle does not allow positional binding to named
 * binds in the CallableStatement interface.
 */
public class StatementParser {

  /**
   * Character sequences which delimit escaped SQL segments.
   */
  enum EscapeDelimiter {
    //Note that order is important: q-quotes before single quotes (otherwise single quote would match first in a search for q-quote end sequence)
    QQUOTE_BRACE("q'{", "}'", true),
    QQUOTE_SQUARE("q'[", "]'", true),
    QQUOTE_PAREN("q'(", ")'", true),
    QQUOTE_ANGLE("q'<", ">'", true),
    QQUOTE_WILDCARD("q'", "'", true),  //special case wildcard qquote for dealing with arbitrary characters
    COMMENT_MULTILINE("/*", "*/", true),
    DOUBLE_QUOTE("\"", "\"", true),
    SINGLE_QUOTE("'", "'", true),
    COMMENT_SINGLELINE("--", "\n", false); //possible problem here, will not work for files with Mac-only (\r) line endings

    final String mStartSequence;
    final String mEndSequence;
    final boolean mRequiresEndDelimiter;

    private EscapeDelimiter(String pStartSequence, String pEndSequence, boolean pRequiresEndDelimiter){
      mStartSequence = pStartSequence;
      mEndSequence = pEndSequence;
      mRequiresEndDelimiter = pRequiresEndDelimiter;
    }

    public String toString(){
      return mStartSequence;
    }
  }

  private StatementParser() {}

  /**
   * Parses a statement as described by {@link #parse(String, String, boolean, boolean)}. Bind names are replaced to the JDBC bind string.
   * Templating of the resulting statement is not allowed.
   * @param pStatement Statement to parse for binds.
   * @param pPurpose Purpose of the statement, mainly used for debugging.
   * @return ParsedStatement representation of the statement.
   * @throws ExParser If an escape sequence isn't terminated or if EOF is reached and unterminated input remains.
   */
  public static ParsedStatement parse(String pStatement, String pPurpose)
  throws ExParser {
    return parse(pStatement, pPurpose, true, false);
  }

  /**
   * Converts a string containing a SQL statement into a {@link ParsedStatement}. The parsing process recognises the following
   * escape sequences and will not replace bind variable references within them:
   * <ul>
   * <li>Single quote (string literal)</li>
   * <li>Double quote (identifier)</li>
   * <li>Q-quoted string (e.g. <tt>q'{What's up}'</tt>)</li>
   * <li>Single line comment (--)</li>
   * <li>Multi line comment</li>
   * </ul>
   * @param pStatement Statement to parse for binds.
   * @param pPurpose Purpose of the statement, mainly used for debugging.
   * @param pReplaceBindNames If true, bind names are replaced with the JDBC bind string. See class JavaDoc for why this
   * is important.
   * @param pAllowTemplating If true, any template markup in the created ParsedStatement will be applied when {@link ParsedStatement#applyTemplates}
   *                         is invoked.
   * @return ParsedStatement representation of the statement.
   * @throws ExParser If an escape sequence isn't terminated or if EOF is reached and unterminated input remains.
   */
  public static ParsedStatement parse(String pStatement, String pPurpose, boolean pReplaceBindNames, boolean pAllowTemplating)
  throws ExParser {

    String lRemainingScript = pStatement;
    List<StatementSegment> lCurrentStatementSegments = new ArrayList<StatementSegment>();

    Track.pushDebug("ParseStatement", pPurpose);
    try {
      //Start with assuming that the first token to be encountered will be unescaped text
      StatementSegment lCurrentScriptSegment = new UnescapedTextSegment(0);
      //Loop through the whole string, using individual segment objects to gradually deplete it. Segments deplete the buffer
      //until they reach a terminating character (i.e. the start or end of an escape sequence, depending on the segment type)
      do {
        StatementSegment lNextScriptPart = lCurrentScriptSegment.consumeBuffer(lRemainingScript);

        //Store the new segment
        lCurrentStatementSegments.add(lCurrentScriptSegment);

        //If there is more of the string, trim the buffer down for the next segment
        lCurrentScriptSegment = lNextScriptPart;
        if(lNextScriptPart != null){
          lRemainingScript = lRemainingScript.substring(lNextScriptPart.getStartIndex());
        }
      }
      while(lRemainingScript.length() > 0 && lCurrentScriptSegment != null);

      if(pAllowTemplating) {
        return new TemplatedParsedStatement(pStatement, lCurrentStatementSegments, pPurpose, pReplaceBindNames);
      }
      else {
        return new ParsedStatement(pStatement, lCurrentStatementSegments, pPurpose, pReplaceBindNames);
      }
    }
    finally {
      Track.pop("ParseStatement");
    }
  }

  /**
   * Parses a statement as in {@link #parseSafely(String, String, boolean, boolean)}. Bind names are replaced to the JDBC bind string.
   * Templating of the resulting statement is not allowed.
   * @param pStatement Statement to be parsed.
   * @param pPurpose Statement purpose for debug/display.
   * @return Parsed statement.
   */
  public static ParsedStatement parseSafely(String pStatement, String pPurpose) {
    return parseSafely(pStatement, pPurpose, true, false);
  }

  /**
   * Parses a statement as in {@link #parse(String, String, boolean, boolean)}. This method should only be used for parsing static
   * queries which are known  to be valid. parse should be used to parse unknown queries and parser exceptions caught
   * and properly reported. This method will throw an ExInternal if parsing fails.
   * @param pStatement Statement to be parsed.
   * @param pPurpose Statement purpose for debug/display.
   * @param pReplaceBindNames If true, bind names are replaced with the JDBC bind string.
   * @param pAllowTemplating If true, templating will be supported in the statement.
   * @return Parsed statement.
   */
  public static ParsedStatement parseSafely(String pStatement, String pPurpose, boolean pReplaceBindNames, boolean pAllowTemplating) {
    try {
      return parse(pStatement, pPurpose, pReplaceBindNames, pAllowTemplating);
    }
    catch (ExParser e) {
      throw new ExInternal("Failed to parse statement for " + pPurpose, e);
    }
  }
}
