/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.component.explorer.popup.admin;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.util.VersionHistoryImporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.upload.UploadService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormUploadInput;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minh.dang@exoplatform.com Oct 5, 2006
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "app:/groovy/webui/component/explorer/popup/admin/UIFormWithMultiRadioBox.gtmpl", events = {
    @EventConfig(listeners = UIImportNode.ImportActionListener.class),
    @EventConfig(listeners = UIImportNode.CancelActionListener.class, phase = Phase.DECODE) })
public class UIImportNode extends UIForm implements UIPopupComponent {

  private final static Log          log                  = ExoLogger.getLogger("ecm.UIImportNode");

  public static final String FORMAT                      = "format";

  public static final String DOC_VIEW                    = "docview";

  public static final String SYS_VIEW                    = "sysview";

  public static final String FILE_UPLOAD                 = "upload";
  
  public static final String IMPORT_BEHAVIOR             = "behavior";
  
  public static final String VERSION_HISTORY_FILE_UPLOAD = "versionHistory";
  
  public static final String MAPPING_FILE                = "mapping.properties";

  public UIImportNode() throws Exception {
    this.setMultiPart(true);
    // Disabling the size limit since it makes no sense in the import case
    UIFormUploadInput uiFileUpload = new UIFormUploadInput(FILE_UPLOAD, FILE_UPLOAD, 0);
    uiFileUpload.setAutoUpload(true);
    addUIFormInput(uiFileUpload);
    addUIFormInput(new UIFormSelectBox(IMPORT_BEHAVIOR, IMPORT_BEHAVIOR, null));
    // Disabling the size limit since it makes no sense in the import case
    UIFormUploadInput uiHistoryFileUpload = 
      new UIFormUploadInput(VERSION_HISTORY_FILE_UPLOAD, VERSION_HISTORY_FILE_UPLOAD, 0);
    uiHistoryFileUpload.setAutoUpload(true);
    addUIFormInput(uiHistoryFileUpload);
    List<SelectItemOption<String>> formatItem = new ArrayList<SelectItemOption<String>>();
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle resourceBundle = context.getApplicationResourceBundle();
    formatItem.add(new SelectItemOption<String>(
        resourceBundle.getString("Import.label." + DOC_VIEW), DOC_VIEW));
    formatItem.add(new SelectItemOption<String>(
        resourceBundle.getString("Import.label." + SYS_VIEW)));
    addUIFormInput(new UIFormRadioBoxInput(FORMAT, DOC_VIEW, formatItem).setAlign(UIFormRadioBoxInput.VERTICAL_ALIGN));
  }

  public void activate() throws Exception {
    List<SelectItemOption<String>> importBehavior = new ArrayList<SelectItemOption<String>>();
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    importBehavior.add(new SelectItemOption<String>(
        res.getString("Import.Behavior.type" + 
            Integer.toString(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW)), 
        Integer.toString(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW)));
    importBehavior.add(new SelectItemOption<String>(
        res.getString("Import.Behavior.type" + 
            Integer.toString(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING)), 
        Integer.toString(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING)));
    importBehavior.add(new SelectItemOption<String>(
        res.getString("Import.Behavior.type" + 
            Integer.toString(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING)), 
        Integer.toString(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING)));
    importBehavior.add(new SelectItemOption<String>(
        res.getString("Import.Behavior.type" + 
            Integer.toString(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW)), 
        Integer.toString(ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW)));
    getUIFormSelectBox(IMPORT_BEHAVIOR).setOptions(importBehavior);
  }

  public void deActivate() throws Exception {
  }
  
