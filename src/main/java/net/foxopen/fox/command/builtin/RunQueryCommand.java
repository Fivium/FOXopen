package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.QueryResultDeliverer;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dbinterface.QueryMode;
import net.foxopen.fox.dbinterface.StatementExecutionBindOptions;
import net.foxopen.fox.dbinterface.StatementExecutionResult;
import net.foxopen.fox.dbinterface.deliverer.InterfaceQueryResultDeliverer;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.paging.DatabasePager;
import net.foxopen.fox.dom.paging.PagerProvider;
import net.foxopen.fox.dom.paging.PagerSetup;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Arrays;
import java.util.Collection;

//TODO pn rename to run-query when original command deleted
public class RunQueryCommand
extends BuiltInCommand {

  private final String mInterfaceName;
  private final String mQueryName;
  private final String mMatchPath;
  private final QueryMode mQueryMode;

  //Can be null if no pagination setup defined
  private final PagerSetup mPagerSetup;

  //Can be null
  private final String mCacheBindsXPath;

  // Parse command returning tokenised representation
  public RunQueryCommand(Mod pMod, DOM pParseUElem)
  throws ExDoSyntax {
    super(pParseUElem);

    mInterfaceName = pParseUElem.getAttr("interface");
    mQueryName = pParseUElem.getAttr("query");
    mMatchPath = pParseUElem.getAttr("match");

    if(XFUtil.isNull(mInterfaceName)) {
      throw new ExInternal("Interface name must be specified");
    }
    else if(XFUtil.isNull(mQueryName)) {
      throw new ExInternal("Query name must be specified");
    }

    String lModeString = pParseUElem.getAttr("mode");
    if(!XFUtil.isNull(lModeString)) {
      try {
        mQueryMode = QueryMode.fromExternalString(lModeString);
      }
      catch (ExModule e) {
        throw new ExInternal("Invalid query mode on query " + mQueryName, e);
      }
    }
    else {
      mQueryMode = null;
    }

    //Validate the query is defined
    InterfaceQuery lInterfaceQuery = pMod.getDatabaseInterface(mInterfaceName).getInterfaceQuery(mQueryName);

    try {
      mPagerSetup = PagerSetup.fromDOMMarkupOrNull(pParseUElem, "pagination-definition", "page-size", "pagination-invoke-name", lInterfaceQuery.getPaginationDefnitionName());
    }
    catch (ExModule e) {
      throw new ExInternal("Error in run-query definition for query " + mQueryName, e);
    }

    mCacheBindsXPath = pParseUElem.getAttr("cache-binds");
    if(!XFUtil.isNull(mCacheBindsXPath) && lInterfaceQuery.getTopNPaginationConfig() == null) {
      throw new ExInternal("cache-binds XPath attribute is only applicable to Top-N queries at this time");
    }
  }

  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    InterfaceQuery lInterfaceQuery = pRequestContext.getCurrentModule().getDatabaseInterface(mInterfaceName).getInterfaceQuery(mQueryName);

    //Default the match path to current attach point if not specified
    String lMatch = mMatchPath;
    if(XFUtil.isNull(lMatch)) {
      lMatch = ".";
    }

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    //Evaluate the match XPath
    DOMList lMatchList;
    try {
      lMatchList = lContextUElem.extendedXPathUL(lMatch, null);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to run query match XPath for query " + mQueryName, e);
    }

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Run Query " + mQueryName);
    try {
      //Loop through all matched nodes - execute the query and deliver to the match node
      for (DOM lMatchNode : lMatchList) {
        try {
          //Establish a pager (and possibly cached binds from the pager) if there is a pager definition
          DatabasePager lPager = null;
          DecoratingBindObjectProvider lBindProvider;
          boolean lCacheBinds;
          if(mPagerSetup != null) {
            lPager = pRequestContext.getModuleFacetProvider(PagerProvider.class).getOrCreateDatabasePager(mPagerSetup.evalute(pRequestContext, lMatchNode.getFoxId()), lInterfaceQuery);

            //The pager might need to provide additional bind details (this can be null)
            lBindProvider = lPager.getDecoratingBindProviderOrNull(lInterfaceQuery);
            //Evaluate the cache binds XPath if defined - developer may not want bind caching enabled for some reason
            lCacheBinds = lPager.allowsCachedBindVariables() && cacheBindsXPath(lContextUElem);
          }
          else {
            lBindProvider = null;
            lCacheBinds = false;
          }

          //Construct a new deliverer based on the query mode (ADD-TO, PURGE-ALL, etc)
          QueryResultDeliverer lDeliverer = InterfaceQueryResultDeliverer.getDeliverer(pRequestContext, lInterfaceQuery, mQueryMode, lMatchNode, lPager);

          //Create parameter object to tell statement executor if we want cached binds back
          StatementExecutionBindOptions lBindOptions = new StatementExecutionBindOptions() {
            @Override public DecoratingBindObjectProvider getDecoratingBindObjectProvider() { return lBindProvider; }
            @Override public boolean cacheBinds() { return lCacheBinds; }
          };

          //Run the statement into the deliverer
          StatementExecutionResult lStatementExecutionResult = lInterfaceQuery.executeStatement(pRequestContext, lMatchNode, lUCon, lBindOptions, lDeliverer);

          //Tell Top-N pagers about the bind variable so they can cache them
          if(lPager != null && lCacheBinds) {
            lPager.setCachedBindVariables(pRequestContext.getPersistenceContext(), lStatementExecutionResult.getCachedBindObjectProvider());
          }
        }
        catch (ExDB e) {
          throw new ExInternal("Failed to run query " + mQueryName, e);
        }
      }
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Run Query " + mQueryName);
    }

    return XDoControlFlowContinue.instance();
  }

  private boolean cacheBindsXPath(ContextUElem pContextUElem) {
    if(!XFUtil.isNull(mCacheBindsXPath)) {
      try {
        return pContextUElem.extendedXPathBoolean(pContextUElem.attachDOM(), mCacheBindsXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to run cache-binds XPath", e);
      }
    }
    else {
      return true;
    }
  }

  public boolean isCallTransition() {
   return false;
  }
  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new RunQueryCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Arrays.asList("run-query", "run-query2");
    }
  }
}
