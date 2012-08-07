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
package org.exoplatform.wcm.webui.scv;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.MissingResourceException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.portlet.PortletPreferences;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.wcm.webui.reader.ContentReader;

/**
 * Author : Do Ngoc Anh *
 * Email: anh.do@exoplatform.com *
 * May 14, 2008
 */
@ComponentConfig(
		lifecycle=Lifecycle.class,
		template="app:/groovy/SingleContentViewer/UIPresentationContainer.gtmpl",
		events = {
		    @EventConfig(listeners=UIPresentationContainer.PreferencesActionListener.class)
		}
)
public class UIPresentationContainer extends UIContainer{
  public final static String  PARAMETER_REGX        = "(.*)/(.*)";

  private boolean isPrint = false;
  private PortletPreferences portletPreferences;
  private String contentParameter = null;
	/**
	 * Instantiates a new uI presentation container.
	 * 
	 * @throws Exception the exception
	 */
	public UIPresentationContainer() throws Exception{   	  
		addChild(UIPresentation.class, null, null);
		PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    portletPreferences = portletRequestContext.getRequest().getPreferences();    
	}
	
	/**
	 * Gets the bar info show.
	 * 
	 * @return the value for info bar setting
	 * 
	 * @throws Exception the exception
	 */
	public boolean isShowInfoBar() throws Exception {		
		if (UIPortlet.getCurrentUIPortlet().getShowInfoBar())
			return true;
		return false;
	}
	
	/**
	 * Gets the title.
	 * 
	 * @param node the node
	 * 
	 * @return the title
	 * 
	 * @throws Exception the exception
	 */
	public String getTitle(Node node) throws Exception {				
		
		String title = null;
		if (node.hasProperty("exo:title")) {
			title = node.getProperty("exo:title").getValue().getString();
		}
		if (node.hasNode("jcr:content")) {
			Node content = node.getNode("jcr:content");
			if (content.hasProperty("dc:title")) {
				try {
					title = content.getProperty("dc:title").getValues()[0].getString();
				} catch (Exception e) {
					title = null;
				}
			}
		}
		if (title==null) title = node.getName();

		return ContentReader.getXSSCompatibilityContent(title);
	}
	public boolean isPrinting() {
	  return this.isPrint;
	}
	
