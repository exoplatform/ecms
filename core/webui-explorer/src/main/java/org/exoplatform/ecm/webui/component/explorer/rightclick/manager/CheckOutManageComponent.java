/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.version.Version;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.component.explorer.control.filter.CanSetPropertyFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotCheckedOutFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotContainBinaryFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotInTrashFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotLockedFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsNotTrashHomeNodeFilter;
import org.exoplatform.ecm.webui.component.explorer.control.filter.IsVersionableFilter;
import org.exoplatform.ecm.webui.component.explorer.control.listener.UIWorkingAreaActionListener;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.AuthoringPublicationConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionData;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.config.VersionLog;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Aug 6, 2009
 */

@ComponentConfig(
    events = {
      @EventConfig(listeners = CheckOutManageComponent.CheckOutActionListener.class)
    }
)

public class CheckOutManageComponent extends UIAbstractManagerComponent {

  private static final List<UIExtensionFilter> FILTERS
      = Arrays.asList(new UIExtensionFilter[]{new IsNotInTrashFilter(),
                                              new CanSetPropertyFilter(),
                                              new IsNotLockedFilter(),
                                              new IsNotCheckedOutFilter(),
                                              new IsVersionableFilter(),
                                              new IsNotTrashHomeNodeFilter(),
                                              new IsNotContainBinaryFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  public static void checkOutManage(Event<? extends UIComponent> event, UIJCRExplorer uiExplorer,
      UIApplication uiApp) throws Exception {
    String nodePath = event.getRequestContext().getRequestParameter(OBJECTID);
    if (nodePath == null) {
      nodePath = uiExplorer.getCurrentWorkspace() + ':' + uiExplorer.getCurrentPath();
    }
    Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(nodePath);
    String wsName = null;
    if (matcher.find()) {
      wsName = matcher.group(1);
      nodePath = matcher.group(2);
    } else {
      throw new IllegalArgumentException("The ObjectId is invalid '"+ nodePath + "'");
    }
    Session session = uiExplorer.getSessionByWorkspace(wsName);
    // Use the method getNodeByPath because it is link aware
    Node node = uiExplorer.getNodeByPath(nodePath, session);
    // Reset the path to manage the links that potentially create virtual path
    nodePath = node.getPath();
    // Reset the session to manage the links that potentially change of workspace
    session = node.getSession();
    // Reset the workspace name to manage the links that potentially change of workspace
    wsName = session.getWorkspace().getName();

    try {
      String userId = "";
      try {
        userId = Util.getPortalRequestContext().getRemoteUser();
      } catch (Exception e) {
        userId = node.getSession().getUserID();
      }
      Version liveVersion = node.getBaseVersion();
      node.checkout();
      String currentState = node.getProperty("publication:currentState").getString();
      VersionLog versionLog = new VersionLog(liveVersion.getName(),currentState, userId,new GregorianCalendar(),"UIPublicationHistory.create-version");
      Map<String, VersionData> revisionsMap = getRevisionData(node);
      VersionData liveRevisionData = new VersionData(liveVersion.getUUID(),currentState,userId);
      revisionsMap.put(liveVersion.getUUID(), liveRevisionData);
      addRevisionData(node, revisionsMap.values());
      addLog(node,versionLog);
      node.save();      
    } catch(PathNotFoundException path) {
      uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
          null,ApplicationMessage.WARNING));
      
      return;
    } catch (Exception e) {
      JCRExceptionManager.process(uiApp, e);
      
      uiExplorer.updateAjax(event);
    }
  }

  public static class CheckOutActionListener extends UIWorkingAreaActionListener<CheckOutManageComponent> {
    public void processEvent(Event<CheckOutManageComponent> event) throws Exception {
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = event.getSource().getAncestorOfType(UIApplication.class);
      checkOutManage(event, uiExplorer, uiApp);
    }
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
  
  /**
   * Adds the log.
   *
   * @param node the node
   * @param versionLog the version log
   * @throws Exception the exception
   */
  private static void addLog(Node node, VersionLog versionLog) throws Exception {
    Value[] values = node.getProperty(AuthoringPublicationConstant.HISTORY).getValues();
    ValueFactory valueFactory = node.getSession().getValueFactory();
    List<Value> list = new ArrayList<Value>(Arrays.asList(values));
    list.add(valueFactory.createValue(versionLog.toString()));
    node.setProperty(AuthoringPublicationConstant.HISTORY, list.toArray(new Value[] {}));
  }
  
  /**
   * Gets the revision data.
   *
   * @param node the node
   * @return the revision data
   * @throws Exception the exception
   */
  private static Map<String, VersionData> getRevisionData(Node node) throws Exception {
    Map<String, VersionData> map = new HashMap<String, VersionData>();
    try {
      for (Value v : node.getProperty(AuthoringPublicationConstant.REVISION_DATA_PROP).getValues()) {
        VersionData versionData = VersionData.toVersionData(v.getString());
        map.put(versionData.getUUID(), versionData);
      }
    } catch (Exception e) {
      return map;
    }
    return map;
  }
  
  /**
   * Adds the revision data.
   *
   * @param node the node
   * @param list the list
   * @throws Exception the exception
   */
  private static void addRevisionData(Node node, Collection<VersionData> list) throws Exception {
    List<Value> valueList = new ArrayList<Value>();
    ValueFactory factory = node.getSession().getValueFactory();
    for (VersionData versionData : list) {
      valueList.add(factory.createValue(versionData.toStringValue()));
    }
    node.setProperty(AuthoringPublicationConstant.REVISION_DATA_PROP,
                     valueList.toArray(new Value[] {}));
  }

}
