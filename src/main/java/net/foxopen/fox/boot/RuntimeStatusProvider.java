package net.foxopen.fox.boot;

import com.google.common.base.Splitter;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.StatusCollection;
import net.foxopen.fox.enginestatus.StatusDestination;
import net.foxopen.fox.enginestatus.StatusDetail;
import net.foxopen.fox.enginestatus.StatusItem;
import net.foxopen.fox.enginestatus.StatusMessage;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusText;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.util.SizeUtil;

import javax.servlet.ServletContext;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class RuntimeStatusProvider
implements StatusProvider {

  private final Date mUpSince = new Date();

  @Override
  public void refreshStatus(StatusDestination pDestination) {

    pDestination.addDetailMessage("Servlet Info", new ServletInfoDetailProvider());
    pDestination.addDetailMessage("System Info", new SystemInfoDetailProvider());
    pDestination.addDetailMessage("JVM Arguments", new JVMArgDetailProvider());
    pDestination.addDetailMessage("GC Info", new GCDetailProvider());
    pDestination.addMessage("Servlet last reloaded", EngineStatus.formatDate(mUpSince));
    pDestination.addMessage("Server version", FoxGlobals.getInstance().getServletContext().getServerInfo());

  }

  private static class ServletInfoDetailProvider
  implements StatusDetail.Provider {
    @Override
    public StatusItem getDetailMessage() {
      ServletContext lServletContext = FoxGlobals.getInstance().getServletContext();

      Collection<Map.Entry<String, Object>> lEntries = new ArrayList<>();
      Enumeration<String> lAttributeNames = lServletContext.getAttributeNames();

      while(lAttributeNames.hasMoreElements()) {

        final String lKey = lAttributeNames.nextElement();
        final Object lValue = lServletContext.getAttribute(lKey);

        lEntries.add(new Map.Entry<String, Object>() {
          @Override
          public String getKey() {
            return lKey;
          }

          @Override
          public Object getValue() {
            return XFUtil.nvl(lValue, "").toString();
          }

          @Override
          public Object setValue(Object value) {
            return "";
          }
        });
      }

      return getStatusItem("servletInfo", lEntries);
    }
  }

  private static class SystemInfoDetailProvider
  implements StatusDetail.Provider {
    @Override
    public StatusItem getDetailMessage() {
      Properties systemProps = System.getProperties();
      Map propsMap = new TreeMap<>(systemProps);
      return getStatusItem("sytemInfo", propsMap.entrySet());
    }
  }

  private static StatusItem getStatusItem(String pCollectionName, Collection<Map.Entry<String, Object>> pEntries) {

    StatusCollection pStatusCollection = new StatusCollection(pCollectionName);

    for(Map.Entry<String, Object> lEntry : pEntries) {
      String lKey = lEntry.getKey();
      String lValue = lEntry.getValue() == null ? null : lEntry.getValue().toString();
      // If the string contains ";" or "," then break it up into a sub-list
      if (lValue != null && (lValue.indexOf(';') != -1 || lValue.indexOf(',') != -1)) {
        StatusCollection lNestedCollection = StatusCollection.fromStringSet("nested", Arrays.asList(lValue.split("([;,])")));
        pStatusCollection.addItem(new StatusMessage(lKey, lNestedCollection));
      }
      else {
        pStatusCollection.addItem(new StatusMessage(lKey, XFUtil.nvl(lValue)));
      }
    }
    return pStatusCollection;
  }

  private static class JVMArgDetailProvider
  implements StatusDetail.Provider {
    @Override
    public StatusItem getDetailMessage() {
      StatusCollection lStatusCollection = new StatusCollection("jvmArgs");
      List<String> lArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

      for (String lArg : lArguments) {
        if(lArg.contains("=")) {
          Iterator<String> lSplit = Splitter.on("=").limit(2).split(lArg).iterator();
          String lArgName = lSplit.next();
          String lArgVal = lSplit.next();
          lStatusCollection.addItem(new StatusMessage(lArgName, lArgVal));
        }
        else {
          lStatusCollection.addItem(new StatusText(lArg));
        }
      }

      return lStatusCollection;
    }
  }

  private static class GCDetailProvider
  implements StatusDetail.Provider {
    @Override
    public StatusItem getDetailMessage() {
      StatusCollection lStatusCollection = new StatusCollection("gcStats");
      for(GarbageCollectorMXBean lBean :ManagementFactory.getGarbageCollectorMXBeans()) {
        StatusCollection lBeanStats = new StatusCollection(lBean.getName());
        lBeanStats.addItem(new StatusMessage("Collection count", Long.toString(lBean.getCollectionCount())));
        lBeanStats.addItem(new StatusMessage("Collection time MS", Long.toString(lBean.getCollectionTime())));
        lStatusCollection.addItem((new StatusMessage("Collector " + lBean.getName(), lBeanStats)));
      }

      lStatusCollection.addItem(new StatusMessage("Max memory", SizeUtil.getBytesSpecificationDescription(Runtime.getRuntime().maxMemory())));
      lStatusCollection.addItem(new StatusMessage("Total memory", SizeUtil.getBytesSpecificationDescription(Runtime.getRuntime().totalMemory())));
      lStatusCollection.addItem(new StatusMessage("Free memory", SizeUtil.getBytesSpecificationDescription(Runtime.getRuntime().freeMemory())));

      return lStatusCollection;
    }
  }

  @Override
  public String getCategoryTitle() {
    return "Runtime";
  }

  @Override
  public String getCategoryMnemonic() {
    return "runtime";
  }

  @Override
  public boolean isCategoryExpandedByDefault() {
    return true;
  }
}
