/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.migration.workspaces;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleData;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.resources.XMLResourceBundleParser;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Dec 11, 2009  
 */
/**
 * This service used to migrate resource bundle which are missing in DMS 2.3
 */
public class ResourceBundleMigrationService implements Startable {

  private static final Log LOG  = ExoLogger.getLogger(ResourceBundleMigrationService.class.getName());
  
  private ResourceBundleService rsbService;
  private LocaleConfigService localeConfigService;
  private InitParams initParams;
  
  public ResourceBundleMigrationService(ResourceBundleService rsbService, 
      LocaleConfigService localeConfigService, InitParams params) throws Exception {
    this.rsbService = rsbService;
    this.localeConfigService = localeConfigService;
    this.initParams = params;
  }
  
  public void start() {
    try {
      processMigrateResourceBundle();
    } catch(Exception e) {
      LOG.error("An unexpected error occurs when migrate resource bundle", e);
    }
    
  }
  
  @SuppressWarnings("unchecked")
  private void processMigrateResourceBundle() throws Exception {
    List<String> initResources = initParams.getValuesParam("migrate.resources").getValues();
    for (String resource : initResources) {
      initResources(resource, Thread.currentThread().getContextClassLoader());
    }
  }
  
  private void initResources(String baseName, ClassLoader cl) {
    String name = baseName.replace('.', '/');
    String fileName = null;
    try {
      ResourceBundleData redata = null;
      List<String> keyExistingList = null;
      ResourceBundle res = null;
      for (LocaleConfig localeConfig : localeConfigService.getLocalConfigs()) {
        String language = localeConfig.getLanguage();
        redata = rsbService.getResourceBundleData(baseName + "_" + language) ;
        res = rsbService.getResourceBundle(baseName, localeConfig.getLocale());
        if(redata == null) {
          LOG.info("The resource bundle for language " + language +" doesn't exist");
          continue;
        }
        Enumeration<String> keysNum = res.getKeys();
        keyExistingList = new ArrayList<String>();
        while(keysNum.hasMoreElements()) {
          keyExistingList.add(keysNum.nextElement());
        }
        String content = null;
        fileName = name + "_" + language + ".xml";
        URL url = cl.getResource(fileName);
        if (url != null) {
          Properties props = XMLResourceBundleParser.asProperties(url.openStream());
          StringBuilder sb = new StringBuilder();
          for (Map.Entry<Object, Object> entry : props.entrySet()) {
            if(keyExistingList.contains(entry.getKey().toString())) {
              continue;
            }
            sb.append(entry.getKey());
            sb.append('=');
            sb.append(entry.getValue());
            sb.append('\n');
          }
          content = sb.toString();
        }

        if (content != null) {
          redata.setData(redata.getData() + "\n" + content);
          rsbService.saveResourceBundle(redata);
        }
      }
    } catch (Exception ex) {
      LOG.error("Error while reading the file: " + fileName, ex);
    }
  }

  public void stop() {
  }

}
