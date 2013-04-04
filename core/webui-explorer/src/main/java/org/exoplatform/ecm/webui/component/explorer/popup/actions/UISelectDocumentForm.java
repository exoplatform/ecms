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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Nov 8, 2006 10:06:40 AM
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/UISelectDocumentFormThumbnailView.gtmpl",
    events = {
      @EventConfig(listeners = UISelectDocumentForm.SelectTemplateActionListener.class)
    }
)
public class UISelectDocumentForm extends UIContainer {

  private final static String DOCUMENT_TEMPLATE_ITERATOR_ID = "DocumentTemplateIterator";

  private String uiComponentTemplate;

  private Map<String, String> documentTemplates = new HashMap<String, String>();

  private String repository;

  private UIPageIterator pageIterator;

  public UISelectDocumentForm() throws Exception {
    pageIterator = addChild(UIPageIterator.class, null, DOCUMENT_TEMPLATE_ITERATOR_ID);
  }

  public Map<String, String> getDocumentTemplates() {
    return documentTemplates;
  }

  public void setDocumentTemplates(Map<String, String> templates) {
    this.documentTemplates = templates;
  }

  public void updatePageListData() throws Exception {
    List<String> templateList = new ArrayList<String>();
    Iterator<String> iter = getDocumentTemplates().keySet().iterator();
    while (iter.hasNext()) {
      String key = iter.next();
      templateList.add(key);
    }

    ListAccess<String> nodeAccList = new ListAccessImpl<String>(String.class, templateList);
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    int nodesPerPage = uiExplorer.getPreference().getNodesPerPage();
    pageIterator.setPageList(new LazyPageList<String>(nodeAccList, nodesPerPage));
  }

  public String getContentType (String label) {
    return getDocumentTemplates().get(label);
  }

  public String getTemplateIconStylesheet(String contentType) {
    return contentType.replace(":", "_");
  }

  public List<?> getChildrenList() throws Exception {
    return pageIterator.getCurrentPageData();
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public UIPageIterator getContentPageIterator() {
    return pageIterator;
  }

  public String getTemplate() {
    return uiComponentTemplate != null ? uiComponentTemplate : super.getTemplate();
  }

  public void setTemplate(String template) {
    this.uiComponentTemplate = template;
  }

  static public class SelectTemplateActionListener extends EventListener<UISelectDocumentForm> {
    public void execute(Event<UISelectDocumentForm> event) throws Exception {
      String contentType = event.getRequestContext().getRequestParameter(OBJECTID);
      UISelectDocumentForm uiSelectForm = event.getSource() ;
      UIDocumentFormController uiDCFormController = uiSelectForm.getParent() ;
      UIDocumentForm documentForm = uiDCFormController.getChild(UIDocumentForm.class) ;
      documentForm.addNew(true);
      documentForm.getChildren().clear() ;
      documentForm.resetInterceptors();
      documentForm.resetProperties();
      documentForm.setContentType(contentType);

      uiSelectForm.setRendered(false);
      documentForm.setRendered(true);

      event.getRequestContext().addUIComponentToUpdateByAjax(uiDCFormController.getAncestorOfType(UIWorkingArea.class));
    }
  }
}
