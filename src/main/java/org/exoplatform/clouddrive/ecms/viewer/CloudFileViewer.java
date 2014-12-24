
package org.exoplatform.clouddrive.ecms.viewer;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudFile;

/**
 * Support for file viewers to show Cloud Drive files preview embedded in ECMS Documents.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileViewer.java 00000 Nov 18, 2014 pnedonosko $
 * 
 */
public interface CloudFileViewer {

  /**
   * Initialize UI component to represent the given cloud file.
   * 
   * @param drive {@link CloudDrive}
   * @param file {@link CloudFile}
   * @throws Exception
   */
  void initFile(CloudDrive drive, CloudFile file) throws Exception;
  
}
