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
package org.exoplatform.services.wcm.metadata;

import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Nov 3, 2008
 */
public class PageMetadataServiceImpl implements PageMetadataService {

  /** The log. */
  private static Log log = ExoLogger.getLogger("wcm:PageMetadataService");

  /** The live portal manager service. */
  private LivePortalManagerService livePortalManagerService;

  /** The categories service. */
  private TaxonomyService taxonomyService;

  /** The folksonomy service. */
  private NewFolksonomyService folksonomyService;

  /**
   * Instantiates a new page metadata service impl.
   *
   * @param livePortalManagerService the live portal manager service
   * @param categoriesService the categories service
   * @param folksonomyService the folksonomy service
   *
   * @throws Exception the exception
   */
  public PageMetadataServiceImpl(LivePortalManagerService livePortalManagerService,
                                 TaxonomyService taxonomyService,
                                 NewFolksonomyService folksonomyService) throws Exception {
    this.livePortalManagerService = livePortalManagerService;
    this.taxonomyService = taxonomyService;
    this.folksonomyService = folksonomyService;
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.wcm.metadata.PageMetadataService#getPortalMetadata
   * (java.lang.String, org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public HashMap<String, String> getPortalMetadata(SessionProvider sessionProvider, String uri) throws Exception {
    String portalName = uri.split("/")[1];
    try {
      Node portal = livePortalManagerService.getLivePortal(sessionProvider, portalName);
      return extractPortalMetadata(portal);
    } catch (Exception e) {
      if (log.isDebugEnabled())
        log.debug(e);
    }
    return null;
  }

  /**
   * Extract portal metadata.
   *
   * @param portalNode the portal node
   *
   * @return the hash map< string, string>
   *
   * @throws Exception the exception
   */
  private HashMap<String,String> extractPortalMetadata(Node portalNode) throws Exception {
    HashMap<String,String> metadata = new HashMap<String,String>();
    NodeTypeManager manager = portalNode.getSession().getWorkspace().getNodeTypeManager();
    NodeType siteMedata = manager.getNodeType("metadata:siteMetadata");
    for(PropertyDefinition pdef: siteMedata.getDeclaredPropertyDefinitions()) {
      String metadataName = pdef.getName();
      String metadataValue = getProperty(portalNode,metadataName);
      if(metadataValue != null)
        metadata.put(metadataName,metadataValue);
    }
    HashMap<String,String> dcElementSet = extractDCElementSetMetadata(portalNode);
    metadata.putAll(dcElementSet);
    return metadata;
  }

  /**
   * Extract dc element set metadata.
   *
   * @param node the node
   *
   * @return the hash map< string, string>
   *
   * @throws Exception the exception
   */
  private HashMap<String,String> extractDCElementSetMetadata(Node node) throws Exception {
    HashMap<String,String> metadata = new HashMap<String,String>();
    Node checkNode = node;
    if(node.isNodeType("nt:file"))
      checkNode = (Node)node.getPrimaryItem();
    if(!checkNode.isNodeType("dc:elementSet")) return metadata;

    NodeType dcElementSet = node.getSession().getWorkspace().getNodeTypeManager().getNodeType("dc:elementSet");
    for(PropertyDefinition pdef: dcElementSet.getDeclaredPropertyDefinitions()) {
      String metadataName = pdef.getName();
      String metadataValue = getValues(checkNode, metadataName);
      if(metadataValue != null && metadataValue.length() >0) {
        String metaTagName = metadataName.replaceFirst(":",".");
        metaTagName = metaTagName.replace("dc","DC");
        metadata.put(metaTagName,metadataValue);
      }
    }
    return metadata;
  }
  /* (non-Javadoc)
   * @see org.exoplatform.services.wcm.metadata.PageMetadataService#extractMetadata(javax.jcr.Node)
   */
  public HashMap<String, String> extractMetadata(Node node) throws Exception {
    HashMap<String, String> medatata = new HashMap<String,String>();
    Node portalNode = findPortal(node);
    String siteTitle = null;
    String portalKeywords = null;
    if(portalNode != null) {
      siteTitle = getProperty(portalNode,SITE_TITLE);
      if(siteTitle == null) siteTitle = portalNode.getName();
      medatata = extractPortalMetadata(portalNode);
      portalKeywords = medatata.get(KEYWORDS);
    }
    String pageTitle = getProperty(node,"exo:title");
    if (pageTitle == null)
      pageTitle = node.getName();
    if (siteTitle != null) {
      StringBuffer sb = new StringBuffer();
      sb.append(pageTitle).append("-").append(siteTitle);
      pageTitle = sb.toString();
    }
    String description = getProperty(node,"exo:summary");
    medatata.put(PAGE_TITLE,pageTitle);
    if(description != null) {
      medatata.put(DESCRIPTION,description);
    }
    String keywords = computeContentKeywords(node,pageTitle);
    if(portalKeywords != null) {
      keywords = keywords.concat(",").concat(portalKeywords);
    }
    HashMap<String,String> dcElementSet = extractDCElementSetMetadata(node);
    medatata.put(KEYWORDS,keywords);
    medatata.putAll(dcElementSet);
    return medatata;
  }

  /**
   * Compute content keywords.
   *
   * @param node the node
   * @param title the title
   *
   * @return the string
   *
   * @throws Exception the exception
   */
  private String computeContentKeywords(Node node, String title) throws Exception {
    StringBuilder builder = new StringBuilder();
    NodeLocation nodeLocation = NodeLocation.make(node);
    String repository = nodeLocation.getRepository();
    try {
      List<Node> iterator = taxonomyService.getCategories(node,repository);
      for(Node category: iterator) {
        builder.append(category.getName()).append(",");
      }
    } catch(Exception e) {
      return builder.toString();
    }

    for(Node tag: folksonomyService.getLinkedTagsOfDocument(node, nodeLocation.getWorkspace())) {
      builder.append(tag.getName()).append(",");
    }
    builder.append(title.replaceAll(" ",","));
    return builder.toString();
  }

  /**
   * Gets the property.
   *
   * @param node the node
   * @param propertyName the property name
   *
   * @return the property
   *
   * @throws Exception the exception
   */
  private String getProperty(Node node, String propertyName) throws Exception {
    return node.hasProperty(propertyName)? node.getProperty(propertyName).getString():null;
  }

  /**
   * Gets the values.
   *
   * @param node the node
   * @param propertyName the property name
   *
   * @return the values
   *
   * @throws Exception the exception
   */
  private String getValues(Node node, String propertyName) throws Exception {
    try {
      Property property = node.getProperty(propertyName);
      PropertyDefinition definition = property.getDefinition();
      if (!definition.isMultiple())
        return property.getValue().getString();
      int propertyType = definition.getRequiredType();
      if (PropertyType.BINARY == propertyType)
        return null;
      if (PropertyType.REFERENCE == propertyType)
        return null;
      StringBuilder builder = new StringBuilder();
      for (Value value : property.getValues()) {
        if (propertyType == PropertyType.DATE) {
          String v = ISO8601.format(value.getDate());
          builder.append(v).append(",");
        } else {
          builder.append(value.getString()).append(",");
        }
      }
      if (builder.charAt(builder.length() - 1) == ',') {
        builder.deleteCharAt(builder.length() - 1);
      }
      return builder.toString();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Find portal.
   *
   * @param child the child
   *
   * @return the node
   *
   * @throws Exception the exception
   */
  protected Node findPortal(Node child) throws Exception{
    try {
      return livePortalManagerService.getLivePortalByChild(child);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when findPortal: ", e);
      }
    }
    return null;
  }
}
