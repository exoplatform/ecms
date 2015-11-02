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
package org.exoplatform.wcm.webui.selector.content.multi;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel;
import org.exoplatform.wcm.webui.selector.content.UIContentSelector;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
/**
 * The Class UIContentBrowsePanelMulti.
 */
@ComponentConfig(
  lifecycle = Lifecycle.class,
  template = "classpath:groovy/wcm/webui/selector/content/multi/UIContentBrowsePanel.gtmpl",
  events = {
    @EventConfig(listeners = UIContentBrowsePanel.ChangeContentTypeActionListener.class),
    @EventConfig(listeners = UIContentBrowsePanelMulti.SelectActionListener.class),
    @EventConfig(listeners = UIContentBrowsePanelMulti.CloseActionListener.class),
    @EventConfig(listeners = UIContentBrowsePanelMulti.SaveTemporaryActionListener.class)
  }
)

public class UIContentBrowsePanelMulti extends UIContentBrowsePanel {

  private static final Log LOG = ExoLogger.getLogger(UIContentBrowsePanelMulti.class.getName());

  /** The item paths. */
  private String itemPaths;
  private String itemTarget;
  /** i18n Delete Confirmation message */
  private String deleteConfirmationMsg = "UIBrowserPanel.Confirm.Delete";
  /**
   * Gets the item paths.
   *
   * @return the item paths
   */
  public String getItemPaths() {
    return itemPaths;
  }
  private String _initPath = "";
  private String _initDrive = "";

  public void setInitPath(String initDrive, String initPath) {
    this._initPath = initPath;
    this._initDrive = initDrive;
  }

  public String getInitDrive() { return this._initDrive; }
  public String getInitPath() { return this._initPath; }

  /**
   * Sets the item paths.
   *
   * @param itemPaths the new item paths
   */
  public void setItemPaths(String itemPaths) {
    this.itemPaths = itemPaths;
    setItemTargetPath(getTargetPath(itemPaths));
  }

  public void setItemTargetPath(String _itemTarget) {
   this.itemTarget = _itemTarget;
  }
  public String getItemTargetPath(){
   return this.itemTarget;
  }
   /**
   *
   * @param savedItems
   * @return
   */
  protected String getTargetPath(String savedItems) {
   int i, n;
   LinkManager linkManager;
   String[] savedItemList =savedItems.split(";");
   String savedItem;
   n = savedItemList.length;
   StringBuilder result = new StringBuilder("");
   linkManager = WCMCoreUtils.getService(LinkManager.class);
   for (i = 0; i<n; i++) {
     savedItem = savedItemList[i];
      String[] locations = (savedItem == null) ? null : savedItem.split(":");
      Node node = (locations != null && locations.length >= 3) ? Utils.getViewableNodeByComposer(
          locations[0], locations[1], locations[2]) : null;
      savedItem = StringUtils.EMPTY;
      if (node != null){
        try {
         savedItem = node.getPath();
         if (linkManager.isLink(node)) {
           node = linkManager.getTarget(node);
           savedItem = node.getPath();
         }
        } catch (ItemNotFoundException e){
          savedItem = StringUtils.EMPTY;
        } catch (RepositoryException e){
          savedItem = StringUtils.EMPTY;
        }
      }
      result.append(savedItem).append(";");
   }
   return result.toString();
  }

