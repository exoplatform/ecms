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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.impl.storage.JCRItemExistsException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.input.UIUploadInput;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minh.dang@exoplatform.com Oct 5, 2006
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "app:/groovy/webui/component/explorer/popup/admin/UIFormWithMultiRadioBox.gtmpl",
                 events = {
    @EventConfig(listeners = UIImportNode.ImportActionListener.class),
    @EventConfig(listeners = UIImportNode.CancelActionListener.class, phase = Phase.DECODE) })
public class UIImportNode extends UIForm implements UIPopupComponent {
  private static final Log LOG  = ExoLogger.getLogger(UIImportNode.class.getName());

  public static final String FORMAT                      = "format";

  public static final String FILE_UPLOAD                 = "upload";

  public static final String IMPORT_BEHAVIOR             = "behavior";

  public static final String VERSION_HISTORY_FILE_UPLOAD = "versionHistory";

  public static final String MAPPING_FILE                = "mapping.properties";

  public UIImportNode() throws Exception {
    this.setMultiPart(true);
    // Disabling the size limit since it makes no sense in the import case
    UIUploadInput uiFileUpload = new UIUploadInput(FILE_UPLOAD, FILE_UPLOAD, 1, 0);
    addUIFormInput(uiFileUpload);
    addUIFormInput(new UIFormSelectBox(IMPORT_BEHAVIOR, IMPORT_BEHAVIOR, null));
    // Disabling the size limit since it makes no sense in the import case
    UIUploadInput uiHistoryFileUpload =
      new UIUploadInput(VERSION_HISTORY_FILE_UPLOAD, VERSION_HISTORY_FILE_UPLOAD, 1, 0);
    addUIFormInput(uiHistoryFileUpload);
  }

