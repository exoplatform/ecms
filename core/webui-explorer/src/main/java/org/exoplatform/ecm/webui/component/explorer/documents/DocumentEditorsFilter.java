/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.documents;

import java.io.IOException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.ApplicationLifecycle;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.webui.application.WebuiApplication;

/**
 * The Class DocumentEditorsFilter.
 */
public class DocumentEditorsFilter implements Filter {

  /** The Constant LOG. */
  protected static final Log    LOG                  = ExoLogger.getLogger(DocumentEditorsFilter.class);

  /** The Constant ECMS_EXPLORER_APP_ID. */
  protected static final String ECMS_EXPLORER_APP_ID = "ecmexplorer/FileExplorerPortlet";

  /**
   * Instantiates a new document editors filter.
   */
  public DocumentEditorsFilter() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    WebAppController controller = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WebAppController.class);
    WebuiApplication app = controller.getApplication(ECMS_EXPLORER_APP_ID);
    // XXX It's known that since portal start this app will not present at very
    // first request to it (Documents Explorer app), thus the filter will not
    // add the lifecycle and it will not initialize the app in the first
    // request.
    if (app != null) {
      // Initialize ECMS Explorer app, this will happen once per app lifetime
      @SuppressWarnings("rawtypes")
      final List<ApplicationLifecycle> lifecycles = app.getApplicationLifecycle();
      if (canAddLifecycle(lifecycles, DocumentEditorsLifecycle.class)) {
        synchronized (lifecycles) {
          if (canAddLifecycle(lifecycles, DocumentEditorsLifecycle.class)) {
            lifecycles.add(new DocumentEditorsLifecycle());
          }
        }
      }
    }
    chain.doFilter(request, response);
  }

  /**
   * Consult if we can add a new lifecycle of given class to the list. This
   * method is not blocking and thread safe, but as result of working over a
   * {@link List} of lifecycles, weakly consistent regarding its answer.
   *
   * @param <C> the generic type
   * @param lifecycles the lifecycles list
   * @param lifecycleClass the lifecycle class to add
   * @return <code>true</code>, if can add, <code>false</code> otherwise
   */
  @SuppressWarnings("rawtypes")
  protected <C extends ApplicationLifecycle> boolean canAddLifecycle(List<ApplicationLifecycle> lifecycles,
                                                                     Class<C> lifecycleClass) {
    return getLifecycle(lifecycles, lifecycleClass) == null;
  }

  /**
   * Returns a lifecycle instance of given class from the list. This method is
   * not blocking and thread safe, but as result of working over a {@link List}
   * of lifecycles, weakly consistent regarding its result.
   *
   * @param <C> the generic type
   * @param lifecycles the lifecycles list
   * @param lifecycleClass the lifecycle class
   * @return the lifecycle instance or <code>null</code> if nothing found in the
   *         given list
   */
  @SuppressWarnings("rawtypes")
  protected <C extends ApplicationLifecycle> C getLifecycle(List<ApplicationLifecycle> lifecycles, Class<C> lifecycleClass) {
    if (lifecycles.size() > 0) {
      // We want iterate from end of the list and don't be bothered by
      // ConcurrentModificationException for a case if someone else will modify
      // the list
      int index = lifecycles.size() - 1;
      do {
        try {
          ApplicationLifecycle lc = lifecycles.get(index);
          if (lc != null && lifecycleClass.isAssignableFrom(lc.getClass())) {
            return lifecycleClass.cast(lc);
          } else {
            index--;
          }
        } catch (IndexOutOfBoundsException e) {
          index--;
        }
      } while (index >= 0);
    }
    return null;
  }
}
