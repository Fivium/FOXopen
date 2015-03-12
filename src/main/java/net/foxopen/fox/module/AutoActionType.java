package net.foxopen.fox.module;


import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import java.util.Comparator;

import net.foxopen.fox.ex.ExModule;


/**
 * Types of auto action which can be defined in a FOX module. All auto action names should start with the string "auto-"
 * followed by one of the values in this Enum.
 */
public enum AutoActionType {
  
  ACTION_INIT("action-init"),
  ACTION_FINAL("action-final"),
  STATE_INIT("state-init"),
  STATE_FINAL("state-final"),
  CALLBACK_INIT("callback-init"),
  CALLBACK_FINAL("callback-final");
  
  private static final String AUTO_PREFIX = "auto-";
  
  private final String mNamePrefix;
  
  public static AutoActionType getTypeFromActionName(String pActionName)
  throws ExModule {
    
    if (!pActionName.startsWith(AUTO_PREFIX)) {
      return null;
    }
    else {
      AutoActionType lFoundType = null;      
      for(AutoActionType lType : values()) {
        if(pActionName.startsWith(lType.mNamePrefix, AUTO_PREFIX.length())) {
          lFoundType = lType;
          break;
        }
      }
      
      if(lFoundType == null) {
        throw new ExModule("Invalid auto-action name: " + pActionName + " - needs to start with [auto-action-init,auto-action-final,auto-state-init,auto-state-final]");
      }
      else {
        return lFoundType;
      }
    }
  }
  
  /**
   * Creates a Multimap for the storage of auto actions. Legacy engine behaviour was to sort the auto actions by action name;
   * this behaviour is currently preserved by providing a custom value comparator to the Multimap.
   * @return
   */
  public static Multimap<AutoActionType, ActionDefinition> createAutoActionMultimap() {
    return TreeMultimap.create(Ordering.natural(),  
      new Comparator<ActionDefinition>() {
        @Override
        public int compare(ActionDefinition pO1, ActionDefinition pO2) {
          return pO1.getActionName().compareTo(pO2.getActionName());
        }
      });
  }
  
  /**
   * Clones an existing Multimap of auto actions, using the correct implementation for action order preservation.
   * @param pExistingMultimap
   * @return
   */
  public static Multimap<AutoActionType, ActionDefinition> cloneAutoActionMultimap(Multimap<AutoActionType, ActionDefinition> pExistingMultimap) {
    Multimap<AutoActionType, ActionDefinition> lNewMultimap = createAutoActionMultimap();
    lNewMultimap.putAll(pExistingMultimap);
    return lNewMultimap;
  }
  
  private AutoActionType(String pNamePrefix) {
    mNamePrefix = pNamePrefix;    
  }
}
