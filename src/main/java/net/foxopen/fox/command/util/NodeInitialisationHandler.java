package net.foxopen.fox.command.util;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInitialisation;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * An API for initialisation of a Data DOM node from
 * a Model DOM node.
 *
 * <p>This simple interface implements the <b>Visitor</b>
 * design pattern to visit and initialise a supplied node.
 */
public interface NodeInitialisationHandler
{
   /**
    * Visit the specified Data DOM node and initialise its value.
    *
    * @param node the Data DOM node to initialise
    */
  public void initialise(ActionRequestContext pRequestContext, DOM node)
  throws ExInitialisation;

}
