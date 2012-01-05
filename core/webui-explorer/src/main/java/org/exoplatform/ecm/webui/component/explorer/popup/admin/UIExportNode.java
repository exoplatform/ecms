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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.compress.CompressData;
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
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputInfo;
import org.exoplatform.webui.form.UIFormRadioBoxInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 5, 2006
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/popup/admin/UIFormWithMultiRadioBox.gtmpl",
    events = {
      @EventConfig(listeners = UIExportNode.ExportActionListener.class),
      @EventConfig(listeners = UIExportNode.ExportHistoryActionListener.class),
      @EventConfig(listeners = UIExportNode.CancelActionListener.class)
    }
)
public class UIExportNode extends UIForm implements UIPopupComponent {

  public static final String NODE_PATH         = "nodePath";

  public static final String FORMAT            = "format";

  public static final String ZIP               = "zip";

  public static final String DOC_VIEW          = "docview";

  public static final String SYS_VIEW          = "sysview";

  public static final String VERSION_SQL_QUERY = "select * from mix:versionable where jcr:path like '$0/%' "
                                                   + "order by exo:dateCreated DESC";

  public static final String ROOT_SQL_QUERY    = "select * from mix:versionable order by exo:dateCreated DESC";

  private boolean isVerionNode_ = false;

  public UIExportNode() throws Exception {
    RequestContext context = RequestContext.getCurrentInstance();
    ResourceBundle resourceBundle = context.getApplicationResourceBundle();
    List<SelectItemOption<String>> formatItem = new ArrayList<SelectItemOption<String>>() ;
    formatItem.add(new SelectItemOption<String>(
        resourceBundle.getString("Import.label." + SYS_VIEW), SYS_VIEW));
    formatItem.add(new SelectItemOption<String>(
        resourceBundle.getString("Import.label." + DOC_VIEW), DOC_VIEW));
    addUIFormInput(new UIFormInputInfo(NODE_PATH, NODE_PATH, null)) ;
    addUIFormInput(new UIFormRadioBoxInput(FORMAT, SYS_VIEW, formatItem).
                   setAlign(UIFormRadioBoxInput.VERTICAL_ALIGN)) ;
    addUIFormInput(new UIFormCheckBoxInput<Boolean>(ZIP, ZIP, null)) ;
  }

  public void update(Node node) throws Exception {
    getUIFormInputInfo(NODE_PATH).setValue(Text.unescapeIllegalJcrChars(node.getPath())) ;
  }

  public void activate() throws Exception {
    update(getAncestorOfType(UIJCRExplorer.class).getCurrentNode()) ;
  }

  public void deActivate() throws Exception { }

  public QueryResult getQueryResult(Node currentNode) throws RepositoryException {
    QueryManager queryManager = currentNode.getSession().getWorkspace().getQueryManager();
    String queryStatement = "";
    if(currentNode.getPath().equals("/")) {
      queryStatement = ROOT_SQL_QUERY;
    } else {
      queryStatement = StringUtils.replace(VERSION_SQL_QUERY,"$0",currentNode.getPath());
    }
    Query query = queryManager.createQuery(queryStatement, Query.SQL);
    return query.execute();
  }

  public String[] getActions() {
    try {
      Node currentNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
      if(currentNode.isNodeType(Utils.MIX_VERSIONABLE)) isVerionNode_ = true;
      QueryResult queryResult = getQueryResult(currentNode);
      if(queryResult.getNodes().getSize() > 0 || isVerionNode_) {
        return new String[] {"Export", "ExportHistory", "Cancel"};
      }
    } catch(Exception e) {
      return new String[] {"Export", "Cancel"};
    }
    return new String[] {"Export", "Cancel"};
  }

