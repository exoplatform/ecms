package org.exoplatform.services.seo;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.seo.impl.SEOServiceImpl;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class DumpSEOServiceImpl extends SEOServiceImpl {
  private ExoCache<String, Object> cache;
  public DumpSEOServiceImpl(InitParams initParams) throws Exception {
    super(initParams);
    cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance("wcm.seo");
    // TODO Auto-generated constructor stub
  }
  public void storePageMetadata(PageMetadataModel metaModel, String portalName, boolean onContent) throws Exception {
    String uri = metaModel.getUri();
    String pageReference = metaModel.getPageReference();
    String keyword = metaModel.getKeywords();
    String description = metaModel.getDescription();
    String robots = metaModel.getRobotsContent();
    String fullStatus = metaModel.getFullStatus();
    boolean sitemap = metaModel.getSitemap();
    float priority = metaModel.getPriority();
    String frequency = metaModel.getFrequency();
    //Store sitemap.xml file
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Session session = null;
    LivePortalManagerService livePortalManagerService = WCMCoreUtils.getService(LivePortalManagerService.class);
    Node dummyNode = livePortalManagerService.getLivePortal(sessionProvider, portalName);
    session = dummyNode.getSession();
    if (!dummyNode.hasNode(METADATA_BASE_PATH)) {
      dummyNode.addNode(METADATA_BASE_PATH);
      session.save();
    }
    Node seoNode = null;
    seoNode = (Node) session.getItem("/sites content/live/classic/documents");
    if (seoNode.isNodeType("exo:pageMetadata")) {
        seoNode.setProperty("exo:metaKeywords", keyword);
        seoNode.setProperty("exo:metaDescription", description);
        seoNode.setProperty("exo:metaFully", fullStatus);
        if(!onContent) {
          seoNode.setProperty("exo:metaRobots", robots);
          seoNode.setProperty("exo:metaSitemap", sitemap);
          seoNode.setProperty("exo:metaPriority", priority);
          seoNode.setProperty("exo:metaFrequency", frequency);
          updateSiteMap(uri, priority, frequency, sitemap, portalName);
        }
        String hash = null;
        if(onContent) hash = getHash(uri);
        else hash = getHash(pageReference);
        if(hash != null) cache.put(hash, metaModel);
    } else {
      String hash = null;
      seoNode.addMixin("exo:pageMetadata");
      seoNode.setProperty("exo:metaKeywords", keyword);
      seoNode.setProperty("exo:metaDescription", description);
      seoNode.setProperty("exo:metaFully", fullStatus);
      if(onContent) {
        seoNode.setProperty("exo:metaUri", seoNode.getUUID());
        hash = getHash(seoNode.getUUID());
      }
      else {
        seoNode.setProperty("exo:metaUri", pageReference);
        seoNode.setProperty("exo:metaRobots", robots);
        seoNode.setProperty("exo:metaSitemap", sitemap);
        seoNode.setProperty("exo:metaPriority", priority);
        seoNode.setProperty("exo:metaFrequency", frequency);
        updateSiteMap(uri, priority, frequency, sitemap, portalName);
        hash = getHash(pageReference);
      }
      if(hash != null) cache.put(hash, metaModel);
    }
    session.save();
  }

  public PageMetadataModel getPageMetadata(String pageUri) throws Exception {
    PageMetadataModel metaModel = null;
    String hash = getHash(pageUri);
    if(cache.get(hash) != null)
      metaModel = (PageMetadataModel)cache.get(hash);
    if(metaModel == null) {
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      Session session = sessionProvider.getSession("collaboration", WCMCoreUtils.getRepository());
      Node pageNode = (Node) session.getItem("/sites content/live/classic/documents");
      if(pageNode.isNodeType("exo:pageMetadata")) {
        metaModel = new PageMetadataModel();
        if (pageNode.hasProperty("exo:metaKeywords"))
          metaModel.setKeywords((pageNode.getProperty("exo:metaKeywords")).getString());
        if (pageNode.hasProperty("exo:metaDescription"))
          metaModel.setDescription((pageNode.getProperty("exo:metaDescription")).getString());
        if (pageNode.hasProperty("exo:metaRobots"))
          metaModel.setRobotsContent((pageNode.getProperty("exo:metaRobots")).getString());
        if (pageNode.hasProperty("exo:metaSitemap"))
          metaModel.setSiteMap(Boolean.parseBoolean((pageNode.getProperty("exo:metaSitemap")).getString()));
        if (pageNode.hasProperty("exo:metaPriority"))
          metaModel.setPriority(Long.parseLong((pageNode.getProperty("exo:metaPriority")).getString()));
        if (pageNode.hasProperty("exo:metaFrequency"))
          metaModel.setFrequency((pageNode.getProperty("exo:metaFrequency")).getString());
        cache.put(hash, metaModel);
      }
    }
    return metaModel;
  }

  public void removePageMetadata(PageMetadataModel metaModel, String portalName, boolean onContent) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    RepositoryService repositoryService = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
    ManageableRepository currentRepo = repositoryService.getCurrentRepository();
    Session session = sessionProvider.getSession(currentRepo.getConfiguration().getDefaultWorkspaceName(), currentRepo);
    String hash = "";
    Node seoNode = null;
    seoNode = (Node) session.getItem("/sites content/live/classic/documents");
    if(seoNode.isNodeType("exo:pageMetadata")) {
      seoNode.removeMixin("exo:pageMetadata");
      if(onContent) hash = getHash(metaModel.getUri());
      else hash = getHash(metaModel.getPageReference());
      cache.remove(hash);
    }
    session.save();
  }

  public Node getContentNode(String seoPath) throws Exception {
    Node seoNode = null;
    if(seoPath != null && seoPath.length() > 0) {
      String tmpPath = seoPath.trim();
      if(tmpPath.startsWith("/"))
        tmpPath = tmpPath.substring(1,tmpPath.length());
      String[] arrPath = tmpPath.split("/");
      if(arrPath != null && arrPath.length > 3) {
        String repo = arrPath[0];
        String ws = arrPath[1];
        if(repo != null && ws != null) {
          boolean isWs = false;
          String nodePath = tmpPath.substring(tmpPath.indexOf(ws) + ws.length(),tmpPath.length());
          if(nodePath != null && nodePath.length() > 0) {
            ManageableRepository manageRepo = WCMCoreUtils.getRepository();
            ArrayList<WorkspaceEntry> wsList = manageRepo.getConfiguration().getWorkspaceEntries();
            for(int i = 0; i< wsList.size(); i++) {
              WorkspaceEntry wsEntry = (WorkspaceEntry)wsList.get(i);
              if(wsEntry.getName().equals(ws)) {
                isWs = true;
                break;
              }
            }
            if(isWs) {
              Session session = WCMCoreUtils.getSystemSessionProvider().getSession(ws, manageRepo) ;
              nodePath = nodePath.replaceAll("//", "/");
              if(session.getItem(nodePath) != null) {
                if(session.getItem(nodePath).isNode()) {
                  seoNode = (Node)session.getItem(nodePath);
                }
              }
            }
          }
        }
      }
    }
    return seoNode;
  }
}
