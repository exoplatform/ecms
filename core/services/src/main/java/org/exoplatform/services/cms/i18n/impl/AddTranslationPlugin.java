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
package org.exoplatform.services.cms.i18n.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.deployment.plugins.TranslationDeploymentDescriptor;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.portal.artifacts.CreatePortalPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          dongpd@exoplatform.com
 * Oct 12, 2012
 */
public class AddTranslationPlugin  extends CreatePortalPlugin {
  
  /**
   * Logger.
   */
  private static final Log LOG = ExoLogger.getLogger(AddTranslationPlugin.class.getName());
  
  private static final Pattern FILE_EXPLORER_URL_SYNTAX = Pattern.compile("([^:/]+):(/.*)");

  /**
   * The init params.
   */
  private InitParams initParams;

  /**
   * The Multilanguage service.
   */
  private MultiLanguageService languageService;

  /**
   * Instantiates a new AddTranslationPlugin plugin.
   * 
   * @param initParams
   *          the init params
   * @param languageService
   *          Multilanguage service
   */
  public AddTranslationPlugin(InitParams initParams,
                              ConfigurationManager configurationManager,
                              RepositoryService repositoryService,
                              MultiLanguageService languageService) {
    super(initParams, configurationManager, repositoryService);
    this.initParams = initParams;
    this.languageService = languageService;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void deployToPortal(SessionProvider sessionProvider, String portalName) throws Exception {
    Iterator iterator = initParams.getObjectParamIterator();
    TranslationDeploymentDescriptor translationDescriptor = null;
    while (iterator.hasNext()) {
      ObjectParameter objectParameter = (ObjectParameter) iterator.next();
      translationDescriptor = (TranslationDeploymentDescriptor) objectParameter.getObject();
      List<String> translationPaths = translationDescriptor.getTranslationNodePaths();
      boolean isOverrideExistence = translationDescriptor.isOverrideExistence();
      
      // Replace {portalName} with specific portal
      List<String> translationRealPaths = new ArrayList<String>();
      if (portalName != null && portalName.length() > 0) {
        for (String path : translationPaths) {
          translationRealPaths.add(StringUtils.replace(path, "{portalName}", portalName));
        }
      }
      
      // Add translation
      int numOfTrans = translationRealPaths.size();
      for (int i = 0; i < numOfTrans; i++) {
        Node currNode = this.getNodeByPath(translationRealPaths.get(i));
        if (currNode == null) continue;
        for (int j = i + 1; j < numOfTrans; j++) {
          Node targetTranslationNode = this.getNodeByPath(translationRealPaths.get(j));
          if (targetTranslationNode == null) continue;
          try {
            languageService.addLinkedLanguage(currNode, targetTranslationNode, isOverrideExistence);
            languageService.addLinkedLanguage(targetTranslationNode, currNode, isOverrideExistence);
          } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
              LOG.error("Add translation " + translationRealPaths.get(j) + " for " + translationRealPaths.get(i) + " FAILED at "
                  + new Date().toString() + "\n", e);
            }
          }
        }
      }
    }
  }
  
  /**
   * Get node by node path.
   * 
   * @param nodePath node path of specific node with syntax [workspace:node path]
   * @return Node of specific node nath
   * @throws RepositoryException 
   * @throws Exception
   */
  private Node getNodeByPath(String nodePath) throws RepositoryException
  {
    try
    {
      Matcher matcher = FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
      if (!matcher.find()) return null;
      String wsName = matcher.group(1);
      nodePath = matcher.group(2);
      Session session = WCMCoreUtils.getSystemSessionProvider().getSession(wsName, WCMCoreUtils.getRepository());
      return (Node)session.getItem(nodePath);
    }
    catch (PathNotFoundException e)
    {
      return null;
    }
    catch (NoSuchWorkspaceException e)
    {
      return null;
    }
  }
}
