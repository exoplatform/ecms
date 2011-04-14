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
package org.exoplatform.ecm.webui.presentation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;

import org.exoplatform.services.log.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.voting.VotingService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;

/*
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
/**
 * The Class UIBaseNodePresentation should implement some common method in
 * NodePresentation like getIcons,getWebDavLink....
 */
public abstract class UIBaseNodePresentation extends UIContainer implements NodePresentation {

  /** The language_. */
  private String language_ ;
  private boolean enableVote;
  private boolean enableComment;
  private static final Log LOG  = ExoLogger.getLogger("admin.UIBaseNodePresentation");
  public static final String INPUT_TEXT_AREA 			= "TEXTAREA".intern();
	public static final String INPUT_WYSIWYG				= "WYSIWYG".intern();
	public static final String INPUT_TEXT						= "TEXT".intern();

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getNode()
   */
  public abstract Node getNode() throws Exception ;

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getTemplatePath()
   */
  public abstract String getTemplatePath() throws Exception ;

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getOriginalNode()
   */
  public abstract Node getOriginalNode() throws Exception ;

  /**
   * Gets the repository name.
   *
   * @return the repository name
   *
   * @throws Exception the exception
   */
  public abstract String getRepositoryName() throws Exception ;

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#encodeHTML(java.lang.String)
   */
  public String encodeHTML(String text) throws Exception { return Utils.encodeHTML(text) ; }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getAttachments()
   */
  public List<Node> getAttachments() throws Exception {
    List<Node> attachments = new ArrayList<Node>() ;
    NodeIterator childrenIterator = getNode().getNodes();;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    while (childrenIterator.hasNext()) {
      Node childNode = childrenIterator.nextNode();
      String nodeType = childNode.getPrimaryNodeType().getName();
      List<String> listCanCreateNodeType =
        Utils.getListAllowedFileType(getNode(), templateService) ;
      if (listCanCreateNodeType.contains(nodeType)) attachments.add(childNode);
    }
    return attachments;
  }

