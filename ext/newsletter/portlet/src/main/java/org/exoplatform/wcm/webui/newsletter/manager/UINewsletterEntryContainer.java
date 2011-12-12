/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.newsletter.manager;

import java.util.Calendar;

import javax.jcr.Node;

import org.exoplatform.services.wcm.newsletter.NewsletterCategoryConfig;
import org.exoplatform.services.wcm.newsletter.NewsletterManagerService;
import org.exoplatform.services.wcm.newsletter.handler.NewsletterTemplateHandler;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.form.UIFormDateTimeInput;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 10, 2009
 */
@ComponentConfig (
    lifecycle = UIContainerLifecycle.class
)
public class UINewsletterEntryContainer extends UIContainer {

  /** The category config. */
  private NewsletterCategoryConfig categoryConfig;

  /** The newsletter path. */
  private String newsletterPath = null;

  /** The is updated. */
  private boolean isUpdated = false;

  /**
   * Instantiates a new uI newsletter entry container.
   *
   * @throws Exception the exception
   */
  public UINewsletterEntryContainer() throws Exception {
  }

  /**
   * Sets the updated.
   *
   * @param isUpdated the new updated
   */
  public void setUpdated(boolean isUpdated){
    this.isUpdated = isUpdated;
  }

  /**
   * Checks if is updated.
   *
   * @return true, if is updated
   */
  public boolean isUpdated(){
    return this.isUpdated;
  }

  /**
   * Sets the newsletter infor.
   *
   * @param newsletterPath the new newsletter infor
   *
   * @throws Exception the exception
   */
  public void setNewsletterInfor(String newsletterPath) throws Exception{
    this.newsletterPath = newsletterPath;
    init();
  }

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("static-access")
  private void init() throws Exception{
    this.getChildren().clear();
    NewsletterManagerService newsletterManagerService = getApplicationComponent(NewsletterManagerService.class);
    UINewsletterEntryDialogSelector newsletterEntryDialogSelector = addChild(UINewsletterEntryDialogSelector.class, null, null);
    UINewsletterEntryForm newsletterEntryForm = createUIComponent(UINewsletterEntryForm.class, null, null);
    newsletterEntryForm.setRepositoryName(
        WCMCoreUtils.getRepository().getConfiguration().getName());
    newsletterEntryForm.setWorkspace(newsletterManagerService.getWorkspaceName());
    if(newsletterPath == null){
      NewsletterTemplateHandler newsletterTemplateHandler = newsletterManagerService.getTemplateHandler();
      Node templateNode = newsletterTemplateHandler.getTemplate(
          WCMCoreUtils.getUserSessionProvider(), NewsLetterUtil.getPortalName(), categoryConfig, null);
      if(templateNode != null) newsletterPath = templateNode.getPath();
      newsletterEntryForm.addNew(true);
    }else{
      UIFormDateTimeInput dateTimeInput = newsletterEntryDialogSelector.getChild(UIFormDateTimeInput.class);
      Calendar calendar = dateTimeInput.getCalendar().getInstance();
      calendar.setTime(newsletterManagerService.getEntryHandler()
                                               .getNewsletterEntryByPath(WCMCoreUtils.getUserSessionProvider(),
                                                                         this.newsletterPath)
                                               .getNewsletterSentDate());
      dateTimeInput.setCalendar(calendar);
      newsletterEntryForm.addNew(false);
      setUpdated(false);
    }
    newsletterEntryForm.setNodePath(newsletterPath);
    newsletterEntryForm.getChildren().clear();
    newsletterEntryForm.resetProperties();
    addChild(newsletterEntryForm);
    newsletterPath = null;
  }

  /**
   * Gets the category config.
   *
   * @return the category config
   */
  public NewsletterCategoryConfig getCategoryConfig() {
    return categoryConfig;
  }

  /**
   * Sets the category config.
   *
   * @param categoryConfig the new category config
   *
   * @throws Exception the exception
   */
  public void setCategoryConfig(NewsletterCategoryConfig categoryConfig) throws Exception {
    this.categoryConfig = categoryConfig;
    init();
  }
}
