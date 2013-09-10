package org.exoplatform.services.seo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;

/**
 * SEOService supplies APIs to manage SEO data of a page or a content.
 * This service includes some major functions which enables you to add, store,
 * get or remove the metadata of a page or a content.
 *
 * @LevelAPI Experimental
 */
public interface SEOService {
 
  /**
   * Store the metadata of a page/content.
   * 
   * @param metaModel The metadata of a page/content stored.
   * @param portalName The name of portal.
   * @param onContent Indicate whether the current page is the content page or the portal page.
   * @throws Exception The exception
   */
  public void storeMetadata(PageMetadataModel metaModel, String portalName, boolean onContent, String language) throws Exception;

  /**
   * Return the metadata of a portal page or a content page.
   *
   * @param params The parameters list of a content page.
   * @param pageReference The reference of the page.
   * @param language The language of the page.
   * @return PageMetadataModel
   * @throws Exception The exception
   */
  public PageMetadataModel getMetadata(ArrayList<String> params, String pageReference, String language) throws Exception;

  /**
   * Return the metadata of a portal page.
   *
   * @param pageReference The reference of the page.
   * @param language The language of the page.
   * @return PageMetadataModel
   * @throws Exception The exception
   */
  public PageMetadataModel getPageMetadata(String pageReference, String language) throws Exception;

  /**
   * Return the metadata of a content page.
   * 
   * @param params The parameters list of a content page.
   * @param language The language of the page.
   * @return PageMetadataModel
   * @throws Exception The exception
   */  
  public PageMetadataModel getContentMetadata(ArrayList<String> params, String language) throws Exception;

  /**
   * Remove the metadata of a page.
   *
   * @param metaModel The metadata of a page/content stored.
   * @param portalName The name of portal.
   * @param onContent Indicate whether the current page is the content page or the portal page.
   * @param language The language of the page.
   * @throws Exception The exception
   */
  public void removePageMetadata(PageMetadataModel metaModel, String portalName, boolean onContent, String language) 
      throws Exception;

  /**
   * Return the content node by the content path.
   * 
   * @param contentPath The content path.
   * @throws Exception The exception
   */
  public Node getContentNode(String contentPath) throws Exception;

  /**
   * Create a key from the page reference or the UUID of the node.
   *
   * @param uri The page reference of the UUID of a node.
   * @return The hash
   * @throws Exception The exception
   */
  public String getHash(String uri) throws Exception ;

  /**
   * Return a sitemap's content of a specific portal.
   * 
   * @param portalName The portal name.
   * @return The sitemap
   * @throws Exception The exception
   */
  public String getSitemap(String portalName) throws Exception;

  /**
   * Return Robots' content of a specific portal
   * 
   * @param portalName The portal name.
   * @return The robots information
   * @throws Exception The exception
   */
  public String getRobots(String portalName) throws Exception;

  /**
   * Return a list of options (INDEX and NOINDEX) for robots to index.
   * 
   * @return List<String>
   * @throws Exception The exception
   */
  public List<String> getRobotsIndexOptions() throws Exception;

  /**
   * Return a list of options (FOLLOW and NOFOLLOW) for robots to follow.
   * 
   * @return List<String>
   * @throws Exception The exception
   */
  public List<String> getRobotsFollowOptions() throws Exception;

  /**
   * Return a list of options for frequency.
   *
   * @return List<String>
   * @throws Exception The exception
   */
  public List<String> getFrequencyOptions() throws Exception;

  /**
   * Get the status of a page for its language.
   *
   * @param path The path of the page
   * @param language The page's language
   * @param onContent True if it concerns the content displayed itself
   * @return The state
   * @throws Exception The exception
   */
  public String getState(String path, String language, boolean onContent) throws Exception;

  /**
   * Get All SEO Languages.
   *
   * @param portalName The Portal name
   * @param seoPath The path of the page
   * @param onContent True if it concerns the content displayed itself
   * @return List<Locale> List of Languages
   * @throws Exception The exception
   */
  public List<Locale> getSEOLanguages(String portalName, String seoPath, boolean onContent) throws Exception;
}