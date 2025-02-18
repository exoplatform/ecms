/**
 *
 */
package org.exoplatform.services.wcm.publication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.jcr.sessions.ACLSessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WCMService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import lombok.SneakyThrows;

import org.picocontainer.Startable;

/**
 * The Class WCMComposerImpl.
 *
 * @author benjamin
 */
@Managed
@NameTemplate( { @Property(key = "view", value = "portal"),
    @Property(key = "service", value = "composer"), @Property(key = "type", value = "content") })
@ManagedDescription("WCM Composer service")
@RESTEndpoint(path = "wcmcomposerservice")
@Deprecated(forRemoval = true, since = "7.0")
public class WCMComposerImpl implements WCMComposer, Startable {

    final static public String EXO_RESTORELOCATION = "exo:restoreLocation";

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The link manager service. */
  private LinkManager linkManager;

  private PublicationService  publicationService;

  private TemplateService templateService;

  private WCMService wcmService;

  private MultiLanguageService multiLanguageService;

  private ACLSessionProviderService aclSessionProviderService;

  private TrashService trashService;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(WCMComposerImpl.class.getName());

  /** The template filter query */
  private String templatesFilter;

  /** OrderBy properties accessed on Front side */
  private List<String> usedOrderBy;
  /** Languages properties accessed on Front side */
  private List<String> usedLanguages;
  /** PrimaryTypes properties accessed on Front side */
  private List<String> usedPrimaryTypes;
  /** shared group membership */
  private String sharedGroup;

