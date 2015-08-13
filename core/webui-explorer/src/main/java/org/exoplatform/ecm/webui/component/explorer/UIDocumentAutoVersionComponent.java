package org.exoplatform.ecm.webui.component.explorer;

import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.cms.documents.impl.AutoVersionServiceImpl;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import java.util.regex.Matcher;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 7/27/15
 * Build popup document auto versioning
 */
@ComponentConfig(
        template = "classpath:groovy/ecm/webui/UIConfirmMessage.gtmpl",
        events = {
                @EventConfig(listeners = UIDocumentAutoVersionComponent.KeepBothActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionComponent.CreateNewVersionActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionComponent.ReplaceActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionComponent.CreateVersionOrReplaceActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionComponent.CancelActionListener.class)
        }
)
public class UIDocumentAutoVersionComponent extends UIContainer implements UIPopupComponent {

  private static final Log    LOG           = ExoLogger.getLogger(UIDocumentAutoVersionComponent.class.getName());
  public static final String KEEP_BOTH = "KeepBoth";
  public static final String CREATE_VERSION = "CreateNewVersion";
  public static final String REPLACE = "Replace";
  public static final String CREATE_OR_REPLACE = "CreateVersionOrReplace";
  public static final String CANCEL = "Cancel";

  private boolean isVersioned = false;
  private String sourcePath;
  private String destPath;
  private String sourceWorkspace;
  private String destWorkspace;
  private String messageKey_;
  private String[] args_ = {};
  private static String[] actions = new String[] {KEEP_BOTH, CREATE_VERSION, REPLACE, CREATE_OR_REPLACE ,CANCEL};

  @Override
  public void activate() { }

  @Override
  public void deActivate() {

  }