  public void activate() {
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

  public void deActivate() {
  }

  private boolean validHistoryUploadFile(Event<?> event) throws Exception {
    UIUploadInput inputHistory = getUIInput(VERSION_HISTORY_FILE_UPLOAD);
    UIApplication uiApp = getAncestorOfType(UIApplication.class);
    ZipInputStream zipInputStream =
        new ZipInputStream(inputHistory.getUploadDataAsStream(inputHistory.getUploadIds()[0]));
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

    return false;
  }

  private String getMimeType(String fileName) throws Exception {
    DMSMimeTypeResolver resolver = DMSMimeTypeResolver.getInstance();
    return resolver.getMimeType(fileName);
  }

  static public class ImportActionListener extends EventListener<UIImportNode> {
    public void execute(Event<UIImportNode> event) throws Exception {
      UIImportNode uiImport = event.getSource();
      UIJCRExplorer uiExplorer = uiImport.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiImport.getAncestorOfType(UIApplication.class);
      UIUploadInput input = uiImport.getUIInput(FILE_UPLOAD);
      UIUploadInput inputHistory = uiImport.getUIInput(VERSION_HISTORY_FILE_UPLOAD);
      Node currentNode = uiExplorer.getCurrentNode();
      Session session = currentNode.getSession() ;
      String nodePath = currentNode.getPath();
      uiExplorer.addLockToken(currentNode);
      String inputUploadId = input.getUploadIds()[0];
      String inputHistoryUploadId = inputHistory.getUploadIds()[0];

      if (input.getUploadResource(inputUploadId) == null) {
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.filename-invalid", null,
            ApplicationMessage.WARNING));

        return;
      }
      if(inputHistory.getUploadResource(inputHistoryUploadId) != null) {
        String mimeTypeHistory = uiImport.getMimeType(inputHistory.getUploadResource(inputHistoryUploadId).getFileName());
        if(!mimeTypeHistory.equals("application/zip")) {
          uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.history-invalid-type", null,
              ApplicationMessage.WARNING));

          return;
        }
        if(!uiImport.validHistoryUploadFile(event)) return;
      }
      String mimeType = uiImport.getMimeType(input.getUploadResource(inputUploadId).getFileName());
      InputStream xmlInputStream = null;
      if ("text/xml".equals(mimeType)) {
        xmlInputStream = new BufferedInputStream(input.getUploadDataAsStream(inputUploadId));
      } else if ("application/zip".equals(mimeType)) {
        ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(input.getUploadDataAsStream(inputUploadId)));
        xmlInputStream = Utils.extractFirstEntryFromZipFile(zipInputStream);
      } else {
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.mimetype-invalid", null,
            ApplicationMessage.WARNING));

        return;
      }
      try {
        int importBehavior =
          Integer.parseInt(uiImport.getUIFormSelectBox(IMPORT_BEHAVIOR).getValue());
        //Process import
        session.importXML(nodePath, xmlInputStream, importBehavior);
        try {
          session.save();
        } catch (ConstraintViolationException e) {
          session.refresh(false);
          Object[] args = { uiExplorer.getCurrentNode().getPrimaryNodeType().getName() };
          uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.constraint-violation-exception",
                                                  args,
                                                  ApplicationMessage.WARNING));

          return;
        }

        //Process import version history
        if(inputHistory.getUploadResource(inputHistoryUploadId) != null) {
          Map<String, String> mapHistoryValue =
            org.exoplatform.services.cms.impl.Utils.getMapImportHistory(inputHistory.getUploadDataAsStream(inputHistoryUploadId));
          org.exoplatform.services.cms.impl.Utils.processImportHistory(
              currentNode, inputHistory.getUploadDataAsStream(inputHistoryUploadId), mapHistoryValue);
        }
          // if an import fails, it's possible when source xml contains errors,
          // user may fix the fail caused items and save session (JSR-170, 7.3.7 Session Import Methods).
          // Or user may decide to make a rollback - make Session.refresh(false)
          // So, we should make rollback in case of error...
          // see Session.importXML() throws IOException, PathNotFoundException, ItemExistsException,
          // ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException
          // otherwise ECM FileExplolrer crashes as it assume all items were imported correct.

        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.import-successful", null));

      } catch (AccessDeniedException ace) {
        session.refresh(false);
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.access-denied", null,
            ApplicationMessage.WARNING));

        return;
      } catch (ConstraintViolationException con) {
        session.refresh(false);
        Object[] args = { uiExplorer.getCurrentNode().getPrimaryNodeType().getName() };
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.constraint-violation-exception",
                                                args,
                                                ApplicationMessage.WARNING));

        return;
      } catch (JCRItemExistsException iee) {
        session.refresh(false);
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.item-exists-exception",
                                                new Object[] { iee.getIdentifier() },
                                                ApplicationMessage.WARNING));

        return;
      } catch (InvalidSerializedDataException isde) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", isde);
        }
        session.refresh(false);
        String msg = isde.getMessage();
        String position = "";
        if (msg != null && msg.indexOf("[") > 0 && msg.indexOf("]") > 0) {
          position = msg.substring(msg.lastIndexOf("["), msg.lastIndexOf("]")+1);
        }
        String fileName = input.getUploadResource(inputUploadId).getFileName();
        Object [] args = new Object[] {position, fileName};
        ApplicationMessage appMsg = new ApplicationMessage("UIImportNode.msg.xml-invalid", args, 
          ApplicationMessage.WARNING);
        appMsg.setArgsLocalized(false);
        uiApp.addMessage(appMsg);
        return;
      } catch (Exception ise) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", ise);
        }
        session.refresh(false);
        uiApp.addMessage(new ApplicationMessage("UIImportNode.msg.filetype-error", null,
            ApplicationMessage.WARNING));

        return;
      } finally {
        UploadService uploadService = uiImport.getApplicationComponent(UploadService.class) ;
        uploadService.removeUploadResource(inputUploadId);
        uploadService.removeUploadResource(inputHistoryUploadId);
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
