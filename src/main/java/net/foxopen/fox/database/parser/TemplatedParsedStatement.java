package net.foxopen.fox.database.parser;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.codes.IterableCode;
import com.github.mustachejava.codes.ValueCode;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.database.sql.bind.template.TemplateVariableObjectProvider;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.track.Track;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A ParsedStatement which contains template markup. Objects of this type are able to use a {@link TemplateVariableObjectProvider}
 * to execute the template with externally defined variables. Currently only Mustache templates are supported, but the Mustache
 * functionality could be refactored into a composed member object.
 */
public class TemplatedParsedStatement
extends ParsedStatement {

  private static final MustacheFactory MUSTACHE_FACTORY = new SQLEscapingMustacheFactory();

  //Compiled Mustache template
  private final Mustache mCompiledTemplate;
  //All variable names used by the template
  private final Set<String> mTemplateVariableNames;

  public TemplatedParsedStatement(String pOriginalStatement, List<StatementSegment> pSegmentList, String pStatementPurpose, boolean pReplaceBindNames) {
    super(pOriginalStatement, pSegmentList, pStatementPurpose, pReplaceBindNames);

    Track.pushInfo("MustacheCompile", getStatementPurpose());
    try {
      mCompiledTemplate = MUSTACHE_FACTORY.compile(new StringReader(getOriginalStatement()), getStatementPurpose());

      //Parse out variable names used by this template
      Set<String> lTemplateVariables = new HashSet<>();
      seekVariableNames(mCompiledTemplate.getCodes(), lTemplateVariables);
      mTemplateVariableNames = Collections.unmodifiableSet(lTemplateVariables);
    }
    finally {
      Track.pop("MustacheCompile");
    }
  }

  /**
   * Recursively reads variable names from the given array of Mustache codes, populating the pFoundVariables set with
   * overall results.
   * @param pCodes Codes to be examined.
   * @param pFoundVariables Result set.
   */
  private static void seekVariableNames(Code[] pCodes, Set<String> pFoundVariables) {
    if(pCodes != null) {
      for (Code lCode : pCodes) {
        if(lCode instanceof IterableCode || lCode instanceof ValueCode) {
          //Captures iterable, not iterable (extends iterable) and value codes
          pFoundVariables.add(lCode.getName());
        }
        //Recurse through tree
        seekVariableNames(lCode.getCodes(), pFoundVariables);
      }
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

  @Override
  public Collection<String> getAllTemplateVariableNames() {
    return mTemplateVariableNames;
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
