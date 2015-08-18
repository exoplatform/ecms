package org.exoplatform.ecm.webui.component.explorer;

import org.exoplatform.ecm.webui.component.explorer.rightclick.manager.PasteManageComponent;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.services.cms.clipboard.jcr.model.ClipboardCommand;
import org.exoplatform.services.cms.documents.AutoVersionService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.input.UICheckBoxInput;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 7/27/15
 * Build popup document auto versioning
 */
@ComponentConfig(
        template = "app:/groovy/webui/component/explorer/versions/UIDocumentAutoVersionForm.gtmpl",
        lifecycle = UIFormLifecycle.class,
        events = {
                @EventConfig(listeners = UIDocumentAutoVersionForm.KeepBothActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionForm.CreateNewVersionActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionForm.ReplaceActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionForm.CreateVersionOrReplaceActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionForm.OnChangeActionListener.class),
                @EventConfig(listeners = UIDocumentAutoVersionForm.CancelActionListener.class, phase = Event.Phase.DECODE)
        }
)
public class UIDocumentAutoVersionForm extends UIForm implements UIPopupComponent {

  private static final Log    LOG           = ExoLogger.getLogger(UIDocumentAutoVersionForm.class.getName());
  public static final String KEEP_BOTH = "KeepBoth";
  public static final String CREATE_VERSION = "CreateNewVersion";
  public static final String REPLACE = "Replace";
  public static final String CREATE_OR_REPLACE = "CreateVersionOrReplace";
  public static final String CANCEL = "Cancel";
  public static final String REMEMBER_VERSIONED_COMPONENT = "UIDocumentAutoVersionForm.UIChkRememberVersioned";
  public static final String REMEMBER_NONVERSIONED_COMPONENT = "UIDocumentAutoVersionForm.UIChkRememberNonVersioned";

  private boolean isVersioned, isSingleProcess = false;
  private String sourcePath;
  private String destPath;
  private String sourceWorkspace;
  private String destWorkspace;
  private String message_;
  private String[] args_ = {};
  private static String[] actions = new String[] {KEEP_BOTH, CREATE_VERSION, REPLACE, CREATE_OR_REPLACE ,CANCEL};
  private static Set<ClipboardCommand> clipboardCommands = null;
  private static ClipboardCommand currentClipboard = null;

  @Override
  public void activate() { }

  @Override
  public void deActivate() {

  }

  public UIDocumentAutoVersionForm(){
    UICheckBoxInput chkRememberVersioned = new UICheckBoxInput(REMEMBER_VERSIONED_COMPONENT, "", false);
    UICheckBoxInput chkRememberNonVersioned = new UICheckBoxInput(REMEMBER_NONVERSIONED_COMPONENT, "", false);
    chkRememberVersioned.setOnChange("OnChange");
    chkRememberVersioned.setChecked(true);
    chkRememberNonVersioned.setChecked(true);
    chkRememberVersioned.setRendered(false);
    chkRememberNonVersioned.setRendered(false);
    this.addChild(chkRememberVersioned);
    this.addChild(chkRememberNonVersioned);
  }

