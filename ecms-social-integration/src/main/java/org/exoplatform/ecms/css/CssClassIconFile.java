/*
 * Copyright (C) 2003-2023 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecms.css;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CssClassIconFile {
  public static final String      DEFAULT_TYPE       = "default";

  public static final String      DEFAULT_CSS        = "FileDefault";

  private String                  type;

  private String                  cssClass;

  private String                  groupFileTypes     = "";

  private List<String>            listGroupFileTypes = null;

  private static CssClassIconFile defaultType;
  
  public CssClassIconFile() {
  }

  
  /**
   * Return default CssClassIconFile.
   * 
   * @return
   */
  public static synchronized CssClassIconFile getDefault() {
    if (defaultType == null) {
      defaultType = new CssClassIconFile();
      defaultType.setType(DEFAULT_TYPE);
      defaultType.setCssClass(DEFAULT_CSS);
    }

    return defaultType;
  }
  
  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the cssClass
   */
  public String getCssClass() {
    return cssClass;
  }

  /**
   * @param cssClass the cssClass to set
   */
  public void setCssClass(String cssClass) {
    this.cssClass = cssClass;
  }

  /**
   * @return the groupFileTypes
   */
  public String getGroupFileTypes() {
    if(groupFileTypes == null) {
      groupFileTypes = "";
    }
    groupFileTypes = groupFileTypes.replaceAll("\\s", "");
    return groupFileTypes;
  }

  /**
   * @param groupFileTypes the groupFileTypes to set
   */
  public void setGroupFileTypes(String groupFileTypes) {
    this.groupFileTypes = groupFileTypes;
  }

  /**
   * @return the listGroupFileTypes
   */
  public List<String> getListGroupFileTypes() {
    return listGroupFileTypes;
  }

  private void setListGroupFileTypes() {
    listGroupFileTypes = new ArrayList<String>(Arrays.asList(getGroupFileTypes().split(",")));
  }

  /**
   * To check the fileType has contain in group fileTypes or not
   * 
   * @param fileType
   * @return
   */
  public boolean containInGroupFileTypes(String fileType) {
    if (fileType == null || fileType.isEmpty()){
      return false;
    }
    
    if(listGroupFileTypes == null) {
      setListGroupFileTypes();
    }

    for (String cssClass : listGroupFileTypes) {
      //
      if (cssClass.indexOf(fileType) >= 0 || cssClass.indexOf(getFileType(fileType)) >= 0) {
        return true;
      }
    }
    return false;
  }

  private String getFileType(String fileType) {
    fileType = fileType.replaceAll("\\.", "");
    return fileType.substring((fileType.indexOf("/") + 1)).toLowerCase();
  }
  
  @Override
  public boolean equals(Object object) {
    if (object instanceof CssClassIconFile) {
      CssClassIconFile binCSSFile = (CssClassIconFile) object;
      return (binCSSFile.getType().equals(type) ||
               binCSSFile.getCssClass().equals(cssClass)) ? true : false;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, cssClass);
  }

  @Override
  public String toString() {
    return new StringBuilder("{type:'").append(type)
        .append("', cssClass:'").append(cssClass).append("', groupFileTypes:'")
        .append(getGroupFileTypes()).append("'}").toString();
  }

}
