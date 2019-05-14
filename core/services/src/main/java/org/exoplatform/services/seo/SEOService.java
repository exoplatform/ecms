package org.exoplatform.services.seo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.jcr.Node;

/**
 * Supplies APIs to manage SEO data of pages or content.
 * This service includes some major functions that allow you to add, store,
 * get or remove metadata of pages or content.
 *
 * @LevelAPI Experimental
 */
public interface SEOService {

  String SAVE_SEO = "org.exoplatform.ecms.seo.save";

  String SEO_REMOVE = "org.exoplatform.ecms.seo.remove";

  /**
   * Stores metadata of a given page/content.
   * 
   * @param metaModel Metadata of the page/content.
   * @param portalName Name of the site that contains the given page/content.
   * @param onContent Indicates whether the current page is content or portal.
   * @throws Exception The exception
   */
  public void storeMetadata(PageMetadataModel metaModel, String portalName, boolean onContent, String language) throws Exception;

  /**
   * Gets metadata of a portal or content page.
   *
   * @param params The parameters list of a content page.
   * @param pageReference Reference of the page.
   * @param language Language of the page.
   * @return The page metadata.
   * @throws Exception The exception
   */
  public PageMetadataModel getMetadata(ArrayList<String> params, String pageReference, String language) throws Exception;

  /**
   * Gets metadata of a portal page.
   *
   * @param pageReference Reference of the page.
   * @param language Language of the page.
   * @return The page metadata.
   * @throws Exception The exception
   */
  public PageMetadataModel getPageMetadata(String pageReference, String language) throws Exception;

  /**
   * Gets metadata of a content page.
   * 
   * @param params The parameters list of a content page.
   * @param language Language of the page.
   * @return The page metadata.
   * @throws Exception The exception
   */  
  public PageMetadataModel getContentMetadata(ArrayList<String> params, String language) throws Exception;

  /**
   * Removes metadata from a given page.
   *
   * @param metaModel Metadata of the given page.
   * @param portalName Name of the site that contains the given page.
   * @param onContent Indicates whether the current page is content or portal.
   * @param language Language of the given page.
   * @throws Exception The exception
   */
  public void removePageMetadata(PageMetadataModel metaModel, String portalName, boolean onContent, String language) 
      throws Exception;

  /**
   * Gets the content node by a given path.
   * 
   * @param contentPath The given path.
   * @throws Exception The exception
   */
  public Node getContentNode(String contentPath) throws Exception;

  /**
   * Creates a hash key from the page reference or the UUID of the node.
   *
   * @param uri The page reference of the node.
   * @return The hash key.
   * @throws Exception The exception
   */
  public String getHash(String uri) throws Exception ;

  /**
   * Gets a sitemap of a given site.
   * 
   * @param portalName Name of the given site.
   * @return The sitemap.
   * @throws Exception The exception
   */
  public String getSitemap(String portalName) throws Exception;

  /**
   * Gets robots content of a given site.
   * 
   * @param portalName Name of the given site.
   * @return The robots content.
   * @throws Exception The exception
   */
  public String getRobots(String portalName) throws Exception;

  /**
   * Gets a list of options (INDEX and NOINDEX) for robots to index.
   * 
   * @return The list of options (INDEX and NOINDEX).
   * @throws Exception The exception
   */
  public List<String> getRobotsIndexOptions() throws Exception;

  /**
   * Gets a list of options (FOLLOW and NOFOLLOW) for robots to follow.
   * 
   * @return The list of options (FOLLOW and NOFOLLOW).
   * @throws Exception The exception
   */
  public List<String> getRobotsFollowOptions() throws Exception;

  /**
   * Gets a list of options for frequency.
   *
   * @return The list of options.
   * @throws Exception The exception
   */
  public List<String> getFrequencyOptions() throws Exception;

  /**
   * Gets state of a page for its language of a given page.
   *
   * @param path Path of the page.
   * @param language Language of the page.
   * @param onContent Indicates whether the given page is content or portal.
   * @return The page state.
   * @throws Exception The exception
   */
  public String getState(String path, String language, boolean onContent) throws Exception;

  /**
   * Gets all SEO languages of a given page.
   *
   * @param portalName Name of the site that contains the given page.
   * @param seoPath Path of the page.
   * @param onContent Indicates whether the given page is content or portal.
   * @return The list of SEO languages.
   * @throws Exception The exception
   */
  public List<Locale> getSEOLanguages(String portalName, String seoPath, boolean onContent) throws Exception;
}