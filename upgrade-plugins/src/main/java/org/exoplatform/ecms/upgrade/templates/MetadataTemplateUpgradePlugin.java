/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.ecms.upgrade.templates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.metadata.impl.MetadataServiceImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * 
 * This class will be used to upgrade template to view and edit metadata like dc:elementSet. 
 * Metadata templates for manual upgration 
 * can be specified in file configuration.properties.<br>
 * Syntax:<br>
 * unchanged-metadata-templates={node name list}
 * For example:<br>
 * unchanged-metadata-templates=metadata:siteMetadata
 *
 */
public class MetadataTemplateUpgradePlugin extends UpgradeProductPlugin {

  private Log log = ExoLogger.getLogger(this.getClass());
  
  private MetadataService metadataService_;  

  public MetadataTemplateUpgradePlugin(MetadataService metadataService, InitParams initParams) {
    super(initParams);
    this.metadataService_ = metadataService;
  }
 
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    String unchangedMetadataTypes = PrivilegedSystemHelper.getProperty("unchanged-metadata-templates");
    if (StringUtils.isEmpty(unchangedMetadataTypes)) {
      unchangedMetadataTypes = "";
    }
    try {
      Set<String> unchangedMetadataTypeSet = new HashSet<String>();
      List<String> configuredMetadataTypeSet = metadataService_.getMetadataList();
      List<String> removedMetadata = new ArrayList<String>();

      for (String unchangedMetadataType : unchangedMetadataTypes.split(",")) {
        unchangedMetadataTypeSet.add(unchangedMetadataType.trim());
      }
      // get all metadata type nodes that need to be removed
      for (String metadataType : configuredMetadataTypeSet) {
        if (!unchangedMetadataTypeSet.contains(metadataType)) {
          removedMetadata.add(metadataType);
          log.info("Metadata " + metadataType + " will be updated.");
        }
      }
      // remove all old metadata node type
      for (String removedMetadataNode : removedMetadata) {
        metadataService_.removeMetadata(removedMetadataNode);
      }
      // reinitialize new templates
      ((MetadataServiceImpl)metadataService_).start();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating metadata template", e);
      }
    } 
  }
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    // --- return true only for the first version of platform
    return VersionComparator.isAfter(newVersion, previousVersion);
  }
 
}
