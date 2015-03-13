package net.foxopen.fox.enginestatus;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.enginestatus.ws.StatusWebServiceCategory;
import net.foxopen.fox.entrypoint.ws.WebServiceServlet;
import net.foxopen.fox.ex.ExInternal;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class EngineStatus {

  private static EngineStatus INSTANCE = new EngineStatus();
  public static EngineStatus instance() {
    return INSTANCE;
  }

  public static final String ALL_CATEGORIES_ATTRIBUTE = "ALL_CATEGORIES";

  /** Category mnems mapped to catgories **/
  private Map<String, StatusCategory> mCategories = new TreeMap<>();

  /** Category mnems mapped to status providers for those categories **/
   private Map<String, StatusProvider> mProviders = new HashMap<>();

  static {
    WebServiceServlet.registerWebServiceCategory(new StatusWebServiceCategory());
  }

  private EngineStatus() {}

  /**
   * Searches for an item with the given mnemonic within the given collection. The search is not recursive.
   * @param pChildren Items to search.
   * @param pItemMnem Item mnemonic to find.
   * @return The matching StatusItem or null if it can't be found.
   */
  static StatusItem getNestedItem(Collection<? extends StatusItem> pChildren, String pItemMnem) {
    for(StatusItem lItem : pChildren) {
      if(pItemMnem.equals(lItem.getMnem())) {
        return lItem;
      }
    }
    return null;
  }

  /**
   * Retrieves the most severe message level from the given collection of StatusItems.
   * @param pChildren StatusItems to search for.
   * @return Max severity or a default of INFO.
   */
  static MessageLevel getMaxChildMessageSeverity(Collection<? extends StatusItem> pChildren) {
    MessageLevel lMax = MessageLevel.INFO;
    for(StatusItem lItem : pChildren) {
      if(lItem.getMaxMessageSeverity().intValue() > lMax.intValue()) {
        lMax = lItem.getMaxMessageSeverity();
      }
    }
    return lMax;
  }

  /**
   * Globally registers the given status provider, creating a new StatusCategory. This should only be invoked once for
   * each required category.
   * @param pStatusProvider Source of transient status messages.
   * @return New status category for setting sticky status messages.
   */
  public StatusCategory registerStatusProvider(StatusProvider pStatusProvider) {
    mProviders.put(pStatusProvider.getCategoryMnemonic(), pStatusProvider);
    return addCategory(pStatusProvider.getCategoryMnemonic(), pStatusProvider.getCategoryTitle(), pStatusProvider.isCategoryExpandedByDefault());
  }

  private StatusCategory addCategory(String pCategoryMnem, String pCategoryTitle, boolean pIsExpanded) {
    StatusCategory lCategory = new StatusCategory(pCategoryMnem, pCategoryTitle, pIsExpanded);
    mCategories.put(pCategoryMnem, lCategory);
    return lCategory;
  }

  private void refreshCategoriesFromProviders() {
    for(StatusProvider lProvider : mProviders.values()) {
      //Assumes category has already been created
      try {
        lProvider.refreshStatus(mCategories.get(lProvider.getCategoryMnemonic()).createStatusDestination());
      }
      catch (Throwable th) {
      }
    }
  }

  public Collection<StatusCategory> getAllCategories() {
    return mCategories.values();
  }

  public StatusCategory getCategory(String pCategoryMnem) {
    if(!mCategories.containsKey(pCategoryMnem)) {
      throw new ExInternal("Category " + pCategoryMnem + " not found");
    }
    return mCategories.get(pCategoryMnem);
  }

  public void refreshCategory(String pCategoryMnem) {
    if(!mProviders.containsKey(pCategoryMnem)) {
      throw new ExInternal("Category " + pCategoryMnem + " not found");
    }
    mProviders.get(pCategoryMnem).refreshStatus(mCategories.get(pCategoryMnem).createStatusDestination());
  }

  public void getHTMLResponse(FoxRequest pFoxRequest) {

    HttpServletRequest lHttpRequest = pFoxRequest.getHttpRequest();
    refreshCategoriesFromProviders();

    lHttpRequest.setAttribute(ALL_CATEGORIES_ATTRIBUTE, mCategories.values());

    RequestDispatcher lRequestDispatcher = lHttpRequest.getRequestDispatcher("/WEB-INF/components-new/engineStatus.jsp");
    try {
      lRequestDispatcher.forward(lHttpRequest, pFoxRequest.getHttpResponse());
    }
    catch (ServletException | IOException e) {
      throw new ExInternal("Error forwarding to status JSP", e);
    }
  }

  static String textToHtml(String pText) {
    if(pText != null) {
      return StringEscapeUtils.escapeHtml4(pText).replace("\n", "<br>");
    }
    else {
      return null;
    }
  }

  static String promptToMnem(String pPrompt) {
    StringBuilder lResult = new StringBuilder();
    int i = 0;
    for(String lWord : pPrompt.split(" ")) {
      if(i++ == 0) {
        lWord = lWord.toLowerCase();
      }
      else {
        lWord = StringUtils.capitalize(lWord);
      }
      lResult.append(lWord);
    }
    return lResult.toString();
  }

  public static String formatDate(Date pDate) {
    return new SimpleDateFormat("dd/MM/YYYY HH:mm:ss").format(pDate);
  }
}