  @Override
  public String getAttachmentURL(Node attNode, Parameter[] params)
      throws Exception {
    return "";
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getComments()
   */
  public List<Node> getComments() throws Exception {
    return getApplicationComponent(CommentsService.class).getComments(getNode(), getLanguage()) ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getComponentInstanceOfType(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public Object getComponentInstanceOfType(String className) {
    Object service = null;
    try {
      ClassLoader loader =  Thread.currentThread().getContextClassLoader();
      Class object = loader.loadClass(className);
      service = getApplicationComponent(object);
    } catch (ClassNotFoundException ex) {
      LOG.error("Unexpected error", ex);
    }
    return service;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getDownloadLink(javax.jcr.Node)
   */
  public String getDownloadLink(Node node) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class) ;
    InputStreamDownloadResource dresource ;
    if(!node.getPrimaryNodeType().getName().equals(Utils.NT_FILE)) return null;
    Node jcrContentNode = node.getNode(Utils.JCR_CONTENT) ;
    InputStream input = jcrContentNode.getProperty(Utils.JCR_DATA).getStream() ;
    dresource = new InputStreamDownloadResource(input, "image") ;
    dresource.setDownloadName(node.getName()) ;
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getIcons(javax.jcr.Node, java.lang.String)
   */
  public String getIcons(Node node, String size) throws Exception { return Utils.getNodeTypeIcon(node, size) ; }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getImage(javax.jcr.Node)
   */
  public String getImage(Node node) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class) ;
    InputStreamDownloadResource dresource ;
    Node imageNode = node.getNode(Utils.EXO_IMAGE) ;
    InputStream input = imageNode.getProperty(Utils.JCR_DATA).getStream() ;
    dresource = new InputStreamDownloadResource(input, "image") ;
    dresource.setDownloadName(node.getName()) ;
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getLanguage()
   */
  public String getLanguage() { return language_ ; }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#setLanguage(java.lang.String)
   */
  public void setLanguage(String language) { language_ = language ; }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getPortalName()
   */
  public String getPortalName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getRelations()
   */
  public List<Node> getRelations() throws Exception {
    List<Node> relations = new ArrayList<Node>() ;
    if (getNode().hasProperty(Utils.EXO_RELATION)) {
      Value[] vals = getNode().getProperty(Utils.EXO_RELATION).getValues();
      for (int i = 0; i < vals.length; i++) {
        String uuid = vals[i].getString();
        Node node = getNodeByUUID(uuid);
        relations.add(node);
      }
    }
    return relations;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getRepository()
   */
  public String getRepository() throws Exception {
    return ((ManageableRepository)getNode().getSession().getRepository()).getConfiguration().getName() ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getRssLink()
   */
  public String getRssLink() { return null ; }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#isRssLink()
   */
  public boolean isRssLink() { return false ; }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getSupportedLocalise()
   */
  public List getSupportedLocalise() throws Exception {
    MultiLanguageService multiLanguageService = getApplicationComponent(MultiLanguageService.class) ;
    return multiLanguageService.getSupportedLanguages(getNode()) ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getViewTemplate(java.lang.String, java.lang.String)
   */
  public String getViewTemplate(String nodeTypeName, String templateName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getTemplatePath(false, nodeTypeName, templateName) ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getWebDAVServerPrefix()
   */
  public String getWebDAVServerPrefix() throws Exception {
    PortletRequestContext portletRequestContext = PortletRequestContext.getCurrentInstance() ;
    String prefixWebDAV = portletRequestContext.getRequest().getScheme() + "://" +
    portletRequestContext.getRequest().getServerName() + ":" +
    String.format("%s",portletRequestContext.getRequest().getServerPort()) ;
    return prefixWebDAV ;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.presentation.NodePresentation#getWorkspaceName()
   */
  public String getWorkspaceName() throws Exception {
    return getNode().getSession().getWorkspace().getName();
  }

  /**
   * Gets the node by uuid.
   *
   * @param uuid the uuid
   *
   * @return the node by uuid
   *
   * @throws Exception the exception
   */
  public Node getNodeByUUID(String uuid) throws Exception{
    ManageableRepository manageRepo = getApplicationComponent(RepositoryService.class).getCurrentRepository();
    String[] workspaces = manageRepo.getWorkspaceNames() ;
    //TODO: SystemProvider or SessionProvider
    SessionProvider provider = SessionProviderFactory.createSystemProvider() ;
    for(String ws : workspaces) {
      try{
        return provider.getSession(ws,manageRepo).getNodeByUUID(uuid) ;
      }catch(Exception e) { }
    }
    return null;
  }

  /**
   * Retrieve all categories of a node.
   *
   * @param node the node
   *
   * @return the categories
   *
   * @throws Exception the exception
   */
  public List<Node> getCategories(Node node) throws Exception {
    TaxonomyService taxonomyService = getApplicationComponent(TaxonomyService.class);
    return taxonomyService.getCategories(node,getRepositoryName());
  }

  /**
   * Retrieve all tags of a node.
   *
   * @param node the node
   *
   * @return the tags
   *
   * @throws Exception the exception
   */
  public List<Node> getTags(Node node) throws Exception {
    NewFolksonomyService folksonomyService = getApplicationComponent(NewFolksonomyService.class);
    return folksonomyService.getLinkedTagsOfDocumentByScope(NewFolksonomyService.PRIVATE,
                                                            getStrValue(Utils.PRIVATE, node),
                                                            node,
                                                            getWorkspaceName());
  }

  /**
   * Retrieve the voting rate.
   *
   * @param node the node
   *
   * @return the votes
   *
   * @throws Exception the exception
   */
  public long getVotingRate(Node node) throws Exception {
    VotingService votingService = getApplicationComponent(VotingService.class);
    return votingService.getVoteTotal(node);
  }

  /**
   * Retrieve the image in property value.
   *
   * @param node the node
   * @param propertyName the property name
   *
   * @return the image in property
   *
   * @throws Exception the exception
   */
  public String getImageURIInProperty(Node node, String propertyName) throws Exception {
    try {
      InputStream input = node.getProperty(propertyName).getStream() ;
      InputStreamDownloadResource dresource = new InputStreamDownloadResource(input, "image") ;
      dresource.setDownloadName(node.getName()) ;
      DownloadService dservice = getApplicationComponent(DownloadService.class) ;
      return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
    } catch (Exception e) {
    }
    return null;
  }

  /**
   * Retrieve the portlet preference value.
   *
   * @param preferenceName the preference name
   *
   * @return the portlet preference value
   */
  public String getPortletPreferenceValue(String preferenceName) {
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    if(requestContext instanceof PortletRequestContext) {
      PortletRequestContext context = PortletRequestContext.class.cast(requestContext);
      return context.getRequest().getPreferences().getValue(preferenceName,null);
    }
    return null;
  }

  /**
   * Retrieve the portlet preference values.
   *
   * @param preferenceName the preference name
   *
   * @return the portlet preference values
   */
  public String[] getPortletPreferenceValues(String preferenceName) {
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    if(requestContext instanceof PortletRequestContext) {
      PortletRequestContext context = PortletRequestContext.class.cast(requestContext);
      return context.getRequest().getPreferences().getValues(preferenceName,null);
    }
    return null;
  }

  public String getTemplateSkin(String nodeTypeName, String skinName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getSkinPath(nodeTypeName, skinName, getLanguage()) ;
  }

  private String getStrValue(String scope, Node node) throws Exception {
    StringBuilder ret = new StringBuilder();
    if (Utils.PRIVATE.equals(scope))
      ret.append(node.getSession().getUserID());
    else if (Utils.GROUP.equals(scope)) {
      for (String group : Utils.getGroups())
        ret.append(group).append(';');
      ret.deleteCharAt(ret.length() - 1);
    }

    return ret.toString();
  }

  public boolean isEnableComment() {
    return enableComment;
  }

  public boolean isEnableVote() {
   return enableVote;
  }

  public void setEnableComment(boolean value) {
    enableComment = value;
  }

  public void setEnableVote(boolean value) {
    enableVote = value;
  }
  /**
   * 
   * @param restPath				rest-service path to execute
   * @param inputType				input type for editing: TEXT, TEXTAREA, WYSIWYG
   * @param propertyName		which property used for editing
   * @param cssClass				class name for CSS, should implement: cssClass, [cssClass]Title
   * 												Edit[cssClass] as relative css
   * 												Should create the function: InlineEditor.presentationRequestChange[cssClass] 
   * 												to request the rest-service
   * @return								String that can be put on groovy template
   * @throws 								Exception
   * @author 								vinh_nguyen
   */
  public static String getInlineEditingField(String currentValue, String inputType, String propertyName, String cssClass, Node orgNode) throws Exception{
  	String portletRealID = org.exoplatform.wcm.webui.Utils.getRealPortletId((PortletRequestContext)
  			WebuiRequestContext.getCurrentInstance());
  	ResourceBundle resourceBundle = WebuiRequestContext.getCurrentInstance().getApplicationResourceBundle();
  	StringBuffer sb = new StringBuffer();
  	StringBuffer actionsb = new StringBuffer();
//  	Node orgNode = getOriginalNode();
  	String repo = ((ManageableRepository)orgNode.getSession().getRepository()).getConfiguration().getName();
  	String workspace = orgNode.getSession().getWorkspace().getName();
  	String uuid = orgNode.getUUID();
  	String strSuggestion="";
  	portletRealID = portletRealID.replace('-', '_');
  	String showBlockId = "Current" + cssClass + "_" + portletRealID;
  	String editBlockEditorID = "Edit" + cssClass + "_" + portletRealID;
  	String editFormID = "Edit" + cssClass + "Form_" + portletRealID;
  	String newValueInputId = "new" + cssClass + "_" + portletRealID;
  	String currentValueID = "old" + cssClass + "_" + portletRealID;
  	String siteName = org.exoplatform.portal.webui.util.Util.getPortalRequestContext().getPortalOwner();
  	try {
  		strSuggestion = resourceBundle.getString("UIPresentation.label.EditingSuggestion");
  	}catch (Exception E){}
  	actionsb.append(" return InlineEditor.presentationRequestChange").append(cssClass).append("('")
  	.append(currentValueID).append("', '").append(newValueInputId).append("', '").append(repo)
  	.append("', '").append(workspace).append("', '").append(uuid).append("', '")
  	.append(editBlockEditorID).append("', '").append(showBlockId).append("', '").append(siteName);

  	if (inputType.equals(INPUT_WYSIWYG)) {
  		actionsb.append("', 1);");
  	}else {
  		actionsb.append("');");
  	}
  	String strAction = actionsb.toString();

  	sb.append("\n<div id=\"").append(showBlockId).append("\" Class=\"").append(cssClass).append("\"");
  	sb.append("title=\"").append(strSuggestion).append("\"");
  	sb.append(" onDblClick=\"InlineEditor.presentationSwitchBlock('").append(showBlockId).append("', '").append(editBlockEditorID).append("');\"");
  	sb.append("onmouseout=\"this.className='").append(cssClass).append("';\" onmouseover=\"this.className='").append(cssClass).append("Hover';\">").append(currentValue).append("</div>\n");
  	sb.append("\t<div id=\"").append(editBlockEditorID).append("\" class=\"Edit").append(cssClass).append(" ClearFix\">\n");
  	sb.append("\t\t<form name=\"").append(editFormID).append("\" id=\"").append(editFormID).append("\" onSubmit=\"").append(strAction).append("\">\n");
  	sb.append("<DIV style=\"display:none; visible:hidden\" id=\"").append(currentValueID).append("\" name=\"").append(currentValueID).append("\">").append(currentValue).append("</DIV>");
  	sb.append("\t\t<a href=\"#\" class =\"CancelButton\" ").append("onClick=\"InlineEditor.presentationSwitchBlock('");
  	sb.append(editBlockEditorID).append("', '").append(showBlockId).append("');\">&nbsp;</a>\n");
  	sb.append("\t\t<a href=\"#\" class =\"AcceptButton\" onclick=\"").append(strAction).append("\">&nbsp;</a>\n");
  	sb.append("\t\t<div class=\"Edit").append(cssClass).append("Input\">\n");
  	if (inputType.equals(INPUT_WYSIWYG)) {
  		sb.append(createCKEditorField(newValueInputId, "'98%'", "200", currentValue));
  	}else if (inputType.equals(INPUT_TEXT_AREA)){
  		sb.append("\t\t<TEXTAREA ").append("\" name =\"");
  		sb.append(newValueInputId).append("\" id =\"").append(newValueInputId).append("\" >");
  		sb.append(currentValue).append("</TEXTAREA>");
  	}else if (inputType.equals(INPUT_TEXT)) {
  		sb.append("\t\t<input type=\"").append(inputType).append("\" name =\"");
  		sb.append(newValueInputId).append("\" id =\"").append(newValueInputId).append("\" value=\"").append(currentValue).append("\"/>");
  	}
  	sb.append("\n\t\t</div>\n\t</form>\n</div>");		
  	return sb.toString();
  }
  
  /**
   * 
   * @param name
   * @param width
   * @param height
   * @param value_
   * @return
   */
  private static String createCKEditorField(String name, String width, String height, String value_) {	
  	String toolbar = "Basic";

  	if (width == null) width = "'100%'";
  	if (height == null) height = "200";
  	StringBuffer contentsCss = new StringBuffer();
  	contentsCss.append("[");
  	SkinService skinService = WCMCoreUtils.getService(SkinService.class);
  	String skin = Util.getUIPortalApplication().getUserPortalConfig().getPortalConfig().getSkin();
  	String portal = Util.getUIPortal().getName();
  	Collection<SkinConfig> portalSkins = skinService.getPortalSkins(skin);
  	SkinConfig customSkin = skinService.getSkin(portal, Util.getUIPortalApplication()
  			.getUserPortalConfig()
  			.getPortalConfig()
  			.getSkin());
  	if (customSkin != null) portalSkins.add(customSkin);
  	for (SkinConfig portalSkin : portalSkins) {
  		contentsCss.append("'").append(portalSkin.createURL()).append("',");
  	}
  	contentsCss.delete(contentsCss.length() - 1, contentsCss.length());
  	contentsCss.append("]");

  	StringBuffer buffer = new StringBuffer();
  	if (value_!=null) {
  		buffer.append("<textarea id='" + name + "' name='" + name + "'>" + value_ + "</textarea>\n");
  	}else {
  		buffer.append("<textarea id='" + name + "' name='" + name + "'></textarea>\n");
  	}
  	buffer.append("<script type='text/javascript'>\n");
  	buffer.append("  //<![CDATA[\n");
  	buffer.append("    var instances = CKEDITOR.instances['" + name + "']; if (instances) instances.destroy(true);\n");
  	buffer.append("    CKEDITOR.replace('" + name + "', {toolbar:'" + toolbar + "', width:" + width
  			+ ", height:" + height + ", contentsCss:" + contentsCss + ", ignoreEmptyParagraph:true});\n");
  	buffer.append("  //]]>\n");
  	
  	buffer.append("</script>\n");

  	return buffer.toString();
  }
}
