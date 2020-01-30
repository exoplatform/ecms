/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.deployment;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.util.VersionHistoryImporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * @author benjaminmestrallet
 */
public class Utils {
  public static final String MAPPING_FILE = "mapping.properties";

  private final static Log   LOG          = ExoLogger.getLogger(Utils.class);

  public static Node makePath(Node rootNode, String path, String nodetype) throws PathNotFoundException, RepositoryException {
    return makePath(rootNode, path, nodetype, null);
  }

  @SuppressWarnings("unchecked")
  public static Node makePath(Node rootNode, String path, String nodetype, Map permissions) throws PathNotFoundException,
                                                                                           RepositoryException {
    String[] tokens = path.split("/");
    Node node = rootNode;
    for (int i = 0; i < tokens.length; i++) {
      String token = tokens[i];
      if (token.length() > 0) {
        if (node.hasNode(token)) {
          node = node.getNode(token);
        } else {
          node = node.addNode(token, nodetype);
          if (node.canAddMixin("exo:privilegeable")) {
            node.addMixin("exo:privilegeable");
          }
          if (permissions != null) {
            ((ExtendedNode) node).setPermissions(permissions);
          }
        }
      }
    }
    return node;
  }

  /**
   * this function used to process import version history for a node
   * 
   * @param currentNode
   * @param versionHistorySourceStream
   * @param mapHistoryValue
   * @throws Exception
   */
  public static void processImportHistory(Node currentNode,
                                          InputStream versionHistorySourceStream,
                                          Map<String, String> mapHistoryValue) throws Exception {

    for (String uuid : mapHistoryValue.keySet()) {
      ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(versionHistorySourceStream));
      byte[] data = new byte[1024];
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ZipEntry entry = zipInputStream.getNextEntry();
      while (entry != null) {
        int available = -1;
        if (entry.getName().equals(uuid + ".xml")) {
          while ((available = zipInputStream.read(data, 0, 1024)) > -1) {
            out.write(data, 0, available);
          }
          try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(out.toByteArray());
            String value = mapHistoryValue.get(uuid);
            Node versionableNode = currentNode.getSession().getNodeByUUID(uuid);
            importHistory((NodeImpl) versionableNode,
                          inputStream,
                          getBaseVersionUUID(value),
                          getPredecessors(value),
                          getVersionHistory(value));
            currentNode.getSession().save();
          } catch (ItemNotFoundException item) {
            currentNode.getSession().refresh(false);
            LOG.error("Can not found versionable node" + item, item);
          } catch (RepositoryException e) {
            currentNode.getSession().refresh(false);
            LOG.error("Import version history failed " + e, e);
          }
          zipInputStream.closeEntry();
          entry = zipInputStream.getNextEntry();
        } else {
          zipInputStream.closeEntry();
          entry = zipInputStream.getNextEntry();
        }
      }
      out.close();
      zipInputStream.close();
    }
  }

  /**
   * do import a version into a node
   * 
   * @param versionableNode
   * @param versionHistoryStream
   * @param baseVersionUuid
   * @param predecessors
   * @param versionHistory
   * @throws RepositoryException
   * @throws IOException
   */
  private static void importHistory(NodeImpl versionableNode,
                                    InputStream versionHistoryStream,
                                    String baseVersionUuid,
                                    String[] predecessors,
                                    String versionHistory) throws RepositoryException, IOException {
    VersionHistoryImporter versionHistoryImporter = new VersionHistoryImporter(versionableNode,
                                                                               versionHistoryStream,
                                                                               baseVersionUuid,
                                                                               predecessors,
                                                                               versionHistory);
    versionHistoryImporter.doImport();
  }

  /**
   * get data from the version history file
   * 
   * @param importHistorySourceStream
   * @return
   * @throws Exception
   */
  public static Map<String, String> getMapImportHistory(InputStream importHistorySourceStream) throws Exception {
    ZipInputStream zipInputStream = new ZipInputStream(importHistorySourceStream);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] data = new byte[1024];
    ZipEntry entry = zipInputStream.getNextEntry();
    Map<String, String> mapHistoryValue = new HashMap<String, String>();
    while (entry != null) {
      int available = -1;
      if (entry.getName().equals(MAPPING_FILE)) {
        while ((available = zipInputStream.read(data, 0, 1024)) > -1) {
          out.write(data, 0, available);
        }
        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String strLine;
        // Read File Line By Line
        while ((strLine = br.readLine()) != null) {
          // Put the history information into list
          if (strLine.indexOf("=") > -1) {
            mapHistoryValue.put(strLine.split("=")[0], strLine.split("=")[1]);
          }
        }
        // Close the input stream
        inputStream.close();
        zipInputStream.closeEntry();
        break;
      }
      entry = zipInputStream.getNextEntry();
    }
    out.close();
    zipInputStream.close();
    return mapHistoryValue;
  }

  private static String getBaseVersionUUID(String valueHistory) {
    String[] arrHistoryValue = valueHistory.split(";");
    return arrHistoryValue[1];
  }

  private static String[] getPredecessors(String valueHistory) {
    String[] arrHistoryValue = valueHistory.split(";");
    String strPredecessors = arrHistoryValue[1];
    if (strPredecessors.indexOf(",") > -1) {
      return strPredecessors.split(",");
    }
    return new String[] { strPredecessors };
  }

  private static String getVersionHistory(String valueHistory) {
    String[] arrHistoryValue = valueHistory.split(";");
    return arrHistoryValue[0];
  }

  /**
   * @deprecated use {@link CommonsUtils#getService(Class)}
   */
  public static <T> T getService(Class<T> clazz) {
    return CommonsUtils.getService(clazz);
  }

  /**
   * @deprecated use {@link CommonsUtils#getService(Class, String)}
   */
  public static <T> T getService(Class<T> clazz, String containerName) {
    return CommonsUtils.getService(clazz, containerName);
  }

}