  /**
   * Instantiates a new WCM composer impl.
   *
   * @throws Exception the exception
   */
  public WCMComposerImpl(InitParams params) throws Exception {
    if (params!=null) {
      ValueParam sharedGroupParam = params.getValueParam("sharedGroup");
      if (sharedGroupParam != null) {
        this.sharedGroup = sharedGroupParam.getValue();
      }
    }

    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    linkManager = WCMCoreUtils.getService(LinkManager.class);
    publicationService = WCMCoreUtils.getService(PublicationService.class);
    templateService = WCMCoreUtils.getService(TemplateService.class);
    wcmService = WCMCoreUtils.getService(WCMService.class);
    multiLanguageService = WCMCoreUtils.getService(MultiLanguageService.class);
    aclSessionProviderService = WCMCoreUtils.getService(ACLSessionProviderService.class);

    usedLanguages = new ArrayList<String>();
    usedLanguages.add(null);
    usedOrderBy = new ArrayList<String>();
    usedOrderBy.add(null);
    usedPrimaryTypes = new ArrayList<String>();
    usedPrimaryTypes.add(null);

  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.publication.WCMComposer#getContent(java.lang
   * .String, java.lang.String, java.lang.String, java.util.HashMap)
   */
  @SneakyThrows
  public Node getContent(String workspace,
                         String nodeIdentifier,
                         Map<String, String> filters,
                         SessionProvider sessionProvider) {
    String mode = filters.get(FILTER_MODE);
    String version = filters.get(FILTER_VERSION);
    String visibility = filters.get(FILTER_VISIBILITY);
    String remoteUser = getRemoteUser();
    String repository = null;

    try {
      repository = repositoryService.getCurrentRepository().getConfiguration().getName();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }

    if (workspace==null) {
      if (nodeIdentifier.lastIndexOf("/") == 0) nodeIdentifier = nodeIdentifier.substring(1);
      String[] params = nodeIdentifier.split("/");
      workspace = params[1];
      try {
        nodeIdentifier = nodeIdentifier.substring(repository.length()+workspace.length()+1);
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }

    Node node = null;
    if (WCMComposer.VISIBILITY_PUBLIC.equals(visibility) && MODE_LIVE.equals(mode)) {
      sessionProvider = remoteUser == null ?
                                           aclSessionProviderService.getAnonymSessionProvider() :
                                           aclSessionProviderService.getACLSessionProvider(getAnyUserACL());
    }
    node = wcmService.getReferencedContent(sessionProvider, workspace, nodeIdentifier);
    if (version == null || !BASE_VERSION.equals(version)) {
      node = getViewableContent(node, filters);
    }

    return node;
  }

  /**
   * Gets the node view.
   *
   * @param node the node
   *
   * @return the node view
   *
   * @throws Exception the exception
   */
  @SneakyThrows
  private Node getViewableContent(Node node, Map<String, String> filters) {
    Node viewNode = null;
    if (trashService == null) {
      trashService = WCMCoreUtils.getService(TrashService.class);
    }
    try {
      node = getTargetNode(node);
    } catch (AccessDeniedException ade) {
      return null;
    }

    if (node != null && trashService.isInTrash(node)) {
      return null;
    }

    String languageFilter = filters.get(FILTER_LANGUAGE);
    Boolean translation = Boolean.parseBoolean(filters.get(FILTER_TRANSLATION));
    if (languageFilter!=null && translation) {
      addUsedLanguage(languageFilter);
      Node lnode = null;
      try {
        lnode = multiLanguageService.getLanguage(node, languageFilter);
      } catch (AccessDeniedException e) {
        if (LOG.isTraceEnabled()) LOG.trace("AccessDenied on "+languageFilter+" translation for "+node.getPath());
      }
      if (lnode!=null) {

        viewNode = getPublishedContent(lnode, filters);
        if (viewNode!=null) {
          return viewNode;
        }
        return null;
      }
    }

    if (node != null) {
      viewNode = getPublishedContent(node, filters);
    }

    return viewNode;
  }


  private Node getPublishedContent(Node node, Map<String, String> filters) throws Exception {
    HashMap<String, Object> context = new HashMap<String, Object>();
    String mode = filters.get(FILTER_MODE);
    context.put(WCMComposer.FILTER_MODE, mode);
    context.put(WCMComposer.PORTLET_MODE, filters.get(PORTLET_MODE));
    String lifecyleName = null;
    try {
      lifecyleName = publicationService.getNodeLifecycleName(node);
    } catch (NotInPublicationLifecycleException e) {
      // Don't log here, this is normal
    }
    if (lifecyleName == null) return node;
    PublicationPlugin publicationPlugin = publicationService.getPublicationPlugins().get(lifecyleName);
    Node viewNode = publicationPlugin.getNodeView(node, context);
    return viewNode;

  }

  private Node getTargetNode(Node showingNode) throws Exception {
    Node targetNode = null;
    if (linkManager.isLink(showingNode)) {
      try {
        targetNode = linkManager.getTarget(showingNode);
      } catch (ItemNotFoundException e) {
        targetNode = showingNode;
      }
    } else {
      targetNode = showingNode;
    }
    return targetNode;
  }

  String displayCategory(Node node, List<Node> taxonomyTrees) {
    try {
      for (Node taxonomyTree : taxonomyTrees) {
        if (node.getPath().contains(taxonomyTree.getPath())) {
          return node.getPath().replace(taxonomyTree.getPath(), taxonomyTree.getName());
        }
      }
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled())LOG.error("Unexpected error when getting node taxonomies");
    }
    return "";
  }

  @Managed
  @ManagedDescription("Used Languages")
  public List<String> getUsedLanguages() {
    return usedLanguages;
  }

  @Managed
  @ManagedDescription("Used Primary Types")
  public List<String> getUsedPrimaryTypes() {
    return usedPrimaryTypes;
  }

  @Managed
  @ManagedDescription("Used Order By")
  public List<String> getUsedOrderBy() {
    return usedOrderBy;
  }

  private void addUsedLanguage(String lang) {
    if (!usedLanguages.contains(lang)) usedLanguages.add(lang);
  }

  private List<AccessControlEntry> getAnyUserACL() {
    List<AccessControlEntry> ret = new ArrayList<AccessControlEntry>();
    ret.add(new AccessControlEntry(sharedGroup, PermissionType.READ));
    return ret;
  }

  /**
   * Get login User
   * @return user name
   */
  private String getRemoteUser() {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity identity = conversationState.getIdentity();
    if (identity != null && !IdentityConstants.ANONIM.equals(identity.getUserId())) {
      return identity.getUserId();
    }
    return null;
  }
}
