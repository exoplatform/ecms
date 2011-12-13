/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.seo;


import java.util.ArrayList;
import java.util.Enumeration;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 4, 2011
 */

@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "classpath:groovy/webui/seo/UISEOPortletToolbar.gtmpl", events = {
                 @EventConfig(listeners = UISEOToolbarForm.AddSEOActionListener.class)
})
public class UISEOToolbarForm extends UIForm {

  /** The Constant SEO_POPUP_WINDOW. */
  public static final String SEO_POPUP_WINDOW = "UISEOPopupWindow";
  private static ArrayList<String> paramsArray = null;
  //private static String pageParent = null;
  private String pageReference = null;
  PageMetadataModel metaModel = null;
  private String fullStatus = "Empty";

  public UISEOToolbarForm() throws Exception
  {
  }

  public static class AddSEOActionListener extends EventListener<UISEOToolbarForm> {
    public void execute(Event<UISEOToolbarForm> event) throws Exception {
      UISEOToolbarForm uiSEOToolbar = event.getSource();
      UISEOForm uiSEOForm = uiSEOToolbar.createUIComponent(UISEOForm.class, null, null);
      String params = event.getRequestContext().getRequestParameter(OBJECTID);
      ExoContainer container = ExoContainerContext.getCurrentContainer() ;
      SEOService seoService = (SEOService)container.getComponentInstanceOfType(SEOService.class);
      if(paramsArray != null) {
        for(int i = 0;i < paramsArray.size();i++) {
          Node contentNode = seoService.getContentNode(paramsArray.get(i).toString());
          if(contentNode != null) {
            uiSEOForm.setOnContent(true);
            break;
          }
        }
      } else uiSEOForm.setOnContent(false);
      uiSEOForm.setParamsArray(paramsArray);
      //uiSEOForm.setPageParent(uiSEOToolbar.pageParent);
      uiSEOForm.initSEOForm(uiSEOToolbar.metaModel);
      int top = -1;
      int left = -1;
      if(params != null && params.length() > 0) {
        String[] arrCoordinate = params.split(",");
        if(arrCoordinate != null && arrCoordinate.length == 2) {
          top = Integer.parseInt(arrCoordinate[0]);
          left = Integer.parseInt(arrCoordinate[1]);
        }
      }
      if(top > -1 && left > -1)
        Utils.createPopupWindow(uiSEOToolbar, uiSEOForm, SEO_POPUP_WINDOW, 325, top, left);
      else
        Utils.createPopupWindow(uiSEOToolbar, uiSEOForm, SEO_POPUP_WINDOW, 325);
    }
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    PortalRequestContext pcontext = Util.getPortalRequestContext();
    String portalName = pcontext.getPortalOwner();
    metaModel = null;
    if (!pcontext.useAjax()) {
      fullStatus = "Empty";
      paramsArray = null;
      String contentParam = null;
        Enumeration params = pcontext.getRequest().getParameterNames();
        if(params.hasMoreElements()) {
          paramsArray = new ArrayList<String>();
          while(params.hasMoreElements()) {
            contentParam = params.nextElement().toString();
            try {
              paramsArray.add(Text.unescape(pcontext.getRequestParameter(contentParam)));
            } catch(Exception ex) {
              paramsArray.add(pcontext.getRequestParameter(contentParam));
            }
          }
        }
    }
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    SEOService seoService = (SEOService)container.getComponentInstanceOfType(SEOService.class);
    pageReference = Util.getUIPortal().getSelectedUserNode().getPageRef();

    if(pageReference != null) {
      SiteKey siteKey = Util.getUIPortal().getSelectedUserNode().getNavigation().getKey();
      SiteKey portalKey = SiteKey.portal(portalName);
      if(siteKey != null && siteKey.equals(portalKey)) {
        metaModel = seoService.getPageMetadata(pageReference);
        //pageParent = Util.getUIPortal().getSelectedUserNode().getParent().getPageRef();
        if(paramsArray != null) {
          PageMetadataModel tmpModel = seoService.getContentMetadata(paramsArray);
          if(tmpModel != null) {
            metaModel = tmpModel;
          } else {
            for(int i = 0;i < paramsArray.size();i++) {
              if(seoService.getContentNode(paramsArray.get(i).toString()) != null ) {
                metaModel = null;
                break;
              }
            }
          }
        }
      }
      else fullStatus = "Disabled";
    }

    /*if(paramsArray != null) {
      onContent = true;
      metaModel = seoService.getContentMetadata(paramsArray);
    }
    else {
      onContent = false;
      pageReference = Util.getUIPortal().getSelectedUserNode().getPageRef();
      SiteKey siteKey = Util.getUIPortal().getSelectedUserNode().getNavigation().getKey();
      SiteKey portalKey = SiteKey.portal(portalName);
      if(siteKey != null && siteKey.equals(portalKey)) metaModel = seoService.getPageMetadata(pageReference);
      else fullStatus = "Disabled";
    }*/

    if(metaModel != null)
        fullStatus = metaModel.getFullStatus();
    super.processRender(context);
  }

  public String getFullStatus() {
    return this.fullStatus;
  }
}
