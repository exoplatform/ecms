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
package org.exoplatform.services.cms.drives.impl;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class ManageDrivePlugin extends BaseComponentPlugin {

  private static String WORKSPACE = "exo:workspace" ;
  private static String PERMISSIONS = "exo:accessPermissions" ;
  private static String VIEWS = "exo:views" ;
  private static String ICON = "exo:icon" ;
  private static String PATH = "exo:path" ;
  private static String VIEW_REFERENCES = "exo:viewPreferences" ;
  private static String VIEW_NON_DOCUMENT = "exo:viewNonDocument" ;
  private static String VIEW_SIDEBAR = "exo:viewSideBar" ;
  private static String SHOW_HIDDEN_NODE = "exo:showHiddenNode" ;
  private static String ALLOW_CREATE_FOLDER = "exo:allowCreateFolders" ;

  private RepositoryService repositoryService_;
  private NodeHierarchyCreator nodeHierarchyCreator_;
  private InitParams params_ ;
  private DMSConfiguration dmsConfiguration_;
  private static final Log LOG  = ExoLogger.getLogger(ManageDrivePlugin.class);

  public ManageDrivePlugin(RepositoryService repositoryService, InitParams params,
      NodeHierarchyCreator nodeHierarchyCreator, DMSConfiguration dmsConfiguration) throws Exception {
    repositoryService_ = repositoryService;
    nodeHierarchyCreator_ = nodeHierarchyCreator ;
    params_ = params ;
    dmsConfiguration_ = dmsConfiguration;
  }

  /**
   * Init all drive data
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void init() throws Exception {
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    while(it.hasNext()){
      DriveData data = (DriveData)it.next().getObject() ;
      try{
        Session session  = getSession();
        addDrive(data, session) ;
        session.logout();
      }catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
        if (LOG.isWarnEnabled()) {
          LOG.warn(" ==> Can not init drive '"+ data.getName()
            +"' in repository '" + repositoryService_.getCurrentRepository().getConfiguration().getName() + "'");
        }
      }

    }
  }

  /**
   * Init data with specified repository
   * @param repository
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void init(String repository) throws Exception {
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    DriveData data = null ;
    Session session = null ;
    while(it.hasNext()){
      data = (DriveData)it.next().getObject() ;
      try {
        session = getSession() ;
        addDrive(data, session) ;
        session.logout();
      } catch(Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
  }

  /**
   * Register new drive node with specified DriveData
   * @param data Drive data
   * @param session
   * @throws Exception
   */
  private void addDrive(DriveData data, Session session) throws Exception {
    String drivesPath = nodeHierarchyCreator_.getJcrPath(BasePath.EXO_DRIVES_PATH);
    Node driveHome = (Node)session.getItem(drivesPath) ;
    Node driveNode = null ;
    if(!driveHome.hasNode(data.getName())){
      driveNode = driveHome.addNode(data.getName(), "exo:drive");
      driveNode.setProperty(WORKSPACE, data.getWorkspace()) ;
      driveNode.setProperty(PERMISSIONS, data.getPermissions()) ;
      driveNode.setProperty(PATH, data.getHomePath()) ;
      driveNode.setProperty(VIEWS, data.getViews()) ;
      driveNode.setProperty(ICON, data.getIcon()) ;
      driveNode.setProperty(VIEW_REFERENCES, Boolean.toString(data.getViewPreferences())) ;
      driveNode.setProperty(VIEW_NON_DOCUMENT, Boolean.toString(data.getViewNonDocument())) ;
      driveNode.setProperty(VIEW_SIDEBAR, Boolean.toString(data.getViewSideBar())) ;
      driveNode.setProperty(SHOW_HIDDEN_NODE, Boolean.toString(data.getShowHiddenNode())) ;
      driveNode.setProperty(ALLOW_CREATE_FOLDER, data.getAllowCreateFolders()) ;
      driveHome.save() ;
      session.save() ;
    }
  }

  /**
   * Return Session object with specified repository name
   * @return Session object
   * @throws Exception
   */
  private Session getSession() throws Exception {
    ManageableRepository manaRepository = repositoryService_.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    return manaRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace());
  }
}
