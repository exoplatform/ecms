
/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.ecms.action;

import org.apache.commons.io.FileUtils;
import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudDriveException;
import org.exoplatform.clouddrive.NotCloudFileException;
import org.exoplatform.clouddrive.NotYetCloudFileException;
import org.exoplatform.clouddrive.ecms.CloudDriveContext;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.wcm.ext.component.activity.SharedFileUIActivity;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SharedCloudFileUIActivity.java 00000 Apr 15, 2016 pnedonosko $
 */
@ComponentConfigs({
    @ComponentConfig(lifecycle = UIFormLifecycle.class,
                     template = "classpath:groovy/ecm/social-integration/UISharedFile.gtmpl",
                     events = { @EventConfig(listeners = FileUIActivity.ViewDocumentActionListener.class),
                         @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
                         @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
                         @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
                         @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
                         @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
                         @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
                         @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class) }),
    @ComponentConfig(type = UIPopupWindow.class, template = "system:/groovy/webui/core/UIPopupWindow.gtmpl",
                     events = @EventConfig(listeners = SharedCloudFileUIActivity.CloseActionListener.class,
                                           name = "ClosePopup") ) })
public class SharedCloudFileUIActivity extends SharedFileUIActivity {

  /** The Constant ACTIVITY_CSS_CLASS. */
  public static final String        ACTIVITY_CSS_CLASS = "uiCloudFileActivity";

  /** The Constant LOG. */
  protected static final Log        LOG                = ExoLogger.getLogger(SharedCloudFileUIActivity.class);

  /** The cloud drives. */
  protected final CloudDriveService cloudDrives;

  /**
   * Instantiates a new shared cloud file UI activity.
   *
   * @throws Exception the exception
   */
  public SharedCloudFileUIActivity() throws Exception {
    this.cloudDrives = WCMCoreUtils.getService(CloudDriveService.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getSize(Node node) {
    CloudFile file = cloudFile(node);
    return file != null ? (file.getSize() > 0 ? FileUtils.byteCountToDisplaySize(file.getSize()) : "") : super.getSize(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected double getFileSize(Node node) {
    CloudFile file = cloudFile(node);
    return file != null ? (file.getSize() > 0 ? file.getSize() : 1) : super.getFileSize(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getWebdavURL() throws Exception {
    // XXX we return link to Google Drive page here, but Google also can offer content download for some
    // formats
    CloudFile file = cloudFile(getContentNode());
    if (file != null) {
      return file.getLink();
    } else {
      return super.getWebdavURL();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getCssClassIconFile(String fileName, String fileType) {
    // when showing Cloud Drive icons, need load them by the JS client
    try {
      Node node = getContentNode();
      String path = node.getPath();
      String workspace = node.getSession().getWorkspace().getName();
      CloudDriveContext.init(WebuiRequestContext.getCurrentInstance(), workspace, path);
    } catch (Throwable e) {
      LOG.error("Error initializing current node for shared cloud file link: " + fileName, e);
    }
    // XXX we add a special CSS class to let the JS client decorator recognize file icons in
    // activity stream for proper sizing (if required, e.g. for Google Docs icons)
    return new StringBuilder(super.getCssClassIconFile(fileName, fileType)).append(' ')
                                                                           .append(ACTIVITY_CSS_CLASS)
                                                                           .toString();
  }

  // TODO experimental, to show cloud file preview directly in activity stream
  // /**
  // * {@inheritDoc}
  // */
  // @SuppressWarnings("unchecked")
  // @Override
  // public <T extends UIComponent> T addChild(Class<T> type, String configId, String id) throws Exception {
  // if (ContentPresentation.class.isAssignableFrom(type)) {
  // return super.addChild(((Class<T>) SharedCloudFileContentPresentation.class), configId, id);
  // }
  // return super.addChild(type, configId, id);
  // }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDownloadLink() {
    // XXX we return link to Google Drive page here, but Google also can offer content download for some
    // formats
    CloudFile file = cloudFile(getContentNode());
    if (file != null) {
      return new StringBuilder("javascript:window.open('").append(file.getLink()).append("')").toString();
    } else {
      return super.getDownloadLink();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isFileSupportPreview(Node data) throws Exception {
    if (data != null) {
      // code adapted from the super's method but with adding a node in to context
      UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
      List<UIExtension> extensions = manager.getUIExtensions(Utils.FILE_VIEWER_EXTENSION_TYPE);

      Map<String, Object> context = new HashMap<String, Object>();
      context.put(Node.class.getName(), data);
      context.put(Utils.MIME_TYPE, data.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString());

      for (UIExtension extension : extensions) {
        if (manager.accept(Utils.FILE_VIEWER_EXTENSION_TYPE, extension.getName(), context)
            && !"Text".equals(extension.getName())) {
          return true;
        }
      }
    }
    return super.isFileSupportPreview(data);
  }

  // ************* internals *************

  /**
   * Cloud file.
   *
   * @param node the node
   * @return the cloud file
   */
  protected CloudFile cloudFile(Node node) {
    try {
      String workspace = node.getSession().getWorkspace().getName();
      String path = node.getPath();
      CloudDrive drive = cloudDrives.findDrive(workspace, path);
      if (drive != null) {
        try {
          return drive.getFile(path);
        } catch (NotYetCloudFileException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Not yet cloud file " + workspace + ":" + path, e);
          }
        } catch (NotCloudFileException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Not cloud file " + workspace + ":" + path, e);
          }
        } catch (DriveRemovedException e) {
          LOG.warn("Cloud drive removed " + workspace + ":" + path, e);
        } catch (NotCloudDriveException e) {
          LOG.warn("Not cloud drive " + workspace + ":" + path, e);
        }
      }
    } catch (RepositoryException e) {
      LOG.error("Error getting cloud file node " + node, e); // TODO use global workspace, docPath
    }
    return null;
  }

}
