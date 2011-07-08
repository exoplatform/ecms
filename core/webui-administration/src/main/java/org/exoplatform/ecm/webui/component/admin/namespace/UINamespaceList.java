/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.admin.namespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.NamespaceRegistry;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 *          phamtuanchip@yahoo.de
 * September 20, 2006
 * 16:37:15
 */
@ComponentConfig(
    template = "system:/groovy/ecm/webui/UIGridWithButton.gtmpl",
    events = {@EventConfig(listeners = UINamespaceList.AddNamespaceActionListener.class)}
)

public class UINamespaceList extends UIPagingGrid {

  private static String[] NAMESPACE_BEAN_FIELD = {"prefix", "uri"} ;

  public UINamespaceList() throws Exception {
    getUIPageIterator().setId("NamespaceListIterator") ;
    configure("prefix", NAMESPACE_BEAN_FIELD, null) ;
  }

  public String[] getActions() { return new String[] {"AddNamespace"} ;}

  @SuppressWarnings("unchecked")
  public void refresh(int currentPage) throws Exception {
    NamespaceRegistry namespaceRegistry = getApplicationComponent(RepositoryService.class).getCurrentRepository()
                                                                                          .getNamespaceRegistry();
    List<NamespaceBean> nspBeans = new ArrayList<NamespaceBean>();
    String[] prefixs = namespaceRegistry.getPrefixes();
    for (int i = 0; i < prefixs.length; i++) {
      String prefix = prefixs[i];
      if (prefix == null || prefix.trim().length() == 0) {
        continue;
      }
      NamespaceBean bean = new NamespaceBean(prefix, namespaceRegistry.getURI(prefix));
      nspBeans.add(bean);
    }
    Collections.sort(nspBeans, new NameSpaceComparator());
    ListAccess<NamespaceBean> namespaceList = new ListAccessImpl<NamespaceBean>(NamespaceBean.class,
                                                                                nspBeans);
    LazyPageList<NamespaceBean> dataPageList = new LazyPageList<NamespaceBean>(namespaceList,
                                                                               getUIPageIterator().getItemsPerPage());
    getUIPageIterator().setTotalItems(nspBeans.size());
    getUIPageIterator().setPageList(dataPageList);
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);
  }

  static public class NameSpaceComparator implements Comparator<NamespaceBean> {
    public int compare(NamespaceBean o1, NamespaceBean o2) throws ClassCastException {
      String name1 = o1.getPrefix();
      String name2 = o2.getPrefix();
      return name1.compareToIgnoreCase(name2);
    }
  }

  static public class AddNamespaceActionListener extends EventListener<UINamespaceList> {
    public void execute(Event<UINamespaceList> event) throws Exception {
      UINamespaceManager uiManager = event.getSource().getParent() ;
      uiManager.initPopup() ;
      UINamespaceForm uiForm = uiManager.findFirstComponentOfType(UINamespaceForm.class) ;
      uiForm.reset() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager) ;
    }
  }

  public static class NamespaceBean {
    private String prefix ;
    private String uri ;

    public NamespaceBean(String prefix, String uri){
      this.prefix = prefix ;
      this.uri = uri ;
    }

    public String getPrefix () { return prefix ;}
    public String getUri () { return uri ;}
  }
}