  public void init(Node currentNode) throws Exception{
    UICheckBoxInput chkRemVersion = this.findComponentById(REMEMBER_VERSIONED_COMPONENT);
    UICheckBoxInput chkRemNonVersioned = this.findComponentById(REMEMBER_NONVERSIONED_COMPONENT);
    if(currentNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE)){
      setActions(new String[]{KEEP_BOTH, CREATE_VERSION, CANCEL});
      chkRemVersion.setRendered(true);
      chkRemNonVersioned.setRendered(false);
    }else{
      setActions(new String[]{KEEP_BOTH, REPLACE, CANCEL});
      chkRemVersion.setRendered(false);
      chkRemNonVersioned.setRendered(true);
    }
    if(isSingleProcess) {
      chkRemVersion.setRendered(false);
      chkRemNonVersioned.setRendered(false);
    }
  }

  public String[] getActions() { return actions; }

  public static class KeepBothActionListener extends EventListener<UIDocumentAutoVersionForm> {
    @Override
    public void execute(Event<UIDocumentAutoVersionForm> event) throws Exception {
      UIDocumentAutoVersionForm autoVersionComponent = event.getSource();
      UIJCRExplorer uiExplorer = autoVersionComponent.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiExplorer.getAncestorOfType(UIApplication.class);
      UICheckBoxInput chkRemVersion = autoVersionComponent.findComponentById(REMEMBER_VERSIONED_COMPONENT);
      UICheckBoxInput chkRemNonVersioned = autoVersionComponent.findComponentById(REMEMBER_NONVERSIONED_COMPONENT);
      boolean chkRem = chkRemVersion.isChecked() && chkRemVersion.isRendered();
      boolean chkRemNon = chkRemNonVersioned.isChecked() && chkRemNonVersioned.isRendered();
      Session destSession = uiExplorer.getSessionByWorkspace(autoVersionComponent.getDestWorkspace());
      Session srcSession = uiExplorer.getSessionByWorkspace(autoVersionComponent.getSourceWorkspace());
      Node sourceNode = uiExplorer.getNodeByPath(autoVersionComponent.getSourcePath(), srcSession);
      String destPath = autoVersionComponent.getDestPath();

      if (destPath != null) {
        Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destPath);
        if (matcher.find()) {
          destPath = matcher.group(2);
        }
      }
      if (!"/".equals(destPath)) destPath = destPath.concat("/");
      destPath = destPath.concat(sourceNode.getName());

      if(!chkRem || !chkRemNon) {
        try {
          copyNode(destSession, autoVersionComponent.getSourceWorkspace(),
                  autoVersionComponent.getSourcePath(), destPath);
        } catch (ItemExistsException iee) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.paste-node-same-name", null,
                  ApplicationMessage.WARNING));

          uiExplorer.updateAjax(event);
          return;
        }
      }
      Node destNode = (Node)destSession.getItem(destPath);
      if(destNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE) && chkRemVersion.getValue()){
        PasteManageComponent.setVersionedRemember(chkRem);
      }else{
        PasteManageComponent.setNonVersionedRemember(chkRemNon);
      }

      Set<ClipboardCommand> _clipboardCommands = autoVersionComponent.getClipboardCommands();
      if(_clipboardCommands!=null && _clipboardCommands.size()>0){
        _clipboardCommands.remove(currentClipboard);
        PasteManageComponent.processPasteMultiple(destNode.getParent(), event, uiExplorer, _clipboardCommands, KEEP_BOTH);
      }else {
        closePopup(autoVersionComponent, uiExplorer, event);
      }

      closePopup(autoVersionComponent, uiExplorer, event);
    }
  }

  public static class CreateNewVersionActionListener extends EventListener<UIDocumentAutoVersionForm> {
    @Override
    public void execute(Event<UIDocumentAutoVersionForm> event) throws Exception {
      UIDocumentAutoVersionForm autoVersionComponent = event.getSource();
      UIJCRExplorer uijcrExplorer = autoVersionComponent.getAncestorOfType(UIJCRExplorer.class);

      UICheckBoxInput chkRemVersion = autoVersionComponent.findComponentById(REMEMBER_VERSIONED_COMPONENT);
      UICheckBoxInput chkRemNonVersioned = autoVersionComponent.findComponentById(REMEMBER_NONVERSIONED_COMPONENT);
      Session destSession = uijcrExplorer.getSessionByWorkspace(autoVersionComponent.getDestWorkspace());
      Session srcSession = uijcrExplorer.getSessionByWorkspace(autoVersionComponent.getSourceWorkspace());
      Node sourceNode = uijcrExplorer.getNodeByPath(autoVersionComponent.getSourcePath(), srcSession);
      String destPath = autoVersionComponent.getDestPath();
      if (destPath != null) {
        Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destPath);
        if(matcher.find()) destPath = matcher.group(2);
      }
      if (!"/".equals(destPath)) destPath = destPath.concat("/");
      destPath = destPath.concat(sourceNode.getName());

      Node destNode = (Node)destSession.getItem(destPath);
      if(autoVersionComponent.isSingleProcess){
        AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
        autoVersionService.autoVersion(destNode, sourceNode);
        closePopup(autoVersionComponent, uijcrExplorer, event);
        return;
      }
      if(destNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE) && chkRemVersion.getValue()){
        PasteManageComponent.setVersionedRemember(chkRemVersion.isChecked() && chkRemVersion.isRendered());
      }else{
        PasteManageComponent.setNonVersionedRemember(chkRemNonVersioned.isChecked() && chkRemNonVersioned.isRendered());
      }
      Set<ClipboardCommand> _clipboardCommands = autoVersionComponent.getClipboardCommands();
      if(_clipboardCommands!=null && _clipboardCommands.size()>0){
        if (ClipboardCommand.COPY.equals(autoVersionComponent.getCurrentClipboard().getType())) {
          _clipboardCommands.remove(autoVersionComponent.getCurrentClipboard());
          AutoVersionService autoVersionService = WCMCoreUtils.getService(AutoVersionService.class);
          autoVersionService.autoVersion(destNode, sourceNode);
        }
        PasteManageComponent.processPasteMultiple(destNode.getParent(), event, uijcrExplorer, _clipboardCommands, CREATE_VERSION);
      }else {
        closePopup(autoVersionComponent, uijcrExplorer, event);
      }

      if(chkRemVersion.getValue() && chkRemNonVersioned.getValue()) closePopup(autoVersionComponent, uijcrExplorer, event);
    }
  }


  public static class ReplaceActionListener extends EventListener<UIDocumentAutoVersionForm> {
    @Override
    public void execute(Event<UIDocumentAutoVersionForm> event) throws Exception {
      UIDocumentAutoVersionForm autoVersionComponent = event.getSource();
      UIJCRExplorer uijcrExplorer = autoVersionComponent.getAncestorOfType(UIJCRExplorer.class);

      UICheckBoxInput chkRemVersion = autoVersionComponent.findComponentById(REMEMBER_VERSIONED_COMPONENT);
      UICheckBoxInput chkRemNonVersioned = autoVersionComponent.findComponentById(REMEMBER_NONVERSIONED_COMPONENT);
      Session destSession = uijcrExplorer.getSessionByWorkspace(autoVersionComponent.getDestWorkspace());
      Session srcSession = uijcrExplorer.getSessionByWorkspace(autoVersionComponent.getSourceWorkspace());
      Node sourceNode = uijcrExplorer.getNodeByPath(autoVersionComponent.getSourcePath(), srcSession);
      String destPath = autoVersionComponent.getDestPath();
      if (destPath != null) {
        Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(destPath);
        if(matcher.find()) destPath = matcher.group(2);
      }
      if (!"/".equals(destPath)) destPath = destPath.concat("/");
      destPath = destPath.concat(sourceNode.getName());

      Node destNode = (Node)destSession.getItem(destPath);
      Node destDriectory = destNode.getParent();
      TrashService trashService = WCMCoreUtils.getService(TrashService.class);
      String trashID = trashService.moveToTrash(destNode, WCMCoreUtils.getUserSessionProvider());
      if(autoVersionComponent.isSingleProcess){
        copyNode(destSession, autoVersionComponent.getSourceWorkspace(),
                autoVersionComponent.getSourcePath(), destPath);
        Node deletedNode = trashService.getNodeByTrashId(trashID);
        deletedNode.remove();
        deletedNode.getSession().save();
        closePopup(autoVersionComponent, uijcrExplorer, event);
        return;
      }

      if(destNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE) && chkRemVersion.getValue()){
        PasteManageComponent.setVersionedRemember(chkRemVersion.isChecked() && chkRemVersion.isRendered());
      }else{
        PasteManageComponent.setNonVersionedRemember(chkRemNonVersioned.isChecked() && chkRemNonVersioned.isRendered());
      }
      Set<ClipboardCommand> _clipboardCommands = autoVersionComponent.getClipboardCommands();
      if(_clipboardCommands!=null && _clipboardCommands.size()>0){
        if (ClipboardCommand.COPY.equals(autoVersionComponent.getCurrentClipboard().getType())) {
          _clipboardCommands.remove(autoVersionComponent.getCurrentClipboard());
          copyNode(destSession, autoVersionComponent.getSourceWorkspace(),
                  autoVersionComponent.getSourcePath(), destPath);
          Node deletedNode = trashService.getNodeByTrashId(trashID);
          deletedNode.remove();
          deletedNode.getSession().save();
        }
        destSession.save();
        PasteManageComponent.processPasteMultiple(destDriectory, event, uijcrExplorer, _clipboardCommands, REPLACE);
      }else {
        closePopup(autoVersionComponent, uijcrExplorer, event);
      }

      if(chkRemVersion.getValue() && chkRemNonVersioned.getValue()) closePopup(autoVersionComponent, uijcrExplorer, event);
    }
  }

  public static class CreateVersionOrReplaceActionListener extends EventListener<UIDocumentAutoVersionForm> {
    @Override
    public void execute(Event<UIDocumentAutoVersionForm> event) throws Exception {
      UIDocumentAutoVersionForm uiConfirm = event.getSource();
      UIPopupWindow popupAction = uiConfirm.getAncestorOfType(UIPopupWindow.class) ;
      popupAction.setShow(true) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  public static class CancelActionListener extends EventListener<UIDocumentAutoVersionForm> {
    @Override
    public void execute(Event<UIDocumentAutoVersionForm> event) throws Exception {
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

  public void setMessage(String message) { message_ = message; }

  public String getMessage() { return message_; }

  public void setArguments(String[] args) { args_ = args; }

  public String[] getArguments() { return args_; }

  public boolean isVersioned() {
    return isVersioned;
  }

  public void setVersioned(boolean isVersioned) {
    this.isVersioned = isVersioned;
  }

  public void setActions(String[] actions) {
    this.actions = actions;
  }

  public void setSingleProcess(boolean isSingleProcess) {
    this.isSingleProcess = isSingleProcess;
  }

  /**
   * Copy node using workspace
   * @param session session of dest node
   * @param srcWorkspaceName source
   * @param srcPath
   * @param destPath
   * @throws Exception
   */
  public static void copyNode(Session session, String srcWorkspaceName, String srcPath,
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

  public Set<ClipboardCommand> getClipboardCommands() {
    return clipboardCommands;
  }

  public void setClipboardCommands(Set<ClipboardCommand> clipboardCommands) {
    this.clipboardCommands = clipboardCommands;
  }

  public ClipboardCommand getCurrentClipboard() {
    return currentClipboard;
  }

  public void setCurrentClipboard(ClipboardCommand currentClipboard) {
    UIDocumentAutoVersionForm.currentClipboard = currentClipboard;
  }

  private static void closePopup(UIDocumentAutoVersionForm autoVersionComponent,
                                 UIJCRExplorer uijcrExplorer, Event<?> event) throws Exception{
    UIPopupWindow popupAction = uijcrExplorer.findFirstComponentOfType(UIPopupWindow.class) ;
    popupAction.setShow(false) ;
    uijcrExplorer.updateAjax(event);
    UICheckBoxInput chkRememberVersioned = autoVersionComponent.findComponentById(REMEMBER_VERSIONED_COMPONENT);
    UICheckBoxInput chkRememberNonVersioned = autoVersionComponent.findComponentById(REMEMBER_NONVERSIONED_COMPONENT);
    chkRememberVersioned.setChecked(false);
    chkRememberNonVersioned.setChecked(false);
    PasteManageComponent.setVersionedRemember(chkRememberVersioned.isChecked() && chkRememberVersioned.isRendered());
    PasteManageComponent.setNonVersionedRemember(chkRememberNonVersioned.isChecked() && chkRememberNonVersioned.isRendered());
    currentClipboard = null;
  }

  static public class OnChangeActionListener extends EventListener<UIDocumentAutoVersionForm> {
    public void execute(Event<UIDocumentAutoVersionForm> event) throws Exception {
      UICheckBoxInput chkRememberVersioned = event.getSource().findComponentById(REMEMBER_VERSIONED_COMPONENT);
      UICheckBoxInput chkRememberNonVersioned = event.getSource().findComponentById(REMEMBER_NONVERSIONED_COMPONENT);
      PasteManageComponent.setVersionedRemember(chkRememberVersioned.isChecked() && chkRememberVersioned.isRendered());
      PasteManageComponent.setNonVersionedRemember(chkRememberNonVersioned.isChecked() && chkRememberNonVersioned.isRendered());
    }
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    super.processRender(context);
  }

}
