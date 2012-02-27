package org.exoplatform.ecm.webui.component.explorer.auditing;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.ext.audit.AuditHistory;
import org.exoplatform.services.jcr.ext.audit.AuditRecord;
import org.exoplatform.services.jcr.ext.audit.AuditService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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
    ListAccess<AuditRecordData> recordList = new ListAccessImpl<AuditRecordData>(AuditRecordData.class,
                                                                                 getRecords());
    LazyPageList<AuditRecordData> dataPageList = new LazyPageList<AuditRecordData>(recordList, 10);
    uiPageIterator_.setPageList(dataPageList);
  }

  public List<AuditRecordData> getRecords() throws Exception {
     List<AuditRecordData> listRec = new ArrayList<AuditRecordData>();
     Node currentNode = getCurrentNode();
     try {
      AuditService auditService = getApplicationComponent(AuditService.class);
      if(Utils.isAuditable(currentNode)){
        if (auditService.hasHistory(currentNode)){
          AuditHistory auHistory = auditService.getHistory(currentNode);
          for(AuditRecord auditRecord : auHistory.getAuditRecords()) {
            listRec.add(new AuditRecordData(auditRecord));
          }
        }
      }
    } catch(Exception e){
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
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
  
  public static class AuditRecordData {
    private String versionName_;
    private String eventType_;
    private String userId_;
    private Calendar date_;
    
    public AuditRecordData(AuditRecord auditRecord) {
      versionName_ = null;
      versionName_ = auditRecord.getVersionName();
      eventType_ = String.valueOf(auditRecord.getEventType());
      userId_ = auditRecord.getUserId();
      date_ = auditRecord.getDate();
    }

    public String getVersionName() {
      return versionName_;
    }

    public void setVersionName(String versionName) {
      versionName_ = versionName;
    }

    public String getEventType() {
      return eventType_;
    }

    public void setEventType(String eventType) {
      eventType_ = eventType;
    }

    public String getUserId() {
      return userId_;
    }

    public void setUserId(String userId) {
      userId_ = userId;
    }

    public Calendar getDate() {
      return date_;
    }

    public void setDate(Calendar date) {
      date_ = date;
    }
  }
}
