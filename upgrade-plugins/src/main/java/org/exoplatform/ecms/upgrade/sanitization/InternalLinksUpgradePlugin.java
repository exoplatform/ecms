package org.exoplatform.ecms.upgrade.sanitization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;


public class InternalLinksUpgradePlugin extends UpgradeProductPlugin {
  private RepositoryService repoService_;
  private static final Log LOG = ExoLogger.getLogger(InternalLinksUpgradePlugin.class.getName());

  public InternalLinksUpgradePlugin(RepositoryService repoService, InitParams initParams){
    super(initParams);
    repoService_ = repoService;
  }
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    /**
     * Migrate binary data jcr:data which still contains "/sites content/live" in its values
     */
    migrateJCRDataContents();
  }
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    //return true anly for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
  }
  
  /**
   * Migrate binnary data jcr:data which still contains "/sites content/live" in its values
   */
  private void migrateJCRDataContents() {
    try {
      Session session = WCMCoreUtils.getSystemSessionProvider().getSession("collaboration",repoService_.getCurrentRepository());
      if (LOG.isInfoEnabled()) {
        LOG.info("=====Start migrate old links from jcr data====");
      }
      List<String> documentMixinTypes = new ArrayList<String>();
      Set<String> nodesToRepublish = new HashSet<String>();
      // for performance reason we limit search to js,html and csscontents
      documentMixinTypes.add("exo:jsFile");
      documentMixinTypes.add("exo:htmlFile");
      documentMixinTypes.add("exo:cssFile");
      documentMixinTypes.add("exo:webContent");
      for (String type : documentMixinTypes) {
        StringBuilder statement = new StringBuilder().append("select * from ").append(type).append(" ORDER BY exo:name DESC ");
        QueryResult result = session.getWorkspace().getQueryManager().createQuery(statement.toString(), Query.SQL).execute();
        NodeIterator nodeIter = result.getNodes();
        while (nodeIter.hasNext()) {
          try {
            Node node = nodeIter.nextNode();
            if (type.equals("exo:webContent")) {
                if (node.hasProperty("exo:summary")) {
                    String summary = node.getProperty("exo:summary").getString();
                    if (summary.contains("/sites content/live/") || summary.contains("/sites%20content/live/")) {
                        LOG.info("=====Migrating data summary '" + node.getPath() + "' =====");
                        String newData = StringUtils.replaceEachRepeatedly(summary,
                                new String[]{"/sites content/live/", "/sites%20content/live/"},
                                new String[]{"/sites/", "/sites/"});
                        node.setProperty("exo:summary", newData);
                        session.save();
                        nodesToRepublish.add(node.getPath());
                    }
                }
            } else {
                if (node.hasNode("jcr:content")) {
                    Node content = node.getNode("jcr:content");
                    if (content.hasProperty("jcr:mimeType")) {
                        String mimeType = content.getProperty("jcr:mimeType").getString();
                        if (mimeType.startsWith("text") || mimeType.contains("javascript")) {
                            String jcrData = content.getProperty("jcr:data").getString();
                            if (jcrData.contains("/sites content/live/") || jcrData.contains("/sites%20content/live/")) {
                                LOG.info("=====Migrating data contents '" + content.getParent().getPath() + "' =====");
                                String newData = StringUtils.replaceEachRepeatedly(jcrData,
                                        new String[]{"/sites content/live/", "/sites%20content/live/"},
                                        new String[]{"/sites/", "/sites/"});
                                content.setProperty("jcr:data", newData);
                                session.save();
                                Node parent = node.getParent();
                                if (parent.isNodeType("exo:webContent")) {
                                    nodesToRepublish.add(parent.getPath());
                                    continue;
                                }
                                //for subnodes in some folders like css, js, documents, medias
                                if (parent.getPath().equals("/")) {
                                    nodesToRepublish.add(node.getPath());
                                    continue;
                                }

                                Node grandParent = parent.getParent();
                                if (grandParent.isNodeType("exo:webContent")) {
                                    nodesToRepublish.add(grandParent.getPath());
                                    continue;
                                }
                                //for subnodes in some folders like images, videos, audio
                                if (grandParent.getPath().equals("/")) {
                                    nodesToRepublish.add(node.getPath());
                                    continue;
                                }
                                Node ancestor = grandParent.getParent();
                                if (ancestor.isNodeType("exo:webContent")) {
                                    nodesToRepublish.add(ancestor.getPath());
                                    continue;
                                }
                                nodesToRepublish.add(node.getPath());
                            }
                        }
                    }
                }
            }
          } catch (Exception e) {
            LOG.error("An unexpected error occurs when migrating JCR Data Content: ",e);
          }

        }
      }
      for (String nodePath : nodesToRepublish) {
        try {
          republishNode((Node)session.getItem(nodePath));
        } catch (Exception e) {
          LOG.error("An unexpected error occurs when republishing content: ",e);
        }
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("===== Migrate data in contents completed =====");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating JCR Data Contents: ",e);
      }
    }
  }
  
  /* Republish node */
  private void republishNode(Node checkedNode) throws Exception {
    PublicationService publicationService = (PublicationService)WCMCoreUtils.getService(PublicationService.class);
    if (publicationService.isNodeEnrolledInLifecycle(checkedNode) && PublicationDefaultStates.PUBLISHED.equalsIgnoreCase(publicationService.getCurrentState(checkedNode))){
      HashMap<String, String> context = new HashMap<String, String>();
      LOG.info("=====Republish '"+ checkedNode.getPath()+ "' =====");
      publicationService.changeState(checkedNode,PublicationDefaultStates.DRAFT,context);
      publicationService.changeState(checkedNode,PublicationDefaultStates.PUBLISHED,context);
    }
  }

}
