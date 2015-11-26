package net.foxopen.fox.command.builtin;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;


public class OrderCommand
extends BuiltInCommand {

  private final String BLANK_NUMBER_ALPHA_ASCEND = "blank-number-alpha-ascend";

  private DOMList byTags;
  private ArrayList byLogic = new ArrayList();

  private OrderCommand(Mod module, DOM commandElement) throws ExInternal {
    super(commandElement);

    if (commandElement.getAttrOrNull("match") == null) {
      throw new ExInternal("Error parsing \"order\" command in module \""+module.getName()+
                           "\" - must have a match attribute");
    }
    String defaultLogicStr = commandElement.getAttrOrNull("logic");
    if (defaultLogicStr == null) {
      defaultLogicStr = BLANK_NUMBER_ALPHA_ASCEND;
    }
    byTags = commandElement.getChildElements();
    for (int j=0; j<byTags.getLength(); j++) {
      DOM kid = byTags.item(j);
      if (!kid.getLocalName().equals("by")) {
        throw new ExInternal("Error parsing \"order\" command in module \""+module.getName()+
                           "\" - expected a \"fm:by\" command as the child element!");
      }
      if (kid.getAttrOrNull("key") == null) {
        throw new ExInternal("Error parsing \"order\" command in module \""+module.getName()+
                           "\" - a \"key\" attribute is expected on the \"fm:by\" command!");
      }
      String logic = kid.getAttrOrNull("logic");
      if (logic == null) {
        logic = defaultLogicStr;
      }
      byLogic.add(logic);
    }
  }

   /**
   * Runs the command with the specified user thread and session.
   *
   * @return userSession the user's session context
   */
    public XDoControlFlow run(ActionRequestContext pRequestContext) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    String match = getAttribute("match");
    if (byTags.getLength() == 0 || match == null || match.length() == 0) {
      return XDoControlFlowContinue.instance();
    }

    TreeMap sortedMap = new TreeMap(new keyComparator(byLogic));
    DOMList orderItems;
    try {
      orderItems = lContextUElem.extendedXPathUL(match, ContextUElem.ATTACH);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to evaluate match attribute for fm:order command", e);
    }

    for (int i=0; i<orderItems.getLength(); i++) {
      // generate sort keys
      DOM orderItem = orderItems.item(i);
      DOM parent = orderItem.getParentOrNull();
      if (parent == null) {
        // TODO: improve logic here
        continue;
      }
      ArrayList key = new ArrayList();
      // key: the parent node
      key.add(parent);
      // key: by statements
      for (int j=0; j<byTags.getLength(); j++) {
        DOM byTag = byTags.item(j);
        String keyXpath = byTag.getAttr("key");
        try {
          DOM keyDOM = lContextUElem.extendedXPath1E(orderItem,keyXpath,false);
          String keyValue = keyDOM.value(false);
          key.add(keyValue);
        } catch (ExActionFailed e) {
          key.add("");
        } catch (ExCardinality e) {
          key.add("");
        }
      }
      // key: the current node
      key.add(orderItem);
      // enter into a sorted tree map
      sortedMap.put(key,orderItem);
    }

    Set sortedSet = sortedMap.keySet();
    Iterator keysItr = sortedSet.iterator();
    DOM grpParent = null;
    ArrayList grpArray = new ArrayList();
    ArrayList grpDomArray = new ArrayList();
    for (int loop=0; loop<sortedSet.size(); loop++) {
      ArrayList key = (ArrayList)keysItr.next();
      DOM parent = (DOM)key.get(0);
      grpArray.add(key);
      grpDomArray.add((DOM)sortedMap.get(key));
      if (grpParent == null) {
        grpParent = parent;
      }
      if (!grpParent.equals(parent) || loop==sortedSet.size()-1) {
        // process group
        int minIndex = Integer.MAX_VALUE;
        DOM minIndexDOM = null;
        for (int count=0; count<grpArray.size(); count++) {
          ArrayList itemKey = (ArrayList)grpArray.get(count);
          DOM itemDOM = (DOM)sortedMap.get(itemKey);
          int index = itemDOM.getSiblingIndex();
          if (index < minIndex) {
            minIndex = index;
            minIndexDOM = itemDOM;
          }
        }

        DOM nextSibling = minIndexDOM;
        boolean found = false;
        while ((nextSibling = nextSibling.getNextSiblingOrNull(false))!=null) {
          if (grpDomArray.contains(nextSibling)) {
            found = true;
            break;
          }
        }
        if (!found) {
          nextSibling = null;
        }

        for (int count=0; count<grpDomArray.size(); count++) {
          DOM item = (DOM)grpDomArray.get(count);
          if (nextSibling == null) {
            item.moveToParent(grpParent);
          } else {
            item.moveToParentBefore(grpParent,nextSibling);
          }
        }

        grpParent = parent;
        grpArray = new ArrayList();
        grpDomArray = new ArrayList();
      }
    }

    return XDoControlFlowContinue.instance();
  }

  class keyComparator implements Comparator {

    ArrayList logicArray;

    public keyComparator(ArrayList pLogicArray) {
      logicArray = pLogicArray;
    }

    public int compare(Object o1, Object o2) {
      ArrayList array1 = (ArrayList)o1;
      ArrayList array2 = (ArrayList)o2;
      Object parent1 = array1.get(0);
      Object parent2 = array2.get(0);
      if (parent1.hashCode() > parent2.hashCode()) {
        return -1;
      }
      else if (parent1.hashCode() < parent2.hashCode()) {
        return 1;
      }
      else {
        for (int j=1; j<array1.size()-1; j++) {
          String a1 = (String)array1.get(j);
          String a2 = (String)array2.get(j);
          String logic = (String)logicArray.get(j-1);
          if (a1 == null) {
            if (logic.equals(BLANK_NUMBER_ALPHA_ASCEND)) {
              return -1;
            }
            else {
              return 1;
            }
          }
          else if (a2 == null) {
            if (logic.equals(BLANK_NUMBER_ALPHA_ASCEND)) {
              return 1;
            }
            else {
              return -1;
            }
          }
          else {
            int comp = a1.compareTo(a2);
            if (comp != 0) {
              if (logic.equals(BLANK_NUMBER_ALPHA_ASCEND)) {
                return comp;
              }
              else {
                if (comp<0) {
                  return 1;
                }
                else {
                  return -1;
                }
              }
            }
          }
        }
        Object element1 = array1.get(array1.size()-1);
        Object element2 = array2.get(array2.size()-1);
        if (element1.hashCode() > element2.hashCode()) {
          return -1;
        }
        else if (element1.hashCode() < element2.hashCode()) {
          return 1;
        }
        else {
          return 0;
        }
      }
    }
  }

  public boolean isCallTransition() {
   return false;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new OrderCommand(pModule, pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("order");
    }
  }
}
