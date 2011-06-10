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
package org.exoplatform.ecm.webui.component.admin.folksonomy;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.admin.UIECMAdminPortlet;
import org.exoplatform.ecm.webui.core.UIPagingGrid;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 11, 2007
 * 2:55:47 PM
 */
@ComponentConfig(template = "system:/groovy/ecm/webui/UIGridWithButton.gtmpl", 
                 events = {
    @EventConfig(listeners = UITagStyleList.EditStyleActionListener.class),
    @EventConfig(listeners = UITagStyleList.RemoveStyleActionListener.class, confirm = "UIFolksonomyManager.msg.confirm-delete"),
    @EventConfig(listeners = UITagStyleList.AddStyleActionListener.class) })
public class UITagStyleList extends UIPagingGrid {

  final static String RANGE_PROP = "exo:styleRange" ;
  final static String HTML_STYLE_PROP = "exo:htmlStyle" ;

  private static String[] BEAN_FIELD = {"name", "documentRange", "tagHTML"} ;
  private static String[] ACTIONS = {"EditStyle", "RemoveStyle"} ;

  public UITagStyleList() throws Exception {
    getUIPageIterator().setId("TagStyleIterator") ;
    configure("name", BEAN_FIELD, ACTIONS) ;
  }

  public String[] getActions() {
    return new String[] {"AddStyle"} ;
  }
  
  public void refresh(int currentPage) throws Exception {
    List<TagStyleData> tagStyleList = new ArrayList<TagStyleData>() ;
    NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class) ;
    String repository = getAncestorOfType(UIECMAdminPortlet.class).getPreferenceRepository() ;
    String workspace = getAncestorOfType(UIECMAdminPortlet.class).getDMSSystemWorkspace(repository);
    TagStyleData tagStyleData = null ;
    for (Node node : newFolksonomyService.getAllTagStyle(workspace)) {
      tagStyleData = new TagStyleData(node.getName(),
                                      getRangeOfStyle(node),
                                      getHtmlStyleOfStyle(node));
      tagStyleList.add(tagStyleData);
    }
    ListAccess<TagStyleData> tagStyleListAccess = new ListAccessImpl<TagStyleData>(TagStyleData.class,
                                                                                   tagStyleList);
    LazyPageList<TagStyleData> dataPageList = new LazyPageList<TagStyleData>(tagStyleListAccess,
                                                                             getUIPageIterator().getItemsPerPage());
    getUIPageIterator().setPageList(dataPageList);
    getUIPageIterator().setTotalItems(tagStyleList.size());
    if (currentPage > getUIPageIterator().getAvailablePage())
      getUIPageIterator().setCurrentPage(getUIPageIterator().getAvailablePage());
    else
      getUIPageIterator().setCurrentPage(currentPage);    
  }

  public String getRangeOfStyle(Node tagStyle) throws Exception {
    return tagStyle.getProperty(RANGE_PROP).getValue().getString() ;
  }

  public String getHtmlStyleOfStyle(Node tagStyle) throws Exception {
    return tagStyle.getProperty(HTML_STYLE_PROP).getValue().getString() ;
  }

  static public class TagStyleData {
    private String tagName_ ;
    private String documentRange_ ;
    private String tagHTML_ ;

    public TagStyleData(String tagName, String documentRange, String tagHTML) {
      tagName_ = tagName ;
      documentRange_ = documentRange ;
      tagHTML_ = tagHTML ;
    }

    public String getName() { return tagName_ ; }
    public String getDocumentRange() { return documentRange_ ; }
    public String getTagHTML() { return tagHTML_ ; }
  }
  
  static public class EditStyleActionListener extends EventListener<UITagStyleList> {
    public void execute(Event<UITagStyleList> event) throws Exception {
      UITagStyleList uiTagStyleList = event.getSource();
      UITagManager uiManager = uiTagStyleList.getParent();
      String selectedName = event.getRequestContext().getRequestParameter(OBJECTID);
      Node selectedTagStyle = uiManager.getSelectedTagStyle(selectedName);
      uiManager.initTaggingFormPopup(selectedTagStyle);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }
  
  static public class AddStyleActionListener extends EventListener<UITagStyleList> {
    public void execute(Event<UITagStyleList> event) throws Exception {
      UITagStyleList uiTagStyleList = event.getSource();
      UITagManager uiManager = uiTagStyleList.getParent();
      String selectedName = event.getRequestContext().getRequestParameter(OBJECTID);
      uiManager.initTaggingFormPopup(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }
  
  static public class RemoveStyleActionListener extends EventListener<UITagStyleList> {
    public void execute(Event<UITagStyleList> event) throws Exception {
      UITagStyleList uiTagStyleList = event.getSource();
      UITagManager uiManager = uiTagStyleList.getParent();
      String selectedName = event.getRequestContext().getRequestParameter(OBJECTID);
      Node selectedTagStyle = uiManager.getSelectedTagStyle(selectedName);
      Node parentNode = selectedTagStyle.getParent();
      selectedTagStyle.remove();
      parentNode.getSession().save();
      uiTagStyleList.refresh(uiTagStyleList.getUIPageIterator().getCurrentPage());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiManager);
    }
  }  
}
