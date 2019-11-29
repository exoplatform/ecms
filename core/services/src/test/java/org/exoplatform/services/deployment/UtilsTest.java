/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.deployment;

import java.io.*;
import java.util.*;

import javax.jcr.*;
import javax.jcr.query.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.*;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.compress.CompressData;
import org.exoplatform.services.deployment.plugins.XMLDeploymentPluginTest;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com May
 * 18, 2012
 */
@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-root-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/commons-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-portal-configuration.xml") })
public class UtilsTest extends BaseCommonsTestCase {

  private static final Log       LOG            = ExoLogger.getLogger(XMLDeploymentPluginTest.class);

  protected final String         REPO_NAME      = "repository";

  protected final String         WORKSPACE_NAME = "portal-test";

  protected PortalContainer      container;

  protected RepositoryService    repositoryService;

  protected Session              session;

  protected Node                 root;

  private static final String ROOT_SQL_QUERY      = "select * from mix:versionable order by exo:dateCreated DESC";

  private static final String VERSION_SQL_QUERY   = "select * from mix:versionable where jcr:path like '$0/%' "
      + "order by exo:dateCreated DESC";

  private static final String DOC_VIEW            = "docview";

  private static final String SYS_VIEW            = "sysview";

  private static final int    CREATE_NEW_BEHAVIOR = 0;

  public void setUp() throws Exception {
    System.setProperty("exo.test.random.name", "" + Math.random());
    super.setUp();
    begin();
    container = PortalContainer.getInstance();
    repositoryService = getService(RepositoryService.class);
    configurationManager = getService(ConfigurationManager.class);

    session = repositoryService.getCurrentRepository().getSystemSession(WORKSPACE_NAME);
    root = session.getRootNode();
    System.setProperty("gatein.email.domain.url", "http://localhost:8080");
  }

  /**
   * Testcase for Utils.makePath()
   * 
   * @throws Exception
   */
  public void testMakePath() throws Exception {
    String level2RelPath = "level1/level2";
    Node level2 = Utils.makePath(root, level2RelPath, "nt:folder");
    assertEquals(level2.getPath(), root.getPath() + level2RelPath);

    String level3RelPath = "level1/level2/level3";
    Node level3 = Utils.makePath(root, level3RelPath, "nt:folder");
    assertEquals(level3.getPath(), root.getPath() + level3RelPath);

    String level5RelPath = "level4/level5";
    Node level5 = Utils.makePath(level3, level5RelPath, "nt:folder");
    assertEquals(level5.getPath(), level3.getPath() + "/" + level5RelPath);

    Map<String, String[]> permissionsMap = new HashMap<String, String[]>();
    permissionsMap.put("*:/platform/web-contributors", new String[] { "read", "addNode", "setProperty" });
    String level7RelPath = "level6/level7";
    Node level7 = Utils.makePath(level5, level7RelPath, "nt:folder", permissionsMap);
    assertEquals(level7.getPath(), level5.getPath() + "/" + level7RelPath);
  }

  /**
   * Testcase for: - Utils.getMapImportHistory() - Utils.processImportHistory()
   * Description: In this testcase, we will try to export data from node
   * testFolder, and then import it into sandbox Input JCR data structure:
   * /[root] |--> testFolder | |-----> subFolder1 | |-----> subFolder2 | |-->
   * sandbox Expected result: /[root] |--> testFolder | |-----> subFolder1 |
   * |-----> subFolder2 | |--> sandbox |--> testFolder |-----> subFolder1
   * |-----> subFolder2
   *
   * @throws Exception
   */
  public void testImportNode() throws Exception {
    /*******************************************
     * Create JCR data structure for test case *
     *******************************************/
    Node testFolder = root.addNode("testFolder", "nt:folder");
    Node sandbox = root.addNode("sandbox", "nt:folder");

    testFolder.addNode("subFolder1", "nt:folder");
    testFolder.addNode("subFolder2", "nt:folder");
    session.save();

    if (!testFolder.isNodeType("mix:versionable")) {
      testFolder.addMixin("mix:versionable");
    }
    session.save();

    /*****************
     * Export data *
     *****************/
    File exportData = exportNode(testFolder, SYS_VIEW); // export test folder to
                                                        // XML
    File zippedVersionHistory = exportVersionHistory(testFolder, SYS_VIEW); // export
                                                                            // version
                                                                            // history
                                                                            // of
    // test
    // folder

    /******************************
     * Import data into sandbox *
     ******************************/
    // import XML data
    session.importXML(sandbox.getPath(), new BufferedInputStream(new TempFileInputStream(exportData)), CREATE_NEW_BEHAVIOR);
    session.save();
    // import version history data
    Map<String, String> mapHistoryValue =
                                        Utils.getMapImportHistory(new BufferedInputStream(new FileInputStream(zippedVersionHistory)));
    Utils.processImportHistory(sandbox, new BufferedInputStream(new TempFileInputStream(zippedVersionHistory)), mapHistoryValue);

    /*****************
     * Assertion *
     *****************/
    assertTrue(sandbox.hasNode("testFolder"));

    Node importedNode = sandbox.getNode("testFolder");
    assertTrue(importedNode.isNodeType("mix:versionable"));
    assertTrue(importedNode.hasNode("subFolder1"));
    assertTrue(importedNode.hasNode("subFolder2"));
  }

