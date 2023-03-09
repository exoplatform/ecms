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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

public class CssClassManager implements Startable {
  private static final Log              LOG                   = ExoLogger.getLogger(CssClassManager.class);

  private static final String           UI_ICON               = "uiIcon";

  public static final String            DEFAULT_CSS_ICON_FILE = "nt_file";

  private List<CssClassPlugin>          plugins               = new ArrayList<CssClassPlugin>();

  private Map<String, CssClassIconFile> cssClassIconFileData  = new HashMap<String, CssClassIconFile>();

  private String dataJsonIconFileType = null;

  public enum ICON_SIZE {
    ICON_16("16x16"), ICON_24("24x24"),
    ICON_48("48x48"), ICON_64("64x64");
    private final String name;

    ICON_SIZE(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

  public CssClassManager(InitParams params) {
  }

  @Override
  public void start() {
    try {
      LOG.info("initializing CSS class icon files...");
      initCssClassIconFile();

    } catch (Exception e) {
      LOG.warn("Error while initializing CSS class icon files: " + e.getMessage());
    }
  }

  @Override
  public void stop() {

  }

  /**
   * Register ComponentPlugin for initialize icon CSS class of files. 
   * 
   * @param classPlugin
   * 
   * @since 4.0.1
   */
  public void registerCssClassPlugin(CssClassPlugin classPlugin) {
    plugins.add(classPlugin);
  }

  /**
   * Initialize icon CSS class of files by ComponentPlugin.
   * 
   * @since 4.0.1
   */
  public void initCssClassIconFile() {
    cssClassIconFileData.put(CssClassIconFile.DEFAULT_TYPE, CssClassIconFile.getDefault());
    //
    for (CssClassPlugin cssClassPlugin : plugins) {
      for (CssClassIconFile cssClass : cssClassPlugin.getCssClassIconFile()) {
        cssClassIconFileData.put(cssClass.getType(), cssClass);
      }
    }
  }

  /**
   * Returns the icon CSS class name of file.
   * 
   * @param fileType - The file's type
   * @param size - The size of icon, if it is null, the value default is 16x16
   * @return
   * 
   * @since 4.0.1
   */
  public String getCSSClassByFileType(String fileType, ICON_SIZE size) {
    Collection<CssClassIconFile> classIconFiles = cssClassIconFileData.values();
    for (CssClassIconFile cssFile : classIconFiles) {
      //
      if (cssFile.containInGroupFileTypes(fileType)) {
        return buildCssClass(cssFile, size);
      }

    }
    return buildCssClass(null, size);
  }

  /**
   * Returns the icon CSS class name of file.
   * 
   * @param fileName - The name of file contain file extension
   * @param size - The size of icon, if it is null, the value default is 16x16
   * @return
   * 
   * @since 4.0.1
   */
  public String getCSSClassByFileName(String fileName, ICON_SIZE size) {
    String fileExtension = CssClassUtils.getFileExtension(fileName);
    CssClassIconFile cssFile = cssClassIconFileData.get(fileExtension);
    if (cssFile == null || cssFile.equals(CssClassIconFile.getDefault())) {
      String fileType = new StringBuffer("File").append(fileExtension.substring(0, 1).toUpperCase())
                            .append(fileExtension.substring(1).toLowerCase()).toString();
      return getCSSClassByFileType(fileType, size);
    }
    
    return buildCssClass(cssFile, size);
  }
  
  private static String buildCssClass(CssClassIconFile cssFile, ICON_SIZE size) {
    if(cssFile == null) {
      cssFile = CssClassIconFile.getDefault();
    }
    if (size == null) {
      size = ICON_SIZE.ICON_16;
    }
    return String.format("%s%s%s %s%s%s", UI_ICON, size.getName(), cssFile.getCssClass(), UI_ICON, size.getName(), DEFAULT_CSS_ICON_FILE);
  }
  
  public String getClassIconJsonData() {
    if (dataJsonIconFileType == null) {
      dataJsonIconFileType = cssClassIconFileData.values().toString();
    }
    return dataJsonIconFileType;
  }
}
