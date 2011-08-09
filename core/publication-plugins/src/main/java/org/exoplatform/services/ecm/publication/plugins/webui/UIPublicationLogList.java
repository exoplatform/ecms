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
package org.exoplatform.services.ecm.publication.plugins.webui;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jun 26, 2008 1:16:08 AM
 */
@ComponentConfig(
    template = "classpath:groovy/workflow/webui/UIPublicationLogList.gtmpl",
    events = {
        @EventConfig(listeners = UIPublicationLogList.CloseActionListener.class)
    }
)
public class UIPublicationLogList extends UIComponentDecorator {

  private UIPageIterator uiPageIterator_ ;
  private NodeLocation currentNode_ ;

  public UIPublicationLogList() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "PublicationLogListIterator");
    setUIComponent(uiPageIterator_) ;
  }

  public void setNode(Node node) throws Exception { currentNode_ = NodeLocation.getNodeLocationByNode(node); }
  
  private Node getCurrentNode() {
    return NodeLocation.getNodeByLocation(currentNode_);
  }

  public List<HistoryBean> getLog() throws NotInPublicationLifecycleException, Exception {
    PublicationService publicationService = getApplicationComponent(PublicationService.class);
    String[][] array = publicationService.getLog(getCurrentNode());
    List<HistoryBean> list = new ArrayList<HistoryBean>();
    for (int i = 0; i < array.length; i++) {
      HistoryBean bean = new HistoryBean();
      String[] currentLog = array[i];
      bean.setDate(bean.formatStringByDateTime(currentLog[0]));
      bean.setNewState(currentLog[1]);
      bean.setUser(currentLog[2]);
      String[] values = new String[currentLog.length - 4];
      System.arraycopy(currentLog, 4, values, 0, currentLog.length-4);
      String description = publicationService.getLocalizedAndSubstituteLog(getCurrentNode(),
          Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale(), currentLog[3], values);
      bean.setDescription(description);
      list.add(bean);
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  public void updateGrid() throws Exception {
    ListAccess<HistoryBean> historyList = new ListAccessImpl<HistoryBean>(HistoryBean.class,
                                                                          getLog());
    LazyPageList<HistoryBean> dataPageList = new LazyPageList<HistoryBean>(historyList, 10);
    uiPageIterator_.setPageList(dataPageList);
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_ ; }

  public List getLogList() throws Exception { return uiPageIterator_.getCurrentPageData() ; }

  public String[] getActions() {return new String[]{"Close"} ;}

  static public class CloseActionListener extends EventListener<UIPublicationLogList> {
    public void execute(Event<UIPublicationLogList> event) throws Exception {
      UIPublicationLogList uiPublicationLogList = event.getSource() ;
      UIPopupWindow uiPopup = uiPublicationLogList.getAncestorOfType(UIPopupWindow.class) ;
      uiPopup.setRendered(false) ;
      uiPopup.setShow(false) ;
    }
  }

  public class HistoryBean {
    private String date;
    private String newState;
    private String user;
    private String description;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNewState() { return newState; }
    public void setNewState(String newState) { this.newState = newState; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    /**
     * Updated by Nguyen Van Chien
     * @param stringInput
     * @return
     */
    public String formatStringByDateTime(String stringInput) {
      String dateYear = stringInput.substring(0, 4);
      String dateMonth = stringInput.substring(4, 6);
      String dateDay = stringInput.substring(6, 8);
      String dateHour = stringInput.substring(9, 11);
      String dateMinute = stringInput.substring(11, 13);
      String dateSecond = stringInput.substring(13, 15);
      StringBuilder builder = new StringBuilder();
      builder.append(dateMonth).append("/")
            .append(dateDay).append("/")
            .append(dateYear).append(" ")
            .append(dateHour).append(":")
            .append(dateMinute).append(":")
            .append(dateSecond);

      return builder.toString();
    }
  }
}
