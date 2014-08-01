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
package org.exoplatform.services.wcm.extensions.publication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.lock.Lock;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.extensions.publication.context.impl.ContextConfig.Context;
import org.exoplatform.services.wcm.extensions.publication.impl.PublicationManagerImpl;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.AuthoringPublicationConstant;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.State;
import org.exoplatform.services.wcm.extensions.utils.ContextComparator;
import org.exoplatform.services.wcm.publication.WebpagePublicationPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class WCMPublicationServiceImpl
                                      extends
                                      org.exoplatform.services.wcm.publication.WCMPublicationServiceImpl {

  private static final Log LOG = ExoLogger.getLogger(WCMPublicationServiceImpl.class.getName());
  
  private String publicationLocation = "collaboration:/";
  
  private String[] notAllowChildNodeEnrollInPubliction = new String[] { NodetypeConstant.EXO_WEBCONTENT };

  /**
   * Instantiates a new WCM publication service. This service delegate to
   * PublicationService to manage the publication
   */
  public WCMPublicationServiceImpl(InitParams initParams) {
    super();
    this.publicationService = WCMCoreUtils.getService(PublicationService.class);
    if(initParams.getValueParam("publicationLocation") != null) {
      publicationLocation = initParams.getValueParam("publicationLocation").getValue();
    }
    if(initParams.getValueParam("notAllowChildNodeEnrollInPubliction") != null) {
      if(initParams.getValueParam("notAllowChildNodeEnrollInPubliction").getValue().indexOf(";") > -1) {
        notAllowChildNodeEnrollInPubliction = 
                initParams.getValueParam("notAllowChildNodeEnrollInPubliction").getValue().split(";");
      }
      
    }
  }

  /**
   * This default implementation uses "States and versions based publication" as
   * a default lifecycle for all sites and "Simple Publishing" for the root
   * user.
   */
  public void enrollNodeInLifecycle(Node node, String siteName, String remoteUser) {
    try {
      if (LOG.isInfoEnabled()) LOG.info(node.getPath() + "::" + siteName + "::"+remoteUser);

      PublicationManagerImpl publicationManagerImpl = WCMCoreUtils.getService(PublicationManagerImpl.class);

      ContextComparator comparator = new ContextComparator();
      TreeSet<Context> treeSetContext = new TreeSet<Context>(comparator);
      treeSetContext.addAll(publicationManagerImpl.getContexts());
      for (Context context : treeSetContext) {
        boolean pathVerified = true;
        boolean nodetypeVerified = true;
        boolean siteVerified = true;
        boolean membershipVerified = true;
        String path = context.getPath();
        String nodetype = context.getNodetype();
        String site = context.getSite();
        List<String> memberships = new ArrayList<String>();
        if (context.getMembership() != null) {
          memberships.add(context.getMembership());
        }
        if (context.getMemberships() != null) {
          memberships.addAll(context.getMemberships());
        }
        if (path != null) {
          String workspace = node.getSession().getWorkspace().getName();
          ManageableRepository manaRepository = (ManageableRepository) node.getSession()
                                                                           .getRepository();
          String repository = manaRepository.getConfiguration().getName();
          String[] pathTab = path.split(":");
          pathVerified = node.getPath().contains(pathTab[2]) && (repository.equals(pathTab[0]))
              && (workspace.equals(pathTab[1]));
        }
        if (nodetype != null)
          nodetypeVerified = nodetype.equals(node.getPrimaryNodeType().getName());
        if (site != null)
          siteVerified = site.equals(siteName);
        if (memberships.size() > 0) {
          for (String membership : memberships) {
            String[] membershipTab = membership.split(":");
            IdentityRegistry identityRegistry = WCMCoreUtils.getService(IdentityRegistry.class);
            Identity identity = identityRegistry.getIdentity(remoteUser);
            membershipVerified = identity.isMemberOf(membershipTab[1], membershipTab[0]);
            if (membershipVerified)
              break;
          }
        }
        if (pathVerified && nodetypeVerified && siteVerified && membershipVerified) {
          Lifecycle lifecycle = publicationManagerImpl.getLifecycle(context.getLifecycle());
          String lifecycleName = this.getWebpagePublicationPlugins()
                                     .get(lifecycle.getPublicationPlugin())
                                     .getLifecycleName();
          if (node.canAddMixin("publication:authoring")) {
            node.addMixin("publication:authoring");
            node.setProperty("publication:lastUser", remoteUser);
            node.setProperty("publication:lifecycle", lifecycle.getName());

          }
          enrollNodeInLifecycle(node, lifecycleName);
          setInitialState(node, lifecycle, remoteUser);
          break;
        }
      }
    } catch (Exception ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Couldn't complete the enrollement : ", ex);
      }
    }
  }

  /**
   * Automatically move to initial state if 'automatic'
   *
   * @param node
   * @param lifecycle
   * @throws Exception
   */
  private void setInitialState(Node node, Lifecycle lifecycle, String remoteUser) throws Exception {
    List<State> states = lifecycle.getStates();
    if (states == null || states.size() <= 0) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("could not find an initial state in lifecycle " + lifecycle.getName());
      }
    } else {
      String initialState = states.get(0).getState();
      PublicationPlugin publicationPlugin = publicationService.getPublicationPlugins()
                                                              .get(AuthoringPublicationConstant.LIFECYCLE_NAME);
      HashMap<String, String> context = new HashMap<String, String>();

      NodeLocation currentRevisionLocation = NodeLocation.getNodeLocationByNode(node);

      Node currentRevision = getCurrentRevision(currentRevisionLocation);
      if (currentRevision != null) {
        context.put(AuthoringPublicationConstant.CURRENT_REVISION_NAME, currentRevision.getName());
      }
      try {
        if (node.isLocked()) {
          Lock lock = node.getLock();
          String owner = lock.getLockOwner();
          if (LOG.isInfoEnabled())
            LOG.info("node is locked by owner, unlocking it for enrollement");
          if (node.holdsLock() && remoteUser.equals(owner)) {
            String lockToken = LockUtil.getLockToken(node);
            if (lockToken != null) {
              node.getSession().addLockToken(lockToken);
            }
            node.unlock();
            node.removeMixin(Utils.MIX_LOCKABLE);
            // remove lock from Cache
            LockUtil.removeLock(node);
          }
        }

        context.put(AuthoringPublicationConstant.IS_INITIAL_PHASE, "true");
        node.setProperty("publication:lastUser", remoteUser);
        publicationPlugin.changeState(node, initialState, context);         
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Error setting staged state : ", e);
        }
      }
    }

  }

  public Node getCurrentRevision(NodeLocation currentRevisionLocation) {
    return NodeLocation.getNodeByLocation(currentRevisionLocation);
  }

  /**
   * This default implementation checks if the state is valid then delegates the
   * update to the node WebpagePublicationPlugin.
   */
  public void updateLifecyleOnChangeContent(Node node,
                                            String siteName,
                                            String remoteUser,
                                            String newState) throws Exception {
    if(!node.getPath().startsWith(publicationLocation.split(":")[1]) 
			&& !publicationService.isNodeEnrolledInLifecycle(node)) return;
    if(node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
      for(String nodeType : notAllowChildNodeEnrollInPubliction) {
        if(!allowEnrollInPublication(node, nodeType)) return;
      }
    }
    if (!publicationService.isNodeEnrolledInLifecycle(node)) {
      enrollNodeInLifecycle(node, siteName, remoteUser);
    }
    String lifecycleName = publicationService.getNodeLifecycleName(node);
    WebpagePublicationPlugin publicationPlugin = this.getWebpagePublicationPlugins()
                                                     .get(lifecycleName);

    publicationPlugin.updateLifecyleOnChangeContent(node, remoteUser, newState);

    listenerService.broadcast(UPDATE_EVENT, cmsService, node);
  }
  
  private boolean allowEnrollInPublication(Node node, String nodeType) throws Exception {
    String path = node.getPath();
    Node parentNode = node.getParent();
    while(!path.equals("/") && path.length() > 0) {
      parentNode = (Node)node.getSession().getItem(path);
      if(parentNode.isNodeType(nodeType)) return false;
      path = StringUtils.substringBefore(path, path.substring(path.lastIndexOf("/"), path.length()));
    }
    return true;
  }
}
