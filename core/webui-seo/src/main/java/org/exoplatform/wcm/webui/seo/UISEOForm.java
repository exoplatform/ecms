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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.validator.FloatNumberValidator;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 17, 2011
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
template = "classpath:groovy/webui/seo/UISEOForm.gtmpl",
events = {
  @EventConfig(listeners = UISEOForm.SaveActionListener.class),
  @EventConfig(phase=Phase.DECODE, listeners = UISEOForm.RefreshActionListener.class),
  @EventConfig(phase=Phase.DECODE, listeners = UISEOForm.UpdateActionListener.class),
  @EventConfig(listeners = UISEOForm.RemoveActionListener.class, confirm = "UISEOForm.msg.confirm-delete"),
  @EventConfig(phase=Phase.DECODE, listeners = UISEOForm.CancelActionListener.class) })

public class UISEOForm extends UIForm{

  public static final String TITLE                   = "title";
  public static final String DESCRIPTION             = "description";
  public static final String KEYWORDS                = "keywords";
  final static public String LANGUAGE_TYPE           = "language" ;
  public static final String ROBOTS                  = "robots";
  public static final String SITEMAP                 = "sitemap";
  public static final String ISINHERITED             = "isInherited";
  public static final String SITEMAP_VISIBLE         = "sitemapvisible";
  public static final String PRIORITY                = "priority";
  public static final String FREQUENCY               = "frequency";
  public static final String ROBOTS_INDEX            = "INDEX";
  public static final String ROBOTS_FOLLOW           = "FOLLOW";
  public static final String FREQUENCY_DEFAULT_VALUE = "Always";

  String title = "";
  String description = "";
  String keywords = "";
  String priority = "";
  String frequency = "";
  String index = "";
  String follow = "";
  boolean sitemap = true;
  boolean inherited = false;

  private static String contentPath = null;
  private static String contentURI = null;
  private boolean onContent = false;
  private boolean isInherited = false;
  private ArrayList<String> paramsArray = null;
  public List<Locale> seoLocales;
  public List<String> seoLanguages;
  private String selectedLanguage;
  private String defaultLanguage;
  private boolean isAddNew = true;

  private static final Log LOG  = ExoLogger.getLogger(UISEOForm.class.getName());

  public String getContentPath() {
    return this.contentPath;
  }

  public void setContentPath(String contentPath) {
    this.contentPath = contentPath;
  }

  public String getContentURI() {
    return this.contentURI;
  }
  public void setContentURI(String contentURI) {
    this.contentURI = contentURI;
  }

  public boolean getOnContent() {
    return this.onContent;
  }

  public void setOnContent(boolean onContent) {
    this.onContent = onContent;
  }

  public boolean getIsInherited() {
    return this.isInherited;
  }

  public void setIsInherited(boolean isInherited) {
    this.isInherited = isInherited;
  }

  public ArrayList<String> getParamsArray() {
    return this.paramsArray;
  }

  public void setSEOLocales(List<Locale> seoLocales) {
    this.seoLocales = seoLocales;
  }

  public List<Locale> getSEOLocales() {
    return this.seoLocales;
  }

  public List<String> getSeoLanguages() {
    return seoLanguages;
  }

  public void setSeoLanguages(List<String> seoLanguages) {
    this.seoLanguages = seoLanguages;
  }

  public void setParamsArray(ArrayList<String> params) {
    this.paramsArray = params;
  }

  public String getSelectedLanguage() {
    return selectedLanguage;
  }

  public void setSelectedLanguage(String selectedLanguage) {
    this.selectedLanguage = selectedLanguage;
  }

  public void setIsAddNew(boolean isAddNew) {
    this.isAddNew = isAddNew;
  }

  public boolean getIsAddNew() {
    return this.isAddNew;
  }

