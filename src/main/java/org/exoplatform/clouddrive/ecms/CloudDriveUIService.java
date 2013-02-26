/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.clouddrive.ecms;

// TODO import com.exoplatform.cloud.multitenancy.TenantLifecycle;
import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;
import org.picocontainer.Startable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Add ImportCloudDocument button in ListView. We're doing this by hack of already stored DMS navigation.
 */
public class CloudDriveUIService implements Startable /* TODO, TenantLifecycle */ {

  private static final Log         LOG                        = ExoLogger.getLogger(CloudDriveUIService.class.getName());

  private static final String      EXO_BUTTONS                = "exo:buttons";

  public static final String       CONNECT_CLOUD_DRIVE_ACTION = "add.connect.clouddrive.action";

  private final RepositoryService  jcrService;

  private final ManageViewService  manageView;

  private final CloudDriveService  driveService;

  private final UIExtensionManager uiExtensions;

  private final List<String>       VIEWS                      = Arrays.asList("b_list-view/List",
                                                                              "e_admin-view/Admin",
                                                                              "c_icon-view/Icons");

  public CloudDriveUIService(RepositoryService repoService,
                             CloudDriveService driveService,
                             UIExtensionManager uiExtensions,
                             ManageViewService manageView) {
    this.jcrService = repoService;
    this.manageView = manageView;
    this.driveService = driveService;
    this.uiExtensions = uiExtensions;
  }

  public void prepareViews() {
    // find all Cloud Drive actions configured for action bar 
    List<String> cdActions = new ArrayList<String>();
    for (UIExtension ext: uiExtensions.getUIExtensions(ManageViewService.EXTENSION_TYPE)) {
      if (ConnectCloudDriveManagerComponent.class.isAssignableFrom(ext.getComponent())) {
        cdActions.add(ext.getName());
      }
    }
    
    SessionProvider jcrSessions = SessionProvider.createSystemProvider();
    try {
      Session session = jcrSessions.getSession("dms-system", jcrService.getCurrentRepository());
      for (String view : VIEWS) {
        Node listNode = (Node) session.getItem(manageView.getViewByName(view, jcrSessions).getPath());
        StringBuilder newActions = new StringBuilder();
        if (listNode.hasProperty(EXO_BUTTONS)) {
          String[] actions = listNode.getProperty(EXO_BUTTONS).getString().split(";");
          for (int i=0; i<actions.length; i++) {
            newActions.append(actions[i].trim());
            if (i < actions.length - 1) {
              newActions.append(';');
              newActions.append(' ');
            }
          }
        }
        
        for (String cda : cdActions) {
          // action not found, add it
          // doing some trick to fix the string: make first char lowercase
          char[] ac = cda.toCharArray();
          if (ac.length > 0) {
            ac[0] = Character.toLowerCase(ac[0]);
          }
          if (newActions.indexOf(new String(ac)) < 0) {
            newActions.append(';');
            newActions.append(' ');
            newActions.append(ac);  
          }
        }
        
        listNode.setProperty(EXO_BUTTONS, newActions.toString());
      }
      session.save();
      LOG.info("Connect Cloud Drive Action added successfully.");
    } catch (Exception e) {
      LOG.error("Error adding Connect Cloud Drive Action: " + e.getMessage(), e);
    } finally {
      jcrSessions.close();
    }
  }

  @Override
  public void start()
  {
    prepareViews();
  }

  @Override
  public void stop() {
    // nothing
  }

  // TODO @Override
  public void tenantStarted(String tenantName)
  {
    try
    {
      jcrService.setCurrentRepositoryName(tenantName);
      prepareViews();
    } catch (RepositoryConfigurationException e) {
      LOG.error("Error adding Connect Cloud Drive Action - cannot set current repository name '" + tenantName + "': " + e.getMessage(), e);
    }
  }

  // TODO @Override
  public void tenantStopped(String tenantName)
  {
    // Nothing
  }
}
