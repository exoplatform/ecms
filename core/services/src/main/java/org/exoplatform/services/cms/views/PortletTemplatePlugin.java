/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.cms.views;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 15, 2008
 */
public class PortletTemplatePlugin extends BaseComponentPlugin{

  private ConfigurationManager configurationManager;
  private InitParams initParams;
  private String portletName;

  /**
   * Instantiates a new portlet template plugin.
   *
   * @param initParams the init params
   * @param configurationManager the configuration manager
   */
  public PortletTemplatePlugin(InitParams initParams, ConfigurationManager configurationManager) {
    this.initParams = initParams;
    this.configurationManager = configurationManager;
    portletName = this.initParams.getValueParam("portletName").getValue();
  }

  /**
   * Gets the portlet name.
   *
   * @return the portlet name
   */
  public String getPortletName() { return portletName; }

  /**
   * Retrieves all portlet template config for a portlet in plugin
   *
   * @return the portlet template configs
   *
   * @throws Exception the exception
   */
  public List<PortletTemplateConfig> getPortletTemplateConfigs() throws Exception {
    List<PortletTemplateConfig> list = new ArrayList<PortletTemplateConfig>();
    String configPath = initParams.getValueParam("portlet.template.path").getValue();
    Iterator<ObjectParameter> iterator = initParams.getObjectParamIterator();
    for(;iterator.hasNext();) {
      Object obj = iterator.next().getObject();
      PortletTemplateConfig config = PortletTemplateConfig.class.cast(obj);
      String templateFile = configPath + "/" + config.getCategory() + "/" + config.getTemplateName();
      InputStream input = configurationManager.getInputStream(templateFile);
      String templateData = IOUtil.getStreamContentAsString(input);
      config.setTemplateData(templateData);
      list.add(config);
    }
    return list;
  }

  /**
   * The Class PortletTemplateConfig.
   */
  public static class PortletTemplateConfig {
    private String category;
    private ArrayList<String> accessPermissions;
    private ArrayList<String> editPermissions;
    private String templateName;
    private String title;
    private String templateData;

    /**
     * Gets the category.
     *
     * @return the category
     */
    public String getCategory() {
      return category;
    }

    /**
     * Sets the category.
     *
     * @param category the new category
     */
    public void setCategory(String category) {
      this.category = category;
    }

    /**
     * Gets the access permissions.
     *
     * @return the access permissions
     */
    public ArrayList<String> getAccessPermissions() {
      return accessPermissions;
    }

    /**
     * Sets the access permissions.
     *
     * @param accessPermissions the new access permissions
     */
    public void setAccessPermissions(ArrayList<String> accessPermissions) {
      this.accessPermissions = accessPermissions;
    }

    /**
     * Gets the edits the permissions.
     *
     * @return the edits the permissions
     */
    public ArrayList<String> getEditPermissions() {
      return editPermissions;
    }

    /**
     * Sets the edits the permissions.
     *
     * @param editPermissions the new edits the permissions
     */
    public void setEditPermissions(ArrayList<String> editPermissions) {
      this.editPermissions = editPermissions;
    }

    /**
     * Gets the template name.
     *
     * @return the template name
     */
    public String getTemplateName() {
      return templateName;
    }

    /**
     * Sets the template name.
     *
     * @param templateName the new template name
     */
    public void setTemplateName(String templateName) {
      this.templateName = templateName;
    }
    
    /**
     * Gets the full text template name.
     *
     * @return the template name
     */
    public String getTitle() {
      return title;
    }

    /**
     * Sets the full text template title.
     *
     * @param title the new template title
     */
    public void setTitle(String title) {
      this.title = title;
    }    

    /**
     * Gets the template data.
     *
     * @return the template data
     */
    public String getTemplateData() {
      return templateData;
    }

    /**
     * Sets the template data.
     *
     * @param templateData the new template data
     */
    public void setTemplateData(String templateData) {
      this.templateData = templateData;
    }
  }
}