  public UISEOForm() throws Exception {
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    SEOService seoService = container.getComponentInstanceOfType(SEOService.class);

    UIFormTextAreaInput uiTitle = new UIFormTextAreaInput(TITLE, TITLE, null);
    uiTitle.setValue(title);
    addUIFormInput(uiTitle);
    UIFormTextAreaInput uiDescription = new UIFormTextAreaInput(DESCRIPTION, DESCRIPTION, null);
    uiDescription.setValue(description);
    addUIFormInput(uiDescription);

    UIFormTextAreaInput uiKeywords = new UIFormTextAreaInput(KEYWORDS, KEYWORDS, null);
    uiKeywords.setValue(keywords);
    addUIFormInput(uiKeywords);
    seoLocales = seoService.getSEOLanguages(portalRequestContext.getPortalOwner(), contentPath, onContent);
    seoLanguages = new ArrayList<>();
    if(seoLocales != null && seoLocales.size() > 0) {
      for (Locale locale : seoLocales) {
        StringBuffer sb = new StringBuffer();
        sb.append(locale.getLanguage());
        String country = locale.getCountry();
        if(StringUtils.isNotEmpty(country)) sb.append("_").append(country);
        seoLanguages.add(sb.toString());
      }
    }

    if(seoLanguages != null) Collections.sort(seoLanguages);
    UIFormSelectBox uiSelectForm = new UIFormSelectBox(LANGUAGE_TYPE, LANGUAGE_TYPE, getLanguages()) ;
    uiSelectForm.setOnChange("Refresh");
    defaultLanguage = portalRequestContext.getLocale().getLanguage();
    if(StringUtils.isNotEmpty(portalRequestContext.getLocale().getCountry()))
      defaultLanguage += "_" + portalRequestContext.getLocale().getCountry();
    selectedLanguage = defaultLanguage;
    if(seoLanguages == null || !seoLanguages.contains(defaultLanguage))
      uiSelectForm.setValue(defaultLanguage);

    addUIFormInput(uiSelectForm) ;

    if(!onContent) {
      List<SelectItemOption<String>> robotIndexItemOptions = new ArrayList<>();
      List<String> robotsindexOptions = seoService.getRobotsIndexOptions();
      List<String> robotsfollowOptions = seoService.getRobotsFollowOptions();
      List<String> frequencyOptions = seoService.getFrequencyOptions();

      if(robotsindexOptions != null && robotsindexOptions.size() > 0) {
        for(int i = 0; i < robotsindexOptions.size(); i++) {
          robotIndexItemOptions.add(new SelectItemOption<>((robotsindexOptions.get(i).toString())));
        }
      }
      UIFormSelectBox robots_index = new UIFormSelectBox(ROBOTS_INDEX, null, robotIndexItemOptions);
      if(index != null && index.length() > 0)
        robots_index.setValue(index);
      else
        robots_index.setValue(ROBOTS_INDEX);
      addUIFormInput(robots_index);


      List<SelectItemOption<String>> robotFollowItemOptions = new ArrayList<>();
      if(robotsfollowOptions != null && robotsfollowOptions.size() > 0) {
        for(int i = 0; i < robotsfollowOptions.size(); i++) {
          robotFollowItemOptions.add(new SelectItemOption<>((robotsfollowOptions.get(i).toString())));
        }
      }
      UIFormSelectBox robots_follow = new UIFormSelectBox(ROBOTS_FOLLOW, null, robotFollowItemOptions);
      if(follow != null && follow.length() > 0)
        robots_follow.setValue(follow);
      else
        robots_follow.setValue(ROBOTS_FOLLOW);
      addUIFormInput(robots_follow);

      UICheckBoxInput visibleSitemapCheckbox = new UICheckBoxInput(SITEMAP, SITEMAP, sitemap);
      addUIFormInput(visibleSitemapCheckbox);

      UIFormStringInput uiPrority = new UIFormStringInput(PRIORITY, null);
      if(!StringUtils.isEmpty(priority)) uiPrority.setValue(priority);
      addUIFormInput(uiPrority.addValidator(FloatNumberValidator.class));

      List<SelectItemOption<String>> frequencyItemOptions = new ArrayList<>();
      if (frequencyOptions != null && frequencyOptions.size() > 0) {
        for (int i = 0; i < frequencyOptions.size(); i++) {
          frequencyItemOptions.add(new SelectItemOption<>(frequencyOptions.get(i).toString(),
              (frequencyOptions.get(i).toString())));
        }
      }
      UIFormSelectBox frequencySelectbox = new UIFormSelectBox(FREQUENCY, null, frequencyItemOptions);
      if(frequency != null && frequency.length() > 0)
        frequencySelectbox.setValue(frequency);
      else
        frequencySelectbox.setValue(FREQUENCY_DEFAULT_VALUE);
      addUIFormInput(frequencySelectbox);
    }


    setActions(new String[]{"Save", "Cancel"});
  }

