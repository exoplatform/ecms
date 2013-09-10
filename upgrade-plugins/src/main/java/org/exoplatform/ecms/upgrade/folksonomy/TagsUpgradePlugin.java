/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.ecms.upgrade.folksonomy;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          vuna@exoplatform.com
 * Mar 19, 2013  
 */
public class TagsUpgradePlugin extends UpgradeProductPlugin {
  
  public static final String PUBLIC_TAG_NODE_PATH = "exoPublicTagNode";
  public static final String COLLABORATION = "collaboration";
  
  private NewFolksonomyService folksonomyService_;
  private NodeHierarchyCreator nodeCreator_;
  private LinkManager linkManager_;
  private static final Log log = ExoLogger.getLogger(TagsUpgradePlugin.class.getName());

  public TagsUpgradePlugin(NewFolksonomyService folksonomyService,
                            NodeHierarchyCreator nodeCreator,
                            LinkManager linkManager,
                            InitParams initParams) {
    super(initParams);
    this.folksonomyService_ = folksonomyService;
    this.nodeCreator_ = nodeCreator;
    this.linkManager_ = linkManager;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    SessionProvider sessionProvider = null;
    try {
      //create temp folder
      sessionProvider = SessionProvider.createSystemProvider();
      Session session = sessionProvider.getSession(COLLABORATION, WCMCoreUtils.getRepository());
      Node tempFolder = session.getRootNode().addNode("temp");
      session.save();
      //iterate though all tags
      String publicTagNodePath = nodeCreator_.getJcrPath(PUBLIC_TAG_NODE_PATH);
      for (NodeIterator oldTagIter = ((Node)session.getItem(publicTagNodePath)).getNodes();
           oldTagIter.hasNext();) {
        Node oldTag = oldTagIter.nextNode();
        try {
          String tagName = oldTag.getName();
          if (log.isInfoEnabled()) {
            log.info("Migrating tag '" + tagName + "'...");
          }
          //move old to temp folder
          session.move(oldTag.getPath(), tempFolder.getPath() + '/' + tagName);
          session.save();
          //get all symlinks of old tag, add tag (new tag) for these documents;
          for (NodeIterator iter = tempFolder.getNode(tagName).getNodes();iter.hasNext();) {
            Node link = iter.nextNode();
            if (linkManager_.isLink(link)) {
              Node targetDoc = linkManager_.getTarget(link, true);
              folksonomyService_.addPublicTag(publicTagNodePath, new String[]{tagName}, targetDoc, COLLABORATION);
            }
          }
          tempFolder.getNode(tagName).remove();
          session.save();
        } catch (Exception e) {
          if (log.isErrorEnabled()) {
            log.error("Can not migrate tag '" + oldTag.getName() + "': ", e);
          }
        }
      }
      tempFolder.remove();
      session.save();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating tags: ", e);        
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
    if (log.isInfoEnabled()) {
      log.info("Finish " + this.getClass().getName() + ".............");
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    // --- return true only for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
  }

}