  /**
   * The listener interface for receiving selectAction events.
   * The class that is interested in processing a selectAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectActionListener<code> method. When
   * the selectAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SelectActionEvent
   */
  public static class SelectActionListener extends EventListener<UIContentBrowsePanelMulti> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentBrowsePanelMulti> event) throws Exception {
      UIContentBrowsePanelMulti contentBrowsePanelMulti = event.getSource();
      String returnFieldName = contentBrowsePanelMulti.getReturnFieldName();
      ((UISelectable)(contentBrowsePanelMulti.getSourceComponent())).doSelect(returnFieldName, contentBrowsePanelMulti.getItemPaths());
    }
  }
  /**
   * The listener interface for receiving SaveTemporaryAction events.
   * The class that is interested in processing a SaveTemporaryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>SaveTemporaryActionListener<code> method. When
   * the selectAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SaveTemporaryActionListener
   */
  public static class SaveTemporaryActionListener extends EventListener<UIContentBrowsePanelMulti> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentBrowsePanelMulti> event) throws Exception {
        UIContentBrowsePanelMulti contentBrowsePanelMulti = event.getSource();
        Node node = null;
        String itemPathtemp = "";

        String operationType = event.getRequestContext().getRequestParameter("oper");
        String dPath = event.getRequestContext().getRequestParameter("path");
        String iDriver = event.getRequestContext().getRequestParameter("driverName");
        String iPath = event.getRequestContext().getRequestParameter("currentPath");
        String tempIPath = iPath;
        String[] locations = (iPath == null) ? null : iPath.split(":");
        if (operationType.equals("clean") && contentBrowsePanelMulti.getItemPaths() != null) {
            contentBrowsePanelMulti.setItemPaths("");
            return;
        }
        if (iDriver != null && iDriver.length() > 0) {
            if (locations != null && locations.length > 2)
                node = Utils.getViewableNodeByComposer(Text.escapeIllegalJcrChars(locations[0]),
                        Text.escapeIllegalJcrChars(locations[1]),
                        Text.escapeIllegalJcrChars(locations[2]),
                        WCMComposer.BASE_VERSION);

            if (node != null) {
                iPath = fixPath(iDriver, node.getPath(), contentBrowsePanelMulti);
                contentBrowsePanelMulti.setInitPath(iDriver, iPath);
            } else {
                contentBrowsePanelMulti.setInitPath(iDriver, iPath);
            }
        } else
             contentBrowsePanelMulti.setInitPath("", "");
               if (operationType.equals("add") && contentBrowsePanelMulti.getItemPaths() != null) {
                  itemPathtemp = contentBrowsePanelMulti.getItemPaths().concat(tempIPath).concat(";");
                   contentBrowsePanelMulti.setItemPaths(itemPathtemp);
               }
               else if (operationType.equals("delete") && contentBrowsePanelMulti.getItemPaths() != null) {
                   itemPathtemp = contentBrowsePanelMulti.getItemPaths();
                   itemPathtemp = StringUtils.remove(itemPathtemp, dPath.concat(";"));
                    contentBrowsePanelMulti.setItemPaths(itemPathtemp);
               }
          else
              contentBrowsePanelMulti.setItemPaths(tempIPath.concat(";"));
              UIContentSelector contentSelector = contentBrowsePanelMulti.getAncestorOfType(UIContentSelector.class);
              contentSelector.setSelectedTab(contentBrowsePanelMulti.getId());
    }

      private String fixPath(String driveName,
                           String path,
                           UIContentBrowsePanelMulti uiBrowser) throws Exception {
      if (path == null || path.length() == 0)
        return "";
      path = Text.escapeIllegalJcrChars(path);
      ManageDriveService managerDriveService = uiBrowser.getApplicationComponent(ManageDriveService.class);
      DriveData driveData = managerDriveService.getDriveByName(driveName);
      if (!path.startsWith(driveData.getHomePath()))
        return "";
      if ("/".equals(driveData.getHomePath()))
        return path;
      return path.substring(driveData.getHomePath().length());
    }
  }

  public String getDeleteConfirmationMsg() {
    return org.exoplatform.ecm.webui.utils.Utils.getResourceBundle(org.exoplatform.ecm.webui.utils.Utils.LOCALE_WEBUI_DMS,
                                                                   deleteConfirmationMsg,
                                                                   UIContentBrowsePanelMulti.class.getClassLoader());
  }

  public String getLocaleMsg(String key) {
    return org.exoplatform.ecm.webui.utils.Utils.getResourceBundle(org.exoplatform.ecm.webui.utils.Utils.LOCALE_WEBUI_DMS,
                                                                   key,
                                                                   UIContentBrowsePanelMulti.class.getClassLoader());
  }
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
  public static class CloseActionListener extends EventListener<UIContentBrowsePanelMulti> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentBrowsePanelMulti> event) throws Exception {
      UIContentBrowsePanelMulti contentBrowsePanelMulti = event.getSource();
      ((UISelectable)(contentBrowsePanelMulti.getSourceComponent())).doSelect(null, null);
    }
  }

}
