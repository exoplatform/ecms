package org.exoplatform.wcm.connector.handler;

import java.text.SimpleDateFormat;

import javax.jcr.Node;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FCKFileHandler {

  public static Element createFileElement(Document document,
      String fileType,
      Node sourceNode,
      Node displayNode,
      String currentPortal) throws Exception {
    return createFileElement(document, fileType, sourceNode, displayNode, currentPortal, null);
  }

  public static Element createFileElement(Document document,
                                          String fileType,
                                          Node sourceNode,
                                          Node displayNode,
                                          String currentPortal,
                                          LinkManager linkManager) throws Exception {
    Element file = document.createElement("File");
    file.setAttribute("name", Utils.getTitle(displayNode));
    SimpleDateFormat formatter = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT,
                                                                                         SimpleDateFormat.SHORT);
    if(sourceNode.hasProperty("exo:dateCreated")){
      file.setAttribute("dateCreated", formatter.format(sourceNode.getProperty("exo:dateCreated").getDate().getTime()));
    }else if(sourceNode.hasProperty("jcr:created")){
      file.setAttribute("dateCreated", formatter.format(sourceNode.getProperty("jcr:created").getDate().getTime()));
    }    
    
    if(sourceNode.hasProperty("exo:dateModified")) {
      file.setAttribute("dateModified", formatter.format(sourceNode.getProperty("exo:dateModified")
                                                                   .getDate()
                                                                   .getTime()));
    } else {
      file.setAttribute("dateModified", null);
    }
    file.setAttribute("creator", sourceNode.getProperty("exo:owner").getString());
    file.setAttribute("path", displayNode.getPath());
    if (linkManager==null) {
     linkManager = WCMCoreUtils.getService(LinkManager.class) ;
    }
    if (linkManager.isLink(sourceNode)) {
     Node targetNode = linkManager.getTarget(sourceNode);
     if (targetNode!=null) {
       file.setAttribute("linkTarget", targetNode.getPath());
     }else {
       file.setAttribute("linkTarget", sourceNode.getPath());
     }
    }else {
     file.setAttribute("linkTarget", sourceNode.getPath());
    }
    if (sourceNode.isNodeType("nt:file")) {
      Node content = sourceNode.getNode("jcr:content");
      file.setAttribute("nodeType", content.getProperty("jcr:mimeType").getString());
    } else {
      file.setAttribute("nodeType", sourceNode.getPrimaryNodeType().getName());
    }
    if (sourceNode.isNodeType(NodetypeConstant.EXO_WEBCONTENT)) {
      file.setAttribute("url",getDocURL(displayNode, currentPortal));
    } else {
      file.setAttribute("url",getFileURL(displayNode));
    }
    if(sourceNode.isNodeType(FCKUtils.NT_FILE)) {
      long size = sourceNode.getNode("jcr:content").getProperty("jcr:data").getLength();
      file.setAttribute("size", "" + size / 1000);
    }else {
      file.setAttribute("size", "");
    }
    if(sourceNode.isNodeType(NodetypeConstant.MIX_VERSIONABLE)){
      file.setAttribute("isVersioned", String.valueOf(true));
    }else{
      file.setAttribute("isVersioned", String.valueOf(false));
    }
    return file;
  }

  /**
   * Gets the file url.
   *
   * @param file the file
   * @return the file url
   * @throws Exception the exception
   */
  protected static String getFileURL(final Node file) throws Exception {
    return FCKUtils.createWebdavURL(file);
  }

  private static String getDocURL(final Node node, String currentPortal) throws Exception {
    String baseURI = "/" + PortalContainer.getCurrentPortalContainerName();
    String accessMode = "private";
    AccessControlList acl = ((ExtendedNode) node).getACL();
    for (AccessControlEntry entry : acl.getPermissionEntries()) {
      if (entry.getIdentity().equalsIgnoreCase(IdentityConstants.ANY)
          && entry.getPermission().equalsIgnoreCase(PermissionType.READ)) {
        accessMode = "public";
        break;
      }
    }
    String repository = ((ManageableRepository) node.getSession().getRepository())
    .getConfiguration().getName();
    String workspace = node.getSession().getWorkspace().getName();
    String nodePath = node.getPath();
    StringBuilder builder = new StringBuilder();
    if(node.isNodeType(NodetypeConstant.NT_FILE)) {
      if("public".equals(accessMode)) {
        return builder.append(baseURI).append("/jcr/").append(repository).append("/")
        .append(workspace).append(nodePath).toString();
      }
      return builder.append(baseURI).append("/private/jcr/").append(repository).append("/")
      .append(workspace).append(nodePath).toString();
    }
    WCMConfigurationService configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    String parameterizedPageViewerURI = configurationService.
        getRuntimeContextParam(WCMConfigurationService.PARAMETERIZED_PAGE_URI);
    return baseURI.replace("/rest", "") + "/" + currentPortal
        + parameterizedPageViewerURI + "/" + repository + "/" + workspace + nodePath;
  }
}
