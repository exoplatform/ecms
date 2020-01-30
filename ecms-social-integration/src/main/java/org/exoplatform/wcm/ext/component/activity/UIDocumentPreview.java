package org.exoplatform.wcm.ext.component.activity;


import java.util.*;

import javax.jcr.Node;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.social.plugin.doc.UIDocViewer;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.*;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

@ComponentConfig(
        template = "war:/groovy/ecm/social-integration/UIDocumentPreview.gtmpl",
        events = {
                @EventConfig(listeners = UIDocumentPreview.CloseActionListener.class)
        }
)
public class UIDocumentPreview extends UIContainer {

  private DocumentService documentService;

  private BaseUIActivity baseUIActivity;

  public UIDocumentPreview() throws Exception {
    this.documentService = CommonsUtils.getService(DocumentService.class);

    this.addChild(UIDocViewer.class, null, "UIDocViewer");
    this.addChild(UIPreviewCommentArea.class, null, "UIPreviewCommentArea");
  }

  public void setContentInfo(String docPath, String repository, String workspace, Node docNode) {
    UIDocViewer uiDocViewer = this.getChild(UIDocViewer.class);
    uiDocViewer.docPath = docPath;
    uiDocViewer.repository = repository;
    uiDocViewer.workspace = workspace;
    uiDocViewer.setOriginalNode(docNode);
    uiDocViewer.setNode(docNode);
  }

  public Node getOriginalNode() throws Exception {
    UIDocViewer uiDocViewer = findFirstComponentOfType(UIDocViewer.class);
    return uiDocViewer.getOriginalNode();
  }

  /**
   * Return the link of the document in the Documents application
   * @return the link of the document in the Documents application
   */
  public String getLinkInDocumentsApp() throws Exception {
    return documentService.getLinkInDocumentsApp(getOriginalNode().getPath());
  }

  private boolean isWebContent() throws Exception {
    UIDocViewer uiDocViewer = findFirstComponentOfType(UIDocViewer.class);
    Node previewNode = uiDocViewer.getNode();
    if (previewNode != null) {
      return previewNode.isNodeType(org.exoplatform.ecm.webui.utils.Utils.EXO_WEBCONTENT);
    }

    return false;
  }

  /**
   * Check if a node is media/image
   * @param data
   * @return
   * @throws Exception
   */
  private boolean isMediaFile(Node data) throws Exception {
    if (data.isNodeType(Utils.NT_FILE)) {
      UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
      List<UIExtension> extensions = manager.getUIExtensions(Utils.FILE_VIEWER_EXTENSION_TYPE);

      Map<String, Object> context = new HashMap<String, Object>();
      context.put(Utils.MIME_TYPE, data.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString());

      for (UIExtension extension : extensions) {
        if (manager.accept(Utils.FILE_VIEWER_EXTENSION_TYPE, extension.getName(), context)
                && !"Text".equals(extension.getName())
                && !"PDF".equals(extension.getName())) {
          return true;
        }
      }
    }

    return false;
  }

  public BaseUIActivity getBaseUIActivity() {
    return baseUIActivity;
  }

  public void setBaseUIActivity(BaseUIActivity baseUIActivity) {
    this.baseUIActivity = baseUIActivity;
  }

  public String getEmbedHtml() {
    BaseUIActivity baseUIActivity = this.getBaseUIActivity();
    if (baseUIActivity instanceof UILinkActivity) {
      String embedHtml = ((UILinkActivity) baseUIActivity).getEmbedHtml();
      if (embedHtml != null) {
        embedHtml = embedHtml.replaceFirst("width=\\\"[0-9]*\\\"","width=\"100%\"")
                .replaceFirst("height=\\\"[0-9]*\\\"","height=\"100%\"");
      }
      return embedHtml;
    }

    return null;
  }

  public static class CloseActionListener extends EventListener<UIDocumentPreview> {
    public void execute(Event<UIDocumentPreview> event) throws Exception {
      UIDocumentPreview uiDocumentPreview = event.getSource();
      UIPopupWindow uiPopupWindow = uiDocumentPreview.getAncestorOfType(UIPopupWindow.class);
      if (!uiPopupWindow.isShow())
        return;
      uiPopupWindow.setShow(false);
      uiPopupWindow.setUIComponent(null);
      UIPopupContainer popupContainer = uiPopupWindow.getAncestorOfType(UIPopupContainer.class);
      WebuiRequestContext requestContext = event.getRequestContext();
      if(uiDocumentPreview.getBaseUIActivity() != null
              && requestContext.getUIApplication().findComponentById(uiDocumentPreview.getBaseUIActivity().getId()) != null) {
        requestContext.addUIComponentToUpdateByAjax(uiDocumentPreview.getBaseUIActivity());
      }
      requestContext.addUIComponentToUpdateByAjax(popupContainer);
    }
  }
}
