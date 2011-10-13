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
package org.exoplatform.wcm.webui;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Dec 13, 2008
 */
public class WebUIPropertiesConfigService {

  /** The Constant SCV_POPUP_SIZE_EDIT_PORTLET_MODE. */
  public final static String SCV_POPUP_SIZE_EDIT_PORTLET_MODE = "SCV.popup.size.in.edit.portlet.mode";

  /** The Constant SCV_POPUP_SIZE_QUICK_EDIT. */
  public final static String SCV_POPUP_SIZE_QUICK_EDIT = "SCV.popup.size.in.quickdedit";

  /** The Constant CLV_POPUP_SIZE_EDIT_PORTLET_MODE. */
  public final static String CLV_POPUP_SIZE_EDIT_PORTLET_MODE = "CLV.popup.size.in.edit.portlet.mode";

  /** The Constant CLV_POPUP_SIZE_QUICK_EDIT. */
  public final static String CLV_POPUP_SIZE_QUICK_EDIT = "CLV.popup.size.in.quickedit";

  /** The properties map. */
  private ConcurrentHashMap<String,Object> propertiesMap = new ConcurrentHashMap<String,Object>();

  /**
   * Instantiates a new web ui properties config service.
   *
   * @param params the params
   */
  @SuppressWarnings("unchecked")
  public WebUIPropertiesConfigService(InitParams params) {
    for(Iterator iterator = params.getPropertiesParamIterator();iterator.hasNext();) {
      PropertiesParam propertiesParam = (PropertiesParam)iterator.next();
      if(SCV_POPUP_SIZE_EDIT_PORTLET_MODE.equalsIgnoreCase(propertiesParam.getName())) {
        PopupWindowProperties properties = readPropertiesFromXML(propertiesParam);
        propertiesMap.put(SCV_POPUP_SIZE_EDIT_PORTLET_MODE,properties);
      }else if(SCV_POPUP_SIZE_QUICK_EDIT.equals(propertiesParam.getName())) {
        PopupWindowProperties properties = readPropertiesFromXML(propertiesParam);
        propertiesMap.put(SCV_POPUP_SIZE_QUICK_EDIT,properties);
      }else if(CLV_POPUP_SIZE_QUICK_EDIT.equals(propertiesParam.getName())) {
        PopupWindowProperties properties = readPropertiesFromXML(propertiesParam);
        propertiesMap.put(CLV_POPUP_SIZE_QUICK_EDIT,properties);
      }else if(CLV_POPUP_SIZE_EDIT_PORTLET_MODE.equals(propertiesParam.getName())) {
        PopupWindowProperties properties = readPropertiesFromXML(propertiesParam);
        propertiesMap.put(CLV_POPUP_SIZE_EDIT_PORTLET_MODE,properties);
      }
    }
  }

  /**
   * Gets the properties.
   *
   * @param name the name
   *
   * @return the properties
   */
  public Object getProperties(String name) {
    return propertiesMap.get(name);
  }

  /**
   * Read properties from xml.
   *
   * @param param the param
   *
   * @return the popup window properties
   */
  private PopupWindowProperties readPropertiesFromXML(PropertiesParam param) {
    PopupWindowProperties properties = new PopupWindowProperties();
    String width = param.getProperty(PopupWindowProperties.WIDTH);
    String height = param.getProperty(PopupWindowProperties.HEIGHT);
    if(width != null && StringUtils.isNumeric(width)) {
      properties.setWidth(Integer.parseInt(width));
    }
    if(height != null && StringUtils.isNumeric(height)) {
      properties.setHeight(Integer.parseInt(height));
    }
    return properties;
  }

  /**
   * The Class PopupWindowProperties.
   */
  public static class PopupWindowProperties {

    /** The Constant WIDTH. */
    public final static String WIDTH = "width";

    /** The Constant HEIGHT. */
    public final static String HEIGHT = "height";

    /** The width. */
    private int width = 500;

    /** The height. */
    private int height = 300;

    /**
     * Gets the width.
     *
     * @return the width
     */
    public int getWidth() { return width; }

    /**
     * Sets the width.
     *
     * @param width the new width
     */
    public void setWidth(int width) { this.width = width;}

    /**
     * Gets the height.
     *
     * @return the height
     */
    public int getHeight() { return height; }

    /**
     * Sets the height.
     *
     * @param height the new height
     */
    public void setHeight(int height) { this.height = height; }
  }
}
