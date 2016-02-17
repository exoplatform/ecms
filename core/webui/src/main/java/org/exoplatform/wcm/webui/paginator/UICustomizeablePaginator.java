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
package org.exoplatform.wcm.webui.paginator;

import org.exoplatform.commons.exception.ExoMessageException;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 23, 2008
 */
@ComponentConfig(
    lifecycle = Lifecycle.class,
    events = @EventConfig(listeners = UICustomizeablePaginator.ShowPageActionListener.class )
)
public class UICustomizeablePaginator extends UIPageIterator {

  private static final Log LOG = ExoLogger.getLogger(UICustomizeablePaginator.class.getName());
  
  /** The template path. */
  private String templatePath;

  /** The resource resolver. */
  private ResourceResolver resourceResolver;

  /** Page Mode */
  private String pageMode;

  /**
   * Instantiates a new uI customizeable paginator.
   */
  public UICustomizeablePaginator() {
  }

  /**
   * Gets the total pages.
   *
   * @return the total pages
   */
  public int getTotalPages() { return getPageList().getAvailablePage(); }

  /**
   * Gets the total items.
   *
   * @return the total items
   */
  public int getTotalItems() { return getPageList().getAvailable(); }

  /**
   * Gets the item per page.
   *
   * @return the item per page
   */
  public int getItemPerPage() { return getPageList().getPageSize(); }

  /**
   * Inits the.
   *
   * @param resourceResolver the resource resolver
   * @param templatePath the template path
   */
  public void init(ResourceResolver resourceResolver, String templatePath) {
    this.resourceResolver = resourceResolver;
    this.templatePath = templatePath;
  }

  /**
   * Sets the template path.
   *
   * @param path the new template path
   */
  public void setTemplatePath(String path) { this.templatePath = path; }

  /**
   * Sets the resource resolver.
   *
   * @param resolver the new resource resolver
   */
  public void setResourceResolver(ResourceResolver resolver) { this.resourceResolver = resolver; }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#getTemplate()
   */
  public String getTemplate() {
    if(templatePath != null)
      return templatePath;
    return super.getTemplate();
  }

  /**
   * gets the page mode (none, more or pagination)
   * @return PageMode
   */
  public String getPageMode() {
    return pageMode;
  }

  /**
   * sets the page mode (none, more or pagination)
   *
   * @param pageMode
   */
  public void setPageMode(String pageMode) {
    this.pageMode = pageMode;
  }
  
  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.
   * exoplatform.webui.application.WebuiRequestContext, java.lang.String)
   */
  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context,String template) {
    if(resourceResolver != null)
      return resourceResolver;
    return super.getTemplateResourceResolver(context,template);
  }

  /**
   * The listener interface for receiving showPageAction events.
   * The class that is interested in processing a showPageAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addShowPageActionListener</code> method. When
   * the showPageAction event occurs, that object's appropriate
   * method is invoked.
   *
   */
  static  public class ShowPageActionListener extends EventListener<UICustomizeablePaginator> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICustomizeablePaginator> event) throws Exception {
      UICustomizeablePaginator uiPaginator = event.getSource() ;
      int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID)) ;
      try {
        uiPaginator.setCurrentPage(page) ;
      } catch (ExoMessageException e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
      UIComponent parent = uiPaginator.getParent();
      if(parent == null) return ;
      event.getRequestContext().addUIComponentToUpdateByAjax(parent);
      parent.broadcast(event,event.getExecutionPhase());
    }
  }
}
