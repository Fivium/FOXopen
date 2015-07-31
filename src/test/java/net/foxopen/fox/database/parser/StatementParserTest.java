package net.foxopen.fox.database.parser;

import net.foxopen.fox.ex.ExParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class StatementParserTest {

  @Test
  public void testBasicParse_Simple()
  throws ExParser {

    ParsedStatement lParsedStatement = StatementParser.parse("SELECT * FROM table WHERE id = :param OR name = 'myname'", "testing");
    assertEquals("Parsed statement contains 1 bind", 1, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :param bind", ":param", lParsedStatement.getBindNameList().get(0));
    assertEquals("Parsed statement rewritten correctly", "SELECT * FROM table WHERE id =  ?  OR name = 'myname'", lParsedStatement.getParsedStatementString());
  }

  @Test
  public void testBasicParse_NamesNotReplaced()
  throws ExParser {
    //Tests that the statement is not rewritten if the rename binds flag is not set
    ParsedStatement lParsedStatement = StatementParser.parse("SELECT * FROM table WHERE id = :param", "testing", false, false);
    assertEquals("Parsed statement contains 1 bind", 1, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :param bind", ":param", lParsedStatement.getBindNameList().get(0));
    assertEquals("Parsed statement no rewritten", "SELECT * FROM table WHERE id = :param", lParsedStatement.getParsedStatementString());
  }


  @Test
  public void testBasicParse_MultipleBinds()
  throws ExParser {

    ParsedStatement lParsedStatement = StatementParser.parse("SELECT :bind1 FROM table WHERE id = :bind2 OR id = :bind3", "testing");

    assertEquals("Parsed statement contains 3 binds", 3, lParsedStatement.getBindNameList().size());
    assertEquals("First bind is :bind1", ":bind1", lParsedStatement.getBindNameList().get(0));
    assertEquals("Second bind is :bind2", ":bind2", lParsedStatement.getBindNameList().get(1));
    assertEquals("Third bind is :bind3", ":bind3", lParsedStatement.getBindNameList().get(2));
    assertEquals("Parsed statement rewritten correctly", "SELECT  ?  FROM table WHERE id =  ?  OR id =  ? ", lParsedStatement.getParsedStatementString());
  }

  @Test
  public void testBasicParse_IgnoresInvalidBindSyntax()
  throws ExParser {

    ParsedStatement lParsedStatement = StatementParser.parse("SELECT * FROM table WHERE id = :_notabind", "testing");
    assertEquals("Parsed statement contains 0 binds", 0, lParsedStatement.getBindNameList().size());

    lParsedStatement = StatementParser.parse("SELECT * FROM table WHERE id = :?notabind", "testing");
    assertEquals("Parsed statement contains 0 binds", 0, lParsedStatement.getBindNameList().size());

    lParsedStatement = StatementParser.parse("DECLARE n NUMBER; BEGIN n := 1; END;", "testing");
    assertEquals("Parsed statement contains 0 binds", 0, lParsedStatement.getBindNameList().size());

  }

  @Test
  public void testBasicParse_EscapedBind_SingleQuotes()
  throws ExParser {

    ParsedStatement lParsedStatement = StatementParser.parse("SELECT ':notabind' FROM table WHERE value = :param", "testing");

    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :param bind", ":param", lParsedStatement.getBindNameList().get(0));
  }

  @Test
  public void testBasicParse_EscapedBind_QQuotes_Bracketed()
  throws ExParser {

    // { } brackets
    ParsedStatement lParsedStatement = StatementParser.parse("SELECT q'{:notabind}' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :param bind", ":param", lParsedStatement.getBindNameList().get(0));

    // [ ] brackets
    lParsedStatement = StatementParser.parse("SELECT q'[:notabind]' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());

    // ( ) brackets
    lParsedStatement = StatementParser.parse("SELECT q'(:notabind)' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());

    // < > brackets
    lParsedStatement = StatementParser.parse("SELECT q'<:notabind>' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());
  }

  @Test
  public void testBasicParse_EscapedBind_QQuotes_ArbitraryDelimiters()
  throws ExParser {
    // !
    ParsedStatement lParsedStatement = StatementParser.parse("SELECT q'!:notabind!' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :param bind", ":param", lParsedStatement.getBindNameList().get(0));

    // #
    lParsedStatement = StatementParser.parse("SELECT q'#:notabind#' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());

    // ?
    lParsedStatement = StatementParser.parse("SELECT q'?:notabind?' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());

    // +
    lParsedStatement = StatementParser.parse("SELECT q'+:notabind+' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());

    // :
    lParsedStatement = StatementParser.parse("SELECT q'::notabind:' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());
  }



  @Test (expected = ExParser.class)
  public void testBasicParse_EscapedBind_QQuotes_BracketMismatch()
  throws ExParser {
    //Should fail because { doesn't match ]
    ParsedStatement lParsedStatement = StatementParser.parse("SELECT q'{:notabind]' FROM table WHERE value = :param", "testing");
  }

  @Test (expected = ExParser.class)
  public void testBasicParse_EscapedBind_QQuotes_ArbitraryDelimitersMismatch()
  throws ExParser {
    //Should fail because ! doesn't match #
    ParsedStatement lParsedStatement = StatementParser.parse("SELECT q'!:notabind#' FROM table WHERE value = :param", "testing");
  }

  @Test
  public void testBasicParse_QQuoteInQuotes()
  throws ExParser {
    // A q-quote like string appears in a quoted string; parser should handle this as 2 seperate strings
    ParsedStatement lParsedStatement = StatementParser.parse("SELECT 'this is not a q'{:quote}' ' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains 2 binds", 2, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :quote bind", ":quote", lParsedStatement.getBindNameList().get(0));
  }

  @Test
  public void testBasicParse_QuoteInQQuotes()
  throws ExParser {
    // A quote appears in a q-quote, this should not be treated as an escape delimiter so :bind should not be found
    ParsedStatement lParsedStatement = StatementParser.parse("SELECT q'{this isn't a :bind}' FROM table WHERE value = :param", "testing");
    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :param bind", ":param", lParsedStatement.getBindNameList().get(0));
  }

  @Test
  public void testBasicParse_EscapedBind_DoubleQuotes()
  throws ExParser {

    ParsedStatement lParsedStatement = StatementParser.parse("SELECT col \":notabind\" FROM table WHERE value = :param", "testing");

    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :param bind", ":param", lParsedStatement.getBindNameList().get(0));
  }

  @Test
  public void testBasicParse_EscapedBind_SingleLineComment()
  throws ExParser {

    ParsedStatement lParsedStatement = StatementParser.parse("SELECT col --:notabind\n" +
      "FROM table WHERE value = :param", "testing");

    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :param bind", ":param", lParsedStatement.getBindNameList().get(0));
  }

  @Test
  public void testBasicParse_EscapedBind_BlockComment()
  throws ExParser {

    ParsedStatement lParsedStatement = StatementParser.parse("SELECT col /*:notabind*/ FROM table WHERE value = :param", "testing");

    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :param bind", ":param", lParsedStatement.getBindNameList().get(0));
  }

  @Test
  public void testBasicParse_EscapedBind_BlockComment_Multiline()
  throws ExParser {

    ParsedStatement lParsedStatement = StatementParser.parse("SELECT col\n" +
      "/* some text \n" +
      " :notabind \n" +
      " some more text */\n" +
      "FROM table WHERE value = :param", "testing");

    assertEquals("Parsed statement contains only 1 bind", 1, lParsedStatement.getBindNameList().size());
    assertEquals("Parsed statement contains :param bind", ":param", lParsedStatement.getBindNameList().get(0));
  }

  @Test (expected = ExParser.class)
  public void testBasicParse_FailsWhenSequenceNotTerminated()
  throws ExParser {
    StatementParser.parse("SELECT col FROM table WHERE text = 'hello", "testing");
  }

}
