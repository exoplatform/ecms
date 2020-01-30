package org.exoplatform.social.plugin.doc.selector;

import java.io.Writer;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.social.plugin.doc.UIDocActivityPopup;
import org.exoplatform.social.plugin.doc.UIFolderActivityPopup;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

/**
 * This component is used to refresh the Component UIDocumentSelector without changing the DOM.
 * The DOM updates are made by Javascript commands
 * 
 */
@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIDocumentSelectorUpdate extends UIContainer {

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    Writer writer = context.getWriter();
    writer.append("<div id='").append(this.getId()).append("'></div>");

    UIDocumentSelector uiParent = this.getParent();
    List<Node> filesList = uiParent.getFiles();

    JavascriptManager jsManager = context.getJavascriptManager();
    RequireJS multiUploadJS = jsManager.require("SHARED/composerMultiUpload", "multiUpload");
    if (!filesList.isEmpty()) {
      for (Node fileNode : filesList) {
        String compId = "fileSelection" + fileNode.getPath().hashCode();
        if (uiParent.isFileSelected(fileNode)) {
          multiUploadJS.addScripts("gj('#" + compId + "').addClass('selected');");
        } else {
          multiUploadJS.addScripts("gj('#" + compId + "').removeClass('selected');");
        }
      }
    }
    RequireJS jqueryJS = jsManager.require("SHARED/jquery", "gj");
    UIComponent parent = uiParent.getParent();
    if (parent instanceof UIDocActivityPopup) {
      if (((UIDocActivityPopup) parent).isLimitReached()) {
        jqueryJS.addScripts("gj('.UIDocActivityPopup .countLimit').addClass('error');");
      } else {
        jqueryJS.addScripts("gj('.UIDocActivityPopup .countLimit').removeClass('error');");
      }
      if(uiParent.hasSelectedFiles()) {
        jqueryJS.addScripts("gj('.selectFileBTN').attr('disabled', null);");
      } else {
        jqueryJS.addScripts("gj('.selectFileBTN').attr('disabled', 'disabled');");
      }
      if(uiParent.isDocumentAlreadySelectedError()) {
        jqueryJS.addScripts("gj('.fileAlreadySelected').show();gj('.fileAlreadySelected b').html('" + uiParent.getLastSelectedDocumentTitle() + "');gj('.fileAlreadySelected').delay(5000).fadeOut('slow');");
        uiParent.setDocumentAlreadySelectedError(false);
      } else {
        jqueryJS.addScripts("gj('.fileAlreadySelected').hide();");
      }
    } else if (parent instanceof UIFolderActivityPopup) {
      if (((UIFolderActivityPopup) parent).isFolder()) {
        jqueryJS.addScripts("gj('.selectFolderBTN').attr('disabled', null);");
      } else {
        jqueryJS.addScripts("gj('.selectFolderBTN').attr('disabled', 'disabled');");
      }
    }

    jqueryJS.addScripts("gj(document).ready(function() { gj(\"*[rel='tooltip']\").tooltip();});");
  }

}
