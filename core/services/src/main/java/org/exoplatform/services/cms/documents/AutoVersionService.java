package org.exoplatform.services.cms.documents;

import javax.jcr.Node;
import java.io.InputStream;
import java.util.List;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 8/13/15
 * #comments here
 */
public interface AutoVersionService {
  /**
   * Versioning for document
   * @param currentNode
   * @throws Exception
   */
  public void autoVersion(Node currentNode) throws Exception;

  /**
   *  Versioning for document
   * @param currentNode
   * @param sourceNode
   * @throws Exception
   */
  public void autoVersion(Node currentNode, Node sourceNode) throws Exception;

  /**
   * Check support versioning of document
   * @param nodePath
   * @return
   * @throws Exception
   */
  public boolean isVersionSupport(String nodePath) throws Exception;

   /**
    * Get List of Drive supported versioning
    * @return List of Drive supported versioning
    * @throws Exception
    */
   List<String> getDriveAutoVersion();
}
