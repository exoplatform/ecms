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
package org.exoplatform.ecm.webui.component.explorer.thumbnail;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.input.UIUploadInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 24, 2008 10:52:13 AM
 */
@ComponentConfigs({
    @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "app:/groovy/webui/component/explorer/thumbnail/UIThumbnailForm.gtmpl", events = {
        @EventConfig(listeners = UIThumbnailForm.SaveActionListener.class),
        @EventConfig(listeners = UIThumbnailForm.CancelActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = UIThumbnailForm.RemoveThumbnailActionListener.class, confirm = "UIThumbnailForm.msg.confirm-delete", phase = Phase.DECODE),
        @EventConfig(listeners = UIThumbnailForm.PreviewActionListener.class) }),
    @ComponentConfig(id="mediumSize", type = UIUploadInput.class, template = "app:/groovy/webui/component/explorer/thumbnail/UIFormUploadInput.gtmpl") })
public class UIThumbnailForm extends UIForm implements UIPopupComponent {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(UIThumbnailForm.class.getName());

  private static final String THUMBNAIL_FIELD = "mediumSize";

  private UploadResource currentUploadResource;
  private BufferedImage currentUploadImage;
  private String currentPreviewLink;

  public UIThumbnailForm() throws Exception {
    initForm();
  }

  private void initForm() throws Exception {
    currentUploadResource = null;
    currentUploadImage = null;
    currentPreviewLink = null;

    setMultiPart(true);
    UIUploadInput uiInput = new UIUploadInput(THUMBNAIL_FIELD, THUMBNAIL_FIELD);
    removeChild(UIUploadInput.class);
    addUIFormInput(uiInput);
  }

  public String getThumbnailImage(Node node) throws Exception {
    return Utils.getThumbnailImage(node, ThumbnailService.MEDIUM_SIZE);
  }

  public String getPreviewImage() throws Exception {
    UIUploadInput input = this.getUIInput(THUMBNAIL_FIELD);
    String uploadId = input.getUploadIds()[0];
    if(input.getUploadResource(uploadId) == null)  return null;
    return Utils.getThumbnailImage(input.getUploadDataAsStream(uploadId), input.getUploadResource(uploadId).getFileName());
  }

