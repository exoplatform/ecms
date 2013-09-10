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
package org.exoplatform.ecm.connector.platform;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.artofsolving.jodconverter.office.OfficeException;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.jodconverter.JodConverterService;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          vuna@exoplatform.com
 * Jun 20, 2012  
 */
@Path("/contents/populate/")
public class PopulateConnector implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(PopulateConnector.class.getName());
  /** The Constant LAST_MODIFIED_PROPERTY. */
  protected static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  protected static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  protected static final String WORKSPACE_NAME = "collaboration";

  /** Source folder */
  private static final String SOURCE_DATA_FOLDER_PATH = "/contents";
  /** Dictionary file */
  private static final String DICTIONARY_FILE = "dictionary.txt";

  /** Files to be imported */
  private static final String[] SOURCE_FILES1 = {"content.doc application/msword", "content.pdf application/pdf",
    "content.ppt application/ppt","content.xls application/xls"
  };
  private static final String[] SOURCE_FILES2 = {"image.jpg image/jpeg", "image.jpeg image/jpeg", "image.gif image/gif", 
  "image.png image/png"};

  private static final String IMPORTED_DOCUMENTS_FOLDER = "importedDocuments";

  private static final int MAX_NORMAL_DATA_RATE = 300;

  private static final int DEFAULT_DOCUMENT_SIZE = 1;//1kb

  private RepositoryService repoService_;
  private CmsService cmsService_;
  private WCMPublicationService publicationService_;
  private DataDistributionManager dataDistributionManager_;
  private TaxonomyService taxonomyService_;
  private JodConverterService jodConverter_;

  public PopulateConnector(RepositoryService repositoryService, CmsService cmsService, WCMPublicationService publicationService,
                           DataDistributionManager dataDistributionManager, TaxonomyService taxonomyService,
                           JodConverterService jodConverter) {
    repoService_ = repositoryService;
    cmsService_ = cmsService;
    publicationService_ = publicationService;
    dataDistributionManager_ = dataDistributionManager;
    taxonomyService_ = taxonomyService;
    jodConverter_ = jodConverter;
  }

  /**
   * Initializes the data to use later
   * @param isPublishDoc indicates if the newly created documents are published.
   * @param isGenerateNewData indicates if data is generated new, not copy
   * @param size size of newly generated data
   * @return
   */
  private Response initializeLoadData(boolean isPublishDoc, boolean isGenerateNewData, int size) {
    SessionProvider sessionProvider = null;
    try {
      sessionProvider = WCMCoreUtils.getUserSessionProvider();
      Session session = sessionProvider.getSession(WORKSPACE_NAME, repoService_.getCurrentRepository());
      //remove importedFolderNode
      if (session.getRootNode().hasNode(IMPORTED_DOCUMENTS_FOLDER)) {
        WCMCoreUtils.getService(TrashService.class).moveToTrash(session.getRootNode().getNode(IMPORTED_DOCUMENTS_FOLDER),
                                                                sessionProvider);
        session.save();
      }
      //create importedFolderNode
      Node importedFolderNode = session.getRootNode().addNode(IMPORTED_DOCUMENTS_FOLDER);
      importedFolderNode.addMixin(NodetypeConstant.EXO_HIDDENABLE);
      session.save();
      List<String> generatedFiles = new ArrayList<String>(Arrays.asList(SOURCE_FILES1));
      List<String> initializedFiles = new ArrayList<String>(Arrays.asList(SOURCE_FILES2));
      if (!isGenerateNewData) {
        generatedFiles.clear();
        initializedFiles.addAll(Arrays.asList(SOURCE_FILES1));
      }
      //import source files into JCR
      for (String importedFile : initializedFiles) {
        String importedFileName = importedFile.split(" ")[0];
        String mimeType = importedFile.split(" ")[1];
        if (!session.itemExists(importedFolderNode.getPath() + "/" + importedFileName)) {
          InputStream inputStream = this.getClass().getResourceAsStream(SOURCE_DATA_FOLDER_PATH + "/" + importedFileName);
          String fileNodeName = cmsService_.storeNode("nt:file", importedFolderNode,
                                                      getInputProperties(importedFileName, inputStream, mimeType), true);
          if (isPublishDoc) {
            publicationService_.updateLifecyleOnChangeContent((Node)session.getItem(fileNodeName), "acme", "root","published");
          }
        }
      }
      //generate source file
      for (String importedFile : generatedFiles) {
        String importedFileName = importedFile.split(" ")[0];
        String mimeType = importedFile.split(" ")[1];
        if (!session.itemExists(importedFolderNode.getPath() + "/" + importedFileName)) {
          String fileNodeName = generateFile(importedFolderNode, importedFileName, size, mimeType);
          if (isPublishDoc) {
            publicationService_.updateLifecyleOnChangeContent((Node)session.getItem(fileNodeName), "acme", "root","published");
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
      return Response.serverError().build();
    }

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

  /**
   * 
   * @param parentNode
   * @param fileName
   * @param size
   * @return
   */
  private String generateFile(Node parentNode, String fileName, int size, String mimeType) throws Exception {
    String fileExtension = fileName.substring(fileName.indexOf('.') + 1);
    //build the set of word to generate document
    BufferedReader br = null;
    br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(SOURCE_DATA_FOLDER_PATH + "/" + 
        DICTIONARY_FILE)));
    String line = null;
    List<String> dictionary = new ArrayList<String>(); 
    while ((line = br.readLine()) != null) {
      for (String word : line.split("[ .,;]")) {
        dictionary.add(word);
      }
    }
    br.close();
    //build the document content
    size = size == 0 ? DEFAULT_DOCUMENT_SIZE : size;
    StringBuilder content = new StringBuilder("Lorem ");
    int sentenceLength = (int)((Math.random() * 20)) + 5;
    while (content.length() < size * 1024) {
      content.append(dictionary.get((int)(Math.random() * dictionary.size())));
      if (--sentenceLength == 0) {
        content.append(". ");
        sentenceLength = (int)((Math.random() * 20)) + 5;
      } else {
        content.append(' ');
      }
    }
    content.append('.');
    File tempFile = null;
    if(fileExtension.equalsIgnoreCase("doc")) {
      //create a temporary txt file containing generated content at previous step
      tempFile = File.createTempFile("content_temp", fileExtension);
      InputStream input = new BufferedInputStream(new ByteArrayInputStream(content.toString().getBytes()));
      OutputStream out = new BufferedOutputStream((new FileOutputStream(tempFile)));
      // create temp file to store original data of nt:file node
      File in = File.createTempFile("content_tmp", null);
      read(input, new BufferedOutputStream(new FileOutputStream(in)));
      try {
        boolean success = jodConverter_.convert(in, tempFile, fileExtension);
        // If the converting was failure then delete the content temporary file
        if (!success) {
          tempFile.delete();
        }
      } catch (OfficeException connection) {
        tempFile.delete();
        if (LOG.isErrorEnabled()) {
          LOG.error("Exception when using Office Service");
        }
      } finally {
        in.delete();
        out.flush();
        out.close();
      }
    } else {
      try {
        DocumentRenderer documentRender = new DocumentRenderer();
        boolean success = documentRender.createDocument(content.toString(), fileName, fileExtension);
        if(success) tempFile = new File(fileName); 
      } catch(Exception ex) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Exception when creating document");
        }
      }
    }
    //import the newly created file into jcr
    InputStream inputStream = new FileInputStream(tempFile);
    String fileNodeName = cmsService_.storeNode("nt:file", parentNode,
                                                getInputProperties(fileName, inputStream, mimeType), true);


    return fileNodeName;
  }

  private void read(InputStream is, OutputStream os) throws Exception {
    int bufferLength = 1024;
    int readLength = 0;
    while (readLength > -1) {
      byte[] chunk = new byte[bufferLength];
      readLength = is.read(chunk);
      if (readLength > 0) {
        os.write(chunk, 0, readLength);
      }
    }
    os.flush();
    os.close();
  }

  /**
   * Initializes the data to use later
   * @param isPublishDoc indicates if the newly created documents are published.
   * @return
   */
  @GET
  @Path("/initialLoad")
  public Response initialLoad(@QueryParam("isPublishDoc") boolean isPublishDoc) {
    return initializeLoadData(isPublishDoc, false, 0);
  }

  /**
   * Creates mass amount of data
   * @param name the node name
   * @param docType type of the document to create
   * @param from document name will start with suffix "from", and increasing by one for the next doc
   * @param to the bottom range of document name
   * @param folderPath the location where all documents will be created
   * @param categories the categories which created documents are attached.
   * @return
   */
  @GET
  @Path("/storage")
  public Response storage(@QueryParam("name") String name, @QueryParam("docType") String docType, 
                          @QueryParam("from") int from, @QueryParam("to") int to, 
                          @QueryParam("workspace") String workspace,
                          @QueryParam("folderPath") String folderPath,
                          @QueryParam("categories") String categories,
                          @QueryParam("size") Integer size) {

    SessionProvider sessionProvider = null;
    try {
      //0.initial data
      if (!folderPath.startsWith("/")) {
        folderPath = "/" + folderPath;
      }
      sessionProvider = WCMCoreUtils.getUserSessionProvider();
      Session sourceSession = sessionProvider.getSession(WORKSPACE_NAME, repoService_.getCurrentRepository());
      Session session = sessionProvider.getSession(workspace, repoService_.getCurrentRepository());

      initializeLoadData(true, true, (size == null ? 0 : size));
      //1.get source node
      Node sourceNode = getSourceNode(sourceSession, IMPORTED_DOCUMENTS_FOLDER, docType);
      Node targetFolder = dataDistributionManager_.getDataDistributionType(DataDistributionMode.NONE).getOrCreateDataNode(
                                                                                                                          session.getRootNode(), folderPath);
      //2.store nodes
      if (to - from < MAX_NORMAL_DATA_RATE) {
        //normal mode
        for (int i = from; i <= to; i++) {
          String storedNodePath = new StringBuilder(folderPath).append("/").append(name).
              append(i).append('.').append(docType).toString();
          if (!session.itemExists(storedNodePath)) {
            session.getWorkspace().copy(WORKSPACE_NAME, sourceNode.getPath(), storedNodePath);
            Node newNode = ((Node)session.getItem(storedNodePath));
            newNode.setProperty("exo:title", name + i + '.' + docType);
            session.save();
          }
          Node newNode = ((Node)session.getItem(storedNodePath));
          addTaxonomy(newNode, categories);
        }
      } else {
        //optimize storage mode
        DataDistributionType dataDistributionType = 
            dataDistributionManager_.getDataDistributionType(DataDistributionMode.OPTIMIZED);
        Node parentFolder = null;
        for (int i = from; i <= to; i++) {
          if ((i == from) || (i % 100 == 0)) {
            parentFolder = dataDistributionType.getOrCreateDataNode(targetFolder, name + i);
          }
          String storedNodePath = new StringBuilder(parentFolder.getPath()).append("/").append(name).
              append(i).append('.').append(docType).toString();
          if (!session.itemExists(storedNodePath)) {
            session.getWorkspace().copy(WORKSPACE_NAME, sourceNode.getPath(), storedNodePath);
            Node newNode = ((Node)session.getItem(storedNodePath));
            newNode.setProperty("exo:title", name + i + '.' + docType);
            session.save();
          }
          Node newNode = ((Node)session.getItem(storedNodePath));
          addTaxonomy(newNode, categories);
        }
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("Data Injector for ECMS finished successfully!....");
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("Data Injector for ECMS failed!....");
      }
      return Response.serverError().build();
    } 

    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

  /**
   * returns the document node in the folderPath corresponding to the given docType 
   * @param session session in which node will be retrieved
   * @param folderPath location of the nodes to search
   * @param docType type of document to get
   * @return the Node
   * @throws Exception
   */
  private Node getSourceNode(Session session, String folderPath, String docType) throws Exception {
    Node folderNode = session.getRootNode().getNode(folderPath);
    for (NodeIterator iter = folderNode.getNodes(); iter.hasNext();) {
      Node childNode = iter.nextNode();
      String nodeName = childNode.getName();
      int index = nodeName.indexOf(".");
      if (index > -1) {
        if (docType != null && docType.equals(nodeName.substring(index+1))) {
          return childNode;
        }
      }
    }
    return null;
  }

  /**
   * adds taxonomies to the given node
   * @param node the node to add taxonomy
   * @param categoryList the categories which will be assigned to the node
   * @throws Exception
   */
  private void addTaxonomy(Node node, String categoryList) throws Exception {
    if (categoryList == null) {
      return;
    }
    for (String category : categoryList.split(",")) {
      try {
        List<String> arrayCategoryPath = new ArrayList<String>();
        for (String categoryPart : category.split("/")) {
          if (categoryPart.trim().length() > 0) {
            arrayCategoryPath.add(categoryPart.trim());
          }
        }
        if (arrayCategoryPath.size() == 1) {
          taxonomyService_.addCategory(node, arrayCategoryPath.get(0), "");
        } else {
          StringBuffer categoryPath = new StringBuffer("/");
          for (int i = 1; i < arrayCategoryPath.size(); i++) {
            categoryPath.append(arrayCategoryPath.get(i)).append("/");
          }
          taxonomyService_.addCategory(node, arrayCategoryPath.get(0), categoryPath.toString());
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error(e);
        }
      }
    }
  }

  /**
   * gets the input properties map by given parameters
   * @param name the node name
   * @param inputStream the input stream
   * @param mimeType the mimetype
   * @return
   */
  private Map<String, JcrInputProperty> getInputProperties(String name,
                                                           InputStream inputStream, String mimeType) {
    Map<String, JcrInputProperty> inputProperties = new HashMap<String, JcrInputProperty>();
    JcrInputProperty nodeInput = new JcrInputProperty();
    nodeInput.setJcrPath("/node");
    nodeInput.setValue(name);
    nodeInput.setMixintype("mix:i18n,mix:votable,mix:commentable");
    nodeInput.setType(JcrInputProperty.NODE);
    inputProperties.put("/node", nodeInput);

    JcrInputProperty jcrContent = new JcrInputProperty();
    jcrContent.setJcrPath("/node/jcr:content");
    jcrContent.setValue("");
    jcrContent.setMixintype("dc:elementSet");
    jcrContent.setNodetype("nt:resource");
    jcrContent.setType(JcrInputProperty.NODE);
    inputProperties.put("/node/jcr:content", jcrContent);

    JcrInputProperty jcrData = new JcrInputProperty();
    jcrData.setJcrPath("/node/jcr:content/jcr:data");
    jcrData.setValue(inputStream);
    inputProperties.put("/node/jcr:content/jcr:data", jcrData);

    JcrInputProperty jcrMimeType = new JcrInputProperty();
    jcrMimeType.setJcrPath("/node/jcr:content/jcr:mimeType");
    jcrMimeType.setValue(mimeType);

    inputProperties.put("/node/jcr:content/jcr:mimeType", jcrMimeType);
    JcrInputProperty jcrLastModified = new JcrInputProperty();
    jcrLastModified.setJcrPath("/node/jcr:content/jcr:lastModified");
    jcrLastModified.setValue(new GregorianCalendar());
    inputProperties.put("/node/jcr:content/jcr:lastModified", jcrLastModified);

    JcrInputProperty jcrEncoding = new JcrInputProperty();
    jcrEncoding.setJcrPath("/node/jcr:content/jcr:encoding");
    jcrEncoding.setValue("UTF-8");
    inputProperties.put("/node/jcr:content/jcr:encoding", jcrEncoding);

    return inputProperties;
  }

}
