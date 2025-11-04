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

import java.util.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class VersionHistoryUtils {

  protected static final Log log = ExoLogger.getLogger(VersionHistoryUtils.class);

  private static final int DOCUMENT_AUTO_DEFAULT_VERSION_MAX = 0;
  private static final int DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED = 0;

  public static final String NT_FILE          = "nt:file";
  public static final String MIX_VERSIONABLE  = "mix:versionable";
  public static final String WEB_CONTENT      = "exo:webContent";

  //Mixin used to store display version Name added after remove base version
  public static  final String MIX_DISPLAY_VERSION_NAME = "mix:versionDisplayName";
  //mav version , incremented by 1 after add a new version
  public static  final String MAX_VERSION_PROPERTY = "exo:maxVersion";
  //list version name (jcr Name, Display Name)
  public static  final String LIST_VERSION_PROPERTY = "exo:versionList";
  public static  final String VERSION_SEPARATOR = ":";

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
    if(!nodeVersioning.isNodeType(NT_FILE) && !nodeVersioning.isNodeType(WEB_CONTENT)) {
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
    if (!nodeVersioning.isCheckedOut()) {
      nodeVersioning.checkout();
      version = nodeVersioning.getBaseVersion();
    } else {
      version = nodeVersioning.checkin();
      nodeVersioning.checkout();
    }

    //check if mixin mix:versionDisplayName is added then,
    //update max version after add new version (increment by 1)
    if (version != null && nodeVersioning.isNodeType(VersionHistoryUtils.MIX_DISPLAY_VERSION_NAME)) {
      int maxVersion = 0;
      if(nodeVersioning.hasProperty(MAX_VERSION_PROPERTY)){
        //Get old max version ID
        maxVersion = (int) nodeVersioning.getProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY).getLong();
        //Update max version IX (maxVersion+1)
        nodeVersioning.setProperty(MAX_VERSION_PROPERTY, maxVersion +1);
      }
      //add a new entry to store the display version for the new added version (jcrID, maxVersion)
      String newRef = version.getName() + VERSION_SEPARATOR + maxVersion;
      List<Value> newValues = new ArrayList<Value>();
      Value[] values;
      if(nodeVersioning.hasProperty(LIST_VERSION_PROPERTY)){
        values = nodeVersioning.getProperty(LIST_VERSION_PROPERTY).getValues();
        newValues.addAll(Arrays.<Value>asList(values));
      }
      Value value2add = nodeVersioning.getSession().getValueFactory().createValue(newRef);
      newValues.add(value2add);
      //Update the list version entries
      nodeVersioning.setProperty(VersionHistoryUtils.LIST_VERSION_PROPERTY, newValues.toArray(new Value[newValues.size()]));
      nodeVersioning.save();
    }

    if(maxAllowVersion!= DOCUMENT_AUTO_DEFAULT_VERSION_MAX || maxLiveTime != DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED) {
      removeRedundant(nodeVersioning);
    }
    nodeVersioning.save();
    return version;
  }

  /**
   * remove  version
   *
   * @param node root Node
   * @param  versionName version name to remove
   */
  public static void removeVersion(Node node, String versionName) throws RepositoryException {
    VersionHistory versionHistory = node.getVersionHistory();

    //Case of remove Base Version, Add mixin mix:versionDisplayName
    if (node.getBaseVersion().getName().equals(versionName)) {
      if (node.canAddMixin(MIX_DISPLAY_VERSION_NAME)) {
        //Init the maxVersion = versionID(base Version) + 1
        int maxVersion  = Integer.parseInt(versionName) + 1;
        node.addMixin(MIX_DISPLAY_VERSION_NAME);
        node.setProperty(MAX_VERSION_PROPERTY, maxVersion);
      } else {
        //remove entry of removed version
        removeRefVersionName(node, versionName);
      }
      node.save();
    } else if (node.isNodeType(MIX_DISPLAY_VERSION_NAME)) {
      //remove entry of removed version
      removeRefVersionName(node, versionName);
      node.save();
    }
    versionHistory.removeVersion(versionName);
  }
  
  public static int getVersion(Node node) {
    String currentVersion = null;
    try {
      if (node.isNodeType(VersionHistoryUtils.MIX_DISPLAY_VERSION_NAME) &&
              node.hasProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY)) {
        //Get max version ID
        int max = (int) node.getProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY).getLong();
        return max - 1;
      }
      currentVersion = node.getBaseVersion().getName();
      if (currentVersion.contains("jcr:rootVersion")) currentVersion = "0";
    }catch (Exception e) {
      currentVersion ="0";
    }
    return Integer.parseInt(currentVersion);
  }

  public static long computeVersionsSize(Node node) {
    long versionSize=0;
    try {
      if (node.isNodeType(VersionHistoryUtils.MIX_VERSIONABLE)) {
        VersionHistory versionHistory = node.getVersionHistory();
        VersionIterator iterator = versionHistory.getAllVersions();
        while (iterator.hasNext()) {
          Version version = iterator.nextVersion();
          if (version.hasNode("jcr:frozenNode")) {
            Node frozen= version.getNode("jcr:frozenNode");
            if (frozen.hasNode("jcr:content")) {
              long currentVersionSize = frozen.getNode("jcr:content").getProperty("jcr:data").getLength();
              versionSize+=currentVersionSize;
            }
          }
        }
      }
    }catch (Exception e) {
      versionSize=0;
    }
    return versionSize;
  }
    /**
     * Remove redundant version
     * - Remove versions has been expired
     * - Remove versions over max allow
     * @param nodeVersioning
     * @throws Exception
     */
    private static void removeRedundant(Node nodeVersioning) throws Exception {
      VersionHistory versionHistory = nodeVersioning.getVersionHistory();
      String baseVersion = nodeVersioning.getBaseVersion().getName();
      String rootVersion = versionHistory.getRootVersion().getName();
      Date now = new Date();
      Map<Long, String> versionTimestampToName = new HashMap<>();
      List<Long> timestamps = new ArrayList<>();
      VersionIterator versions = versionHistory.getAllVersions();
      while (versions.hasNext()) {
        Version version = versions.nextVersion();
        String versionName = version.getName();
        if (rootVersion.equals(versionName) || baseVersion.equals(versionName)) {
          continue;
        }
        long created = version.getCreated().getTimeInMillis();
        if (maxLiveTime != DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED &&
                now.getTime() - created > maxLiveTime) {

          versionHistory.removeVersion(versionName);
          continue;
        }
        versionTimestampToName.put(created, versionName);
        timestamps.add(created);
      }
      if (maxAllowVersion != DOCUMENT_AUTO_DEFAULT_VERSION_MAX &&
              timestamps.size() >= maxAllowVersion) {
        Collections.sort(timestamps);
        removeExpiredVersions(versionHistory, timestamps, versionTimestampToName, rootVersion, baseVersion, true);
        if (timestamps.size() >= maxAllowVersion) {
          removeExpiredVersions(versionHistory, timestamps, versionTimestampToName, rootVersion, baseVersion, false);
        }
      }
    }

  /**
   * Remove expired version
   * Remove exceeded versions and skip base and root version depending on labeled or not
   *
   * @param versionHistory
   * @param timestamps
   * @param map
   * @param rootVersion
   * @param baseVersion
   * @param skipLabeled
   */
  private static void removeExpiredVersions(
          VersionHistory versionHistory,
          List<Long> timestamps,
          Map<Long, String> map,
          String rootVersion,
          String baseVersion,
          boolean skipLabeled) throws Exception {

    int i = 1;
    while (timestamps.size() >= maxAllowVersion && i < timestamps.size()) {
      long ts = timestamps.get(i);
      String name = map.get(ts);
      Version version = versionHistory.getVersion(name);
      if (rootVersion.equals(name) || baseVersion.equals(name)) {
        i++;
        continue;
      }
      if (skipLabeled) {
        String[] labels = versionHistory.getVersionLabels(version);
        boolean hasLabel = Arrays.stream(labels).anyMatch(StringUtils::isNotEmpty);
        if (hasLabel) {
          i++;
          continue;
        }
      }
      versionHistory.removeVersion(name);
      timestamps.remove(i);
    }
  }

  /**
   * Remove version Name entry from the list of versions
   * @param node
   * @param versionName
   * @throws RepositoryException
   */
  private static void removeRefVersionName(Node node, String versionName) throws RepositoryException {
    if(node.hasProperty(LIST_VERSION_PROPERTY)){
      if(node.hasProperty(VersionHistoryUtils.LIST_VERSION_PROPERTY)){
        Value[] values = node.getProperty(VersionHistoryUtils.LIST_VERSION_PROPERTY).getValues();
        List<Value> newValues = new ArrayList<Value>();
        for (Value value : values){
          if (!value.getString().split(VERSION_SEPARATOR)[0].equals(versionName))
            newValues.add(value);
        }
        node.setProperty(VersionHistoryUtils.LIST_VERSION_PROPERTY, newValues.toArray(new Value[newValues.size()]));
      }
    }
  }
}
