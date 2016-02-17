package org.exoplatform.wcm.connector.collaboration;

import java.io.FileNotFoundException;
import java.security.AccessControlException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;



/**
 * Created by The eXo Platform SEA
 * Author : Ha Quang Tan
 * tan.haquang@exoplatform.com
 * Mar 23, 2011
 */
@Path("/contents/editing/")
public class InlineEditingService implements ResourceContainer{
  private static final Log LOG = ExoLogger.getLogger(InlineEditingService.class.getName());

  final static public String EXO_TITLE               = "exo:title";

  final static public String EXO_SUMMARY             = "exo:summary";

  final static public String EXO_TEXT                = "exo:text";

  final static public String EXO_RSS_ENABLE          = "exo:rss-enable";

  public final static String POST_EDIT_CONTENT_EVENT = "CmsService.event.postEdit";

  private final String       localeFile              = "locale.portlet.i18n.WebUIDms";
  /**
   * SERVICE: Edit title of document.
   *
   * @param newTitle the new title of document
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param nodeUIID the UIID of node
   * @param siteName the site name
   *
   * @return the response
   */
  @POST
  @Path("/title/")
  public Response editTitle(@FormParam("newValue") String newTitle,
                            @QueryParam("repositoryName") String repositoryName,
                            @QueryParam("workspaceName") String workspaceName,
                            @QueryParam("nodeUIID") String  nodeUIID,
                            @QueryParam("siteName") String  siteName,
                            @QueryParam("language") String  language){
    return modifyProperty(EXO_TITLE, newTitle, repositoryName, workspaceName, nodeUIID, siteName, language);
  }

  /**
   * SERVICE: Edit summary of document.
   *
   * @param newSummary the new summary of document
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param nodeUIID the UIID of node
   * @param siteName the site name
   *
   * @return the response
   */
  @POST
  @Path("/summary/")
  public Response editSummary(@FormParam("newValue") String newSummary,
                              @QueryParam("repositoryName") String repositoryName,
                              @QueryParam("workspaceName") String workspaceName,
                              @QueryParam("nodeUIID") String  nodeUIID,
                              @QueryParam("siteName") String  siteName,
                              @QueryParam("language") String  language){
    return modifyProperty(EXO_SUMMARY, newSummary, repositoryName, workspaceName, nodeUIID, siteName, language);
  }
  /**
   * SERVICE: Edit summary of document.
   *
   * @param newValue the new summary of document
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param nodeUIID the UIID of node
   * @param siteName the site name
   *
   * @return the response
   */
  @POST
  @Path("/text/")
  public Response editText( @FormParam("newValue") String newValue,
                            @QueryParam("repositoryName") String repositoryName,
                            @QueryParam("workspaceName") String workspaceName,
                            @QueryParam("nodeUIID") String  nodeUIID,
                            @QueryParam("siteName") String  siteName,
                            @QueryParam("language") String  language){
    return modifyProperty(EXO_TEXT, newValue, repositoryName, workspaceName, nodeUIID, siteName, language);
  }

  /**
   * SERVICE: Edit value of any property
   *
   * @param propertyName
   * @param newValue
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param nodeUIID the UIID of node
   * @param siteName the site name
   * @param language
   * @return the response
   */
  @POST
  @Path("/property/")
  public Response editProperty( @QueryParam("propertyName") String propertyName,
                                @FormParam("newValue") String newValue,
                                @QueryParam("repositoryName") String repositoryName,
                                @QueryParam("workspaceName") String workspaceName,
                                @QueryParam("nodeUIID") String  nodeUIID,
                                @QueryParam("siteName") String  siteName,
                                @QueryParam("language") String  language){
    String decodedPropertyName =  Text.unescapeIllegalJcrChars(propertyName);
    return modifyProperty(decodedPropertyName, newValue, repositoryName, workspaceName, nodeUIID, siteName, language);
  }