  /**
   * Export a node to XML with document view or system view
   * 
   * @param currentNode
   * @param format specify the format used to exported the current JCR node, it
   *          could be docview format or sysview format
   * @return XML file
   * @throws Exception
   */
  private File exportNode(Node currentNode, String format) throws Exception {
    // the file which will keep the exported data of the node.
    File tempExportedFile = getExportedFile(format, ".xml");

    // do export
    OutputStream out = new BufferedOutputStream(new FileOutputStream(tempExportedFile));
    if (format.equals(DOC_VIEW)) {
      session.exportDocumentView(currentNode.getPath(), out, false, false);
    } else {
      session.exportSystemView(currentNode.getPath(), out, false, false);
    }
    out.flush();
    out.close();

    return tempExportedFile;
  }

  /**
   * Export version history data of a node
   * 
   * @param currentNode
   * @param format specify the format used to exported version history it could
   *          be docview format or sysview format
   * @return a zipped file containing the version history data of a node
   * @throws Exception
   */
  private File exportVersionHistory(Node currentNode, String format) throws Exception {
    QueryResult queryResult = getVersionableChidren(currentNode);
    NodeIterator queryIter = queryResult.getNodes();

    CompressData zipService = new CompressData();
    OutputStream out = null;
    InputStream in = null;
    List<File> lstExporedFile = new ArrayList<File>();
    File exportedFile = null;
    File zipFile = null;
    File propertiesFile = getExportedFile("mapping", ".properties");
    OutputStream propertiesBOS = new BufferedOutputStream(new FileOutputStream(propertiesFile));
    InputStream propertiesBIS = new BufferedInputStream(new TempFileInputStream(propertiesFile));
    while (queryIter.hasNext()) {
      exportedFile = getExportedFile("data", ".xml");
      lstExporedFile.add(exportedFile);
      out = new BufferedOutputStream(new FileOutputStream(exportedFile));
      in = new BufferedInputStream(new TempFileInputStream(exportedFile));
      Node node = queryIter.nextNode();
      String historyValue = getHistoryValue(node);
      propertiesBOS.write(historyValue.getBytes());
      propertiesBOS.write('\n');
      if (format.equals(DOC_VIEW))
        session.exportDocumentView(node.getVersionHistory().getPath(), out, false, false);
      else
        session.exportSystemView(node.getVersionHistory().getPath(), out, false, false);
      out.flush();
      zipService.addInputStream(node.getUUID() + ".xml", in);
    }

    if (currentNode.isNodeType("mix:versionable")) {
      exportedFile = getExportedFile("data", ".xml");
      lstExporedFile.add(exportedFile);
      out = new BufferedOutputStream(new FileOutputStream(exportedFile));
      in = new BufferedInputStream(new TempFileInputStream(exportedFile));
      String historyValue = getHistoryValue(currentNode);
      propertiesBOS.write(historyValue.getBytes());
      propertiesBOS.write('\n');
      if (format.equals(DOC_VIEW))
        session.exportDocumentView(currentNode.getVersionHistory().getPath(), out, false, false);
      else
        session.exportSystemView(currentNode.getVersionHistory().getPath(), out, false, false);
      out.flush();
      zipService.addInputStream(currentNode.getUUID() + ".xml", in);
    }

    propertiesBOS.flush();
    zipService.addInputStream("mapping.properties", propertiesBIS);
    zipFile = getExportedFile("data", ".zip");
    in = new BufferedInputStream(new FileInputStream(zipFile));
    out = new BufferedOutputStream(new FileOutputStream(zipFile));
    out.flush();
    zipService.createZip(out);

    return zipFile;
  }

  /**
   * Get the version history information of a node
   * 
   * @param node
   * @return
   * @throws Exception
   */
  private String getHistoryValue(Node node) throws Exception {
    String versionHistory = node.getProperty("jcr:versionHistory").getValue().getString();
    String baseVersion = node.getProperty("jcr:baseVersion").getValue().getString();
    Value[] predecessors = node.getProperty("jcr:predecessors").getValues();
    StringBuilder historyValue = new StringBuilder();
    StringBuilder predecessorsBuilder = new StringBuilder();
    for (Value value : predecessors) {
      if (predecessorsBuilder.length() > 0)
        predecessorsBuilder.append(",");
      predecessorsBuilder.append(value.getString());
    }
    historyValue.append(node.getUUID())
                .append("=")
                .append(versionHistory)
                .append(";")
                .append(baseVersion)
                .append(";")
                .append(predecessorsBuilder.toString());
    return historyValue.toString();
  }

  /**
   * get the versionable nodes of the current node
   * 
   * @param currentNode
   * @return
   * @throws RepositoryException
   */
  private QueryResult getVersionableChidren(Node currentNode) throws RepositoryException {
    QueryManager queryManager = currentNode.getSession().getWorkspace().getQueryManager();
    String queryStatement = "";
    if (currentNode.getPath().equals("/")) {
      queryStatement = ROOT_SQL_QUERY;
    } else {
      queryStatement = StringUtils.replace(VERSION_SQL_QUERY, "$0", currentNode.getPath());
    }
    Query query = queryManager.createQuery(queryStatement, Query.SQL);
    return query.execute();
  }

  private File getExportedFile(String prefix, String suffix) throws IOException {
    return File.createTempFile(prefix.concat(UUID.randomUUID().toString()), suffix);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * This input stream will remove the file which it connects to when the
   * virtual machine terminates
   * 
   * @author hailt
   */
  private static class TempFileInputStream extends FileInputStream {

    private final File file;

    public TempFileInputStream(File file) throws FileNotFoundException {
      super(file);
      this.file = file;
      try {
        file.deleteOnExit();
      } catch (Exception e) {
        // ignore me
      }
    }

    @Override
    protected void finalize() throws IOException {
      try {
        file.delete();
      } catch (Exception e) {
        // ignore me
      }
      super.finalize();
    }
  }

}
