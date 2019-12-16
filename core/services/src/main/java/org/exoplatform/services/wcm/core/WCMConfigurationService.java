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
package org.exoplatform.services.wcm.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.exoplatform.commons.utils.ExoProperties;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/*
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 20, 2008
 */
public class WCMConfigurationService {

  public static final String SITE_PATH_EXP = "\\{sitePath\\}";
  public static final String SITE_NAME_EXP = "\\{siteName\\}";

  public static final String PARAMETERIZED_PAGE_URI         = "parameterizedPageURI";

  public static final String PRINT_PAGE_URI                 = "printPageURI";

  public static final String PRINT_VIEWER_PAGE              = "printViewerPage";

  public static final String PARAMETERIZED_URI_PARAM         = "parameterizedURIParam";

  public static final String EDITOR_PAGE_URI                = "editorPageURI";
  
  public static final String EDIT_PAGE_URI                  = "editPageURI";

  public static final String SITE_EXPLORER_URI              = "siteExplorerURI";

  public static final String CREATE_WIKI_PAGE_URI           = "createWikiPageURI";

  public static final String CLV_PORTLET                    = "CLVPortlet";

  public static final String SCV_PORTLET                    = "SCVPortlet";

  public static final String FORM_VIEW_TEMPLATE_PATH        = "formViewTemplatePath";

  public static final String PAGINATOR_TEMPLAET_PATH        = "paginatorTemplatePath";

  private static final Log LOG = ExoLogger.getLogger(WCMConfigurationService.class.getName());

  private NodeLocation livePortalsLocation = null;
  private ExoProperties runtimeContextParams;
  private DriveData siteDriveConfig;

  private String sharedPortal = null;

  @SuppressWarnings("unchecked")
  public WCMConfigurationService(InitParams initParams, RepositoryService repoService) throws Exception {
    Iterator<PropertiesParam> iterator = initParams.getPropertiesParamIterator();
    while (iterator.hasNext()) {
      PropertiesParam param = iterator.next();
      if ("share.portal.config".endsWith(param.getName())) {
        String portalName = param.getProperty("portalName");
        sharedPortal = portalName;
        if (LOG.isInfoEnabled()) {
          LOG.info("Name of shared portal to share resources for all portals in repository is: "+ portalName);
        }
      } else if("RuntimeContextParams".equalsIgnoreCase(param.getName())) {
        runtimeContextParams = param.getProperties();
      }
    }
    Iterator<ObjectParameter> locations = initParams.getObjectParamIterator();
    while (locations.hasNext()) {
      ObjectParameter objectParameter = locations.next();
      if ("live.portals.location.config".equals(objectParameter.getName())) {
        NodeLocation objectParam = (NodeLocation)objectParameter.getObject();
        livePortalsLocation  = objectParam;
        if (LOG.isInfoEnabled()) {
          LOG.info("Location that resources for all live portal is stored in repository"
            + " is in workspace: "+ objectParam.getWorkspace() + " and with path: "+objectParam.getPath());
        }
      }
      if("site.drive.config".equals(objectParameter.getName())) {
        siteDriveConfig = (DriveData)objectParameter.getObject();
      }
    }
  }

  public DriveData getSiteDriveConfig() {return this.siteDriveConfig; }

  public NodeLocation getLivePortalsLocation() {
    return livePortalsLocation;
  }

  public String getRuntimeContextParam(String paramName) {
    if(runtimeContextParams != null)
      return runtimeContextParams.get(paramName);
    return null;
  }

  public Collection<String> getRuntimeContextParams() {
    if(runtimeContextParams != null)
      return runtimeContextParams.values();
    return null;
  }

  public String getSharedPortalName() {
    return sharedPortal;
  }

  public Collection<NodeLocation> getAllLivePortalsLocation() {
    return Arrays.asList(new NodeLocation[]{livePortalsLocation});
  }

}