  private String getHistoryValue(Node node) throws Exception {
    String versionHistory = node.getProperty("jcr:versionHistory").getValue().getString();
    String baseVersion = node.getProperty("jcr:baseVersion").getValue().getString();
    Value[] predecessors = node.getProperty("jcr:predecessors").getValues();
    StringBuilder historyValue = new StringBuilder();
    StringBuilder predecessorsBuilder = new StringBuilder();
    for(Value value : predecessors) {
      if(predecessorsBuilder.length() > 0) predecessorsBuilder.append(",") ;
      predecessorsBuilder.append(value.getString());
    }
    historyValue.append(node.getUUID()).append("=").append(versionHistory).
      append(";").append(baseVersion).append(";").append(predecessorsBuilder.toString());
    return historyValue.toString();
  }

  /**
   * Create temp file to allow download a big data
   * @return file
   */
  private static File getExportedFile(String prefix, String suffix) throws IOException {
    return File.createTempFile(prefix.concat(UUID.randomUUID().toString()), suffix);
  }

  static public class ExportActionListener extends EventListener<UIExportNode> {
    public void execute(Event<UIExportNode> event) throws Exception {
      UIExportNode uiExport = event.getSource();
      UIJCRExplorer uiExplorer = uiExport.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiExport.getAncestorOfType(UIApplication.class);
      File exportedFile = UIExportNode.getExportedFile("export", ".xml");
      OutputStream out = new BufferedOutputStream(new FileOutputStream(exportedFile));
      InputStream in = new BufferedInputStream(new TempFileInputStream(exportedFile));
      CompressData zipService = new CompressData();
      DownloadService dservice = uiExport.getApplicationComponent(DownloadService.class);
      InputStreamDownloadResource dresource;
      String format = uiExport.<UIFormRadioBoxInput> getUIInput(FORMAT).getValue();
      boolean isZip = uiExport.getUIFormCheckBoxInput(ZIP).isChecked();
      Node currentNode = uiExplorer.getCurrentNode();
      Session session = currentNode.getSession() ;
      String nodePath = currentNode.getPath();
      File zipFile = null;
      try {
        if(isZip) {
          if (format.equals(DOC_VIEW))
            session.exportDocumentView(nodePath, out, false, false);
          else
            session.exportSystemView(nodePath, out, false, false);
          out.flush();
          out.close();
          zipFile = UIExportNode.getExportedFile("data", ".zip");
          out = new BufferedOutputStream(new FileOutputStream(zipFile));
          zipService.addInputStream(format + ".xml", in);
          zipService.createZip(out);
          in.close();
          exportedFile.delete();
          in = new BufferedInputStream(new TempFileInputStream(zipFile));
          dresource = new InputStreamDownloadResource(in, "application/zip");
          dresource.setDownloadName(format + ".zip");
        } else {
          if (format.equals(DOC_VIEW))
            session.exportDocumentView(nodePath, out, false, false);
          else
            session.exportSystemView(nodePath, out, false, false);
          out.flush();
          dresource = new InputStreamDownloadResource(in, "text/xml");
          dresource.setDownloadName(format + ".xml");
        }
        String downloadLink = dservice.getDownloadLink(dservice.addDownloadResource(dresource));
        event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');");
        uiExplorer.cancelAction();
      } catch (OutOfMemoryError error) {
        uiApp.addMessage(new ApplicationMessage("UIExportNode.msg.OutOfMemoryError", null, ApplicationMessage.ERROR));
        
        return;
      } finally {
        out.close();
      }
    }
  }

