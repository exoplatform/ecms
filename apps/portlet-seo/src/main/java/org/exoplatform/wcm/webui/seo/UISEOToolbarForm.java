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


import java.util.Enumeration;
import java.util.Map;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.wcm.webui.seo.UISEOForm;
import org.exoplatform.portal.mop.SiteKey;
import java.util.ArrayList;
/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 4, 2011  
 */

@ComponentConfig(lifecycle = UIFormLifecycle.class,
                 template = "app:/groovy/webui/seo/UISEOPortletToolbar.gtmpl", events = {
                 @EventConfig(listeners = UISEOToolbarForm.AddSEOActionListener.class)
})
public class UISEOToolbarForm extends UIForm {
  
  /** The Constant SEO_POPUP_WINDOW. */
  public static final String SEO_POPUP_WINDOW = "UISEOPopupWindow";
  private static boolean onContent = false;
  private static ArrayList paramsArray = null;
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
      uiSEOForm.setOnContent(onContent);
      uiSEOForm.setParamsArray(paramsArray);
      uiSEOForm.initSEOForm(uiSEOToolbar.metaModel);      
      Utils.createPopupWindow(uiSEOToolbar, uiSEOForm, SEO_POPUP_WINDOW, 500);
    }
  }
  
  public void processRender(WebuiRequestContext context) throws Exception {
		PortalRequestContext pcontext = Util.getPortalRequestContext();
		String portalName = pcontext.getPortalOwner();
		if (!pcontext.useAjax()) {
			fullStatus = "Empty";
			paramsArray = null;
	    String contentParam = null;
	      Enumeration params = pcontext.getRequest().getParameterNames();   
	      Map paramsMap = pcontext.getRequest().getParameterMap();
	      if(params.hasMoreElements()) {
	        paramsArray = new ArrayList();
	        while(params.hasMoreElements()) {
	          contentParam = params.nextElement().toString(); 
	          paramsArray.add(pcontext.getRequestParameter(contentParam));          
	        }
	      } 
		}    
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    SEOService seoService = (SEOService)container.getComponentInstanceOfType(SEOService.class);
    if(paramsArray != null) {
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
    }       
    if(metaModel != null) 
        fullStatus = metaModel.getFullStatus();  
    
    super.processRender(context);
  }
  
  public String getFullStatus() {
    return this.fullStatus;
  }  
}
