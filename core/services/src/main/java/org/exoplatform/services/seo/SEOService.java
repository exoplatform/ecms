package org.exoplatform.services.seo;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
 
public interface SEOService {
 
  /**
   * Store metadata of page/content
   * 
   * @param metaModel The metadata of page/content to store
   * @param portalName The name of portal
   * @param onContent Indicates whether current page is content page or portal page
   * @throws Exception
   */
  public void storePageMetadata(PageMetadataModel metaModel, String portalName, boolean onContent) throws Exception;
  /**
   * Returns metadata of a portal page or content page
   * 
   * @param pageUri The URI of page
   * @return
   * @throws Exception
   */ 
  public PageMetadataModel getMetadata(ArrayList<String> params, String pageReference) throws Exception;
  /**
   * Returns metadata of a portal page 
   * 
   * @param pageUri The reference of page   
   * @return
   * @throws Exception
   */  
  public PageMetadataModel getPageMetadata(String pageReference) throws Exception;
  /**
   * Returns metadata of a content page 
   * 
   * @param params The list of parameters for content page   
   * @return
   * @throws Exception
   */  
  public PageMetadataModel getContentMetadata(ArrayList<String> params) throws Exception;
  /**
   * Remove metadata of page
   * 
   * @param metaModel Object store metadata of page
   * @throws Exception
   */
  public void removePageMetadata(PageMetadataModel metaModel, String portalName, boolean onContent) throws Exception;
  /**
   * Return content node by content path
   * 
   * @param contentPath The content patch
   * @throws Exception
   */
  public Node getContentNode(String contentPath) throws Exception;
  /**
   * Return hash
   * @param uri
   * @return
   * @throws Exception
   */
  public String getHash(String uri) throws Exception ;
  /**
   * Returns sitemap content of portal 
   * 
   * @param portalName The portal name
   * @return
   * @throws Exception
   */  
  
  public String getSitemap(String portalName) throws Exception;
  /**
   * Returns robots content of portal 
   * 
   * @param portalName The portal name
   * @return
   * @throws Exception
   */  
  
  public String getRobots(String portalName) throws Exception;
  /**
   * Returns a list of options for robots index
   * 
   * @return
   * @throws Exception
   */
  public List<String> getRobotsIndexOptions() throws Exception;
  /**
   * Returns a list of options for robots follow
   * 
   * @return
   * @throws Exception
   */
  public List<String> getRobotsFollowOptions() throws Exception;
  /**
   * Returns a list of options for frequency
   * 
   * @return
   * @throws Exception
   */
  public List<String> getFrequencyOptions() throws Exception;
}