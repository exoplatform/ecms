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
package org.exoplatform.services.workflow.impl.jbpm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.dom4j.Element;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ExoResourceBundle;
import org.exoplatform.services.resources.XMLResourceBundleParser;
import org.exoplatform.services.workflow.Form;
import org.jbpm.file.def.FileDefinition;

/**
 * Created y the eXo platform team
 * User: Benjamin Mestrallet
 * Date: 17 mai 2004
 */
public class FormImpl implements Form{

  private String stateName;
  private List variables;
  private List submitButtons;
  private ResourceBundle resourceBundle;
  private boolean customizedView;
  private boolean delegatedView;
  private String customizedViewString;
  private byte[] iconBytes;
  private byte[] stateImageBytes;

  private static final Log LOG = ExoLogger.getLogger(FormImpl.class.getName());

  public FormImpl(FileDefinition fileDefinition, Element element, Locale locale) {
    Element childElement = element.element("resource-bundle");
    String formFileName = "";
    if(childElement != null)
      formFileName = childElement.getText();
    ClassLoader cl = this.getClass().getClassLoader();

    // Manage properties
    String localisedFileName = getLocalisedString(formFileName, locale);
    String content = "";
    URL url = cl.getResource(localisedFileName);
    if (url == null) url = cl.getResource(formFileName + ".xml");
    if (url != null) {
      try {
        Properties props = XMLResourceBundleParser.asProperties(url.openStream());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
          sb.append(entry.getKey());
          sb.append('=');
          sb.append(entry.getValue());
          sb.append('\n');
        }
        content = sb.toString();
      } catch (Exception e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("resource bundle not found");
        }
      }
    }

    // If there isn't any XML file (format XML)
    if (content.length() == 0) {
      String fileName = formFileName + "_" + locale.getLanguage() + ".properties";
      url = cl.getResource(fileName);
      if (url == null) url = cl.getResource(formFileName + ".properties");
      if (url != null) {
        try {
          InputStream is = url.openStream();
          byte[] buf = IOUtil.getStreamContentAsBytes(is);
          content = new String(buf, "UTF-8");
          is.close();
        } catch (Exception e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("resource bundle not found");
          }
        }
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Try to find localised resource : " + localisedFileName);
    }
    if (content.length() > 0) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("resource bundle found true");
      }
      resourceBundle = new ExoResourceBundle(content);
    } else{
      if (LOG.isDebugEnabled()) {
        LOG.debug("resource bundle not found");
      }
    }

    childElement = element.element("state-name");
    if(childElement != null)
      this.stateName = childElement.getText();

    initializeVariables(element);
    initializeSubmitButtons(element);

    childElement = element.element("customized-view");
    if(childElement != null)
      this.customizedViewString = childElement.getText();
    if(customizedViewString != null && !"".equals(customizedViewString)){
      customizedView = true;
    }

    childElement = element.element("delegated-view");
    String delegatedViewString = "";
    if(childElement != null)
      delegatedViewString = childElement.getText();
    if("true".equals(delegatedViewString)){
      delegatedView = true;
    }


    //manages bound images
    this.iconBytes = getBytes(fileDefinition, stateName + "-icon.gif");
    this.stateImageBytes = getBytes(fileDefinition, stateName + "-state.gif");
  }

  private String getLocalisedString(String fileName, Locale locale) {
    return fileName + "_" + locale.getLanguage() + ".xml";
  }

  @SuppressWarnings("unchecked")
  private void initializeVariables(Element element) {
    this.variables = new ArrayList();
    Map attributes = null;
    Iterator iter = element.elements("variable").iterator();
    while (iter.hasNext()) {
      Element variableElement = (Element) iter.next();
      attributes = new HashMap();
      String variableName = variableElement.attributeValue("name");
      attributes.put("name", variableName);
      String componentName = variableElement.attributeValue("component");
      attributes.put("component", componentName);
      String editable = variableElement.attributeValue("editable");
      attributes.put("editable", editable);
      String mandatory = variableElement.attributeValue("mandatory");
      attributes.put("mandatory", mandatory);
      String visiable = variableElement.attributeValue("visiable");
      attributes.put("visiable", visiable);
      this.variables.add(attributes);
    }
  }

  @SuppressWarnings("unchecked")
  private void initializeSubmitButtons(Element element) {
    this.submitButtons = new ArrayList();
    Map attributes = null;
    Iterator iter = element.elements("submitbutton").iterator();
    while (iter.hasNext()) {
      Element submitButtonElement = (Element) iter.next();
      attributes = new HashMap();
      String value = submitButtonElement.attributeValue("name");
      attributes.put("name", value);
      String transitionName = submitButtonElement.attributeValue("transition-name");
      attributes.put("transition", transitionName);
      this.submitButtons.add(attributes);
    }
  }

  public List getVariables() {
    return variables;
  }

  public List getSubmitButtons() {
    return submitButtons;
  }

  public String getStateName() {
    return stateName;
  }

  public ResourceBundle getResourceBundle() {
    return resourceBundle;
  }

  public boolean isCustomizedView() {
    return customizedView;
  }

  public String getCustomizedView() {
    return customizedViewString;
  }

  public boolean isDelegatedView() {
    return delegatedView;
  }

  public String getIconURL() {
    return getURL(iconBytes);
  }

  public String getStateImageURL() {
    return getURL(stateImageBytes);
  }

  public byte[] getBytes(FileDefinition fileDefinition, String file) {
    try {
      return fileDefinition.getBytes(file);
    } catch (Exception t) {
      return null;
    }
  }

  public String getURL(byte[] bytes) {
    DownloadService dS = (DownloadService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(
        DownloadService.class);
    InputStream iS = new ByteArrayInputStream(bytes);
    String id = dS.addDownloadResource(new InputStreamDownloadResource(iS, "image/gif"));
    return dS.getDownloadLink(id);
  }

}
