package net.foxopen.fox.module.serialiser.widgets;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.fieldset.FieldSelectConfig;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public enum WidgetBuilderType {
  BUTTON(new WidgetBuilderProperties(WidgetFlag.ACTION, WidgetFlag.RUNNABLE), "button"),
  CAPTCHA(new WidgetBuilderProperties(WidgetFlag.TEXT_VALUE), "captcha", "obscure"),
  CARTOGRAPHIC(new WidgetBuilderProperties(), "cartographic"),
  CELLMATES(new WidgetBuilderProperties(WidgetFlag.INTERNAL_ONLY), "cellmates"),
  DATE(new WidgetBuilderProperties(WidgetFlag.TEXT_VALUE, WidgetFlag.RUNNABLE), "date"),
  DATE_TIME(new WidgetBuilderProperties(WidgetFlag.TEXT_VALUE, WidgetFlag.RUNNABLE), "datetime"),
  ERROR_REF(new WidgetBuilderProperties(WidgetFlag.ACTION, WidgetFlag.RUNNABLE), "error-ref"),
  FILE(new WidgetBuilderProperties(), "file", "file-new"),
  FORM(new WidgetBuilderProperties(WidgetFlag.INTERNAL_ONLY), "form"),
  HTML(new WidgetBuilderProperties(WidgetFlag.TEXT_VALUE, WidgetFlag.RUNNABLE), "html"),
  IMAGE(new WidgetBuilderProperties(WidgetFlag.RUNNABLE), "image"),
  INPUT(new WidgetBuilderProperties(WidgetFlag.TEXT_VALUE, WidgetFlag.RUNNABLE), "input", "input-resizable"),
  LINK(new WidgetBuilderProperties(WidgetFlag.ACTION, WidgetFlag.RUNNABLE), "link", "alive"),
  LIST(new WidgetBuilderProperties(WidgetFlag.INTERNAL_ONLY), "list"),
  MAILTO(new WidgetBuilderProperties(WidgetFlag.ACTION), "mailto", "mailto-btn"),
  MENU_OUT(new WidgetBuilderProperties(WidgetFlag.INTERNAL_ONLY), "menu-out"),
  PASSWORD(new WidgetBuilderProperties(WidgetFlag.TEXT_VALUE, WidgetFlag.RUNNABLE), "password"),
  PHANTOM_BUFFER(new WidgetBuilderProperties(WidgetFlag.INTERNAL_ONLY), "phantom-buffer"),
  PHANTOM_MENU(new WidgetBuilderProperties(WidgetFlag.INTERNAL_ONLY), "phantom-menu"),
  PRINT(new WidgetBuilderProperties(WidgetFlag.ACTION), "print"),
  RADIO(new WidgetBuilderProperties(WidgetFlag.RUNNABLE), new FieldSelectConfig(true, false, false), "radio"),
  SELECTOR(new WidgetBuilderProperties(WidgetFlag.RUNNABLE), new FieldSelectConfig(true, true, false), "selector"),
  SEARCH_SELECTOR(new WidgetBuilderProperties(WidgetFlag.RUNNABLE), new FieldSelectConfig(false, false, false), "search-selector"),
  STATIC_TEXT(new WidgetBuilderProperties(WidgetFlag.TEXT_VALUE), "static-text"),
  SUBMIT(new WidgetBuilderProperties(WidgetFlag.ACTION, WidgetFlag.RUNNABLE), "submit"),
  TEXT(new WidgetBuilderProperties(WidgetFlag.TEXT_VALUE), "text"),
  TICKBOX(new WidgetBuilderProperties(WidgetFlag.RUNNABLE), new FieldSelectConfig(false, false, true), "tickbox"),
  TIMER(new WidgetBuilderProperties(WidgetFlag.TEXT_VALUE, WidgetFlag.RUNNABLE), "timer"),
  UNIMPLEMENTED(new WidgetBuilderProperties(), null),
  URL(new WidgetBuilderProperties(WidgetFlag.ACTION), "url", "url-btn");

  private enum WidgetFlag {
    TEXT_VALUE,
    ACTION,
    RUNNABLE,
    INTERNAL_ONLY
  }

  private static class WidgetBuilderProperties {
    private final boolean mIsTextValue; // True if this widget needs access to underlying DOM text content
    private final boolean mIsAction; // True if this widget is only for an action and has no text content
    private final boolean mIsRunnable; // True if this widget can be runnable and post an action via click or onchange
    private final boolean mInternalOnly; // True if this widget is only usable by the engine and not an option for developers

    public WidgetBuilderProperties(WidgetFlag... pFlags) {
      List<WidgetFlag> lFlags = Arrays.asList(pFlags);
      mIsTextValue = lFlags.contains(WidgetFlag.TEXT_VALUE);
      mIsAction = lFlags.contains(WidgetFlag.ACTION);
      mIsRunnable = lFlags.contains(WidgetFlag.RUNNABLE);
      mInternalOnly = lFlags.contains(WidgetFlag.INTERNAL_ONLY);
    }

    public boolean isIsTextValue() {
      return mIsTextValue;
    }

    public boolean isIsAction() {
      return mIsAction;
    }

    public boolean isIsRunnable() {
      return mIsRunnable;
    }

    public boolean isInternalOnly() {
      return mInternalOnly;
    }
  }

  private static Map<String, WidgetBuilderType> WIDGET_LOOKUP_MAP = new HashMap<>(WidgetBuilderType.values().length);
  static {
    for (WidgetBuilderType lWidgetBuilderType : WidgetBuilderType.values()) {
      for (String lAlias : lWidgetBuilderType.mAliases) {
        WIDGET_LOOKUP_MAP.put(lAlias.toLowerCase(), lWidgetBuilderType);
      }
    }
  }

  private final WidgetBuilderProperties mWidgetBuilderProperties;
  private final FieldSelectConfig mFieldSelectConfig;
  private final Set<String> mAliases = new HashSet<>();

  private WidgetBuilderType(WidgetBuilderProperties pWidgetBuilderProperties, FieldSelectConfig pFieldSelectConfig, String... pAliases) {
    mWidgetBuilderProperties = pWidgetBuilderProperties;
    mFieldSelectConfig = pFieldSelectConfig;

    if (pAliases != null) {
      mAliases.addAll(Arrays.asList(pAliases));
    }
  }

  private WidgetBuilderType(WidgetBuilderProperties pWidgetFlags, String... pAliases) {
    this(pWidgetFlags, null, pAliases);
  }

  /**
   * Gets a WidgetBuilderType value from a widget name.
   * @param pWidgetName Name of the widget
   * @return Enum value that matched the name, or the "unimplemented" WidgetBuilderType if no match could be found.
   */
  public static WidgetBuilderType fromString(String pWidgetName, EvaluatedNode pEvalNode, boolean pTrackInfo) {
    WidgetBuilderType lWidgetType = UNIMPLEMENTED; // Default to the UNIMPLEMENTED widget builder type

    if (pWidgetName != null) {
      String lWidgetName = pWidgetName.toLowerCase();
      if (pTrackInfo) {
        //If they set a widget, start off checking for known regression issues and tracking
        if ("mailto-btn".equals(lWidgetName)) {
          Track.info("MailtoWidget", "The mailto-btn widget is no longer supported, use mailto instead: " + pEvalNode.getIdentityInformation(), TrackFlag.REGRESSION_CHANGE);
        }
        else if (lWidgetName.startsWith("obscure")) {
          Track.info("CaptchaWidget", "The obscure widget is no longer supported, use captcha instead: " + pEvalNode.getIdentityInformation(), TrackFlag.REGRESSION_CHANGE);
        }
        else if (lWidgetName.startsWith("alive")) {
          if (!XFUtil.isNull(pEvalNode.getStringAttribute(NodeAttribute.EXTERNAL_URL))) {
            Track.info("LinkWidget", "The alive widget is no longer supported, use url instead and change externalUrl to href: " + pEvalNode.getIdentityInformation(), TrackFlag.REGRESSION_CHANGE);
          }
          else {
            Track.info("LinkWidget", "The alive widget is no longer supported, use link instead: " + pEvalNode.getIdentityInformation(), TrackFlag.REGRESSION_CHANGE);
          }
        }
        else if ("input-resizable".equals(lWidgetName)) {
          Track.info("InputWidget", "The input-resizable widget is no longer supported, use input instead and add the autoResize attribute to it: " + pEvalNode.getIdentityInformation(), TrackFlag.REGRESSION_CHANGE);
        }
      }

      // If the parameter matches a widget builder type use that
      lWidgetType = XFUtil.nvl(WIDGET_LOOKUP_MAP.get(lWidgetName), lWidgetType);
    }

    if (pTrackInfo && pEvalNode.isRunnable() && !lWidgetType.isRunnable()) {
      Track.alert("PointlessActionDefined", "Action defined on '" + lWidgetType + "' widget, this won't do anything: " + pEvalNode.getIdentityInformation(), TrackFlag.BAD_MARKUP);
    }

    // Return found widget builder type or whatever it was set to intially
    return lWidgetType;
  }

  /**
   * Tests if this WidgetBuilderType represents a widget which relies on an underlying DOM node's text value.
   * @return
   */
  public boolean isTextValue() {
    return mWidgetBuilderProperties.isIsTextValue();
  }

  /**
   * Tests if this WidgetBuilderType represents a widget which is only used to run an action and does not rely on any
   * underlying data.
   * @return
   */
  public boolean isAction() {
    return mWidgetBuilderProperties.isIsAction();
  }

  /**
   * Tests if this WidgetBuilderType represents a widget which can be runnable and potentially post the page with a FOX action
   * @return
   */
  public boolean isRunnable() {
    return mWidgetBuilderProperties.isIsRunnable();
  }

  /**
   * Tests if this WidgetBuilderType represents a widget which can not be specified via a widget attribute in a module
   * @return
   */
  public boolean isInternalOnly() {
    return mWidgetBuilderProperties.isInternalOnly();
  }

  /**
   * May be null if not a selector type widget.
   * @return
   */
  public FieldSelectConfig getFieldSelectConfig() {
    return mFieldSelectConfig;
  }

  /**
   * Compare a widget type string to the WidgetBuilderType
   *
   * @param pWidgetName Name of the widget to compare to this WidgetBuilderType
   * @return true if WidgetBuilderType can be specified by pWidgetName
   */
  public boolean isMatchingWidget(String pWidgetName) {
    return mAliases.contains(pWidgetName);
  }
}