  static public class ExportHistoryActionListener extends EventListener<UIExportNode> {
    public void execute(Event<UIExportNode> event) throws Exception {
      UIExportNode uiExport = event.getSource() ;
      UIJCRExplorer uiExplorer = uiExport.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiExport.getAncestorOfType(UIApplication.class);
      CompressData zipService = new CompressData();
      DownloadService dservice = uiExport.getApplicationComponent(DownloadService.class) ;
      InputStreamDownloadResource dresource ;
      Node currentNode = uiExplorer.getCurrentNode();
      String sysWsName = uiExplorer.getRepository().getConfiguration().getSystemWorkspaceName();
      Session session = uiExplorer.getSessionByWorkspace(sysWsName);
      QueryResult queryResult = uiExport.getQueryResult(currentNode);
      NodeIterator queryIter = queryResult.getNodes();
      String format = uiExport.<UIFormRadioBoxInput>getUIInput(FORMAT).getValue() ;
      OutputStream out = null;
      InputStream in = null;
      List<File> lstExporedFile = new ArrayList<File>();
      File exportedFile = null;
      File zipFile = null;
      File propertiesFile = UIExportNode.getExportedFile("mapping", ".properties");
      OutputStream propertiesBOS = new BufferedOutputStream(new FileOutputStream(propertiesFile));
      InputStream propertiesBIS = new BufferedInputStream(new TempFileInputStream(propertiesFile));
      try {
        while(queryIter.hasNext()) {
          exportedFile = UIExportNode.getExportedFile("data", ".xml");
          lstExporedFile.add(exportedFile);
          out = new BufferedOutputStream(new FileOutputStream(exportedFile));
          in = new BufferedInputStream(new TempFileInputStream(exportedFile));
          Node node = queryIter.nextNode();
          String historyValue = uiExport.getHistoryValue(node);
          propertiesBOS.write(historyValue.getBytes());
          propertiesBOS.write('\n');
        if(format.equals(DOC_VIEW))
              session.exportDocumentView(node.getVersionHistory().getPath(), out, false, false );
        else
          session.exportSystemView(node.getVersionHistory().getPath(), out, false, false );
          out.flush();
          zipService.addInputStream(node.getUUID() + ".xml", in);
        }
        if(currentNode.isNodeType(Utils.MIX_VERSIONABLE)) {
          exportedFile = UIExportNode.getExportedFile("data", ".xml");
          lstExporedFile.add(exportedFile);
          out = new BufferedOutputStream(new FileOutputStream(exportedFile));
          in = new BufferedInputStream(new TempFileInputStream(exportedFile));
          String historyValue = uiExport.getHistoryValue(currentNode);
          propertiesBOS.write(historyValue.getBytes());
          propertiesBOS.write('\n');
          if (format.equals(DOC_VIEW))
            session.exportDocumentView(currentNode.getVersionHistory().getPath(), out, false, false);
          else
            session.exportSystemView(currentNode.getVersionHistory().getPath(), out, false, false);
          out.flush();
          zipService.addInputStream(currentNode.getUUID() + ".xml",in);
        }
        propertiesBOS.flush();
        zipService.addInputStream("mapping.properties", propertiesBIS);
        zipFile = UIExportNode.getExportedFile("data", "zip");
        in = new BufferedInputStream(new TempFileInputStream(zipFile));
        out = new BufferedOutputStream(new FileOutputStream(zipFile));
        out.flush();
        zipService.createZip(out);
        dresource = new InputStreamDownloadResource(in, "application/zip") ;
        dresource.setDownloadName(format + "_versionHistory.zip");
        String downloadLink = dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
        event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');");
      } catch (OutOfMemoryError error) {
        uiApp.addMessage(new ApplicationMessage("UIExportNode.msg.OutOfMemoryError", null, ApplicationMessage.ERROR));
        
        return;
      } finally {
        propertiesBOS.close();
        propertiesBIS.close();
        if (out != null) {
          out.close();
        }
      }
    }
  }

  static public class CancelActionListener extends EventListener<UIExportNode> {
    public void execute(Event<UIExportNode> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class) ;
      uiExplorer.cancelAction() ;
    }
  }

  private static class TempFileInputStream extends FileInputStream {

    private final File file;

    public TempFileInputStream(File file) throws FileNotFoundException {
      super(file);
      this.file = file;
      try {
        file.deleteOnExit();
      } catch (Exception e) {
        // ignore me
      }
    }

    @Override
    protected void finalize() throws IOException {
      try {
        file.delete();
      } catch (Exception e) {
        // ignore me
      }
      super.finalize();
    }
  }
}
