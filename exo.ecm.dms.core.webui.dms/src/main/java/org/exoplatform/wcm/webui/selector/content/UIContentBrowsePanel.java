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
package org.exoplatform.wcm.webui.selector.content;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.ecm.webui.tree.UIBaseNodeTreeSelector;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.selector.UISelectPathPanel;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
@ComponentConfigs({
  @ComponentConfig(
      lifecycle = Lifecycle.class,
      template = "classpath:groovy/wcm/webui/selector/content/UIContentBrowsePanel.gtmpl",
      events = {
        @EventConfig(listeners = UIContentBrowsePanel.ChangeContentTypeActionListener.class)
      }
  ),
  @ComponentConfig(
      type = UISelectPathPanel.class,
      id = "UIContentBrowsePathSelector",
      template = "classpath:groovy/wcm/webui/selector/content/UIContentBrowsePathSelector.gtmpl",
      events = @EventConfig(listeners = UISelectPathPanel.SelectActionListener.class)
  )
})
public abstract class UIContentBrowsePanel extends UIBaseNodeTreeSelector implements UIPopupComponent{
  public static final String[] WEBCONTENT_NODERTYPE = new String[]{"exo:webContent", "exo:article"};
  public static final String[] MEDIA_MIMETYPE = new String[]{"application", "image", "audio", "video"};
  public static final String WEBCONTENT = "WebContent";
  public static final String DMSDOCUMENT = "DMSDocument";
  public static final String MEDIA = "Media";
  public final String SELECT_TYPE_CONTENT = "selectTypeContent";
  public String[] types = new String[]{WEBCONTENT, DMSDOCUMENT, MEDIA};
  public String selectedValues = WEBCONTENT;
  
  /**
   * Instantiates a new uI web content path selector.
   * 
   * @throws Exception the exception
   */
  private NodeLocation currentPortalLocation;
  public String contentType;

  public UIContentBrowsePanel() throws Exception {
    contentType = WEBCONTENT;
  }
  
  public void reRenderChild(String typeContent) throws Exception{
    if(typeContent == null || typeContent.equals(WEBCONTENT)){
      this.contentType = WEBCONTENT;
    }else if(typeContent.equals(DMSDOCUMENT)){
      this.contentType = DMSDOCUMENT;
    } else {
      this.contentType = MEDIA;
    }
  }

  public abstract void doSelect(Node node, WebuiRequestContext requestContext) throws Exception;
  
  /**
   * Inits the.
   * 
   * @throws Exception the exception
   */
  public void init() throws Exception {
    Node currentPortal = getCurrentPortal();
    UISelectPathPanel selectPathPanel = getChild(UISelectPathPanel.class);
    String[] acceptedNodeTypes = null;
    String[] acceptedMimeTypes = null;
    if(contentType == null || contentType.equals(WEBCONTENT)){
      acceptedNodeTypes = WEBCONTENT_NODERTYPE;
      acceptedMimeTypes = null;
      selectPathPanel.setWebContent(true);
      selectPathPanel.setDMSDocument(false);
    } else if(contentType.equals(MEDIA)){
      acceptedNodeTypes = new String[]{"nt:file"};
      acceptedMimeTypes = MEDIA_MIMETYPE;
      selectPathPanel.setWebContent(false);
      selectPathPanel.setDMSDocument(false);
    } else if(contentType.equals(DMSDOCUMENT)){
      String repositoryName = ((ManageableRepository)(currentPortal.getSession().getRepository())).getConfiguration().getName();
      List<String> listAcceptedNodeTypes = getApplicationComponent(TemplateService.class).getDocumentTemplates(repositoryName);
      List<String> listAcceptedNodeTypesTemp = new ArrayList<String>();
      for(String nodeType : listAcceptedNodeTypes) {
        for(int i = 0; i < WEBCONTENT_NODERTYPE.length; i++) {
          if(nodeType.equalsIgnoreCase(WEBCONTENT_NODERTYPE[i])) continue;
        }
        listAcceptedNodeTypesTemp.add(nodeType);
      }
      acceptedNodeTypes = new String[listAcceptedNodeTypesTemp.size()];
      listAcceptedNodeTypesTemp.toArray(acceptedNodeTypes);
      selectPathPanel.setWebContent(false);
      selectPathPanel.setDMSDocument(true);
    }
    selectPathPanel.setAcceptedNodeTypes(acceptedNodeTypes);
    selectPathPanel.setAcceptedMimeTypes(acceptedMimeTypes);
    LivePortalManagerService livePortalManagerService = getApplicationComponent(LivePortalManagerService.class);
    String currentPortalName = Util.getUIPortalApplication().getOwner();
    SessionProvider provider = Utils.getSessionProvider();
    currentPortal = livePortalManagerService.getLivePortal(provider, currentPortalName);
    currentPortalLocation = NodeLocation.make(currentPortal);
  }

  @Override
  public void onChange(Node node, Object context) throws Exception {  }

  public void activate() throws Exception {  }

  public void deActivate() throws Exception {  }

  /**
   * @return the currentPortal
   */
  public Node getCurrentPortal() {
    return NodeLocation.getNodeByLocation(currentPortalLocation);
  }

  /**
   * @param currentPortal the currentPortal to set
   */
  public void setCurrentPortal(Node currentPortal) {
    currentPortalLocation = NodeLocation.make(currentPortal);
  }
  
  public static class ChangeContentTypeActionListener extends EventListener<UIContentBrowsePanel> {
    public void execute(Event<UIContentBrowsePanel> event) throws Exception {
      UIContentBrowsePanel contentBrowsePanel = event.getSource();
      String type = event.getRequestContext().getRequestParameter(OBJECTID);
      if(type.equals(contentBrowsePanel.selectedValues)) return;
      contentBrowsePanel.selectedValues = type;
      UISelectPathPanel selectPathPanel = contentBrowsePanel.getChild(UISelectPathPanel.class);
      selectPathPanel.setParentNode(null);
      selectPathPanel.updateGrid();
      contentBrowsePanel.reRenderChild(contentBrowsePanel.selectedValues);
      contentBrowsePanel.init();
      event.getRequestContext().addUIComponentToUpdateByAjax(contentBrowsePanel);
    }
  }
}
