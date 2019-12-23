
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
package org.exoplatform.ecm.webui.component.explorer.documents;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.cms.views.ViewConfig.Tab;
import org.exoplatform.services.cms.views.impl.ManageViewPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;

/**
 * Customize existing view of ECMS Document Explorer.
 * It allows to add buttons in existing tabs of Document Explorer views.
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CustomizeViewPlugin.java 00000 Mar 2, 2016 pnedonosko $
 * 
 */
public class CustomizeViewPlugin extends ManageViewPlugin {

  /** The params. */
  protected final InitParams           params;

  /** The repository service. */
  protected final RepositoryService    repositoryService;

  /** The node hierarchy creator. */
  protected final NodeHierarchyCreator nodeHierarchyCreator;

  /** The cservice. */
  protected final ConfigurationManager cservice;

  /** The dms configuration. */
  protected final DMSConfiguration     dmsConfiguration;

  /** The template service. */
  protected final TemplateService      templateService;

  /** The configured template. */
  protected final Set<String>          configuredTemplate = new HashSet<String>();

  /** The configured views. */
  protected final Set<String>          configuredViews    = new HashSet<String>();

  /**
   * Instantiates a new customize view plugin.
   *
   * @param repositoryService the repository service
   * @param params the params
   * @param cservice the cservice
   * @param nodeHierarchyCreator the node hierarchy creator
   * @param dmsConfiguration the dms configuration
   * @param templateService the template service
   * @throws Exception the exception
   */
  public CustomizeViewPlugin(RepositoryService repositoryService,
                             InitParams params,
                             ConfigurationManager cservice,
                             NodeHierarchyCreator nodeHierarchyCreator,
                             DMSConfiguration dmsConfiguration,
                             TemplateService templateService)
      throws Exception {
    super(repositoryService, params, cservice, nodeHierarchyCreator, dmsConfiguration);

    this.params = params;
    this.repositoryService = repositoryService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.cservice = cservice;
    this.dmsConfiguration = dmsConfiguration;
    this.templateService = templateService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getConfiguredTemplates() {
    // return empty set as we do only customize the templates
    return Collections.emptySet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getConfiguredViews() {
    // return empty set as we do only customize the views
    return Collections.emptySet();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() throws Exception {
    importCustomizedViews();
  }

  /// ****** internals ******

  /**
   * Import customized views.
   *
   * @throws Exception the exception
   */
  private void importCustomizedViews() throws Exception {
    configuredTemplate.clear();
    configuredViews.clear();

    Iterator<ObjectParameter> objectsIter = params.getObjectParamIterator();
    String viewsPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_VIEWS_PATH);
    // String templatesPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_VIEWTEMPLATES_PATH);
    // String warViewPath = predefinedViewsLocation +
    // templatesPath.substring(templatesPath.lastIndexOf("exo:ecm") + 7);

    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration.getConfig();
    Session session = manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace());
    Node viewHomeNode = (Node) session.getItem(viewsPath);

    while (objectsIter.hasNext()) {
      Object object = objectsIter.next().getObject();
      if (object instanceof ViewConfig) {
        ViewConfig viewObject = (ViewConfig) object;
        String viewNodeName = viewObject.getName();
        if (viewHomeNode.hasNode(viewNodeName)) {
          configuredViews.add(viewNodeName);
          Node viewNode = customizeView(viewHomeNode,
                                        viewNodeName,
                                        viewObject.getPermissions(),
                                        viewObject.isHideExplorerPanel(),
                                        viewObject.getTemplate());
          for (Tab tab : viewObject.getTabList()) {
            customizeTab(viewNode, tab.getTabName(), tab.getButtons());
          }
        }
      }
    }
    session.save();
    session.logout();
  }

  /**
   * Customize view.
   *
   * @param viewManager the view manager
   * @param viewName the view name
   * @param permissions the permissions
   * @param hideExplorerPanel the hide explorer panel
   * @param template the template
   * @return the node
   * @throws Exception the exception
   */
  protected Node customizeView(Node viewManager,
                               String viewName,
                               String permissions,
                               Boolean hideExplorerPanel,
                               String template) throws Exception {
    Node viewNode = viewManager.getNode(viewName);
    return viewNode;
  }

  /**
   * Customize tab.
   *
   * @param view the view
   * @param tabName the tab name
   * @param buttons the buttons
   * @throws Exception the exception
   */
  protected void customizeTab(Node view, String tabName, String buttons) throws Exception {
    buttons = buttons.trim();
    if (view.hasNode(tabName) && buttons.length() > 0) {
      Node tab = view.getNode(tabName);
      Set<String> addButtons = new LinkedHashSet<String>();
      for (String action : buttons.split(";")) {
        action = action.trim();
        if (action.length() > 0) {
          addButtons.add(action);
        }
      }

      if (tab.hasProperty("exo:buttons")) {
        Property exoButtons = tab.getProperty("exo:buttons");
        String newButtons = mergeButtons(exoButtons.getString(), addButtons);
        exoButtons.setValue(newButtons);
      }
      view.save();
    }
  }

  /**
   * Read all buttons from given node, in buttons property, to given string builder.
   *
   * @param buttons the buttons
   * @param addButtons the add buttons
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected String mergeButtons(String buttons, Set<String> addButtons) throws RepositoryException {
    StringBuilder buttonStr = new StringBuilder();
    // first add customized buttons
    for (String action : addButtons) {
      if (buttons.indexOf(action) < 0) { // but add only if not already exists
        if (buttonStr.length() > 0) {
          buttonStr.append(';');
          buttonStr.append(' ');
        }
        buttonStr.append(action);
      }
    }
    if (buttonStr.length() > 0 && buttons.length() > 0) {
      buttonStr.append(';');
      buttonStr.append(' ');
    }
    // then all existing ones (we assume this string already well formatted)
    buttonStr.append(buttons);
    return buttonStr.toString();
  }
}