  public Node getSelectedNode() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getRealCurrentNode();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.form.UIForm#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    Node currentRealNode = getSelectedNode();
    if (getThumbnailImage(currentRealNode) == null) {
      setActions(new String[] {"Save", "Cancel"});
    } else {
      setActions(new String[] {"RemoveThumbnail", "Cancel"});
    }
    super.processRender(context);
  }

  public Node getThumbnailNode(Node node) throws Exception {
    ThumbnailService thumbnailService = getApplicationComponent(ThumbnailService.class);
    return thumbnailService.getThumbnailNode(node);
  }

  /**
   * @return the currentUploadImage
   */
  public BufferedImage getCurrentUploadImage() {
    return currentUploadImage;
  }

  /**
   * @param currentUploadImage the currentUploadImage to set
   */
  public void setCurrentUploadImage(BufferedImage currentUploadImage) {
    this.currentUploadImage = currentUploadImage;
  }

  /**
   * @return the currentUploadResource
   */
  public UploadResource getCurrentUploadResource() {
    return currentUploadResource;
  }

  /**
   * @param currentUploadResource the currentUploadResource to set
   */
  public void setCurrentUploadResource(UploadResource currentUploadResource) {
    this.currentUploadResource = currentUploadResource;
  }

  /**
   * @param currentPreviewLink the currentPreviewLink to set
   */
  public void setCurrentPreviewLink(String currentPreviewLink) {
    this.currentPreviewLink = currentPreviewLink;
  }

  /**
   * @return the currentPreviewLink
   */
  public String getCurrentPreviewLink() {
    return currentPreviewLink;
  }

  /**
   * return the Modified date property of the selected node
   *
   * @param node the thumbnail node of the selected node
   * @return the Modified date property
   * @throws Exception
   */
  public String getThumbnailModifiedTime(Node node) throws Exception {
    boolean hasLastModifiedProperty = node.hasProperty(ThumbnailService.THUMBNAIL_LAST_MODIFIED);
    if (hasLastModifiedProperty) {
      return node.getProperty(ThumbnailService.THUMBNAIL_LAST_MODIFIED).getDate().getTime().toString();
    } else {
      LOG.warn("Thumbnail last modified property of the node " + node.getPath() + " doesn't exist.");
      return null;
    }
  }

  static  public class SaveActionListener extends EventListener<UIThumbnailForm> {
    public void execute(Event<UIThumbnailForm> event) throws Exception {
      UIThumbnailForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);

      // Check resource available
      if(uiForm.getCurrentUploadResource() == null) {
        uiApp.addMessage(new ApplicationMessage("UIThumbnailForm.msg.fileName-error", null,
                                                ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);

        return;
      }

      // Check if file is image
      String fileName = uiForm.getCurrentUploadResource().getFileName();
      DMSMimeTypeResolver mimeTypeSolver = DMSMimeTypeResolver.getInstance();
      String mimeType = mimeTypeSolver.getMimeType(fileName) ;
      if(!mimeType.startsWith("image")) {
        uiApp.addMessage(new ApplicationMessage("UIThumbnailForm.msg.mimetype-incorrect", null,
            ApplicationMessage.WARNING));

        return;
      }

      // Add lock token
      Node selectedNode = uiExplorer.getRealCurrentNode();
      uiExplorer.addLockToken(selectedNode);

      // Create thumbnail
      ThumbnailService thumbnailService = WCMCoreUtils.getService(ThumbnailService.class);
      try {
        thumbnailService.createThumbnailImage(selectedNode, uiForm.getCurrentUploadImage(), mimeType);

        selectedNode.getSession().save();
        uiExplorer.updateAjax(event);
      } catch(AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIThumbnailForm.msg.access-denied", null, ApplicationMessage.WARNING));
      } catch(VersionException ver) {
        uiApp.addMessage(new ApplicationMessage("UIThumbnailForm.msg.is-checked-in", null, ApplicationMessage.WARNING));
      } catch(LockException lock) {
        Object[] arg = { selectedNode.getPath() };
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", arg, ApplicationMessage.WARNING));
      } catch(PathNotFoundException path) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception", null,ApplicationMessage.WARNING));
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("An unexpected error occurs", e);
        }
        JCRExceptionManager.process(uiApp, e);
      }
    }
  }

  static  public class PreviewActionListener extends EventListener<UIThumbnailForm> {
    public void execute(Event<UIThumbnailForm> event) throws Exception {
      UIThumbnailForm uiForm = event.getSource();

      // Current review link
      UIUploadInput input = (UIUploadInput)uiForm.getUIInput(THUMBNAIL_FIELD);
      String uploadId = input.getUploadIds()[0];
      if (input.getUploadDataAsStream(uploadId) != null) {
        uiForm.setCurrentPreviewLink(uiForm.getPreviewImage());
        uiForm.setCurrentUploadImage(ImageIO.read(input.getUploadDataAsStream(uploadId)));
        uiForm.setCurrentUploadResource(input.getUploadResource(uploadId));
      } else {
        uiForm.setCurrentPreviewLink(null);
        uiForm.setCurrentUploadImage(null);
        uiForm.setCurrentUploadResource(null);
      }

      // New upload input
      uiForm.removeChild(UIUploadInput.class);
      UIUploadInput uiInput = new UIUploadInput(THUMBNAIL_FIELD, THUMBNAIL_FIELD) ;
      uiForm.addUIFormInput(uiInput) ;
    }
  }

  static  public class RemoveThumbnailActionListener extends EventListener<UIThumbnailForm> {
    public void execute(Event<UIThumbnailForm> event) throws Exception {
      UIThumbnailForm uiForm = event.getSource();
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      Node selectedNode = uiExplorer.getRealCurrentNode();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      uiExplorer.addLockToken(selectedNode);
      ThumbnailService thumbnailService = uiForm.getApplicationComponent(ThumbnailService.class);
      Node thumbnailNode = thumbnailService.getThumbnailNode(selectedNode);
      if(thumbnailNode != null) {
        try {
          // Remove thumbmail
          thumbnailNode.remove();
          selectedNode.getSession().save();

          // Reset form
          uiForm.initForm();
          event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
        } catch(LockException lock) {
          Object[] arg = { selectedNode.getPath() };
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", arg, ApplicationMessage.WARNING));
        } catch(AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.access-denied", null, ApplicationMessage.WARNING));
          uiExplorer.updateAjax(event);
        } catch(PathNotFoundException path) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception", null,ApplicationMessage.WARNING));
        } catch(Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("An unexpected error occurs", e);
          }
          JCRExceptionManager.process(uiApp, e);
        }
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UIThumbnailForm> {
    public void execute(Event<UIThumbnailForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  public void activate() {}

  public void deActivate() {}
}
