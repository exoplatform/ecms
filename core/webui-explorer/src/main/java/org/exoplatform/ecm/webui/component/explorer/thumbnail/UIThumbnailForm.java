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
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

import org.exoplatform.services.log.Log;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormUploadInput;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 24, 2008 10:52:13 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/thumbnail/UIThumbnailForm.gtmpl",
    events = {
      @EventConfig(listeners = UIThumbnailForm.SaveActionListener.class),
      @EventConfig(listeners = UIThumbnailForm.CancelActionListener.class, phase = Phase.DECODE),
      @EventConfig(listeners = UIThumbnailForm.RemoveThumbnailActionListener.class,
          confirm = "UIThumbnailForm.msg.confirm-delete", phase = Phase.DECODE)
    }
)
public class UIThumbnailForm extends UIForm implements UIPopupComponent {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger("explorer.UIJCRExplorer");

  final static public String THUMBNAIL_FIELD = "mediumSize";
  private boolean thumbnailRemoved_ = false;

  public UIThumbnailForm() throws Exception {
    setMultiPart(true) ;
    UIFormUploadInput uiInput = new UIFormUploadInput(THUMBNAIL_FIELD, THUMBNAIL_FIELD) ;
    uiInput.setAutoUpload(true);
    addUIFormInput(uiInput) ;
  }

  public String getThumbnailImage(Node node) throws Exception {
    return Utils.getThumbnailImage(node, ThumbnailService.MEDIUM_SIZE);
  }

  public Node getSelectedNode() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getRealCurrentNode();
  }

  public Node getThumbnailNode(Node node) throws Exception {
    ThumbnailService thumbnailService = getApplicationComponent(ThumbnailService.class);
    return thumbnailService.getThumbnailNode(node);
  }

  public boolean isRemovedThumbnail() { return thumbnailRemoved_; }

  public String[] getActions() { return new String[] {"Save", "Cancel"}; }

  static  public class SaveActionListener extends EventListener<UIThumbnailForm> {
    public void execute(Event<UIThumbnailForm> event) throws Exception {
      UIThumbnailForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      UIFormUploadInput input = (UIFormUploadInput)uiForm.getUIInput(THUMBNAIL_FIELD);
      if(input.getUploadResource() == null) {
        uiApp.addMessage(new ApplicationMessage("UIUploadForm.msg.fileName-error", null,
                                                ApplicationMessage.WARNING));
        
        return;
      }
      Node selectedNode = uiExplorer.getRealCurrentNode();
      uiExplorer.addLockToken(selectedNode);
      String fileName = input.getUploadResource().getFileName();
      DMSMimeTypeResolver mimeTypeSolver = DMSMimeTypeResolver.getInstance();
      String mimeType = mimeTypeSolver.getMimeType(fileName) ;
      if(!mimeType.startsWith("image")) {
        uiApp.addMessage(new ApplicationMessage("UIThumbnailForm.msg.mimetype-incorrect", null,
            ApplicationMessage.WARNING));
        
        return;
      }
      InputStream inputStream = input.getUploadDataAsStream();
      ThumbnailService thumbnailService = uiForm.getApplicationComponent(ThumbnailService.class);
      BufferedImage image = ImageIO.read(inputStream);
      try {
        thumbnailService.createThumbnailImage(selectedNode, image, mimeType);
      } catch(AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIThumbnailForm.msg.access-denied", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch(VersionException ver) {
        uiApp.addMessage(new ApplicationMessage("UIThumbnailForm.msg.is-checked-in", null,
            ApplicationMessage.WARNING));
        
        return;
      } catch(LockException lock) {
        Object[] arg = { selectedNode.getPath() };
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", arg,
            ApplicationMessage.WARNING));
        
        return;
      } catch(PathNotFoundException path) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
            null,ApplicationMessage.WARNING));
        
        return;
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("An unexpected error occurs", e);
        }
        JCRExceptionManager.process(uiApp, e);
        
        return;
      }
      selectedNode.getSession().save();
      uiExplorer.updateAjax(event);
    }
  }

  static  public class RemoveThumbnailActionListener extends EventListener<UIThumbnailForm> {
    public void execute(Event<UIThumbnailForm> event) throws Exception {
      UIThumbnailForm uiForm = event.getSource();
      UIJCRExplorer uiExplorer = uiForm.getAncestorOfType(UIJCRExplorer.class);
      uiForm.thumbnailRemoved_ = true;
      Node selectedNode = uiExplorer.getRealCurrentNode();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class);
      uiExplorer.addLockToken(selectedNode);
      ThumbnailService thumbnailService = uiForm.getApplicationComponent(ThumbnailService.class);
      Node thumbnailNode = thumbnailService.getThumbnailNode(selectedNode);
      if(thumbnailNode != null) {
        try {
          thumbnailNode.remove();
          selectedNode.getSession().save();
        } catch(LockException lock) {
          Object[] arg = { selectedNode.getPath() };
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked", arg,
              ApplicationMessage.WARNING));
          
          return;
        } catch(AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.access-denied", null,
              ApplicationMessage.WARNING));
          
          uiExplorer.updateAjax(event);
          return;
        } catch(PathNotFoundException path) {
          uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
              null,ApplicationMessage.WARNING));
          
          return;
        } catch(Exception e) {
          if (LOG.isErrorEnabled()) {
            LOG.error("An unexpected error occurs", e);
          }
          JCRExceptionManager.process(uiApp, e);
          
          return;
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
      uiExplorer.setIsHidePopup(true);
      uiExplorer.updateAjax(event);
    }
  }

  static  public class CancelActionListener extends EventListener<UIThumbnailForm> {
    public void execute(Event<UIThumbnailForm> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      uiExplorer.cancelAction();
    }
  }

  public void activate() throws Exception {}

  public void deActivate() throws Exception {}
}
