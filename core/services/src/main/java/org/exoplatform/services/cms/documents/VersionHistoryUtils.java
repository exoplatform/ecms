/*
 * Copyright (C) 20016 eXo Platform.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.cms.documents;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersionHistoryUtils {

  protected static final Log log = ExoLogger.getLogger(VersionHistoryUtils.class);

  private static final int DOCUMENT_AUTO_DEFAULT_VERSION_MAX = 0;
  private static final int DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED = 0;

  public static final String NT_FILE          = "nt:file";
  public static final String MIX_VERSIONABLE  = "mix:versionable";
  private static String maxAllowVersionProp   = "jcr.documents.versions.max";
  private static String expirationTimeProp    = "jcr.documents.versions.expiration";

  private static int maxAllowVersion;
  private static long maxLiveTime;

  static {
    try {
      maxAllowVersion = Integer.parseInt(System.getProperty(maxAllowVersionProp));
      maxLiveTime = Integer.parseInt(System.getProperty(expirationTimeProp));
      //ignore invalid input config
      if(maxAllowVersion < 0) maxAllowVersion = DOCUMENT_AUTO_DEFAULT_VERSION_MAX;
      if(maxLiveTime < 0) maxLiveTime = DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED;
    }catch(NumberFormatException nex){
      maxAllowVersion = DOCUMENT_AUTO_DEFAULT_VERSION_MAX;
      maxLiveTime = DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED;
    }
    maxLiveTime = maxLiveTime * 24 * 60 * 60 * 1000;
  }

  /**
   * Create new version and clear redundant versions
   *
   * @param nodeVersioning
   */
  public static Version createVersion(Node nodeVersioning) throws Exception {
    if(!nodeVersioning.isNodeType(NT_FILE)) {
      if(log.isDebugEnabled()){
        log.debug("Version history is not impact with non-nt:file documents, there'is not any version created.");
      }
      return null;
    }

    if(!nodeVersioning.isNodeType(MIX_VERSIONABLE)){
      if(nodeVersioning.canAddMixin(MIX_VERSIONABLE)) {
        nodeVersioning.addMixin(MIX_VERSIONABLE);
        nodeVersioning.save();
      }
      return null;
    }
    Version version = null;
     if (!nodeVersioning.isCheckedOut())
     {
        nodeVersioning.checkout();
     }
     else
     {
        version = nodeVersioning.checkin();
        nodeVersioning.checkout();
     }

    if(maxAllowVersion!= DOCUMENT_AUTO_DEFAULT_VERSION_MAX || maxLiveTime != DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED) {
      removeRedundant(nodeVersioning);
    }
    nodeVersioning.save();
    return version;
  }

  /**
   * Remove redundant version
   * - Remove versions has been expired
   * - Remove versions over max allow
   * @param nodeVersioning
   * @throws Exception
   */
  private static void removeRedundant(Node nodeVersioning) throws Exception{
    VersionHistory versionHistory = nodeVersioning.getVersionHistory();
    String baseVersion = nodeVersioning.getBaseVersion().getName();
    String rootVersion = nodeVersioning.getVersionHistory().getRootVersion().getName();
    VersionIterator versions = versionHistory.getAllVersions();
    Date currentDate = new Date();
    Map<String, String> lstVersions = new HashMap<String, String>();
    List<String> lstVersionTime = new ArrayList<String>();
    while (versions.hasNext()) {
      Version version = versions.nextVersion();
      if(rootVersion.equals(version.getName()) || baseVersion.equals(version.getName())) continue;

      if (maxLiveTime!= DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED &&
              currentDate.getTime() - version.getCreated().getTime().getTime() > maxLiveTime) {
        versionHistory.removeVersion(version.getName());
      } else {
        lstVersions.put(String.valueOf(version.getCreated().getTimeInMillis()), version.getName());
        lstVersionTime.add(String.valueOf(version.getCreated().getTimeInMillis()));
      }
    }
    if (maxAllowVersion <= lstVersionTime.size() && maxAllowVersion!= DOCUMENT_AUTO_DEFAULT_VERSION_MAX) {
      Collections.sort(lstVersionTime);
      String[] lsts = lstVersionTime.toArray(new String[lstVersionTime.size()]);
      for (int j = 0; j <= lsts.length - maxAllowVersion; j++) {
        versionHistory.removeVersion(lstVersions.get(lsts[j]));
      }
    }
  }
}
