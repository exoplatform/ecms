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
package org.exoplatform.services.cms.folksonomy.impl;

import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.folksonomy.impl.TagStyleConfig.HtmlTagStyle;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

public class TagStylePlugin extends BaseComponentPlugin{

  final private static String EXO_TAG_STYLE = "exo:tagStyle" ;
  final private static String TAG_RATE_PROP = "exo:styleRange" ;
  final private static String HTML_STYLE_PROP = "exo:htmlStyle" ;
  private InitParams params_ ;
  private RepositoryService repositoryService_ ;
  private NodeHierarchyCreator nodeHierarchyCreator_ ;


  public TagStylePlugin(InitParams params, RepositoryService repoService,
      NodeHierarchyCreator nodeHierarchyCreator) throws Exception {
    params_ = params ;
    repositoryService_ = repoService ;
    nodeHierarchyCreator_ = nodeHierarchyCreator ;
  }

  /**
   * Init tag style nodes in repository.
   */
  @SuppressWarnings("unchecked")
  public void init() throws Exception {
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    TagStyleConfig tagConfig ;
    Session session = null;
    while(it.hasNext()) {
      tagConfig = (TagStyleConfig)it.next().getObject() ;
      if(tagConfig.getAutoCreatedInNewRepository()) {
        session = getSession();
        addTag(session, tagConfig) ;
        session.logout();
      } else {
        session = getSession();
        addTag(session, tagConfig) ;
        session.logout();
      }
    }
  }

  /**
   * Init tag style nodes in specific repository.
   */
  @SuppressWarnings("unchecked")
  public void init(String repository) throws Exception {
    Iterator<ObjectParameter> it = params_.getObjectParamIterator() ;
    TagStyleConfig tagConfig ;
    Session session ;
    while(it.hasNext()) {
      tagConfig = (TagStyleConfig)it.next().getObject() ;
      if(tagConfig.getAutoCreatedInNewRepository() || repository.equals(tagConfig.getRepository())) {
        session = getSession();
        addTag(session, tagConfig) ;
        session.logout();
      }
    }
  }

  /**
   * Method addTag will set value of HTML_STYLE_PROP property and value of TAG_RATE_PROP property
   * for tag style node.
   */
  private void addTag(Session session, TagStyleConfig tagConfig) throws Exception {
    String exoTagStylePath = nodeHierarchyCreator_.getJcrPath(BasePath.EXO_NEW_TAG_STYLE_PATH) ;
    Node exoTagStyleHomeNode = (Node)session.getItem(exoTagStylePath) ;
    List<HtmlTagStyle> htmlStyle4Tag = tagConfig.getTagStyleList() ;
    for(HtmlTagStyle style: htmlStyle4Tag) {
      Node tagStyleNode = Utils.makePath(exoTagStyleHomeNode,"/"+style.getName(),EXO_TAG_STYLE) ;
      tagStyleNode.setProperty(TAG_RATE_PROP,style.getTagRate()) ;
      tagStyleNode.setProperty(HTML_STYLE_PROP,style.getHtmlStyle()) ;
    }
    exoTagStyleHomeNode.save() ;
    session.save() ;
  }

  /**
  * Get session in system workspace from current repository name
  * @param repository        repository name
  * @return                  Session
  * @throws Exception
  */
 private Session getSession() throws Exception{
   ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
   ExoContainer myContainer = ExoContainerContext.getCurrentContainer();
   DMSConfiguration dmsConfiguration = (DMSConfiguration)
   myContainer.getComponentInstanceOfType(DMSConfiguration.class);
   DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration.getConfig();
   return manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace());
 }
}
