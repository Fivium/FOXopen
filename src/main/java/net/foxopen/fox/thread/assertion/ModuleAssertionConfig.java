package net.foxopen.fox.thread.assertion;

import com.google.common.base.Splitter;
import net.foxopen.fox.dom.DOM;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Assertion configuration options marked up on a module definition.
 */
public class ModuleAssertionConfig {

  private final String mEntryThemeName;
  private final String mBeforeActionName;
  private final String mAfterActionName;

  private final Set<String> mTestCategories;

  /**
   * Creates a new instance from module markup.
   * @param pDOM DOM containing assertion configuration options.
   * @return New ModuleAssertionConfig based on given markup.
   */
  public static ModuleAssertionConfig fromMarkupDOM(DOM pDOM) {

    String lEntryTheme = pDOM.get1SNoEx("fm:entry-theme");
    String lBeforeAction = pDOM.get1SNoEx("fm:before-action");
    String lAfterAction = pDOM.get1SNoEx("fm:after-action");

    List<String> lCategoryList = Splitter.on(",").trimResults().splitToList(pDOM.get1SNoEx("fm:test-categories"));
    Set<String> lCategories = Collections.unmodifiableSet(new HashSet<>(lCategoryList));

    return new ModuleAssertionConfig(lEntryTheme, lBeforeAction, lAfterAction, lCategories);
  }

  private ModuleAssertionConfig(String pEntryThemeName, String pBeforeActionName, String pAfterActionName, Set<String> pTestCategories) {
    mEntryThemeName = pEntryThemeName;
    mBeforeActionName = pBeforeActionName;
    mAfterActionName = pAfterActionName;
    mTestCategories = pTestCategories;
  }


  /**
   * @return The name of the entry theme to use when setting up for assertion actions. May be null/empty.
   */
  public String getEntryThemeName() {
    return mEntryThemeName;
  }

  /**
   * @return The name of an action to run before every assertion action. May be null/empty.
   */
  public String getBeforeActionName() {
    return mBeforeActionName;
  }

  /**
   * @return The name of an action to run after every assertion action. May be null/empty.
   */
  public String getAfterActionName() {
    return mAfterActionName;
  }

  /**
   * @return 0 or more test categories which this module has been associated with.
   */
  public Set<String> getTestCategories() {
    return mTestCategories;
  }
}
