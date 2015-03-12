package net.foxopen.fox.module.parsetree.evaluatedpresentationnode.behaviour;

import net.foxopen.fox.dom.paging.DOMPager;

/**
 * EvaluatedPresentationNodes may implement this interface if they are able to provide a DOM pager to consuming
 * EvaluatedNodeInfos.
 */
public interface DOMPagerBehaviour
extends EvaluatedPresentationNodeBehaviour {

  public DOMPager getDOMPagerOrNull();

}
