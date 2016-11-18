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
package org.exoplatform.clouddrive.ecms;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.clouddrive.CloudDriveStorage;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.NotCloudDriveException;
import org.exoplatform.clouddrive.ecms.filters.LocalNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIAddressBar;
import org.exoplatform.ecm.webui.component.explorer.control.UIControl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;

/**
 * PushCloudFile action in working area used to force ignored nodes inside Cloud Drive to push to cloud side.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: PushCloudFileManagerComponent.java 00000 Dec 15, 2014 pnedonosko $
 */
@ComponentConfig(
                 lifecycle = UIContainerLifecycle.class,
                 events = { @EventConfig(
                                         listeners = PushCloudFileManagerComponent.PushCloudFileActionListener.class) })
public class PushCloudFileManagerComponent extends BaseCloudDriveManagerComponent {

  protected static final Log                     LOG        = ExoLogger.getLogger(PushCloudFileManagerComponent.class);

  public static final String                     EVENT_NAME = "PushCloudFile";

  protected static final List<UIExtensionFilter> FILTERS    = Arrays.asList(new UIExtensionFilter[] { new LocalNodeFilter() });

  public static class PushCloudFileActionListener extends EventListener<PushCloudFileManagerComponent> {
    public void execute(Event<PushCloudFileManagerComponent> event) throws Exception {
      UIJCRExplorer explorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      String nodePath;
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      String wsName = explorer.getCurrentWorkspace();
      if (objectId.startsWith(wsName)) {
        try {
          nodePath = objectId.substring(wsName.length() + 1);
        } catch (IndexOutOfBoundsException e) {
          nodePath = null;
        }
      } else if (objectId.startsWith("/")) {
        nodePath = objectId;
      } else {
        nodePath = null;
      }

      if (nodePath != null) {
        Item item = explorer.getSession().getItem(nodePath);
        if (item.isNode()) {
          Node node = (Node) item;
          CloudDriveService driveService = WCMCoreUtils.getService(CloudDriveService.class);
          CloudDrive drive = driveService.findDrive(node);
          if (drive != null) {
            CloudDriveStorage storage = (CloudDriveStorage) drive;
            try {
              storage.create(node);
            } catch (DriveRemovedException e) {
              LOG.warn(e.getMessage());
            } catch (NotCloudDriveException e) {
              LOG.warn(e.getMessage());
            }
          }

          // code adopted from UIAddressBar.RefreshSessionActionListener.execute() -- effect of refresh here
          explorer.refreshExplorer();
          UIWorkingArea workingArea = explorer.getChild(UIWorkingArea.class);
          UIActionBar actionBar = workingArea.getChild(UIActionBar.class);
          UIControl control = explorer.getChild(UIControl.class);
          if (control != null) {
            UIAddressBar addressBar = control.getChild(UIAddressBar.class);
            if (addressBar != null) {
              actionBar.setTabOptions(addressBar.getSelectedViewName());
            }
          }
        } else {
          throw new IllegalArgumentException("Not node " + nodePath);
        }
      } else {
        throw new IllegalArgumentException("Cannot extract node path from objectId " + objectId);
      }
    }
  }

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String renderEventURL(boolean ajax, String name, String beanId, Parameter[] params) throws Exception {
    if (EVENT_NAME.equals(name)) {
      initContext();
    }
    return super.renderEventURL(ajax, name, beanId, params);
  }
}