  public void initSEOForm(PageMetadataModel pageModel) throws Exception{
    PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
    if(pageModel != null) {
      title = pageModel.getTitle();
      description = pageModel.getDescription();
      keywords = pageModel.getKeywords();
      frequency = pageModel.getFrequency();
      if(pageModel.getPriority() >= 0)
        priority = String.valueOf(pageModel.getPriority());
      else priority = null;
      if(pageModel.getRobotsContent() != null && pageModel.getRobotsContent().length() > 0) {
        index = pageModel.getRobotsContent().split(",")[0].trim();
        follow = pageModel.getRobotsContent().split(",")[1].trim();
      }
      sitemap = pageModel.getSitemap();
    } else {
      if(!onContent)
        title = portalRequestContext.getTitle();
      else title = "";
      description = "";
      keywords = "";
      priority = "";
      frequency = "";
      index = "";
      follow = "";
      sitemap = true;
    }

    ExoContainer container = ExoContainerContext.getCurrentContainer() ;
    SEOService seoService = (SEOService)container.getComponentInstanceOfType(SEOService.class);

    UIFormTextAreaInput uiTitle = this.getUIFormTextAreaInput(TITLE);
    if(uiTitle != null) uiTitle.setValue(title);

    UIFormTextAreaInput uiDescription = this.getUIFormTextAreaInput(DESCRIPTION);
    if(uiDescription != null) uiDescription.setValue(description);

    UIFormTextAreaInput uiKeywords = this.getUIFormTextAreaInput(KEYWORDS);
    if(uiKeywords != null) uiKeywords.setValue(keywords);

    UIFormSelectBox uiSelectForm = this.getUIFormSelectBox(LANGUAGE_TYPE);
    uiSelectForm.setSelectedValues(new String[] {"language"});
    if(uiSelectForm != null) {
      seoLocales = seoService.getSEOLanguages(portalRequestContext.getPortalOwner(), contentPath, onContent);
      seoLanguages = new ArrayList<>();
      if(seoLocales != null && seoLocales.size() > 0) {
        for (Locale locale : seoLocales) {
          StringBuffer sb = new StringBuffer();
          sb.append(locale.getLanguage());
          String country = locale.getCountry();
          if(StringUtils.isNotEmpty(country)) sb.append("_").append(country);
          seoLanguages.add(sb.toString());
        }
      }
      if(seoLanguages.size() <= 0) setSelectedLanguage(null);
      List<SelectItemOption<String>> languages = getLanguages();
      if(languages.size() == 1) this.setIsAddNew(false);
      else this.setIsAddNew(true);
      uiSelectForm.setOptions(languages);
      uiSelectForm.setValue(selectedLanguage);
    }

    if(!onContent) {
      List<SelectItemOption<String>> robotIndexItemOptions = new ArrayList<>();
      List<String> robotsindexOptions = seoService.getRobotsIndexOptions();
      List<String> robotsfollowOptions = seoService.getRobotsFollowOptions();
      List<String> frequencyOptions = seoService.getFrequencyOptions();

      if(robotsindexOptions != null && robotsindexOptions.size() > 0) {
        for(int i = 0; i < robotsindexOptions.size(); i++) {
          robotIndexItemOptions.add(new SelectItemOption<>((robotsindexOptions.get(i).toString())));
        }
      }
      UIFormSelectBox robots_index = this.getUIFormSelectBox(ROBOTS_INDEX);
      if(robots_index != null) {
        if(index != null && index.length() > 0)
          robots_index.setValue(index);
        else
          robots_index.setValue(ROBOTS_INDEX);
      }

      List<SelectItemOption<String>> robotFollowItemOptions = new ArrayList<>();
      if(robotsfollowOptions != null && robotsfollowOptions.size() > 0) {
        for(int i = 0; i < robotsfollowOptions.size(); i++) {
          robotFollowItemOptions.add(new SelectItemOption<>((robotsfollowOptions.get(i).toString())));
        }
      }
      UIFormSelectBox robots_follow = this.getUIFormSelectBox(ROBOTS_FOLLOW);
      if(robots_follow != null) {
        if(follow != null && follow.length() > 0)
          robots_follow.setValue(follow);
        else
          robots_follow.setValue(ROBOTS_FOLLOW);
      }

      UICheckBoxInput visibleSitemapCheckbox = this.getUICheckBoxInput(SITEMAP);
      if(visibleSitemapCheckbox != null) visibleSitemapCheckbox.setChecked(sitemap);


      UIFormStringInput uiPrority = this.getUIStringInput(PRIORITY);
      if(uiPrority != null) {
        if(!StringUtils.isEmpty(priority)) uiPrority.setValue(priority);
        else uiPrority.setValue("");
      }


      List<SelectItemOption<String>> frequencyItemOptions = new ArrayList<>();
      if (frequencyOptions != null && frequencyOptions.size() > 0) {
        for (int i = 0; i < frequencyOptions.size(); i++) {
          frequencyItemOptions.add(new SelectItemOption<>(frequencyOptions.get(i).toString(),
              (frequencyOptions.get(i).toString())));
        }
      }
      UIFormSelectBox frequencySelectbox = this.getUIFormSelectBox(FREQUENCY);
      if(frequencySelectbox != null) {
        if(frequency != null && frequency.length() > 0)
          frequencySelectbox.setValue(frequency);
        else
          frequencySelectbox.setValue(FREQUENCY_DEFAULT_VALUE);
      }
    }
  }