	public boolean isShowTitle() {
	  return Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_TITLE, "false"));
	}
	public boolean isShowDate() {
    return Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_DATE, "false"));
  }
	public boolean isShowOptionBar() {
    return Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.SHOW_OPTIONBAR, "false"));
  }

	public boolean isContextual() {
		return Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.CONTEXTUAL_MODE, "false"));
	}
	
	public String getCurrentState() throws Exception {
	  UIPresentation presentation = getChild(UIPresentation.class);
	  Node node = presentation.getOriginalNode();
	  if (node!=null) {
	    if (node.hasProperty("publication:currentState")) {
	      PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
	      String state = node.getProperty("publication:currentState").getValue().getString(); 
	      try {
	        state = portletRequestContext.getApplicationResourceBundle().getString("PublicationStates."+state);
	      } catch (MissingResourceException e) { }
	      return state;
	    }
	  }
	  return "";
	}

	/**
	 * Gets the created date.
	 * 
	 * @param node the node
	 * 
	 * @return the created date
	 * 
	 * @throws Exception the exception
	 */
	public String getCreatedDate(Node node) throws Exception {
		if (node.hasProperty("exo:dateCreated")) {
			Calendar calendar = node.getProperty("exo:dateCreated").getValue().getDate();
			return new SimpleDateFormat("dd.MM.yyyy '|' hh'h'mm").format(calendar.getTime());
		}
		return null;
	}

	/**
	 * Gets the node.
	 * 
	 * @return the node
	 * 
	 * @throws Exception the exception
	 */
	public Node getNodeView() {
	  UIPresentation presentation = getChild(UIPresentation.class);
	  try {
	    Node viewNode;
		//Check for the saved parameter
		viewNode = getParameterizedNode();
		if (viewNode!= null) {
		  if (viewNode.isNodeType("nt:frozenNode")) {
            try {
              String nodeUUID = viewNode.getProperty("jcr:frozenUuid").getString();
              presentation.setOriginalNode(viewNode.getSession().getNodeByUUID(nodeUUID));
              presentation.setNode(viewNode);
//              return viewNode.getSession().getNodeByUUID(nodeUUID);
            } catch (Exception ex) {
              return viewNode;
            }
		  }
		  return viewNode;
		}		  
		PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
		portletPreferences = portletRequestContext.getRequest().getPreferences();
		String repository = portletPreferences.getValue(UISingleContentViewerPortlet.REPOSITORY, null);    
		String workspace = portletPreferences.getValue(UISingleContentViewerPortlet.WORKSPACE, null);
		String nodeIdentifier = portletPreferences.getValue(UISingleContentViewerPortlet.IDENTIFIER, null);
		String sharedCache = portletPreferences.getValue(UISingleContentViewerPortlet.ENABLE_CACHE, "true");
    sharedCache = "true".equals(sharedCache) ? WCMComposer.VISIBILITY_PUBLIC:WCMComposer.VISIBILITY_USER;    
    viewNode = Utils.getRealNode(repository, workspace, nodeIdentifier, false, sharedCache);
		if (viewNode!=null) {
	      boolean isDocumentType = false;
	      if (viewNode.isNodeType("nt:frozenNode")) isDocumentType = true; 
	      // check node is a document node
	      TemplateService templateService = getApplicationComponent(TemplateService.class);
	      List<String> documentTypes = templateService.getDocumentTemplates(repository);
	      for (String documentType : documentTypes) {
	        if (viewNode.isNodeType(documentType)) {
	          isDocumentType = true;
	          break;
	        }
	      }
	      if (!isDocumentType) return null;
	      if (viewNode != null && viewNode.isNodeType("nt:frozenNode")) {
	        String nodeUUID = viewNode.getProperty("jcr:frozenUuid").getString();
	        presentation.setOriginalNode(viewNode.getSession().getNodeByUUID(nodeUUID));
	        presentation.setNode(viewNode);	      
	      } else {
	        presentation.setOriginalNode(viewNode);
	        presentation.setNode(viewNode);
	      }
		}
		  return viewNode;
		} catch (Exception e) {
			return null;
		}
	} 
	/**
   * Gets the node.
   * 
   * @return the node
   * 
   * @throws Exception the exception
   */
  public Node getParameterizedNode() throws Exception {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletPreferences preferences = portletRequestContext.getRequest().getPreferences();
    String sharedCache = preferences.getValue(UISingleContentViewerPortlet.ENABLE_CACHE, "false");
    sharedCache = "true".equals(sharedCache) ? WCMComposer.VISIBILITY_PUBLIC:WCMComposer.VISIBILITY_USER;
    
    PortalRequestContext preq = Util.getPortalRequestContext();
    if (!preq.useAjax()) {
      contentParameter = getRequestParameters();
    }
     
    if (contentParameter == null) return null;
    String strRepository = contentParameter.substring(0, contentParameter.indexOf("/"));
    UIPresentation presentation = getChild(UIPresentation.class);
    Node nodeView = Utils.getViewableNodeByComposer(null, null, contentParameter, null, sharedCache);
    if (nodeView!=null) {
      boolean isDocumentType = false;
      if (nodeView.isNodeType("nt:frozenNode")) isDocumentType = true; 
      // check node is a document node
      if (!isDocumentType) {
        TemplateService templateService = getApplicationComponent(TemplateService.class);
        List<String> documentTypes = templateService.getDocumentTemplates(strRepository);
        for (String documentType : documentTypes) {
          if (nodeView.isNodeType(documentType)) {
            isDocumentType = true;
            break;
          }
        }
      }
      if (!isDocumentType) return null;
      if (nodeView != null && nodeView.isNodeType("nt:frozenNode")) {
        String nodeUUID = nodeView.getProperty("jcr:frozenUuid").getString();
        presentation.setOriginalNode(nodeView.getSession().getNodeByUUID(nodeUUID));
        presentation.setNode(nodeView);
      } else if (nodeView == null) {
        return null;
      } else {
        presentation.setOriginalNode(nodeView);
        presentation.setNode(nodeView);
      }
      isPrint = Boolean.parseBoolean(Util.getPortalRequestContext().getRequestParameter("isPrint"));
    }
    return nodeView;
  }
  
  /**
   * Gets the request parameters.
   * 
   * @return the request parameters
   */
  private String getRequestParameters() throws Exception {
    String parameters = null;
    if (!Boolean.parseBoolean(portletPreferences.getValue(UISingleContentViewerPortlet.CONTEXTUAL_MODE, "false"))) {
      return null;
    }
    try {     
      parameters = URLDecoder.decode(StringUtils.substringAfter(Util.getPortalRequestContext().getNodePath(), Util.getUIPortal().getSelectedNode().getUri() + "/"), "UTF-8");     
    } catch (UnsupportedEncodingException e) {
      return null;
    }
    String parameterName = portletPreferences.getValue(UISingleContentViewerPortlet.PARAMETER, "");
    if (!parameters.matches(PARAMETER_REGX)) {
      String path = Util.getPortalRequestContext().getRequestParameter(parameterName);
      if (path == null){
        return null;
      }        
      parameters = Util.getPortalRequestContext().getRequestParameter(parameterName).substring(1);
      return parameters;
    }
    return parameters;
  }

  /**
   *
   *
   * @return
   * @throws RepositoryException
   */
  @Deprecated
  public String getPrintUrl() throws RepositoryException{
    return getPrintUrl(null);
  }

	/**
	 * Get the print's page URL
	 * 
	 * @return <code>true</code> if the Quick Print is shown. Otherwise, <code>false</code>
	 */
	public String getPrintUrl(Node node) throws RepositoryException{
	  String printParameterName;
    Node tempNode = node;
    if (tempNode==null) {
	    tempNode = getNodeView();
    }
    String strPath = tempNode.getPath();
		String repository = ((ManageableRepository)tempNode.getSession().getRepository()).getConfiguration().getName();
		String workspace = tempNode.getSession().getWorkspace().getName();
		String portalURI = Util.getPortalRequestContext().getPortalURI();
		String printPageUrl = portletPreferences.getValue(UISingleContentViewerPortlet.PRINT_PAGE, "");
		printParameterName = portletPreferences.getValue(UISingleContentViewerPortlet.PRINT_PARAMETER, "");
		String printUrl = portalURI + printPageUrl + "?" + printParameterName +  "=/" + repository + "/" + workspace + strPath + "&isPrint=true";
		return printUrl;
	}

  @Deprecated
  public String getQuickEditLink(){
    return getQuickEditLink(null);
  }

  /**
   * Get the quick edit url
   *
   * @param node
   * @return
   */
	public String getQuickEditLink(Node node){
    Node tempNode = node;
    if (tempNode==null) {
	    tempNode = getNodeView();
    }
	  return Utils.getEditLink(tempNode, true, false);
	}
	
	/**
   * The listener interface for receiving preferencesAction events.
   * The class that is interested in processing a preferencesAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addPreferencesActionListener<code> method. When
   * the preferencesAction event occurs, that object's appropriate
   * method is invoked.
   */ 
	public static class PreferencesActionListener extends EventListener<UIPresentationContainer>{   
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPresentationContainer> event) throws Exception {      
      UIPresentationContainer presentationContainer = event.getSource();
      UISCVPreferences pcvConfigForm = presentationContainer.createUIComponent(UISCVPreferences.class, null, null);
      Utils.createPopupWindow(presentationContainer, pcvConfigForm, UISingleContentViewerPortlet.UIPreferencesPopupID, 600);
    }
  }
}