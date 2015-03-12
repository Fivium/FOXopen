package net.foxopen.fox.dom.handler;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.thread.stack.ModuleStateChangeListener;

public class TempDOMHandler
implements DOMHandler, ModuleStateChangeListener {

  private final DOM mTempDOM;

  public static TempDOMHandler createTempDOMHandler(StatefulXThread pThread) {
    TempDOMHandler lDOMHandler = new TempDOMHandler();
    pThread.getModuleCallStack().registerStateChangeListener(lDOMHandler);
    return lDOMHandler;
  }

  private TempDOMHandler() {
    mTempDOM = DOM.createDocument(ContextLabel.TEMP.asString());
    mTempDOM.getDocControl().setDocumentReadWriteAutoIds();
  }

  @Override
  public void open(ActionRequestContext pRequestContext) {
  }

  @Override
  public DOM getDOM() {
    return mTempDOM;
  }

  @Override
  public void close(ActionRequestContext pRequestContext) {
  }

  @Override
  public boolean isTransient() {
    return false;
  }

  @Override
  public String getContextLabel() {
    return ContextLabel.TEMP.asString();
  }

  public void flushDOM(){
    mTempDOM.removeAllChildren();
  }

  @Override
  public void handleStateChange(RequestContext pRequestContext, ModuleStateChangeListener.EventType pEventType, ModuleCallStack pCallStack) {
    if(pEventType == ModuleStateChangeListener.EventType.MODULE) {
      //Clear the temp DOM in the event of a module push/pop
      flushDOM();
    }
  }

  @Override
  public int getLoadPrecedence() {
    return LOAD_PRECEDENCE_HIGH;
  }
}
