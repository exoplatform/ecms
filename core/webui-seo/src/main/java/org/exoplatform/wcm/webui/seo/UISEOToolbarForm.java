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


import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.reader.ContentReader;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
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

  private static final Log LOG = ExoLogger.getLogger(UISEOToolbarForm.class.getName());

  /** The Constant SEO_POPUP_WINDOW. */
  public static final String SEO_POPUP_WINDOW = "UISEOPopupWindow";
  private static ArrayList<String> paramsArray = null;
  private static String pageReference = null;
  private PageMetadataModel metaModel = null;
  private String fullStatus = "Empty";
  private String lang = null;

  public PageMetadataModel getMetaModel() {
    return metaModel;
  }

  public void setMetaModel(PageMetadataModel metaModel) {
    this.metaModel = metaModel;
  }

  public UISEOToolbarForm() throws Exception
  {
  }

  public static class AddSEOActionListener extends EventListener<UISEOToolbarForm> {
    public void execute(Event<UISEOToolbarForm> event) throws Exception {
      UISEOToolbarForm uiSEOToolbar = event.getSource();
      PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
      UISEOForm uiSEOForm = uiSEOToolbar.createUIComponent(UISEOForm.class, null, null);
      SEOService seoService = WCMCoreUtils.getService(SEOService.class);
      if(paramsArray != null) {
        for(int i = 0;i < paramsArray.size();i++) {
          Node contentNode = seoService.getContentNode(paramsArray.get(i).toString());
          if(contentNode != null) {
            uiSEOForm.setOnContent(true);
            uiSEOForm.setContentPath(paramsArray.get(i).toString());
            uiSEOForm.setContentURI(contentNode.getUUID());
            break;
          }
        }
        uiSEOToolbar.setMetaModel(seoService.getContentMetadata(paramsArray, uiSEOToolbar.lang));
      } else {
        uiSEOForm.setContentPath(pageReference);
        uiSEOForm.setOnContent(false);
        uiSEOToolbar.setMetaModel(seoService.getPageMetadata(pageReference, uiSEOToolbar.lang));
      }     

      uiSEOForm.setParamsArray(paramsArray);   
      if(uiSEOToolbar.getMetaModel() == null) {
        //If have node seo data for default language, displaying seo data for the first language in the list
        List<Locale> seoLocales = seoService.getSEOLanguages(portalRequestContext.getPortalOwner(), uiSEOForm.getContentPath(),
                                                uiSEOForm.getOnContent());
        if(seoLocales.size()> 0) {
          Locale locale = seoLocales.get(0);
          StringBuffer sb = new StringBuffer();
          sb.append(locale.getLanguage());
          String country = locale.getCountry(); 
          if(StringUtils.isNotEmpty(country)) sb.append("_").append(country);
          String lang = sb.toString();
          uiSEOToolbar.setMetaModel(seoService.getMetadata(uiSEOForm.getParamsArray(), pageReference, lang));
          uiSEOForm.setSelectedLanguage(lang);
        }
      } 
      uiSEOForm.initSEOForm(uiSEOToolbar.getMetaModel());

      Utils.createPopupWindow(uiSEOToolbar, uiSEOForm, SEO_POPUP_WINDOW, true, 640);
    }
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    PortalRequestContext pcontext = Util.getPortalRequestContext();
    StringBuffer sb = new StringBuffer();
    sb.append(pcontext.getLocale().getLanguage());
    if(StringUtils.isNotEmpty(pcontext.getLocale().getCountry()))
      sb.append("_").append(pcontext.getLocale().getCountry());
    lang = sb.toString();
    String portalName = pcontext.getPortalOwner();
    metaModel = null;
    fullStatus = "Empty";
    if (!pcontext.useAjax()) {      
      paramsArray = null;
      String contentParam;
      Enumeration params = pcontext.getRequest().getParameterNames();
      if(params.hasMoreElements()) {
        paramsArray = new ArrayList<>();
        while(params.hasMoreElements()) {
          contentParam = params.nextElement().toString();
          String contentValue;
          try {
            contentValue = Text.unescape(pcontext.getRequestParameter(contentParam));
          } catch(Exception ex) {
            contentValue = pcontext.getRequestParameter(contentParam);
          }
          contentValue = ContentReader.getXSSCompatibilityContent(contentValue);
          if(paramsArray !=null) {
            paramsArray.add(Text.escapeIllegalJcrChars(contentValue));
          }
        }
      }
    }
    SEOService seoService = WCMCoreUtils.getService(SEOService.class);
    pageReference = Util.getUIPortal().getSelectedUserNode().getPageRef().format();

    if(pageReference != null) {
      SiteKey siteKey = Util.getUIPortal().getSelectedUserNode().getNavigation().getKey();
      SiteKey portalKey = SiteKey.portal(portalName);
      if(siteKey != null && siteKey.equals(portalKey)) {
        metaModel = seoService.getPageMetadata(pageReference, lang);
        if(paramsArray != null) {
          PageMetadataModel tmpModel = null;
          try{
            tmpModel = seoService.getContentMetadata(paramsArray,lang);
          }catch(PathNotFoundException ex) {
            if (LOG.isErrorEnabled()) {
              LOG.error("Cannot found the content metadata", ex);
            }
          }
          if(tmpModel != null) {
            metaModel = tmpModel;
          } else {
            try {
              for(int i = 0;i < paramsArray.size();i++) {
                Node contentNode = seoService.getContentNode(paramsArray.get(i).toString());

                if(contentNode != null ) {
                  metaModel = null;
                  break;
                }
              }
            }catch(PathNotFoundException ex) {
              metaModel = null;
            }
          }
        }
      }
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
