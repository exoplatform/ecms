
/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

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
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SharedCloudFileUIActivity.java 00000 Apr 15, 2016 pnedonosko $
 */
@ComponentConfigs({
    @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/ecm/social-integration/UISharedFile.gtmpl", events = {
        @EventConfig(listeners = FileUIActivity.ViewDocumentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
        @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
        @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class) })/*,
    @ComponentConfig(type = UIPopupWindow.class, template = "system:/groovy/webui/core/UIPopupWindow.gtmpl", events = @EventConfig(listeners = SharedCloudFileUIActivity.CloseActionListener.class, name = "ClosePopup"))*/ 
    })
public class SharedCloudFileUIActivity extends FileUIActivity {

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
    // XXX In PLF 4.4.4 shared files appear w/o small preview in activity stream
    // -
    // this done by size 0 (template prints preview for size > 0)
    // But we send real size anyway, as we want show an icon (which may describe
    // a drive type, like gdoc icon)
    // and in JS we'll remove the click action and Preview button.
    // return file != null ? 0 : super.getFileSize(node);
    return file != null ? (file.getSize() > 0 ? file.getSize() : 1) : super.getFileSize(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getWebdavURL(int i) throws Exception {
    // XXX we return link to cloud page here, but providers also can offer
    // content download for some formats
    CloudFile file = cloudFile(getContentNode(i));
    if (file != null) {
      return file.getLink(); // it can be a page on cloud provider site
    } else {
      return super.getWebdavURL(i);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getCssClassIconFile(String fileName, String fileType, int i) {
    // when showing Cloud Drive icons, need load them by the JS client
    try {
      Node node = getContentNode(i);
      if (node != null) {
        String path = node.getPath();
        String workspace = node.getSession().getWorkspace().getName();
        CloudDriveContext.init(WebuiRequestContext.getCurrentInstance(), workspace, path);
      } // otherwise Cloud Drive has nothing to do with this
    } catch (Throwable e) {
      LOG.error("Error initializing current node for shared cloud file link: " + fileName, e);
    }
    // XXX we add a special CSS class to let the JS client decorator recognize
    // file icons in activity stream for proper sizing (if required, e.g. for
    // Google Docs icons)
    return new StringBuilder(super.getCssClassIconFile(fileName, fileType, i)).append(' ').append(ACTIVITY_CSS_CLASS).toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDownloadLink(int i) {
    // XXX we return link to Google Drive page here, but Google also can offer
    // content download for some formats
    CloudFile file = cloudFile(getContentNode(i));
    if (file != null) {
      // TODO In PLF 5.0 we need fix the download URL link in Javascript to open it
      // in new window
      return file.getLink();
    } else {
      return super.getDownloadLink(i);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isFileSupportPreview(Node data) throws Exception {
    if (data != null) {
      // code adapted from the super's method but with adding a node in to
      // context
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAuthor(int i) {
    CloudFile file = cloudFile(getContentNode(i));
    return file != null ? getUserFullName(file.getAuthor()) : super.getAuthor(i);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getDocUpdateDate(Node node) {
    CloudFile file = cloudFile(node);
    if (file != null) {
      Calendar modified = file.getModifiedDate();
      TimeZone tz = modified.getTimeZone();
      ZoneId zid = tz == null ? ZoneId.systemDefault() : tz.toZoneId();
      LocalDateTime parsedDate = LocalDateTime.ofInstant(modified.toInstant(), zid);
      return parsedDate.format(getDateTimeFormatter());
    }
    return super.getDocUpdateDate(node);
  }

  // ************* internals *************

  /**
   * Cloud file.
   *
   * @param node the node
   * @return the cloud file
   */
  protected CloudFile cloudFile(Node node) {
    if (node != null) {
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
        // TODO use global workspace, docPath
        LOG.error("Error getting cloud file node " + node, e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getUserFullName(String userName) {
    // In different cloud drives user name can be a full name or an user account
    // name, which itself can be the same as eXo user name or an email, thus we
    // do the best to show a full name and if not possible show it as is.
    if (userName.indexOf(' ') == -1 && userName.indexOf('@') == -1) {
      // Assume it's eXo user name...
      String fullName = super.getUserFullName(userName);
      if (fullName == null || fullName.length() == 0) {
        fullName = userName; // it is not
      }
      return fullName;
    } else {
      return userName;
    }
  }

}
