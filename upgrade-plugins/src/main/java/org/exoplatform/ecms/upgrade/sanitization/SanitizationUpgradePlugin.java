
/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.ecms.upgrade.sanitization;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Mar 9, 2013
 * 8:46:00 AM  
 */
public class SanitizationUpgradePlugin extends UpgradeProductPlugin {

  private DMSConfiguration dmsConfiguration_;
  private RepositoryService repoService_;
  private ManageViewService viewService_;
  private static final Log LOG = ExoLogger.getLogger(SanitizationUpgradePlugin.class.getName());
  
  public SanitizationUpgradePlugin(RepositoryService repoService, DMSConfiguration dmsConfiguration, 
          ManageViewService viewService, InitParams initParams) {
    super(initParams);
    repoService_ = repoService;
    dmsConfiguration_ = dmsConfiguration;
    viewService_ = viewService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getName() + ".............");
    }
    //Migrate data for all user views
    migrateViews();
    
    //Migrate data for view templates
    migrateViewTemplates();
    
    //Migrate data for all drives
    migrateDrives();

    /**
     * Migrate portlet preferences which contains the "/sites content/live" path to "/sites"
     */
    migratePortletPreferences();

  }
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    //return true anly for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
  }  

  private void migrateViews() {
    try {
      Session session = WCMCoreUtils.getSystemSessionProvider().getSession(dmsConfiguration_.getConfig().getSystemWorkspace(),
                                                                                  repoService_.getCurrentRepository());
      String[] oldViewTemplates = {"ListView", "ContentView", "ThumbnailsView", 
              "IconView", "TimelineView", "CoverFlow", "SystemView", "SlideShowView"};
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start migrate data for all user views=====");
      }
      String statement = "SELECT * FROM exo:view ORDER BY exo:name DESC";
      QueryResult result = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL).execute();
      NodeIterator nodeIter = result.getNodes();
      while(nodeIter.hasNext()) {
        Node viewNode = nodeIter.nextNode();
        String template = viewNode.getProperty("exo:template").getString();
        String templateName = StringUtils.substringAfterLast(template, "/");
        for(String oldTemp : oldViewTemplates) {
          if(templateName.equals(oldTemp)) {
            if(isContainOldView(viewNode.getName())) {
              if (LOG.isInfoEnabled()) {
                LOG.info("=====Removing view '"+viewNode.getName()+"'=====");
              }
              viewNode.remove();
            } else {
              String newTemplate = "List";
              if(templateName.equals("ListView")) newTemplate = template.replace(templateName, "List");
              else if(templateName.equals("ContentView")) newTemplate = template.replace(templateName, "Content");
              else if(templateName.equals("ThumbnailsView")) newTemplate = template.replace(templateName, "Thumbnails");
              else if(templateName.equals("IconView")) newTemplate = template.replace(templateName, "Thumbnails");
              else if(templateName.equals("TimelineView")) newTemplate = template.replace(templateName, "List");
              else if(templateName.equals("CoverFlow")) newTemplate = template.replace(templateName, "Thumbnails");
              else if(templateName.equals("SystemView")) newTemplate = template.replace(templateName, "List");
              else if(templateName.equals("SlideShowView")) newTemplate = template.replace(templateName, "Thumbnails");
              if (LOG.isInfoEnabled()) {
                LOG.info("=====Modifying view '"+viewNode.getName()+"'=====");
              }
              viewNode.setProperty("exo:template", newTemplate);
            }
          }
        }
      }
      session.save();
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Completed the migration data for user views=====");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrate views", e);
      }
    }
  }
  
  private boolean isContainOldView(String viewName) {
    String[] oldViewNames = {"timeline-view", "list-view", "icon-view", "admin-view", "simple-view", "slide-show", "cover-flow", 
          "anonymous-view", "taxonomy-list", "taxonomy-icons", "system-view", "wcm-view", "authoring-view", "wcm-category-view"};
    for(String vName : oldViewNames) {
      if(viewName.contains(vName)) return true;
    }
    return false;
  }
    
  
  private void migrateViewTemplates() {
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start migrate data for all user views template=====");
      }
      Session session = WCMCoreUtils.getSystemSessionProvider().getSession(dmsConfiguration_.getConfig().getSystemWorkspace(),
              repoService_.getCurrentRepository());
      String[] oldViewTemplates = {"ListView", "ContentView", "ThumbnailsView", 
              "IconView", "TimelineView", "CoverFlow", "SystemView", "SlideShowView"};
      List<Node> templates = 
              viewService_.getAllTemplates(BasePath.ECM_EXPLORER_TEMPLATES, WCMCoreUtils.getSystemSessionProvider());
      for(Node template : templates) {
        for(String oldViewTemp : oldViewTemplates) {
          if(template.getName().equals(oldViewTemp)) {
            LOG.info(" * Removing the old view template " +template.getName()+ "");
            template.remove();
            break;
          }
        }
      }
      session.save();
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Completed the migration data for user views template=====");
      }   
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrate view templates", e);
      }
    }
  }
  
  private void migrateDrives() {
    if (LOG.isInfoEnabled()) {
      LOG.info("=====Start migrate data for drives=====");
    }
    try {
      Session session = WCMCoreUtils.getSystemSessionProvider().getSession(dmsConfiguration_.getConfig().getSystemWorkspace(),
                                                                                  repoService_.getCurrentRepository());
      String statement = "SELECT * FROM exo:drive ORDER BY exo:name DESC";
      QueryResult result = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL).execute();
      NodeIterator nodeIter = result.getNodes();
      while(nodeIter.hasNext()) {
        Node drive = nodeIter.nextNode();
        if (LOG.isInfoEnabled()) {
          LOG.info(" * Migrating the drive " +drive.getName()+ "");
        }        
        String path = drive.getProperty("exo:path").getString();
        if(path.startsWith("/sites content/live")) {
          drive.setProperty("exo:path", path.replace("/sites content/live", "/sites"));
          drive.setProperty("exo:views", "Web");
        } else if(path.equals("/Groups${groupId}")) {
          path = path.replace("/Groups${groupId}", "/Groups${groupId}/Documents");
          drive.setProperty("exo:path", path);
          drive.setProperty("exo:views", "List, Icons");
        } else if(drive.getName().equals("collaboration") && path.equals("/")) {
          drive.remove();
        } else if(drive.getName().equals("Trash") && path.equals("/Trash")) {
          drive.setProperty("exo:views", "Admin");
        } else if(drive.getName().equals("acme-category")) {
          drive.remove();
        } else if(drive.getName().equals("Private") || (drive.getName().equals("Public"))) {
          drive.remove();
        } else if(drive.getProperty("exo:workspace").getString().equals("dms-system") && path.equals("/")) {
          drive.remove();
        } else {
          String views = drive.getProperty("exo:views").getString();
          String[] arrView = {};
          if(views.indexOf(",") > -1) {
            arrView = views.split(","); 
          } else {
            arrView = new String[] {views};
          }
          StringBuilder strViews = new StringBuilder();
          for(String view : arrView) {
            if(strViews.length() > 0) strViews.append(", ");
            view = view.trim();
            if(view.contains("timeline-view")) {
              strViews.append("List");
            } else if(view.contains("list-view")) { 
              strViews.append("List");
            } else if(view.contains("icon-view")) {
              strViews.append("Icons");
            } else if(view.contains("cover-flow")) {
              strViews.append("Icons");
            } else if(view.contains("admin-view")) {
              strViews.append("Admin");
            } else if(view.contains("simple-view")) {
              strViews.append("Icons");
            } else if(view.contains("slide-show")) {
              strViews.append("Icons");
            } else if(view.contains("anonymous-view")) {
              strViews.append("Icons");
            } else if(view.contains("taxonomy-list")) {
              strViews.append("Categories");
            } else if(view.contains("taxonomy-icons")) {
              strViews.append("Categories");
            } else if(view.contains("system-view")) {
              strViews.append("List");
            } else if(view.contains("wcm-view")) {
              strViews.append("Web");
            } else if(view.contains("authoring-view")) {
              strViews.append("Web");
            } else if(view.contains("wcm-category-view")) {
              strViews.append("Categories");
            } else {
              strViews.append(view);
            }
          }
          drive.setProperty("exo:views", strViews.toString());
        }
      }
      session.save();
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Completed the migration data for drives=====");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrate drives", e);
      }
    } 
  }

  /**
   * Migrate portlet preferences which contains the "/sites content/live" path to "/sites"
   */
  private void migratePortletPreferences() {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getName() + ".............");
    }
    Session session;
    try {
      session = WCMCoreUtils.getSystemSessionProvider().getSession("portal-system",
          repoService_.getCurrentRepository());
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start migrate old preferences=====");
      }
      String statement = "select * from mop:portletpreference  where mop:value like '%/sites content/live/%'";
      QueryResult result = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL).execute();
      NodeIterator nodeIter = result.getNodes();
      while(nodeIter.hasNext()) {
        Node preferenceNode = nodeIter.nextNode();
        String oldPath =preferenceNode.getProperty("mop:value").getValues()[0].getString();
        String newPath= StringUtils.replace(oldPath, "/sites content/live/", "/sites/");

        preferenceNode.setProperty("mop:value", new String[]{newPath});
        session.save();
      }

      if (LOG.isInfoEnabled()) {
        LOG.info("===== Portlet preference upgrade completed =====");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating old preferences: ", e);
      }
    }


  }

}
