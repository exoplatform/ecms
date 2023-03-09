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

import org.exoplatform.container.PortalContainer;

public class CssClassUtils {

  public static CssClassManager getCssClassManager() {
    return (CssClassManager) PortalContainer.getInstance().getComponentInstanceOfType(CssClassManager.class);
  }

  public static String getCSSClassByFileType(String fileType, CssClassManager.ICON_SIZE size) {
    return getCssClassManager().getCSSClassByFileType(fileType, size);
  }

  public static String getCSSClassByFileName(String fileName, CssClassManager.ICON_SIZE size) {
    return getCssClassManager().getCSSClassByFileName(fileName, size);
  }
  
  public static String getCSSClassByFileNameAndFileType(String fileName, String fileType, CssClassManager.ICON_SIZE size) {
    String cssClass = getCSSClassByFileName(fileName, size);
    if (cssClass.indexOf(CssClassIconFile.DEFAULT_CSS) >= 0) {
      cssClass = getCSSClassByFileType(fileType, size);
    }

    return cssClass;
  }

  public static String getFileExtension(String fileName) {
    if (fileName != null && fileName.trim().length() > 0) {
      return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    } else {
      return CssClassIconFile.DEFAULT_TYPE;
    }
  }
}
