package org.exoplatform.wcm.connector.collaboration;

import java.io.FileNotFoundException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SEA
 * Author : Ha Quang Tan
 * tan.haquang@exoplatform.com
 * Mar 23, 2011
 */
@Path("/contents/editing/")
public class InlineEditingService implements ResourceContainer{
	private static Log log = ExoLogger.getLogger(InlineEditingService.class);
	final static public String EXO_TITLE 								= "exo:title".intern();
	final static public String EXO_SUMMARY 							= "exo:summary".intern();

	final static public String EXO_RSS_ENABLE 					= "exo:rss-enable".intern();	
	public final static String POST_EDIT_CONTENT_EVENT = "CmsService.event.postEdit".intern();
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
	 *
	 * @throws Exception the exception
	 */
	@POST
	@Path("/title/")
	public Response editTitle(@FormParam("newValue") String newTitle,
			@QueryParam("repositoryName") String repositoryName,
			@QueryParam("workspaceName") String workspaceName,
			@QueryParam("nodeUIID") String  nodeUIID,
			@QueryParam("siteName") String  siteName){
		return modifyProperty(EXO_TITLE, newTitle, repositoryName, workspaceName, nodeUIID, siteName);
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
	 *
	 * @throws Exception the exception
	 */
	@POST
	@Path("/summary/")
	public Response editSummary(@FormParam("newValue") String newSummary,
			@QueryParam("repositoryName") String repositoryName,
			@QueryParam("workspaceName") String workspaceName,
			@QueryParam("nodeUIID") String  nodeUIID,
			@QueryParam("siteName") String  siteName){
		return modifyProperty(EXO_SUMMARY, newSummary, repositoryName, workspaceName, nodeUIID, siteName);
	}

	/**
	 * SERVICE: Edit value of any property
	 *
	 * @param newSummary the new summary of document
	 * @param repositoryName the repository name
	 * @param workspaceName the workspace name
	 * @param nodeUIID the UIID of node
	 * @param siteName the site name
	 *
	 * @return the response
	 *
	 * @throws Exception the exception
	 */
	@POST
	@Path("/property/")
	public Response editProperty( @QueryParam("propertyName") String propertyName,
			@FormParam("newValue") String newValue,
			@QueryParam("repositoryName") String repositoryName,
			@QueryParam("workspaceName") String workspaceName,
			@QueryParam("nodeUIID") String  nodeUIID,
			@QueryParam("siteName") String  siteName){
		return modifyProperty(propertyName, newValue, repositoryName, workspaceName, nodeUIID, siteName);
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
	 *
	 * @throws Exception the exception
	 */
	public Response modifyProperty(String propertyName, String newValue, String repositoryName, String workspaceName,
			String nodeUIID,String siteName){
		try {
			SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
			RepositoryService repositoryService = (RepositoryService)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RepositoryService.class);
			ManageableRepository manageableRepository = repositoryService.getRepository(repositoryName);
			Session session = sessionProvider.getSession(workspaceName, manageableRepository);		    
			try {
				Node node = (Node)session.getNodeByUUID(nodeUIID);			    
				if (!sameValue(newValue, node, propertyName)) {
					if (newValue.length() > 0) {
						newValue = Text.escapeIllegalJcrChars(newValue.trim());
						ExoContainer container = ExoContainerContext.getCurrentContainer();
						PortalContainerInfo containerInfo = (PortalContainerInfo)container.getComponentInstanceOfType(PortalContainerInfo.class);
						String containerName = containerInfo.getContainerName();		    	    	    
						ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class, containerName);
						if (propertyName.equals(EXO_TITLE)) {
							if (!node.hasProperty(EXO_TITLE))
								node.addMixin(EXO_RSS_ENABLE);
						}
						node.setProperty(propertyName, newValue);			            
						node.save();
						ConversationState conversationState = ConversationState.getCurrent();		
						conversationState.setAttribute("siteName", siteName);
						listenerService.broadcast(POST_EDIT_CONTENT_EVENT, null, node);			            	
					}
				}
				session.save();
			} catch (AccessDeniedException ace) {
				log.error("AccessDeniedException: ", ace);
				return Response.status(Status.UNAUTHORIZED).build();
			} catch (FileNotFoundException fie) {
				log.error("FileNotFoundException: ", fie);
				return Response.status(Status.NOT_FOUND).build();		    
			}  catch (LockException lockex) {
				log.error("LockException", lockex);
				return Response.status(Status.UNAUTHORIZED).build();
			}
		} catch (Exception e) {
			log.error("Error when perform edit title: ", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} 
		return Response.status(Status.OK).build();
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
		return node.getProperty(propertyName).getString().equals(newValue);
	}

}