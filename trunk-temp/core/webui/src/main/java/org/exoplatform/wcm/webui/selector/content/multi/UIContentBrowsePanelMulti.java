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

import javax.jcr.Node;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel;
import org.exoplatform.wcm.webui.selector.content.UIContentSelector;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wcm.webui.Utils;

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

  /** The item paths. */
  private String itemPaths;
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
      String itemPaths = event.getRequestContext().getRequestParameter(OBJECTID);
      ((UISelectable)(contentBrowsePanelMulti.getSourceComponent())).doSelect(returnFieldName, itemPaths);
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
      Node node=null;
      String itemPaths = event.getRequestContext().getRequestParameter(OBJECTID);
      String iDriver = event.getRequestContext().getRequestParameter("driverName");
      String iPath = event.getRequestContext().getRequestParameter("currentPath");
      String repoName;
      String[] locations = (iPath == null) ? null : iPath.split(":");
      if (iDriver!=null && iDriver.length()>0) {
	    if (locations!=null && locations.length>2) node= Utils.getViewableNodeByComposer(locations[0], locations[1], locations[2],WCMComposer.BASE_VERSION);
	    if (node!=null) {
		  repoName = ((ManageableRepository)node.getSession().getRepository()).getConfiguration().getName();
		  iPath = fixPath(iDriver, node.getPath(), repoName, contentBrowsePanelMulti);	
		  contentBrowsePanelMulti.setInitPath(iDriver, iPath);
	    }else {    	  
	      contentBrowsePanelMulti.setInitPath(iDriver,iPath);
	    }
      }else contentBrowsePanelMulti.setInitPath("", "");
      
      contentBrowsePanelMulti.setItemPaths(itemPaths);      
      UIContentSelector contentSelector = contentBrowsePanelMulti.getAncestorOfType(UIContentSelector.class);
      contentSelector.setSelectedTab(contentBrowsePanelMulti.getId());
    }
    private String fixPath(String driveName, String path, String repository, UIContentBrowsePanelMulti uiBrowser) throws Exception {
        if (path == null || path.length() == 0 || repository == null || repository.length() == 0 )
          return "";
        
        ManageDriveService managerDriveService = uiBrowser.getApplicationComponent(ManageDriveService.class);
        DriveData driveData = managerDriveService.getDriveByName(driveName, repository);
        if (!path.startsWith(driveData.getHomePath()))
          return "";
        if ("/".equals(driveData.getHomePath()))
          return path;
        return path.substring(driveData.getHomePath().length());      
      }
  }
  public String getDeleteConfirmationMsg() {
	return  org.exoplatform.ecm.webui.utils.Utils.getResourceBundle(org.exoplatform.ecm.webui.utils.Utils.LOCALE_WEBUI_DMS, deleteConfirmationMsg, UIContentBrowsePanelMulti.class.getClassLoader());
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