  /**
   * Edit generic property of document.
   * @param propertyName property that need to edit
   * @param newValue the new 'requested property' of document
   * @param repositoryName the repository name
   * @param workspaceName the workspace name
   * @param nodeUIID the UIID of node
   * @param siteName the site name
   *
   * @return the response
   */
  public Response modifyProperty(String propertyName, String newValue, String repositoryName, String workspaceName,
      String nodeUIID,String siteName, String language){
    ResourceBundle resourceBundle = null;
    String messageKey = "";
    String message = "";
    Document document = null;
    Element localeMsg = null;
    try {
      Locale locale = new Locale(language);
      ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
      resourceBundle = resourceBundleService.getResourceBundle(localeFile, locale);
    } catch(Exception ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform create ResourceBundle: ", ex);
      }
    }
    try {
      document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    } catch(Exception ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform create Document object: ", ex);
      }
    }
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    try {
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = sessionProvider.getSession(workspaceName, manageableRepository);
      try {
        localeMsg = document.createElement("bundle");
        Node node = session.getNodeByUUID(nodeUIID);
        node = (Node)session.getItem(node.getPath());
        if(canSetProperty(node)) {
          if (!sameValue(newValue, node, propertyName)) {
            if (newValue.length() > 0) {
              newValue = Text.unescapeIllegalJcrChars(newValue.trim());
              PortalContainerInfo containerInfo = WCMCoreUtils.getService(PortalContainerInfo.class);
              String containerName = containerInfo.getContainerName();
              ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class, containerName);
              if (propertyName.equals(EXO_TITLE)) {
                if (!node.hasProperty(EXO_TITLE))
                  node.addMixin(EXO_RSS_ENABLE);
              }
              if (!propertyName.contains("/")) {
                if (node.hasProperty(propertyName) && node.getProperty(propertyName).getDefinition().isMultiple()) {
                  Value[] currentValue = node.getProperty(propertyName).getValues();
                  if (currentValue==null) currentValue = new Value[1];
                  currentValue[0] = session.getValueFactory().createValue(newValue);
                  node.setProperty(propertyName, currentValue);
                }else {
                  node.setProperty(propertyName, newValue);
                }
              } else {
                int iSlash = propertyName.lastIndexOf("/");
                String subnodePath = propertyName.substring(0, iSlash);
                String subnodeProperty = propertyName.substring(iSlash+1);
                Node subnode = node.getNode(subnodePath);
                if (subnode.hasProperty(subnodeProperty) && subnode.getProperty(subnodeProperty).getDefinition().isMultiple()) {
                  Value[] currentValue = subnode.getProperty(subnodeProperty).getValues();
                  if (currentValue==null) currentValue = new Value[1];
                  currentValue[0] = session.getValueFactory().createValue(newValue);
                  subnode.setProperty(subnodeProperty, currentValue);
                } else {
                  subnode.setProperty(subnodeProperty, newValue);
                }
              }
              ConversationState conversationState = ConversationState.getCurrent();
              conversationState.setAttribute("siteName", siteName);
              listenerService.broadcast(POST_EDIT_CONTENT_EVENT, null, node);
              session.save();
            }
          }
        } else {
          messageKey = "AccessDeniedException.msg";
          message = resourceBundle.getString(messageKey);
          localeMsg.setAttribute("message", message);
          document.appendChild(localeMsg);
          return Response.ok(new DOMSource(document), MediaType.TEXT_XML).cacheControl(cacheControl).build();
        }
      } catch (AccessDeniedException ace) {
        if (LOG.isErrorEnabled()) {
          LOG.error("AccessDeniedException: ", ace);
        }
        messageKey = "AccessDeniedException.msg";
        message = resourceBundle.getString(messageKey);
        localeMsg.setAttribute("message", message);
        document.appendChild(localeMsg);
        return Response.ok(new DOMSource(document), MediaType.TEXT_XML).cacheControl(cacheControl).build();
      } catch (FileNotFoundException fie) {
        if (LOG.isErrorEnabled()) {
          LOG.error("FileNotFoundException: ", fie);
        }
        messageKey = "ItemNotFoundException.msg";
        message = resourceBundle.getString(messageKey);
        localeMsg.setAttribute("message", message);
        document.appendChild(localeMsg);
        return Response.ok(new DOMSource(document), MediaType.TEXT_XML).cacheControl(cacheControl).build();
      }  catch (LockException lockex) {
        if (LOG.isErrorEnabled()) {
          LOG.error("LockException", lockex);
        }
        messageKey = "LockException.msg";
        message = resourceBundle.getString(messageKey);
        localeMsg.setAttribute("message", message);
        document.appendChild(localeMsg);
        return Response.ok(new DOMSource(document), MediaType.TEXT_XML).cacheControl(cacheControl).build();
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform edit title: ", e);
      }
      messageKey = "UIPresentation.label.Exception";
      message = resourceBundle.getString(messageKey);
      localeMsg.setAttribute("message", message);
      document.appendChild(localeMsg);
      return Response.ok(new DOMSource(document), MediaType.TEXT_XML).cacheControl(cacheControl).build();
    }
    localeMsg.setAttribute("message", "OK");
    document.appendChild(localeMsg);
    return Response.ok(new DOMSource(document), MediaType.TEXT_XML).cacheControl(cacheControl).build();
  }
  /**
   * Compare new value with current value property
   *
   * @param newValue the new value of property
   * @param node the document node
   *
   * @return the result of compare
   *
   * @throws Exception the exception
   */
  private boolean sameValue(String newValue, Node node, String propertyName) throws Exception {
    if (!node.hasProperty(propertyName))
      return (newValue == null || newValue.length() == 0);
    if (node.getProperty(propertyName).getDefinition().isMultiple()){
      try {
        return node.getProperty(propertyName).getValues()[0].getString().equals(newValue);
      }catch (Exception e) {
        return false;
      }
    }
    return node.getProperty(propertyName).getString().equals(newValue);
  }

  /**
   * Can set property.
   *
   * @param node the node
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
  public static boolean canSetProperty(Node node) throws RepositoryException {
    return checkPermission(node,PermissionType.SET_PROPERTY);
  }

  private static boolean checkPermission(Node node,String permissionType) throws RepositoryException {
    try {
      ((ExtendedNode)node).checkPermission(permissionType);
      return true;
    } catch(AccessControlException e) {
      return false;
    }
  }
}