  private boolean validHistoryUploadFile(Event<?> event) throws Exception {
    UIFormUploadInput inputHistory = getUIInput(VERSION_HISTORY_FILE_UPLOAD);
    UIApplication uiApp = getAncestorOfType(UIApplication.class);
    ZipInputStream zipInputStream = new ZipInputStream(inputHistory.getUploadDataAsStream());
    ZipEntry entry = zipInputStream.getNextEntry();
    while(entry != null) {
      if(entry.getName().equals(MAPPING_FILE)) {
        zipInputStream.closeEntry();
        return true;
      }
      zipInputStream.closeEntry();
      entry = zipInputStream.getNextEntry();
    }
    zipInputStream.close();
    uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.history-invalid-content", null, 
        ApplicationMessage.WARNING));
    event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
    return false;
  }
  
  private void importHistory(
      NodeImpl versionableNode, 
      InputStream versionHistoryStream, 
      String baseVersionUuid, 
      String[] predecessors, 
      String versionHistory) throws RepositoryException, IOException {
    VersionHistoryImporter versionHistoryImporter = 
      new VersionHistoryImporter(versionableNode, versionHistoryStream, baseVersionUuid, predecessors, versionHistory);
    versionHistoryImporter.doImport();
  }
  
  private Map<String, String> getMapImportHistory() throws Exception  {
    UIFormUploadInput inputHistory = getUIInput(VERSION_HISTORY_FILE_UPLOAD);
    ZipInputStream zipInputStream = new ZipInputStream(inputHistory.getUploadDataAsStream());
    ByteArrayOutputStream out= new ByteArrayOutputStream();
    byte[] data  = new byte[1024];   
    ZipEntry entry = zipInputStream.getNextEntry();
    Map<String, String> mapHistoryValue = new HashMap<String, String>();
    while(entry != null) {
      int available = -1;
      if(entry.getName().equals(MAPPING_FILE)) {
        while ((available = zipInputStream.read(data, 0, 1024)) > -1) {
          out.write(data, 0, available); 
        }
        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String strLine;
        //Read File Line By Line
        while ((strLine = br.readLine()) != null)   {
          //Put the history information into list
          if(strLine.indexOf("=") > -1) {
            mapHistoryValue.put(strLine.split("=")[0], strLine.split("=")[1]);
          }
        }
        //Close the input stream
        inputStream.close();
        zipInputStream.closeEntry();
        break;
      }
      entry = zipInputStream.getNextEntry();
    }
    out.close();
    zipInputStream.close();
    return mapHistoryValue;
  }
  
  private String getBaseVersionUUID(String valueHistory) {
    String[] arrHistoryValue = valueHistory.split(";");
    return arrHistoryValue[1];
  }
  
  private String[] getPredecessors(String valueHistory) {
    String[] arrHistoryValue = valueHistory.split(";");
    String strPredecessors = arrHistoryValue[1];
    if(strPredecessors.indexOf(",") > -1) {
      return strPredecessors.split(",");
    }
    return new String[] { strPredecessors };
  }
  
  private String getVersionHistory(String valueHistory) {
    String[] arrHistoryValue = valueHistory.split(";");
    return arrHistoryValue[0];
  }
  
  private void processImportHistory(Node currentNode) throws Exception {
    UIFormUploadInput inputHistory = getUIInput(VERSION_HISTORY_FILE_UPLOAD);
    Map<String, String> mapHistoryValue = getMapImportHistory();
    for(String uuid : mapHistoryValue.keySet()) {
      ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputHistory.getUploadDataAsStream()));
      byte[] data  = new byte[1024];   
      ByteArrayOutputStream out= new ByteArrayOutputStream();
      ZipEntry entry = zipInputStream.getNextEntry();
      while(entry != null) {
        int available = -1;
        if(entry.getName().equals(uuid + ".xml")) {
          while ((available = zipInputStream.read(data, 0, 1024)) > -1) {
            out.write(data, 0, available); 
          }
          try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());
            String value = mapHistoryValue.get(uuid);
            Node versionableNode = currentNode.getSession().getNodeByUUID(uuid);
            importHistory((NodeImpl)versionableNode, inputStream, 
                getBaseVersionUUID(value), getPredecessors(value), getVersionHistory(value));
            currentNode.getSession().save();
          } catch(ItemNotFoundException item) {
            currentNode.getSession().refresh(false);
            log.error("Can not found versionable node" + item, item);
          } catch(Exception e) {
            currentNode.getSession().refresh(false);
            log.error("Import version history failed " + e, e);
          }
          zipInputStream.closeEntry();
          entry = zipInputStream.getNextEntry();
        } else {
          zipInputStream.closeEntry();
          entry = zipInputStream.getNextEntry();
        }
      }
      out.close();
      zipInputStream.close();
    }
  }
  
  private String getMimeType(String fileName) throws IOException {
    DMSMimeTypeResolver resolver = DMSMimeTypeResolver.getInstance();
    return resolver.getMimeType(fileName);
  }

  static public class ImportActionListener extends EventListener<UIImportNode> {
    public void execute(Event<UIImportNode> event) throws Exception {
      UIImportNode uiImport = event.getSource();
      UIJCRExplorer uiExplorer = uiImport.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiImport.getAncestorOfType(UIApplication.class);
      UIFormUploadInput input = uiImport.getUIInput(FILE_UPLOAD);
      UIFormUploadInput inputHistory = uiImport.getUIInput(VERSION_HISTORY_FILE_UPLOAD);
      Node currentNode = uiExplorer.getCurrentNode();
      Session session = currentNode.getSession() ;
      String nodePath = currentNode.getPath();
      uiExplorer.addLockToken(currentNode);
      
      if (input.getUploadResource() == null) {
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.filename-invalid", null, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      }
      if(inputHistory.getUploadResource() != null) {
        String mimeTypeHistory = uiImport.getMimeType(inputHistory.getUploadResource().getFileName());
        if(!mimeTypeHistory.equals("application/zip")) {
          uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.history-invalid-type", null, 
              ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
          return;
        }
        if(!uiImport.validHistoryUploadFile(event)) return;
      }
      String mimeType = uiImport.getMimeType(input.getUploadResource().getFileName());
      InputStream xmlInputStream = null;
      if ("text/xml".equals(mimeType)) {
        xmlInputStream = new BufferedInputStream(input.getUploadDataAsStream());
      } else if ("application/zip".equals(mimeType)) {
        ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(input.getUploadDataAsStream()));
        xmlInputStream = Utils.extractFirstEntryFromZipFile(zipInputStream);
      } else {
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.mimetype-invalid", null, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      }
      try {
        int importBehavior = 
          Integer.parseInt(uiImport.getUIFormSelectBox(IMPORT_BEHAVIOR).getValue());
        //Process import
        session.importXML(nodePath, xmlInputStream, importBehavior);
        try {
          if (!uiExplorer.getPreference().isJcrEnable()) session.save();
        } catch (ConstraintViolationException e) {
          session.refresh(false);
          Object[] args = { uiExplorer.getCurrentNode().getPrimaryNodeType().getName() };
          uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.constraint-violation-exception",
                                                  args,
                                                  ApplicationMessage.WARNING));
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
          return;
        }
        
        //Process import version history
        if(inputHistory.getUploadResource() != null) {
          uiImport.processImportHistory(currentNode);
        }
          // TODO
          // if an import fails, it's possible when source xml contains errors,
          // user may fix the fail caused items and save session (JSR-170, 7.3.7 Session Import Methods).
          // Or user may decide to make a rollback - make Session.refresh(false)  
          // So, we should make rollback in case of error...
          // see Session.importXML() throws IOException, PathNotFoundException, ItemExistsException, 
          // ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException
          // otherwise ECM FileExplolrer crashes as it assume all items were imported correct.

        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.import-successful", null));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
      } catch (AccessDeniedException ace) {
        log.error("XML Import error " + ace, ace);
        session.refresh(false);
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.access-denied", null, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch (ConstraintViolationException con) {
        log.error("XML Import error " + con, con);
        session.refresh(false);
        Object[] args = { uiExplorer.getCurrentNode().getPrimaryNodeType().getName() };
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.constraint-violation-exception",
                                                args,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch (Exception ise) {
        log.error("XML Import error " + ise, ise);
        session.refresh(false);
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.filetype-error", null, 
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } finally {
        UploadService uploadService = uiImport.getApplicationComponent(UploadService.class) ;
        uploadService.removeUpload(input.getUploadId());
        uploadService.removeUpload(inputHistory.getUploadId());
        session.logout();
      }

      uiExplorer.updateAjax(event);
    }
  }

  static public class CancelActionListener extends EventListener<UIImportNode> {
    public void execute(Event<UIImportNode> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

}