  public void init(Node currentNode) throws Exception{
    if(currentNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE)){
      setActions(new String[]{KEEP_BOTH, CREATE_VERSION, CANCEL});
    }else{
      setActions(new String[]{KEEP_BOTH, REPLACE, CANCEL});
    }
  }

  public String[] getActions() { return actions; }

  public static class KeepBothActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIDocumentAutoVersionComponent autoVersionComponent = event.getSource();
      UIJCRExplorer uijcrExplorer = autoVersionComponent.getAncestorOfType(UIJCRExplorer.class);
      Session destSession = uijcrExplorer.getSessionByWorkspace(autoVersionComponent.getDestWorkspace());
      Session srcSession = uijcrExplorer.getSessionByWorkspace(autoVersionComponent.getSourceWorkspace());
      Node sourceNode = uijcrExplorer.getNodeByPath(autoVersionComponent.getSourcePath(), srcSession);
      String destPath = autoVersionComponent.getDestPath();
      if (destPath != null) {
        Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destPath);
        if (matcher.find()) {
          destPath = matcher.group(2);
        } else {
          throw new IllegalArgumentException("The ObjectId is invalid '" + destPath + "'");
        }
      }
      if (!"/".equals(destPath)) destPath = destPath.concat("/");
      copyNode(destSession, autoVersionComponent.getSourceWorkspace(),
              autoVersionComponent.getSourcePath(), destPath.concat(sourceNode.getName()));

      UIPopupWindow popupAction = event.getSource().getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      uijcrExplorer.updateAjax(event);
    }
  }

  public static class CreateNewVersionActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIDocumentAutoVersionComponent autoVersionComponent = event.getSource();
      UIJCRExplorer uijcrExplorer = autoVersionComponent.getAncestorOfType(UIJCRExplorer.class);

      Session destSession = uijcrExplorer.getSessionByWorkspace(autoVersionComponent.getDestWorkspace());
      Session srcSession = uijcrExplorer.getSessionByWorkspace(autoVersionComponent.getSourceWorkspace());
      Node sourceNode = uijcrExplorer.getNodeByPath(autoVersionComponent.getSourcePath(), srcSession);
      String destPath = autoVersionComponent.getDestPath();
      if (destPath != null) {
        Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destPath);
        if (matcher.find()) {
          destPath = matcher.group(2);
        } else {
          throw new IllegalArgumentException("The ObjectId is invalid '" + destPath + "'");
        }
      }
      if (!"/".equals(destPath)) destPath = destPath.concat("/");
      destPath = destPath.concat(sourceNode.getName());

      Node destNode = (Node)destSession.getItem(destPath);
      AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
      autoVersionService.autoVersion(destNode, sourceNode);

      UIPopupWindow popupAction = autoVersionComponent.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      uijcrExplorer.updateAjax(event);
    }
  }


  public static class ReplaceActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIDocumentAutoVersionComponent autoVersionComponent = event.getSource();
      UIJCRExplorer uijcrExplorer = autoVersionComponent.getAncestorOfType(UIJCRExplorer.class);

      Session destSession = uijcrExplorer.getSessionByWorkspace(autoVersionComponent.getDestWorkspace());
      Session srcSession = uijcrExplorer.getSessionByWorkspace(autoVersionComponent.getSourceWorkspace());
      Node sourceNode = uijcrExplorer.getNodeByPath(autoVersionComponent.getSourcePath(), srcSession);
      String destPath = autoVersionComponent.getDestPath();
      if (destPath != null) {
        Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destPath);
        if (matcher.find()) {
          destPath = matcher.group(2);
        } else {
          throw new IllegalArgumentException("The ObjectId is invalid '" + destPath + "'");
        }
      }
      if (!"/".equals(destPath)) destPath = destPath.concat("/");
      destPath = destPath.concat(sourceNode.getName());

      Node destNode = (Node)destSession.getItem(destPath);
      destNode.remove();
      destNode.getSession().save();

      copyNode(destSession, autoVersionComponent.getSourceWorkspace(),
              autoVersionComponent.getSourcePath(), destPath);

      UIPopupWindow popupAction = autoVersionComponent.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      uijcrExplorer.updateAjax(event);
    }
  }

  public static class CreateVersionOrReplaceActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIDocumentAutoVersionComponent uiConfirm = event.getSource();
      UIPopupWindow popupAction = uiConfirm.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  public static class CancelActionListener extends EventListener<UIDocumentAutoVersionComponent> {
    @Override
    public void execute(Event<UIDocumentAutoVersionComponent> event) throws Exception {
      UIPopupWindow popupAction = event.getSource().getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(false) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  public String getSourcePath() {
    return sourcePath;
  }

  public void setSourcePath(String sourcePath) {
    this.sourcePath = sourcePath;
  }

  public String getDestPath() {
    return destPath;
  }

  public void setDestPath(String destPath) {
    this.destPath = destPath;
  }

  public String getSourceWorkspace() {
    return sourceWorkspace;
  }

  public void setSourceWorkspace(String sourceWorkspace) {
    this.sourceWorkspace = sourceWorkspace;
  }

  public String getDestWorkspace() {
    return destWorkspace;
  }

  public void setDestWorkspace(String destWorkspace) {
    this.destWorkspace = destWorkspace;
  }

  public void setMessageKey(String messageKey) { messageKey_ = messageKey; }

  public String getMessageKey() { return messageKey_; }

  public void setArguments(String[] args) { args_ = args; }

  public String[] getArguments() { return args_; }

  public boolean isVersioned() {
    return isVersioned;
  }

  public void setVersioned(boolean isVersioned) {
    this.isVersioned = isVersioned;
  }

  private void setActions(String[] actions) {
    this.actions = actions;
  }

  /**
   * Copy node using workspace
   * @param session session of dest node
   * @param srcWorkspaceName source
   * @param srcPath
   * @param destPath
   * @throws Exception
   */
  private static void copyNode(Session session, String srcWorkspaceName, String srcPath,
                              String destPath) throws Exception {
    Workspace workspace = session.getWorkspace();
    if (workspace.getName().equals(srcWorkspaceName)) {
      workspace.copy(srcPath, destPath);
      Node destNode = (Node) session.getItem(destPath);
      Utils.removeReferences(destNode);
    } else {
      try {
        if (LOG.isDebugEnabled())
          LOG.debug("Copy to another workspace");
        workspace.copy(srcWorkspaceName, srcPath, destPath);
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("an unexpected error occurs while pasting the node", e);
        }
        if (LOG.isDebugEnabled())
          LOG.debug("Copy to other workspace by clone");
        try {
          workspace.clone(srcWorkspaceName, srcPath, destPath, false);
        } catch (Exception f) {
          if (LOG.isErrorEnabled()) {
            LOG.error("an unexpected error occurs while pasting the node", f);
          }
        }
      }
    }
  }

}
