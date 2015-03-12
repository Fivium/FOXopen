package net.foxopen.fox.dom.xpath;

import com.google.common.base.Joiner;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.enginestatus.StatusCategory;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusTable;
import net.foxopen.fox.ex.ExInternal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class XPathStatusProvider
implements StatusProvider {
  @Override
  public void refreshStatus(StatusCategory pCategory) {
    StatusTable lTable = pCategory.addTable("Cached XPaths", "XPath", "Compile Time", "Exec Count", "Avg Exec Time", "Implicated Labels", "Uses context node/doc?");
    lTable.setRowProvider(new StatusTable.RowProvider() {
      @Override
      public void generateRows(StatusTable.RowDestination pRowDestination) {
        FoxCache<String, FoxXPath> lSortedFoxList = CacheManager.getCache(BuiltInCacheDefinition.FOX_XPATH_EVALUATORS);
        List<FoxXPath> lSortedList = new ArrayList<FoxXPath>(lSortedFoxList.values());

        //TODO restore ordering functionality
        String pOrderBy = "usage";
        boolean lOrderByUsage;
        if("usage".equals(XFUtil.nvl(pOrderBy, "usage"))){
          lOrderByUsage = true;
        }
        else if("time".equals(pOrderBy)){
          lOrderByUsage = false;
        }
        else {
          throw new ExInternal("Order by supports 'usage' or 'time' only.");
        }

        Collections.sort(
          lSortedList,
          lOrderByUsage ?
          new Comparator<FoxXPath>() {
            public int compare(FoxXPath xp1, FoxXPath xp2) {
              return xp2.getUsageCount() - xp1.getUsageCount();
            }
          }
          : new Comparator<FoxXPath>() {
            public int compare(FoxXPath xp1, FoxXPath xp2) {
              return (int) ((xp2.getCumulativExecTimeMS() / xp2.getUsageCount()) - (xp1.getCumulativExecTimeMS() / xp1.getUsageCount()));
            }
          }
        );

        for(FoxXPath lXPath : lSortedList) {
          pRowDestination.addRow()
            .setColumn(lXPath.getOriginalPath())
            .setColumn(Long.toString(lXPath.getCompileTime()))
            .setColumn(Integer.toString(lXPath.getUsageCount()))
            .setColumn(Double.toString(lXPath.getUsageCount() > 0 ? lXPath.getCumulativExecTimeMS() / lXPath.getUsageCount() : 0))
            .setColumn(lXPath.getLabelSet() != null ? Joiner.on(", ").join(lXPath.getLabelSet()) : "")
            .setColumn(Boolean.toString(lXPath.usesContextItemOrDocument()));
        }
      }
    });
  }

  @Override
  public String getCategoryTitle() {
    return "XPath Info";
  }

  @Override
  public String getCategoryMnemonic() {
    return "xpath";
  }

  @Override
  public boolean isCategoryExpandedByDefault() {
    return false;
  }
}
