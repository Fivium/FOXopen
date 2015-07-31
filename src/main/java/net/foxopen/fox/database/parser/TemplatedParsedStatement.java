package net.foxopen.fox.database.parser;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.database.sql.bind.template.TemplateVariableObjectProvider;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.track.Track;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * A ParsedStatement which contains template markup (i.e. a Mustache template). Objects of this type are able to use a
 * {@link TemplateVariableObjectProvider} to execute the template with externally defined variables.
 */
public class TemplatedParsedStatement
extends ParsedStatement {

  private static final MustacheFactory MUSTACHE_FACTORY = new SQLEscapingMustacheFactory();

  //Compiled Mustache template
  private final Mustache mCompiledTemplate;

  public TemplatedParsedStatement(String pOriginalStatement, List<StatementSegment> pSegmentList, String pStatementPurpose, boolean pReplaceBindNames) {
    super(pOriginalStatement, pSegmentList, pStatementPurpose, pReplaceBindNames);

    Track.pushInfo("MustacheCompile", getStatementPurpose());
    try {
      mCompiledTemplate = MUSTACHE_FACTORY.compile(new StringReader(getOriginalStatement()), getStatementPurpose());
    }
    finally {
      Track.pop("MustacheCompile");
    }
  }

  @Override
  public ParsedStatement applyTemplates(BindObjectProvider pBindProvider) {

    //Only apply templates if the BindProvider is of the correct type
    if(pBindProvider instanceof TemplateVariableObjectProvider) {
      Track.pushInfo("MustacheApply", getStatementPurpose());
      try {
        Writer lMustacheResult = new StringWriter();

        //Note: this signature of compile doesn't cache results
        mCompiledTemplate.execute(lMustacheResult, ((TemplateVariableObjectProvider) pBindProvider).asTemplateVariableMap());

        try {
          //Create a new ParsedStatement from the result of the mustache template apply
          ParsedStatement lParseResult = StatementParser.parse(lMustacheResult.toString(), getStatementPurpose(), isBindNamesReplaced(), false);
          Track.logInfoText("MustacheApplyResult", lParseResult.getParsedStatementString());
          return lParseResult;
        }
        catch (ExParser e) {
          throw new ExInternal("Failed to parse converted query after mustache template apply", e);
        }
      }
      finally {
        Track.pop("MustacheApply");
      }
    }
    else {
      return super.applyTemplates(pBindProvider);
    }
  }

  /**
   * A MustacheFactory which escapes sensitive SQL characters into obvious replacement tokens, to alert developers to SQL
   * injection vulnerabilities. This overrides the default Mustache behaviour which escapes HTML entities.
   */
  private static class SQLEscapingMustacheFactory
  extends DefaultMustacheFactory {

    private SQLEscapingMustacheFactory(){
      super();
    }

    @Override
    public void encode(String pValue, Writer pWriter) {
      try {
        for(int i=0; i < pValue.length(); i++) {
          char c = pValue.charAt(i);

          if(c == '"') {
            pWriter.append("<DQUOTE>");
          }
          else if(c == '\'') {
            pWriter.append("<SQUOTE>");
          }
          else if(c == '&') {
            pWriter.append("<AMP>");
          }
          else {
            pWriter.append(c);
          }
        }
      }
      catch (IOException e) {
        throw new MustacheException("Failed to encode value " + pValue, e);
      }
    }
  }
}
