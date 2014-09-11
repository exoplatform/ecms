
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
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * This upgrade plugin will be used to migrate all the old data to the new one which related to
 * the changing of Sanitization in PLF4
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

    /* Migrate data for all user views */
    migrateViews();

    /* Migrate data for view templates */
    migrateViewTemplates();

    /* Migrate data for all drives */
    migrateDrives();

    /**
     * Migrate portlet preferences which contains the "/sites content/live" path to "/sites"
     */
    migratePortletPreferences();

    /**
     * Migrate exo:links which still contains "/sites content/live" in its properties
     */
    migrateLinkInContents();

    /**
     * Migrate activities which contains "/sites content/live" in the url
     */
    migrateSocialActivities();

    /**
     * Migrate taxonomy actions which contains some properties which still point to old path related to "/sites content/live"
     */
    migrateTaxonomyAction();

    /**
     * Migrate preference 'Drive name' of site explorer portlet which should be changed to Collaboration instead of collaboration"
     */
    migrateDriveNameOfPortletPreferences();

  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    //return true anly for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
  }

  /* Migrate data for all user views */
  private void migrateViews() {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession(dmsConfiguration_.getConfig().getSystemWorkspace(),
                                                   repoService_.getCurrentRepository());
      String[] oldViewTemplates = {"ListView", "ContentView", "ThumbnailsView", 
          "IconView", "TimelineView", "CoverFlow", "SystemView", "SlideShowView"};
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start migrate data for all user views=====");
      }
      Node views = (Node)session.getItem("/exo:ecm/views/userviews");
      NodeIterator nodeIter = views.getNodes();
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
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
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

  /* Migrate data for view templates */
  private void migrateViewTemplates() {
    SessionProvider sessionProvider = null;
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start migrate data for all user views template=====");
      }
      sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession(dmsConfiguration_.getConfig().getSystemWorkspace(),
                                                   repoService_.getCurrentRepository());
      String[] oldViewTemplates = {"ListView", "ContentView", "ThumbnailsView", 
          "IconView", "TimelineView", "CoverFlow", "SystemView", "SlideShowView"};
      List<Node> templates = viewService_.getAllTemplates(BasePath.ECM_EXPLORER_TEMPLATES, sessionProvider);
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
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  /* Migrate data for all drives */
  private void migrateDrives() {
    if (LOG.isInfoEnabled()) {
      LOG.info("=====Start migrate data for drives=====");
    }
    SessionProvider sessionProvider = null;    
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession(dmsConfiguration_.getConfig().getSystemWorkspace(),
                                                   repoService_.getCurrentRepository());
      Node drives = (Node)session.getItem("/exo:ecm/exo:drives");
      NodeIterator nodeIter = drives.getNodes();
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
        } else if("Personal Documents".equals(drive.getName())) {
          drive.setProperty("exo:viewNonDocument", false);
        } else if(drive.getProperty("exo:workspace").getString().equals("dms-system") && path.equals("/")) {
          drive.remove();
        } else {
          String views = drive.getProperty("exo:views").getString();
          String[] arrView;
          if(views.contains(",")) {
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
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  /**
   * Migrate portlet preferences which contains the "/sites content/live" path to "/sites"
   */
  private void migratePortletPreferences() {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession("portal-system",
                                                   repoService_.getCurrentRepository());
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start migrate old preferences=====");
      }
      String statement = "select * from mop:portletpreference where mop:value like '%/sites content/live/%'";
      QueryResult result = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL).execute();
      NodeIterator nodeIter = result.getNodes();
      while(nodeIter.hasNext()) {
        Node preferenceNode = nodeIter.nextNode();
        String oldPath =preferenceNode.getProperty("mop:value").getValues()[0].getString();
        String newPath= StringUtils.replace(oldPath, "/sites content/live/", "/sites/");

        preferenceNode.setProperty("mop:value", new String[]{newPath});
      }
      session.save();
      if (LOG.isInfoEnabled()) {
        LOG.info("===== Portlet preference upgrade completed =====");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating old preferences: ", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  /**
   * Migrate exo:links which still contains "/sites content/live" in its properties
   */
  private void migrateLinkInContents() {
    SessionProvider sessionProvider = null;    
    try {
      sessionProvider = SessionProvider.createSystemProvider();	
      Session session = sessionProvider.getSession("collaboration", repoService_.getCurrentRepository());
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start migrate old link in contents=====");
      }
      String statement = "select * from exo:linkable where exo:links like '%/sites content/live/%'";
      QueryResult result = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL).execute();
      NodeIterator nodeIter = result.getNodes();
      while(nodeIter.hasNext()) {
        Node contentNode = nodeIter.nextNode();
        if (LOG.isInfoEnabled()) {
          LOG.info("=====Migrating content '"+contentNode.getPath()+"' =====");
        }
        Value[] oldLinks = contentNode.getProperty("exo:links").getValues();
        List<String> newLinks = new ArrayList<String>();
        for(Value linkValue : oldLinks) {
          newLinks.add(StringUtils.replace(linkValue.getString(), "/sites content/live/", "/sites/"));
        }
        contentNode.setProperty("exo:links", newLinks.toArray(new String[newLinks.size()]));
      }
      session.save();
      if (LOG.isInfoEnabled()) {
        LOG.info("===== Migrate content links completed =====");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating content links: ", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  /**
   * Migrate activities which contains "/sites content/live" in the url
   */
  private void migrateSocialActivities() {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession("social", repoService_.getCurrentRepository());

      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start migrate soc:url for all activities=====");
      }
      String statement = "SELECT * FROM soc:activity WHERE soc:url like '%/sites content/live/%'";
      QueryResult result = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL).execute();
      NodeIterator nodeIter = result.getNodes();
      while(nodeIter.hasNext()) {
        Node activity = nodeIter.nextNode();
        String nodeUrl = activity.getProperty("soc:url").getString();
        activity.setProperty("soc:url", StringUtils.replace(nodeUrl, "/sites content/live/", "/sites/"));
      }
      session.save();
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Completed the migration for soc:url=====");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating activities: ", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  /**
   * Migrate taxonomy actions which contains some properties which still point to old path related to "/sites content/live"
   */
  private void migrateTaxonomyAction() {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      String wsName = repoService_.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      Session session = sessionProvider.getSession(wsName, repoService_.getCurrentRepository());

      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start to migrate taxonomy actions=====");
      }
      String statement = 
          "select * from exo:taxonomyAction where (exo:targetPath like '%/sites content/live/%' or exo:storeHomePath like '%/sites content/live/%')";
      QueryResult result = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL).execute();
      NodeIterator nodeIter = result.getNodes();
      while(nodeIter.hasNext()) {
        Node taxoAction = nodeIter.nextNode();
        String targetPath = taxoAction.getProperty("exo:targetPath").getString();
        String homePath = taxoAction.getProperty("exo:storeHomePath").getString();
        taxoAction.setProperty("exo:targetPath", StringUtils.replace(targetPath, "/sites content/live/", "/sites/"));
        taxoAction.setProperty("exo:storeHomePath", StringUtils.replace(homePath, "/sites content/live/", "/sites/"));
      }
      session.save();
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Completed the migration for taxonomy action=====");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating for taxonomy actions: ", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  /**
   * Migrate preference 'Drive name' of site explorer portlet which should be changed to Collaboration instead of collaboration"
   */
  private void migrateDriveNameOfPortletPreferences() {
    SessionProvider sessionProvider = null;    
    try {
      sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession("portal-system",
                                                   repoService_.getCurrentRepository());
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start migrate portlet preferences drive name=====");
      }
      String statement = "select * from mop:portletpreference where exo:name='mop:driveName' and mop:value='collaboration'";
      QueryResult result = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL).execute();
      NodeIterator nodeIter = result.getNodes();
      while(nodeIter.hasNext()) {
        Node preferenceNode = nodeIter.nextNode();
        preferenceNode.setProperty("mop:value", new String[]{"Collaboration"});
      }
      session.save();
      if (LOG.isInfoEnabled()) {
        LOG.info("===== Preference drive name have been upgrade completed =====");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating preferences drive name: ", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }  
}
