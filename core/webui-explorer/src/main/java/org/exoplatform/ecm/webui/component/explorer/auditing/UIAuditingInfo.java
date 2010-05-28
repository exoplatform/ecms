package org.exoplatform.ecm.webui.component.explorer.auditing;


import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.log.Log;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.ext.audit.AuditHistory;
import org.exoplatform.services.jcr.ext.audit.AuditRecord;
import org.exoplatform.services.jcr.ext.audit.AuditService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Listing of the log of auditing
 * 
 * @author CPop
 */
@ComponentConfig(
  template = "app:/groovy/webui/component/explorer/auditing/UIAuditingInfo.gtmpl",
  events = {
    @EventConfig(listeners = UIAuditingInfo.CloseActionListener.class)        
  }
)
public class UIAuditingInfo extends UIContainer implements UIPopupComponent {
  private UIPageIterator uiPageIterator_ ;
  private static final Log LOG  = ExoLogger.getLogger("explorer.UIAuditingInfo");
  public UIAuditingInfo() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, "AuditingInfoIterator");
  }

  public void activate() throws Exception { }
  public void deActivate() throws Exception { }

  public Node getCurrentNode() throws Exception { 
    return getAncestorOfType(UIJCRExplorer.class).getCurrentNode(); 
  }
  
  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }
  
  public List getListRecords() throws Exception { return uiPageIterator_.getCurrentPageData(); }
  
  @SuppressWarnings("unchecked")
  public void updateGrid() throws Exception {   
    ObjectPageList objPageList = new ObjectPageList(getRecords(), 10);
    uiPageIterator_.setPageList(objPageList);
  }
  
  public String getVersionName(AuditRecord ar) {
    String versionName;
    try {      
      versionName = ar.getVersionName();
    } catch (Exception e) {
      versionName = null;
    }
    return versionName;
  }
  
  public List<AuditRecord> getRecords() throws Exception {
     List<AuditRecord> listRec = new ArrayList<AuditRecord>();
     Node currentNode = getCurrentNode(); 
     try {
      AuditService auditService = getApplicationComponent(AuditService.class);
      if(Utils.isAuditable(currentNode)){
        if (auditService.hasHistory(currentNode)){
          AuditHistory auHistory = auditService.getHistory(currentNode);
          listRec = auHistory.getAuditRecords();
        }    
      }
    } catch(Exception e){
      LOG.error("Unexpected error", e);
    }
    return listRec;
  }
  
  static public class CloseActionListener extends EventListener<UIAuditingInfo> {
    public void execute(Event<UIAuditingInfo> event) throws Exception {
      UIAuditingInfo uiAuditingInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiAuditingInfo.getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }
}
