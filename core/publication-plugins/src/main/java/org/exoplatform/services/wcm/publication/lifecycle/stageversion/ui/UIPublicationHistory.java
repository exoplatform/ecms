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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionLog;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponentDecorator;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 * minh.dang@exoplatform.com
 * Edited : Phan Le Thanh Chuong
 * chuong.phan@exoplatform.com
 * June 4, 2009 13:42:23 AM
 */
@SuppressWarnings("deprecation")
@ComponentConfig(
    template = "classpath:groovy/wcm/webui/publication/lifecycle/stageversion/ui/UIPublicationHistory.gtmpl",
    events = {
        @EventConfig(listeners = UIPublicationHistory.CloseActionListener.class)
    }
)
public class UIPublicationHistory extends UIComponentDecorator {

  /** The ui page iterator_. */
  private UIPageIterator uiPageIterator_ ;

  /** The current node_. */
  private NodeLocation currentNodeLocation ;

  /**
   * Instantiates a new uI publication history.
   *
   * @throws Exception the exception
   */
  public UIPublicationHistory() throws Exception {
    uiPageIterator_ = createUIComponent(UIPageIterator.class, null, "PublicationLogListIterator");
    setUIComponent(uiPageIterator_) ;
  }

  /**
   * Inits the.
   *
   * @param node the node
   */
  public void init(Node node) {
   currentNodeLocation = NodeLocation.make(node);
  }

  /**
   * Gets the log.
   *
   * @return the log
   *
   * @throws NotInPublicationLifecycleException the not in publication lifecycle exception
   * @throws Exception the exception
   */
  public List<VersionLog> getLog() throws NotInPublicationLifecycleException, Exception {
    if (currentNodeLocation == null) return new ArrayList<VersionLog>();
    List<VersionLog> logs = new ArrayList<VersionLog>();
    Node currentNode_ = NodeLocation.getNodeByLocation(currentNodeLocation);
    Value[] values = currentNode_.getProperty(StageAndVersionPublicationConstant.HISTORY).getValues();
    for (Value value : values) {
      String logString = value.getString();
      VersionLog bean = VersionLog.toVersionLog(logString);
      logs.add(bean);
    }
    return logs;
  }

  /**
   * Update grid.
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public void updateGrid() throws Exception {
    ListAccess<VersionLog> verLogList = new ListAccessImpl<VersionLog>(VersionLog.class, getLog());
    LazyPageList<VersionLog> dataPageList = new LazyPageList<VersionLog>(verLogList, 10);
    uiPageIterator_.setPageList(dataPageList);
  }

  /**
   * Gets the uI page iterator.
   *
   * @return the uI page iterator
   */
  public UIPageIterator getUIPageIterator() { return uiPageIterator_ ; }

  /**
   * Gets the log list.
   *
   * @return the log list
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public List getLogList() throws Exception { return uiPageIterator_.getCurrentPageData() ; }

  /**
   * Gets the actions.
   *
   * @return the actions
   */
  public String[] getActions() {return new String[]{"Close"} ;}

  /**
   * The listener interface for receiving closeAction events.
   * The class that is interested in processing a closeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCloseActionListener<code> method. When
   * the closeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see CloseActionEvent
   */
  static public class CloseActionListener extends EventListener<UIPublicationHistory> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationHistory> event) throws Exception {
      UIPublicationHistory uiPublicationLogList = event.getSource() ;
      UIPopupContainer uiPopupContainer = (UIPopupContainer) uiPublicationLogList.getAncestorOfType(UIPopupContainer.class);
      uiPopupContainer.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  /**
   * The Class HistoryBean.
   */
  public class HistoryBean {

    /** The date. */
    private String date;

    /** The new state. */
    private String newState;

    /** The user. */
    private String user;

    /** The description. */
    private String description;

    /**
     * Gets the date.
     *
     * @return the date
     */
    public String getDate() { return date; }

    /**
     * Sets the date.
     *
     * @param date the new date
     */
    public void setDate(String date) { this.date = date; }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() { return description; }

    /**
     * Sets the description.
     *
     * @param description the new description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Gets the new state.
     *
     * @return the new state
     */
    public String getNewState() { return newState; }

    /**
     * Sets the new state.
     *
     * @param newState the new new state
     */
    public void setNewState(String newState) { this.newState = newState; }

    /**
     * Gets the user.
     *
     * @return the user
     */
    public String getUser() { return user; }

    /**
     * Sets the user.
     *
     * @param user the new user
     */
    public void setUser(String user) { this.user = user; }

    /**
     * Updated by Nguyen Van Chien.
     *
     * @param stringInput the string input
     *
     * @return the string
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
