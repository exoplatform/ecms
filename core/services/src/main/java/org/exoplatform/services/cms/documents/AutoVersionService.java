package org.exoplatform.services.cms.documents;

import javax.jcr.Node;
import java.io.InputStream;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 8/13/15
 * #comments here
 */
public interface AutoVersionService {
  public void autoVersion(Node currentNode) throws Exception;
  public void autoVersion(Node currentNode, Node sourceNode) throws Exception;
  public void autoVersion(Node currentNode, InputStream inputStream, String mimeType) throws Exception;
}
