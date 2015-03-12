package net.foxopen.fox.command.util;

import java.text.DecimalFormat;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.xpath.ContextualityLevel;


/**
 * Shared logic for running a for-each loop, including setting up the status item etc. Used by for-each command and for-each
 * in setout.
 *
 * There are two types of for loop: DOM loops and number loops.
 */
public class ForEachIterator {

  private final boolean mIsDOMLoop;

  private final String mItemContextName;
  private final String mStatusContextName;

  private final Double mNumRangeFrom;
  private final Double mNumRangeTo;
  private final Double mNumRangeStep;

  public static interface IterationExecutable {
    /**
     * Does any required processing within a loop iteration.
     * @param pOptionalCurrentItem If this is a DOM loop, the iterator will provide the current item as this argument.
     * Otherwise the argument will be null.
     * @return True if the loop should continue to the next iteration, or false if the loop should be aborted.
     */
    boolean execute(DOM pOptionalCurrentItem, ForEachIterator.Status pIteratorStatus);
  }

  /**
   * Leave double arguments null to use defaults.
   * @param pIsDOMLoop
   * @param pItemContextName
   * @param pStatusContextName
   * @param pNumRangeFrom
   * @param pNumRangeTo
   * @param pNumRangeStep
   */
  public ForEachIterator(boolean pIsDOMLoop, String pItemContextName, String pStatusContextName, Double pNumRangeFrom, Double pNumRangeTo, Double pNumRangeStep) {
    mIsDOMLoop = pIsDOMLoop;
    mItemContextName = pItemContextName;
    mStatusContextName = pStatusContextName;
    mNumRangeFrom = pNumRangeFrom;
    mNumRangeTo = pNumRangeTo;
    mNumRangeStep = pNumRangeStep;
  }

  /**
   *
   * @param pContextUElem
   * @param pIterationDOMList Can be null if this is not a DOM loop.
   * @param pIterationExecutable
   */
  public void doForEach(ContextUElem pContextUElem, DOMList pIterationDOMList, IterationExecutable pIterationExecutable) {

    if(pIterationDOMList == null) {
      pIterationDOMList = new DOMList();
    }

    Status lIteratorStatus = new Status();
    DOM statusDOM = DOM.createDocument("iterator-status");
    pContextUElem.localise("fm:for-each/" + mItemContextName);
    try {
      pContextUElem.setUElem(mStatusContextName, ContextualityLevel.LOCALISED, statusDOM);

      lIteratorStatus.begin       = XFUtil.nvl(mNumRangeFrom, 0d);
      lIteratorStatus.end         = XFUtil.nvl(mNumRangeTo, (double) pIterationDOMList.getLength()-1);
      lIteratorStatus.step        = XFUtil.nvl(mNumRangeStep, 1d);
      lIteratorStatus.index       = 0;
      lIteratorStatus.count       = 1;
//      lIteratorStatus.currentItem = (pIterationDOMList.getLength() > 0 ? pIterationDOMList.item(0) : null);
      lIteratorStatus.currentStep = lIteratorStatus.begin;
      lIteratorStatus.isFirst     = true;
      lIteratorStatus.isLast      = (lIteratorStatus.currentStep == lIteratorStatus.end);

      if (lIteratorStatus.end < lIteratorStatus.begin && !mIsDOMLoop) {
        // Range model: May need to swap range bounds do that end >= begin?
        double temp = lIteratorStatus.begin;
        lIteratorStatus.begin = lIteratorStatus.end;
        lIteratorStatus.end   = temp;
      }

      int numberOfIterations = (mIsDOMLoop ? pIterationDOMList.getLength() : Math.abs((int) ((lIteratorStatus.end - lIteratorStatus.begin) / lIteratorStatus.step))+1);

      boolean lContinueAllowed = true;
      for (int n=0; n < numberOfIterations && lContinueAllowed; n++) {

        updateStatusDOMFromIteratorStatus(statusDOM, lIteratorStatus);

        DOM lCurrentItem = null;
        if (pIterationDOMList.getLength() > n) {
          lCurrentItem = pIterationDOMList.item(n);
          pContextUElem.setUElem(mItemContextName, ContextualityLevel.LOCALISED, lCurrentItem);
        }

        lContinueAllowed = pIterationExecutable.execute(lCurrentItem, lIteratorStatus);

        lIteratorStatus.index++;
        lIteratorStatus.count++;
//        lIteratorStatus.currentItem = (pIterationDOMList.getLength() > n ? pIterationDOMList.item(n) : null);
        lIteratorStatus.currentStep += lIteratorStatus.step;
        lIteratorStatus.isFirst = false;
        lIteratorStatus.isLast = (lIteratorStatus.currentStep == lIteratorStatus.end);
      }
    }
    finally {
      pContextUElem.delocalise("fm:for-each/" + mItemContextName);
    }
  }


  private static void updateStatusDOMFromIteratorStatus(DOM statusDOM, Status status) {
    final DecimalFormat df = new DecimalFormat("#.##");
    statusDOM.getCreate1ENoCardinalityEx("index").setText(Integer.toString(status.index));
    statusDOM.getCreate1ENoCardinalityEx("count").setText(Integer.toString(status.count));
    statusDOM.getCreate1ENoCardinalityEx("currentStep").setText(df.format(status.currentStep));
    statusDOM.getCreate1ENoCardinalityEx("isFirst").setText(Boolean.valueOf(status.isFirst).toString());
    statusDOM.getCreate1ENoCardinalityEx("isLast").setText(Boolean.valueOf(status.isLast).toString());
    statusDOM.getCreate1ENoCardinalityEx("begin").setText(df.format(status.begin));
    statusDOM.getCreate1ENoCardinalityEx("end").setText(df.format(status.end));
    statusDOM.getCreate1ENoCardinalityEx("step").setText(df.format(status.step));
  }


  public class Status {
//    DOM     currentItem;
    private int     index;
    private int     count;
    private boolean isFirst;
    private boolean isLast;
    private double  begin;
    private double  end;
    private double  step;
    private double  currentStep;

    public int getIndex() {
      return index;
    }

    public int getCount() {
      return count;
    }

    public boolean isIsFirst() {
      return isFirst;
    }

    public boolean isIsLast() {
      return isLast;
    }

    public double getBegin() {
      return begin;
    }

    public double getEnd() {
      return end;
    }

    public double getStep() {
      return step;
    }

    public double getCurrentStep() {
      return currentStep;
    }
  }
}
