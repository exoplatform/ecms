/**
 *
 */
package org.exoplatform.services.wcm.publication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
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
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
public class WCMComposerImpl implements WCMComposer, Startable {

    final static public String EXO_RESTORELOCATION = "exo:restoreLocation";

  /** The repository service. */
  private RepositoryService repositoryService;

  /** The link manager service. */
  private LinkManager linkManager;

  private PublicationService  publicationService;

  private TaxonomyService  taxonomyService;

  private TemplateService templateService;

  private WCMService wcmService;

  private MultiLanguageService multiLanguageService;
  
  private ACLSessionProviderService aclSessionProviderService;

  private TrashService trashService;
  /** The cache. */
  private ExoCache<String, Object> cache;

  private boolean isCached = true;

  private boolean useDefaultLanguage = true;

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(WCMComposerImpl.class);

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
      ValueParam useCache = params.getValueParam("useCache");
      if (useCache != null)
        this.isCached = Boolean.parseBoolean(useCache.getValue());
      ValueParam useDefaultLanguage = params.getValueParam("useDefaultLanguage");
      if (useDefaultLanguage != null)
        this.useDefaultLanguage = Boolean.parseBoolean(useDefaultLanguage.getValue());
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
    cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance("wcm.composer");
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
  @Deprecated
  public Node getContent(String repository,
                         String workspace,
                         String nodeIdentifier,
                         HashMap<String, String> filters,
                         SessionProvider sessionProvider) throws Exception {
    return getContent(workspace, nodeIdentifier, filters, sessionProvider);
  }
  
  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.publication.WCMComposer#getContent(java.lang
   * .String, java.lang.String, java.lang.String, java.util.HashMap)
   */
  public Node getContent(String workspace,
                         String nodeIdentifier,
                         HashMap<String, String> filters,
                         SessionProvider sessionProvider) throws Exception {
    String mode = filters.get(FILTER_MODE);
    String version = filters.get(FILTER_VERSION);
    String language = filters.get(FILTER_LANGUAGE);
    String visibility = filters.get(FILTER_VISIBILITY);
    String remoteUser = null;
    String repository = null;
    if (WCMComposer.VISIBILITY_PUBLIC.equals(visibility)) {
      remoteUser = "##PUBLIC##VISIBILITY";
    } else {
      remoteUser = getRemoteUser();
    }
    try {
      repository = ((ManageableRepository)repositoryService.getCurrentRepository()).getConfiguration().getName();
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
    if (MODE_LIVE.equals(mode) && isCached) {
      String hash = getHash(nodeIdentifier, version, remoteUser, language, null, null, null, null);
      Node cachedNode = (Node)cache.get(hash);
      if (cachedNode != null) return cachedNode;
    }
    Node node = null;
    try {
      if (WCMComposer.VISIBILITY_PUBLIC.equals(visibility) && MODE_LIVE.equals(mode) && remoteUser != null) {
        sessionProvider = aclSessionProviderService.getACLSessionProvider(getAnyUserACL());
      }
      node = wcmService.getReferencedContent(sessionProvider, workspace, nodeIdentifier);
    } catch (RepositoryException e) {
      node = getNodeByCategory(nodeIdentifier);
    }
    if (version == null || !BASE_VERSION.equals(version)) {
      node = getViewableContent(node, filters);
    }
    if (MODE_LIVE.equals(mode) && isCached) {
      String hash = getHash(nodeIdentifier, version, remoteUser, language, null, null, null, null);
      cache.put(hash, node);
    }
    return node;
  }
  
  @Deprecated
  public List<Node> getContents(String repository,
                                String workspace,
                                String path,
                                HashMap<String, String> filters,
                                SessionProvider sessionProvider) throws Exception {
    return getContents(workspace, path, filters, sessionProvider);
  }
  
  @SuppressWarnings("unchecked")
  public List<Node> getContents(String workspace,
                                String path,
                                HashMap<String, String> filters,
                                SessionProvider sessionProvider) throws Exception {
    String mode = filters.get(FILTER_MODE);
    String version = filters.get(FILTER_VERSION);
    String orderBy = filters.get(FILTER_ORDER_BY);
    String orderType = filters.get(FILTER_ORDER_TYPE);
    String language = filters.get(FILTER_LANGUAGE);
    String recursive = filters.get(FILTER_RECURSIVE);
    String primaryType = filters.get(FILTER_PRIMARY_TYPE);
    String visibility = filters.get(FILTER_VISIBILITY);    
    String remoteUser = null;
    if (WCMComposer.VISIBILITY_PUBLIC.equals(visibility)) {
      remoteUser = "##PUBLIC##VISIBILITY";
    } else {
      remoteUser = getRemoteUser();
    }

    if (MODE_EDIT.equals(mode) && "publication:liveDate".equals(orderBy)) {
      orderBy = "exo:dateModified";
      filters.put(FILTER_ORDER_BY, orderBy);
    }
    if (MODE_LIVE.equals(mode) && "exo:title".equals(orderBy)) {
      orderBy = "exo:titlePublished "+orderType+", exo:title";
      filters.put(FILTER_ORDER_BY, orderBy);
    }

    if (MODE_LIVE.equals(mode) && isCached) {
      String hash = getHash(path, version, remoteUser, language, recursive, orderBy, orderType, primaryType);
      List<Node> cachedNodes = (List<Node>)cache.get(hash);
      if (cachedNodes != null) return cachedNodes;
    }
    List<Node> nodes = new ArrayList<Node>();
    try {
      if (WCMComposer.VISIBILITY_PUBLIC.equals(visibility) && MODE_LIVE.equals(mode) && remoteUser != null) {
        sessionProvider = aclSessionProviderService.getACLSessionProvider(getAnyUserACL());
      }
      if (LOG.isDebugEnabled()) LOG.debug("##### "+path+":"+version+":"+remoteUser+":"+orderBy+":"+orderType);
      NodeIterator nodeIterator = getViewableContents(workspace, path, filters, sessionProvider, false);
    
      Node node = null, viewNode = null;
      while (nodeIterator != null && nodeIterator.hasNext()) {
        node = nodeIterator.nextNode();
        viewNode = getViewableContent(node, filters);
        if (viewNode != null) {
          nodes.add(viewNode);
        }
      }
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    if (MODE_LIVE.equals(mode) && isCached) {
      String hash = getHash(path, version, remoteUser, language, recursive, orderBy, orderType, primaryType);
      cache.put(hash, nodes);
    }
    return nodes;
  }

  public Result getPaginatedContents(NodeLocation nodeLocation,
                                     HashMap<String, String> filters,
                                     SessionProvider sessionProvider) throws Exception {
    String path = nodeLocation.getPath();
    String workspace = nodeLocation.getWorkspace();
    
    String mode = filters.get(FILTER_MODE);
    String version = filters.get(FILTER_VERSION);
    String orderBy = filters.get(FILTER_ORDER_BY);
    String orderType = filters.get(FILTER_ORDER_TYPE);
    String language = filters.get(FILTER_LANGUAGE);
    String recursive = filters.get(FILTER_RECURSIVE);
    String primaryType = filters.get(FILTER_PRIMARY_TYPE);
    String visibility = filters.get(FILTER_VISIBILITY);
    long offset = (filters.get(FILTER_OFFSET)!=null)?new Long(filters.get(FILTER_OFFSET)):0;
    long totalSize = (filters.get(FILTER_TOTAL)!=null)?new Long(filters.get(FILTER_TOTAL)):0;
    
    String remoteUser = null;
    if (WCMComposer.VISIBILITY_PUBLIC.equals(visibility)) {
      remoteUser = "##PUBLIC##VISIBILITY";
    } else {
      remoteUser = getRemoteUser();
    }

    if (MODE_EDIT.equals(mode) && "publication:liveDate".equals(orderBy)) {
      orderBy = "exo:dateModified";
      filters.put(FILTER_ORDER_BY, orderBy);
    }
    if (MODE_LIVE.equals(mode) && "exo:title".equals(orderBy)) {
      orderBy = "exo:titlePublished "+orderType+", exo:title";
      filters.put(FILTER_ORDER_BY, orderBy);
    }

    if (MODE_LIVE.equals(mode) && isCached && offset==0) {
      String hash = getHash(path, version, remoteUser, language, recursive, orderBy, orderType, primaryType);
      Result cachedNodes = (Result)cache.get(hash);
      if (cachedNodes != null) return cachedNodes;
    }
    if (LOG.isDebugEnabled()) LOG.debug("##### "+path+":"+version+":"+remoteUser+":"+orderBy+":"+orderType);

    NodeIterator nodeIterator ;
    if (totalSize==0) {
      SessionProvider systemProvider = WCMCoreUtils.getSystemSessionProvider();
      nodeIterator = getViewableContents(workspace, path, filters, systemProvider, false);
      if (nodeIterator != null) {
        totalSize = nodeIterator.getSize();
      }
    }

    if (WCMComposer.VISIBILITY_PUBLIC.equals(visibility) && MODE_LIVE.equals(mode) && remoteUser != null) {
      sessionProvider = aclSessionProviderService.getACLSessionProvider(getAnyUserACL());
    }

    nodeIterator = getViewableContents(workspace, path, filters, sessionProvider, true);
    List<Node> nodes = new ArrayList<Node>();
    Node node = null, viewNode = null;
    if (nodeIterator != null) {
      while (nodeIterator.hasNext()) {
        node = nodeIterator.nextNode();
        viewNode = getViewableContent(node, filters);
        if (viewNode != null) {
          nodes.add(viewNode);
        }
      }
    }
    Result result = new Result(nodes, offset, totalSize, nodeLocation, filters);

    if (MODE_LIVE.equals(mode) && isCached && offset==0 ) {
      String hash = getHash(path, version, remoteUser, language, recursive, orderBy, orderType, primaryType);
//     cache.remove(hash);
      cache.put(hash, result);
    }
    return result;
  }
  
  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.publication.WCMComposer#getContents(java.lang
   * .String, java.lang.String, java.lang.String, java.util.HashMap)
   */
  private NodeIterator getViewableContents(String workspace,
                                           String path,
                                           HashMap<String, String> filters,
                                           SessionProvider sessionProvider, boolean paginated) throws Exception {
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(workspace, manageableRepository);
    QueryManager manager = session.getWorkspace().getQueryManager();
    String orderBy = filters.get(FILTER_ORDER_BY);
    String orderFilter = getOrderSQLFilter(filters);
    String recursive = filters.get(FILTER_RECURSIVE);
    String primaryType = filters.get(FILTER_PRIMARY_TYPE);
    String queryFilter = filters.get(FILTER_QUERY);
    String queryFilterFull = filters.get(FILTER_QUERY_FULL);
    StringBuffer statement = new StringBuffer();
    boolean filterTemplates = true;
    if (queryFilterFull!=null) {
      statement.append(queryFilterFull);
    } else {
      addUsedPrimaryTypes(primaryType);
      if (primaryType == null) {
        primaryType = "nt:base";
        Node currentFolder = null;
        if ("/".equals(path)) {
          currentFolder = session.getRootNode();
        } else if (session.getRootNode().hasNode(path.substring(1))) {
          currentFolder = session.getRootNode().getNode(path.substring(1));
        } else {
          return null;
        }
               
        if (currentFolder != null && currentFolder.isNodeType("exo:taxonomy")) {
          primaryType = "exo:taxonomyLink";
        }
      } else {
        filterTemplates = false;
      }
      addUsedOrderBy(orderBy);

      statement.append("SELECT * FROM " + primaryType + " WHERE (jcr:path LIKE '" + path + "/%'");
      if (recursive==null || "false".equals(recursive)) {
        statement.append(" AND NOT jcr:path LIKE '" + path + "/%/%')");
      } else {
        statement.append(")");
      }
      if (filterTemplates) statement.append(" AND " + getTemplatesSQLFilter());
      if (queryFilter!=null) {
        statement.append(queryFilter);
      }
      statement.append(orderFilter);
    }
    Query query = manager.createQuery(statement.toString(), Query.SQL);

    if (paginated) {
      long offset = (filters.get(FILTER_OFFSET)!=null)?new Long(filters.get(FILTER_OFFSET)):0;
      long limit = (filters.get(FILTER_LIMIT)!=null)?new Long(filters.get(FILTER_LIMIT)):0;
      if (limit>0) {
        ((QueryImpl)query).setOffset(offset);
        ((QueryImpl)query).setLimit(limit);
      }
    }

    return query.execute().getNodes();
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
  private Node getViewableContent(Node node, HashMap<String, String> filters) throws Exception {
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
    if (languageFilter!=null) {
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
        } else if (!useDefaultLanguage) {
          return null;
        }
      }
    }

    if (node != null) {
      viewNode = getPublishedContent(node, filters);
    }

    return viewNode;
  }


  private Node getPublishedContent(Node node, HashMap<String, String> filters) throws Exception {
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


  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.publication.WCMComposer#updateContent(java
   * .lang.String, java.lang.String, java.lang.String, java.util.HashMap)
   */
  @Deprecated
  public boolean updateContent(String repository,
                               String workspace,
                               String path,
                               HashMap<String, String> filters) throws Exception {
    return updateContent(workspace, path, filters);
  }
  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.publication.WCMComposer#updateContent(java
   * .lang.String, java.lang.String, java.lang.String, java.util.HashMap)
   */
  public boolean updateContent(String workspace, String path, HashMap<String, String> filters) throws Exception {
    if (isCached) {
      String[] orderTypes = {null, "ASC", "DESC"};
      if (LOG.isDebugEnabled()) LOG.debug("updateContent : "+path);
      String part = (path.lastIndexOf("/") >= 0) ? path.substring(0, path.lastIndexOf("/")) : path;
      String remoteUser = getRemoteUser();

      String oid = null;
      SessionProvider sessionProvider = SessionProvider.createSystemProvider();
      try {
        /**
         * Replace repository parameter by a Repository instance as we get wrong
         * toString from Repository in WCMPublicationService
         */
        /**
         * END quick fix
         */
        Node node = wcmService.getReferencedContent(sessionProvider, workspace, path);
        if (node!=null) {
          if (node.isNodeType("mix:referenceable")) oid = node.getUUID();
          /* remove parent cache */
          updateContents(workspace, part, filters);
          taxonomyService = WCMCoreUtils.getService(TaxonomyService.class);
          for (Node catnode : taxonomyService.getAllCategories(node)) {
            updateContents(catnode.getSession().getWorkspace().getName(), catnode.getPath(), filters);
          }

        }
      } catch (RepositoryException e) {
        if (LOG.isErrorEnabled()) LOG.error("Can't find UUID for path : "+workspace+":"+path);
      }

      for (String lang:usedLanguages) {
        for (String recursive:new String[]{"true", "false"}) {
          for (String orderBy:usedOrderBy) {
            for (String orderType:orderTypes) {
              for (String primaryType:usedPrimaryTypes) {

                /* remove live cache */
                String hash = getHash(path, null, null, lang, null, orderBy, orderType, primaryType);
                cache.remove(hash);
                /* remove base content cache */
                hash = getHash(path, BASE_VERSION, null, lang, null, orderBy, orderType, primaryType);
                cache.remove(hash);

                Node node = wcmService.getReferencedContent(sessionProvider, workspace, path);
                List<Node> listCategory = getCategories(node);
                List<Node> lstTaxonomyTrees = getAllTaxonomyTrees();
                if (listCategory != null && listCategory.size() > 0) {
                  for (Node categoryNode: listCategory) {
                    StringBuffer valBuf = new StringBuffer();
                    valBuf.append(displayCategory(categoryNode, lstTaxonomyTrees));
                    if (valBuf != null && valBuf.length() > 0) {
                      valBuf.append("/").append(node.getName());
                      hash = getHash(valBuf.toString(),
                                     filters.get(FILTER_VERSION),
                                     remoteUser,
                                     lang,
                                     null,
                                     orderBy,
                                     orderType,
                                     primaryType);
                      cache.remove(hash);
                    }
                  }
                }

                /* remove parent cache */
                hash = getHash(part, null, null, lang, recursive, orderBy, orderType, primaryType);
                cache.remove(hash);
                if (oid!=null) {
                  /* remove live cache */
                  hash = getHash(oid, null, null, lang, null, orderBy, orderType, primaryType);
                  cache.remove(hash);
                }
                if (remoteUser!=null) {
                  /* remove live cache for current user */
                  hash = getHash(path, null, remoteUser, lang, null, orderBy, orderType, primaryType);
                  cache.remove(hash);
                  /* remove base content cache for current user */
                  hash = getHash(path, BASE_VERSION, remoteUser, lang, null, orderBy, orderType, primaryType);
                  cache.remove(hash);
                  /* remove parent cache for current user */
                  hash = getHash(part, null, remoteUser, lang, null, orderBy, orderType, primaryType);
                  cache.remove(hash);
                  if (oid!=null) {
                    /* remove live cache */
                    hash = getHash(oid, null, remoteUser, lang, null, orderBy, orderType, primaryType);
                    cache.remove(hash);
                  }
                }
              }

            }
          }
        }
      }
      sessionProvider.close();
    }

    return true;
  }
  

  @Deprecated
  public List<Node> getCategories(Node node, String repository) throws Exception {
    return getCategories(node);
  }
  
  public List<Node> getCategories(Node node) throws Exception {
    if (taxonomyService==null) taxonomyService = WCMCoreUtils.getService(TaxonomyService.class);
    List<Node> listCategories = new ArrayList<Node>();
    List<Node> listNode = getAllTaxonomyTrees();
    for(Node itemNode : listNode) {
      listCategories.addAll(taxonomyService.getCategories(node, itemNode.getName()));
    }
    return listCategories;
  }  

  List<Node> getAllTaxonomyTrees() throws RepositoryException {
    if (taxonomyService==null) taxonomyService = WCMCoreUtils.getService(TaxonomyService.class);
    return taxonomyService.getAllTaxonomyTrees();
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

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.publication.WCMComposer#updateContents(java
   * .lang.String, java.lang.String, java.lang.String, java.util.HashMap)
   */
  @Deprecated
  public boolean updateContents(String repository,
                                String workspace,
                                String path,
                                HashMap<String, String> filters) throws Exception {
    return updateContents(workspace, path, filters);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.publication.WCMComposer#updateContents(java
   * .lang.String, java.lang.String, java.lang.String, java.util.HashMap)
   */
  public boolean updateContents(String workspace, String path, HashMap<String, String> filters) throws Exception {
    if (isCached) {
      String[] orderTypes = {null, "ASC", "DESC"};
      String remoteUser = getRemoteUser();

      if (LOG.isDebugEnabled()) LOG.debug("updateContents : "+path);

      for (String lang:usedLanguages) {
        for (String recursive:new String[]{"true", "false"}) {
          for (String orderBy:usedOrderBy) {
            for (String orderType:orderTypes) {
              for (String primaryType:usedPrimaryTypes) {
                String hash = getHash(path, null, null, lang, recursive, orderBy, orderType, primaryType);
                cache.remove(hash);
                hash = getHash(path, BASE_VERSION, null, lang, recursive, orderBy, orderType, primaryType);
                cache.remove(hash);
                if (remoteUser!=null) {
                  hash = getHash(path, null, remoteUser, lang, recursive, orderBy, orderType, primaryType);
                  cache.remove(hash);
                  hash = getHash(path, BASE_VERSION, remoteUser, lang, recursive, orderBy, orderType, primaryType);
                  cache.remove(hash);
                }
              }
            }
          }
        }
      }

    }
    return true;
  }  
  /**
   * We currently support 2 modes :
   * MODE_LIVE : PUBLISHED state only
   * MODE_EDIT : PUBLISHED, DRAFT, PENDING, STAGED, APPROVED allowed.
   *
   * @param mode the current mode (MODE_LIVE or MODE_EDIT)
   *
   * @return the allowed states
   */
  public List<String> getAllowedStates(String mode) {
    List<String> states = new ArrayList<String>();
    if (MODE_LIVE.equals(mode)) {
      states.add(PublicationDefaultStates.PUBLISHED);
    } else if (MODE_EDIT.equals(mode)) {
      states.add(PublicationDefaultStates.PUBLISHED);
      states.add(PublicationDefaultStates.DRAFT);
      states.add(PublicationDefaultStates.PENDING);
      states.add(PublicationDefaultStates.STAGED);
      states.add(PublicationDefaultStates.APPROVED);
    }
    return states;
  }


  @Managed
  @ManagedDescription("Clean all templates in Composer")
    public void cleanTemplates() throws Exception {
      this.templatesFilter = null;
      getTemplatesSQLFilter();
      if (LOG.isDebugEnabled()) LOG.debug("WCMComposer templates have been cleaned !");
    }

  @Managed
  @ManagedDescription("Is the cache used ?")
  public boolean isCached() {
    return isCached;
  }

  @Managed
  @ManagedDescription("Use the default language if translation is not published ?")
  public boolean useDefaultLanguage() {
    return useDefaultLanguage;
  }

  @Managed
  @ManagedDescription("How many nodes in the cache ?")
  public int getCachedEntries() {
    return this.cache.getCacheSize();
  }

  @Managed
  @ManagedDescription("Activate/deactivate the composer cache ?")
  public void setCached(@ManagedDescription("Enable/Disable the cache ?") @ManagedName("isCached") boolean isCached) {
    this.isCached = isCached;
  }

  public void setDefaultLanguagePolicy(boolean useDefaultLanguage) {
    /**
     * we don't expose this thru JMX as it's not persistent and it won't work in Cluster environement.
     */
    this.useDefaultLanguage = useDefaultLanguage;
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


  /* (non-Javadoc)
   * @see org.picocontainer.Startable#start()
   */
  public void start() {}

  /* (non-Javadoc)
   * @see org.picocontainer.Startable#stop()
   */
  public void stop() {}

  /**
   * Gets the order sql filter.
   *
   * @param filters the filters
   *
   * @return the order sql filter
   */
  private String getOrderSQLFilter(HashMap<String, String> filters) {
    StringBuffer orderQuery = new StringBuffer(" ORDER BY ");
    String orderBy = filters.get(FILTER_ORDER_BY);
    String orderType = filters.get(FILTER_ORDER_TYPE);
    if (orderType == null)
      orderType = "DESC";
    if (orderBy == null)
      orderBy = "exo:title";
    orderQuery.append(orderBy).append(" ").append(orderType);
    return orderQuery.toString();
  }

  /**
   * Gets all document nodetypes and write a query statement
   * @param repository the repository's name
   * @return a part of the query allow search all document node and taxonomy link also. Return null if there is any exception.
   */
  private String getTemplatesSQLFilter() {
    if (templatesFilter != null) return templatesFilter;
    return updateTemplatesSQLFilter();
  }
  /**
   * Update all document nodetypes and write a query statement
   * @param repository the repository's name
   * @return a part of the query allow search all document node and taxonomy link also. Return null if there is any exception.
   */
  public String updateTemplatesSQLFilter() {    
    try {
      List<String> documentTypes = templateService.getDocumentTemplates();
      StringBuffer documentTypeClause = new StringBuffer("(");
      for (int i = 0; i < documentTypes.size(); i++) {
        String documentType = documentTypes.get(i);
        documentTypeClause.append("jcr:primaryType = '" + documentType + "'");
        if (i != (documentTypes.size() - 1)) documentTypeClause.append(" OR ");
      }
      templatesFilter = documentTypeClause.toString();
      templatesFilter += " OR jcr:primaryType = 'exo:taxonomyLink' OR jcr:primaryType = 'exo:symlink')";
      return templatesFilter;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when perform getTemlatesSQLFilter: ", e);
      }
      return null;
    }
  }

  /**
   * Gets the node by category.
   *
   * @param parameters the parameters
   *
   * @return the node by category
   *
   * @throws Exception the exception
   */
  private Node getNodeByCategory(String parameters) throws Exception {
    try {
      if (taxonomyService==null) taxonomyService = WCMCoreUtils.getService(TaxonomyService.class);
      Node taxonomyTree = taxonomyService.getTaxonomyTree(parameters.split("/")[0]);
      Node symlink = taxonomyTree.getNode(parameters.substring(parameters.indexOf("/") + 1));
      return linkManager.getTarget(symlink);
    } catch (Exception e) {
      return null;
    }
  }

  private String getHash(String path,
                         String version,
                         String remoteUser,
                         String language,
                         String recursive,
                         String orderBy,
                         String orderType,
                         String primaryType) throws Exception {
    StringBuffer key = new StringBuffer(path);
    if (version!=null) key.append("::").append(version);
    if (remoteUser!=null) key.append(";;").append(remoteUser);
    if (language!=null) key.append(",,").append(language);
    if (orderBy!=null) key.append("??").append(orderBy);
    if (orderType!=null) key.append("!!").append(orderType);
    if (primaryType!=null) key.append("))").append(primaryType);
      return key.toString();//MessageDigester.getHash(key.toString());
  }

  private void addUsedLanguage(String lang) {
    if (!usedLanguages.contains(lang)) usedLanguages.add(lang);
  }
  private void addUsedOrderBy(String orderBy) {
    if (!usedOrderBy.contains(orderBy)) usedOrderBy.add(orderBy);
  }
  private void addUsedPrimaryTypes(String primaryType) {
    if (!usedPrimaryTypes.contains(primaryType)) usedPrimaryTypes.add(primaryType);
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
    String remoteUser = null;
    try {
      remoteUser = Util.getPortalRequestContext().getRemoteUser();
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    return remoteUser;
  }
}