  public static class SaveActionListener extends EventListener<UISEOForm> {

    public void execute(Event<UISEOForm> event) throws Exception {
      UISEOForm uiForm = event.getSource();
      UIApplication uiApp = uiForm.getAncestorOfType(UIApplication.class) ;
      String description = uiForm.getUIFormTextAreaInput(DESCRIPTION).getValue();
      String keywords = uiForm.getUIFormTextAreaInput(KEYWORDS).getValue() ;
      PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
      String lang = null;
      if(uiForm.getSelectedLanguage() != null) lang = uiForm.getSelectedLanguage();
      else {
        lang = uiForm.getUIFormSelectBox(LANGUAGE_TYPE).getValue() ;
        StringBuffer sb = new StringBuffer();        
        if(lang == null || lang.equals(LANGUAGE_TYPE)) {
          lang = portalRequestContext.getLocale().getLanguage();
          sb.append(portalRequestContext.getLocale().getLanguage());
          if(StringUtils.isNotEmpty(portalRequestContext.getLocale().getCountry()))
            sb.append("_").append(portalRequestContext.getLocale().getCountry());
          lang = sb.toString();
        }
      }
      uiForm.setSelectedLanguage(lang);
      String portalName = portalRequestContext.getPortalOwner();
      String uri = portalRequestContext.createURL(NodeURL.TYPE, new NavigationResource(Util.getUIPortal().getSelectedUserNode())).toString();
      String fullStatus = null;
      String pageReference = Util.getUIPortal().getSelectedUserNode().getPageRef().format();

      if(!uiForm.onContent) {
        String title = uiForm.getUIFormTextAreaInput(TITLE).getValue();
        String robots_index = uiForm.getUIFormSelectBox(ROBOTS_INDEX).getValue() ;
        String robots_follow = uiForm.getUIFormSelectBox(ROBOTS_FOLLOW).getValue() ;
        String rebots_content = robots_index + ", " + robots_follow;
        boolean isVisibleSitemap = uiForm.getUICheckBoxInput(SITEMAP).isChecked();
        float priority = -1;
        if(uiForm.getUIStringInput(PRIORITY).getValue() != null && uiForm.getUIStringInput(PRIORITY).getValue().length() > 0) {
          priority = Float.parseFloat(uiForm.getUIStringInput(PRIORITY).getValue()) ;
          if(priority < 0.0 || priority > 1.0) {
            uiApp.addMessage(new ApplicationMessage("FloatNumberValidator.msg.Invalid-number", null, ApplicationMessage.WARNING));
            return;
          }
        }
        String frequency = uiForm.getUIFormSelectBox(FREQUENCY).getValue() ;
        try {
          PageMetadataModel metaModel = new PageMetadataModel();
          metaModel.setTitle(title);
          metaModel.setDescription(description);
          metaModel.setFrequency(frequency);
          metaModel.setKeywords(keywords);
          metaModel.setPriority(priority);
          metaModel.setRobotsContent(rebots_content);
          metaModel.setSiteMap(isVisibleSitemap);
          metaModel.setUri(uri);
          metaModel.setPageReference(pageReference);
          if(description!= null && keywords != null && priority != -1)
            fullStatus = "Full";
          else fullStatus = "Partial";
          metaModel.setFullStatus(fullStatus);

          SEOService seoService = uiForm.getApplicationComponent(SEOService.class);
          seoService.storeMetadata(metaModel, portalName, uiForm.onContent, uiForm.getSelectedLanguage());
          uiForm.initSEOForm(metaModel);
          if(uiForm.getAncestorOfType(UISEOToolbarPortlet.class) != null)
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UISEOToolbarPortlet.class)) ;
          else {
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupContainer.class).getParent());
          }
        } catch (Exception ex) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Unexpected error ", ex);
          }
          uiApp.addMessage(new ApplicationMessage("UISEOForm.msg.repository-exception",
                                                  null,
                                                  ApplicationMessage.ERROR));
          return;
        }
      } else {
        try {
          PageMetadataModel metaModel = new PageMetadataModel();
          metaModel.setDescription(description);
          metaModel.setKeywords(keywords);
          metaModel.setUri(uri);
          metaModel.setPageReference(pageReference);
          if(description != null && keywords != null)
            fullStatus = "Full";
          else fullStatus = "Partial";
          metaModel.setFullStatus(fullStatus);
          SEOService seoService = uiForm.getApplicationComponent(SEOService.class);
          Node contentNode = null;
          for(int i=0;i<uiForm.paramsArray.size(); i++) {
            String contentPath = uiForm.paramsArray.get(i).toString();
            contentNode = seoService.getContentNode(contentPath);
            if(contentNode != null) break;
          }
          metaModel.setUri(contentNode.getUUID());
          seoService.storeMetadata(metaModel, portalName, uiForm.onContent, uiForm.getSelectedLanguage());
          uiForm.initSEOForm(metaModel);
          if(uiForm.getAncestorOfType(UISEOToolbarPortlet.class) != null)
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UISEOToolbarPortlet.class)) ;
          else {
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
            event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupContainer.class).getParent());
          }
        } catch (RepositoryException ex) {
          if (LOG.isErrorEnabled()) {
            LOG.error("Unexpected error ", ex);
          }
          uiApp.addMessage(new ApplicationMessage("UISEOForm.msg.repository-exception",
                                                  null,
                                                  ApplicationMessage.ERROR));
          return;
        }
      }
    }
  }

  public static class CancelActionListener extends EventListener<UISEOForm> {
    public void execute(Event<UISEOForm> event) throws Exception {
      UISEOForm uiSEO = event.getSource();
      UIPopupContainer uiSEOToolbar = uiSEO.getAncestorOfType(UIPopupContainer.class);
      if(uiSEOToolbar != null)
        uiSEOToolbar.removeChildById(UISEOToolbarForm.SEO_POPUP_WINDOW);
    }
  }

  public static class RefreshActionListener extends EventListener<UISEOForm> {
    public void execute(Event<UISEOForm> event) throws Exception {
      PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
      UISEOForm uiForm = event.getSource();
      String portalName = portalRequestContext.getPortalOwner();
      String lang = uiForm.getUIFormSelectBox(LANGUAGE_TYPE).getValue();
      if(lang.equals("language")) return;
      uiForm.setSelectedLanguage(lang);
      String pageReference = Util.getUIPortal().getSelectedUserNode().getPageRef().format();
      SEOService seoService = uiForm.getApplicationComponent(SEOService.class);
      PageMetadataModel seoData = new PageMetadataModel();
      PageMetadataModel metaModel = seoService.getMetadata(uiForm.paramsArray, pageReference, uiForm.defaultLanguage);
      if(metaModel == null) metaModel = new PageMetadataModel();

      if(uiForm.onContent) {
        seoData.setUri(uiForm.getContentURI());
        metaModel.setUri(uiForm.getContentURI());
      } else {
        seoData.setPageReference(pageReference);
        seoData.setTitle(portalRequestContext.getTitle());
        metaModel.setTitle(portalRequestContext.getTitle());
      }

      seoData.setFullStatus("Empty");
      seoService.storeMetadata(seoData, portalName, uiForm.onContent, lang);
      uiForm.initSEOForm(metaModel);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm) ;
    }
  }

  public static class UpdateActionListener extends EventListener<UISEOForm> {
    public void execute(Event<UISEOForm> event) throws Exception {
      UISEOForm uiForm = event.getSource();
      String lang = event.getRequestContext().getRequestParameter(OBJECTID) ;
      uiForm.setSelectedLanguage(lang);
      SEOService seoService = uiForm.getApplicationComponent(SEOService.class);
      String pageReference = Util.getUIPortal().getSelectedUserNode().getPageRef().format();
      PageMetadataModel metaModel = seoService.getMetadata(uiForm.paramsArray, pageReference, lang);
      if(metaModel == null || (metaModel != null && metaModel.getFullStatus().equals("Empty"))) {
        metaModel = seoService.getMetadata(uiForm.paramsArray, pageReference, uiForm.defaultLanguage);
      }
      uiForm.initSEOForm(metaModel);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm) ;
    }
  }


  public static class RemoveActionListener extends EventListener<UISEOForm> {
    public void execute(Event<UISEOForm> event) throws Exception {
      UISEOForm uiForm = event.getSource();
      PortalRequestContext portalRequestContext = Util.getPortalRequestContext();
      String lang = event.getRequestContext().getRequestParameter(OBJECTID) ;
      SEOService seoService = uiForm.getApplicationComponent(SEOService.class);
      PageMetadataModel metaModel = new PageMetadataModel();
      String pageReference = Util.getUIPortal().getSelectedUserNode().getPageRef().format();
      metaModel.setPageReference(pageReference);
      if(uiForm.onContent) {
        Node contentNode = null;
        for(int i=0;i<uiForm.paramsArray.size(); i++) {
          String contentPath = uiForm.paramsArray.get(i).toString();
          contentNode = seoService.getContentNode(contentPath);
          if(contentNode != null) break;
        }
        if(contentNode != null) metaModel.setUri(contentNode.getUUID());
      }
      String portalName = portalRequestContext.getPortalOwner();
      seoService.removePageMetadata(metaModel, portalName, uiForm.onContent, lang);
      uiForm.setSEOLocales(seoService.getSEOLanguages(portalRequestContext.getPortalOwner(), contentPath, uiForm.onContent));
      List<String> seoLanguages = new ArrayList<>();
      for (Locale locale : uiForm.getSEOLocales()) {
        StringBuffer sb = new StringBuffer();
        sb.append(locale.getLanguage());
        String country = locale.getCountry();
        if(StringUtils.isNotEmpty(country)) sb.append("_").append(country);
        seoLanguages.add(sb.toString());
      }
      uiForm.setSeoLanguages(seoLanguages);
      String laguageFocus = uiForm.defaultLanguage;
      if(seoLanguages.size()> 0 && !seoLanguages.contains(uiForm.defaultLanguage))
        laguageFocus = seoLanguages.get(0);
      metaModel = seoService.getMetadata(uiForm.paramsArray, pageReference, laguageFocus);
      if(metaModel != null) uiForm.setSelectedLanguage(laguageFocus);
      else uiForm.getUIFormSelectBox(LANGUAGE_TYPE).setValue(uiForm.defaultLanguage);
      uiForm.initSEOForm(metaModel);

      if(uiForm.getAncestorOfType(UISEOToolbarPortlet.class) != null)
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UISEOToolbarPortlet.class)) ;
      else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIPopupContainer.class).getParent());
      }
    }
  }
  public List<SelectItemOption<String>> getLanguages() throws Exception {
    WebuiRequestContext rc = WebuiRequestContext.getCurrentInstance();
    Locale inLocale = WebuiRequestContext.getCurrentInstance().getLocale();
    // Get default locale
    Locale defaultLocale = Locale.getDefault();
    // set default locale to current user selected language
    Locale.setDefault(Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale());

    LocaleConfigService localService = WCMCoreUtils.getService(LocaleConfigService.class) ;
    List<SelectItemOption<String>> languages = new ArrayList<>() ;
    Iterator<LocaleConfig> iter = localService.getLocalConfigs().iterator() ;
    ResourceBundle resourceBundle = rc.getApplicationResourceBundle();
    while (iter.hasNext()) {
      LocaleConfig localConfig = iter.next() ;
      Locale locale = localConfig.getLocale();
      StringBuffer sb = new StringBuffer();
      sb.append(locale.getLanguage());
      String country = locale.getCountry();
      if(StringUtils.isNotEmpty(country)) sb.append("_").append(country);
      String lang = sb.toString();
      if(seoLanguages == null || !seoLanguages.contains(lang)) {
        try {
          languages.add(new SelectItemOption<>(CapitalFirstLetters(locale.getDisplayName(inLocale)), lang)) ;
        } catch(MissingResourceException mre) {
          languages.add(new SelectItemOption<>(lang, lang)) ;
        }
      }
    }

    // Set back to the default locale
    Locale.setDefault(defaultLocale);
    Collections.sort(languages, new ItemOptionComparator());
    languages.add(0,new SelectItemOption<>(getLabel(resourceBundle, "select-language"), "language")) ;
    return languages ;
  }

  public String CapitalFirstLetters(String str) {
    str = Character.toString(str.charAt(0)).toUpperCase()+str.substring(1);
    return str;
  }

  class ItemOptionComparator implements Comparator<SelectItemOption<String>> {
    @Override
    public int compare(SelectItemOption<String> o1, SelectItemOption<String> o2) {
      return o1.getLabel().compareTo(o2.getLabel());
    }
  }
  class SEOItemComparator implements Comparator<Locale> {
    @Override
    public int compare(Locale locale1, Locale locale2) {
      return locale1.getDisplayLanguage().compareTo(locale2.getDisplayLanguage());
    }
  }
}
