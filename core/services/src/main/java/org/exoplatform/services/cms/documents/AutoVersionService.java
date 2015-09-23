package org.exoplatform.services.cms.documents;

import javax.jcr.Node;
import java.util.List;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 8/13/15
 * #comments here
 */
public interface AutoVersionService {

  public final String DRIVES_AUTO_VERSION = "ecms.documents.versioning.drives";
  public final String DRIVES_AUTO_VERSION_MAX = "ecms.documents.versions.max";
  public final String DRIVES_AUTO_VERSION_EXPIRED = "ecms.documents.versions.expiration";
  public final int DOCUMENT_AUTO_DEFAULT_VERSION_MAX=0;
  public final int DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED=0;

  public final String PERSONAL_DRIVE_PARRTEN = "/Users/${userId}/Private";
  public final String GROUP_DRIVE_PARRTEN = "/Groups${groupId}/Documents";

  public final String PERSONAL_DRIVE_PREFIX = "/Users";
  public final String GROUP_DRIVE_PREFIX = "/Groups";

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
  public boolean isVersionSupport(String nodePath, String workspace) throws Exception;

   /**
    * Get List of Drive supported versioning
    * @return List of Drive supported versioning
    * @throws Exception
    */
  public List<String> getDriveAutoVersion();
}
