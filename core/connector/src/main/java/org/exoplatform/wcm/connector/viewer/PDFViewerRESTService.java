/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.wcm.connector.viewer;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.artofsolving.jodconverter.office.OfficeException;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.jodconverter.JodConverterService;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.pdfviewer.ObjectKey;
import org.exoplatform.services.pdfviewer.PDFViewerService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.pobjects.Stream;
import org.icepdf.core.util.GraphicsRenderingHints;

/**
 * Returns a PDF content to be displayed on the web page.
 *
 * @LevelAPI Provisional
 *
 * @anchor PDFViewerRESTService
 */
@Path("/pdfviewer/{repoName}/{workspaceName}/{pageNumber}/{rotation}/{scale}/{uuid}/")
public class PDFViewerRESTService implements ResourceContainer {

  private static final int MAX_NAME_LENGTH= 150;
  private static final String LASTMODIFIED = "Last-Modified";
  private RepositoryService repositoryService_;
  private ExoCache<Serializable, Object> pdfCache;
  private JodConverterService jodConverter_;
  private static final Log LOG  = ExoLogger.getLogger(PDFViewerRESTService.class.getName());

  public PDFViewerRESTService(RepositoryService repositoryService,
                              CacheService caService,
                              JodConverterService jodConverter) throws Exception {
    repositoryService_ = repositoryService;
    jodConverter_ = jodConverter;
    PDFViewerService pdfViewerService = WCMCoreUtils.getService(PDFViewerService.class);
    if(pdfViewerService != null){
      pdfCache = pdfViewerService.getCache();
    }else{
      pdfCache = caService.getCacheInstance(PDFViewerRESTService.class.getName());
    }
  }

  /**
   * Returns a thumbnail image for a PDF document.
   *
   * @param repoName The repository name.
   * @param wsName The workspace name.
   * @param uuid The identifier of the document.
   * @param pageNumber The page number.
   * @param rotation The page rotation. The valid values are: 0.0f, 90.0f, 180.0f, 270.0f.
   * @param scale The Zoom factor which is applied to the rendered page.
   * @return Response inputstream.
   * @throws Exception The exception
   *
   * @anchor PDFViewerRESTService.getCoverImage
   */
  @GET
  public Response getCoverImage(@PathParam("repoName") String repoName,
      @PathParam("workspaceName") String wsName,
      @PathParam("uuid") String uuid,
      @PathParam("pageNumber") String pageNumber,
      @PathParam("rotation") String rotation,
      @PathParam("scale") String scale) throws Exception {
    return getImageByPageNumber(repoName, wsName, uuid, pageNumber, rotation, scale);
  }

  private Response getImageByPageNumber(String repoName, String wsName, String uuid,
      String pageNumber, String strRotation, String strScale) throws Exception {
    StringBuilder bd = new StringBuilder();
    StringBuilder bd1 = new StringBuilder();
    bd.append(repoName).append("/").append(wsName).append("/").append(uuid);
    Session session = null;
    try {
      Object objCache = pdfCache.get(new ObjectKey(bd.toString()));
      InputStream is = null;
      ManageableRepository repository = repositoryService_.getCurrentRepository();
      session = getSystemProvider().getSession(wsName, repository);
      Node currentNode = session.getNodeByUUID(uuid);
      String lastModified = (String) pdfCache.get(new ObjectKey(bd1.append(bd.toString())
                                                                .append("/jcr:lastModified").toString()));
      if(objCache!=null) {
        File content = new File((String) pdfCache.get(new ObjectKey(bd.toString())));
        if (!content.exists()) {
          initDocument(currentNode, repoName);
        }
        is = pushToCache(new File((String) pdfCache.get(new ObjectKey(bd.toString()))),
                          repoName, wsName, uuid, pageNumber, strRotation, strScale, lastModified);
      } else {
        File file = getPDFDocumentFile(currentNode, repoName);
        is = pushToCache(file, repoName, wsName, uuid, pageNumber, strRotation, strScale, lastModified);
      }
      return Response.ok(is, "image").header(LASTMODIFIED, lastModified).build();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return Response.ok().build();
  }

  private SessionProvider getSystemProvider() {
    SessionProviderService service = WCMCoreUtils.getService(SessionProviderService.class);
    return service.getSystemSessionProvider(null) ;
  }

  private InputStream pushToCache(File content, String repoName, String wsName, String uuid,
      String pageNumber, String strRotation, String strScale, String lastModified) throws FileNotFoundException {
    StringBuilder bd = new StringBuilder();
    bd.append(repoName).append("/").append(wsName).append("/").append(uuid).append("/").append(
        pageNumber).append("/").append(strRotation).append("/").append(strScale);
    StringBuilder bd1 = new StringBuilder().append(bd).append("/jcr:lastModified");
    String filePath = (String) pdfCache.get(new ObjectKey(bd.toString()));
    String fileModifiedTime = (String) pdfCache.get(new ObjectKey(bd1.toString()));
    if (filePath == null || !(new File(filePath).exists()) || !StringUtils.equals(lastModified, fileModifiedTime)) {
      File file = buildFileImage(content, uuid, pageNumber, strRotation, strScale);
      filePath = file.getPath();
      pdfCache.put(new ObjectKey(bd.toString()), filePath);
      pdfCache.put(new ObjectKey(bd1.toString()), lastModified);
    }
    return new BufferedInputStream(new FileInputStream(new File(filePath)));
  }

  private Document buildDocumentImage(File input, String name) {
     Document document = new Document();

     // Turn off Log of org.icepdf.core.pobjects.Document to avoid printing error stack trace in case viewing
     // a PDF file which use new Public Key Security Handler.
     // TODO: Remove this statement after IcePDF fix this
     Logger.getLogger(Document.class.toString()).setLevel(Level.OFF);

    // Capture the page image to file
    try {
      // cut the file name if name is too long, because OS allows only file with name < 250 characters
      name = reduceFileNameSize(name);
      document.setInputStream(new BufferedInputStream(new FileInputStream(input)), name);
    } catch (PDFException ex) {
      if (LOG.isDebugEnabled()) {
        LOG.error("Error parsing PDF document " + ex);
      }
    } catch (PDFSecurityException ex) {
      if (LOG.isDebugEnabled()) {
        LOG.error("Error encryption not supported " + ex);
      }
    } catch (FileNotFoundException ex) {
      if (LOG.isDebugEnabled()) {
        LOG.error("Error file not found " + ex);
      }
    } catch (IOException ex) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Error handling PDF document: {} {}", name, ex.toString());
      }
    }

    return document;
  }

  private File buildFileImage(File input, String path, String pageNumber, String strRotation, String strScale) {
     Document document = buildDocumentImage(input, path);

     // Turn off Log of org.icepdf.core.pobjects.Stream to not print error stack trace in case
     // viewing a PDF file including CCITT (Fax format) images
     // TODO: Remove these statement and comments after IcePDF fix ECMS-3765
     Logger.getLogger(Stream.class.toString()).setLevel(Level.OFF);

     // save page capture to file.
     float scale = 1.0f;
     try {
       scale = Float.parseFloat(strScale);
       // maximum scale support is 300%
       if (scale > 3.0f) {
         scale = 3.0f;
       }
     } catch (NumberFormatException e) {
       scale = 1.0f;
     }
     float rotation = 0.0f;
     try {
       rotation = Float.parseFloat(strRotation);
     } catch (NumberFormatException e) {
       rotation = 0.0f;
     }
     int maximumOfPage = document.getNumberOfPages();
     int pageNum = 1;
     try {
       pageNum = Integer.parseInt(pageNumber);
     } catch(NumberFormatException e) {
       pageNum = 1;
     }
     if(pageNum >= maximumOfPage) pageNum = maximumOfPage;
     else if(pageNum < 1) pageNum = 1;
     // Paint each pages content to an image and write the image to file
     BufferedImage image = (BufferedImage) document.getPageImage(pageNum - 1, GraphicsRenderingHints.SCREEN,
         Page.BOUNDARY_CROPBOX, rotation, scale);
     RenderedImage rendImage = image;
     File file = null;
     try {
       file= File.createTempFile("imageCapture1_" + pageNum,".png");
       /*
       file.deleteOnExit();
         PM Comment : I removed this line because each deleteOnExit creates a reference in the JVM for future removal
         Each JVM reference takes 1KB of system memory and leads to a memleak
       */
       ImageIO.write(rendImage, "png", file);
     } catch (IOException e) {
       if (LOG.isErrorEnabled()) {
         LOG.error(e);
       }
     } finally {
       image.flush();
       // clean up resources
       document.dispose();
     }
     return file;
  }

  /**
   * Initializes the PDF document from InputStream in the _nt\:file_ node.
   * @param currentNode The name of the current node.
   * @param repoName  The repository name.
   * @return
   * @throws Exception
   */
  public Document initDocument(Node currentNode, String repoName) throws Exception {
    return buildDocumentImage(getPDFDocumentFile(currentNode, repoName), currentNode.getName());
  }

  /**
   * Writes PDF data to file.
   * @param currentNode The name of the current node.
   * @param repoName The repository name.
   * @return
   * @throws Exception
   */
  public File getPDFDocumentFile(Node currentNode, String repoName) throws Exception {
    String wsName = currentNode.getSession().getWorkspace().getName();
    String uuid = currentNode.getUUID();
    StringBuilder bd = new StringBuilder();
    StringBuilder bd1 = new StringBuilder();
    bd.append(repoName).append("/").append(wsName).append("/").append(uuid);
    bd1.append(bd).append("/jcr:lastModified");
    String path = (String) pdfCache.get(new ObjectKey(bd.toString()));
    String lastModifiedTime = (String)pdfCache.get(new ObjectKey(bd1.toString()));
    File content = null;
    String name = currentNode.getName().replaceAll(":","_");
    Node contentNode = currentNode.getNode("jcr:content");
    String lastModified = getJcrLastModified(currentNode);

    if (path == null
            || !(content = new File(path)).exists()
            || !lastModified.equals(lastModifiedTime)
            || (content.length() != contentNode.getProperty("jcr:data").getLength())) {
      String mimeType = contentNode.getProperty("jcr:mimeType").getString();
      InputStream input = new BufferedInputStream(contentNode.getProperty("jcr:data").getStream());
      // Create temp file to store converted data of nt:file node
      if (name.indexOf(".") > 0) name = name.substring(0, name.lastIndexOf("."));
      // cut the file name if name is too long, because OS allows only file with name < 250 characters
      name = reduceFileNameSize(name);
      content = File.createTempFile(name + "_tmp", ".pdf");
      /*
      file.deleteOnExit();
        PM Comment : I removed this line because each deleteOnExit creates a reference in the JVM for future removal
        Each JVM reference takes 1KB of system memory and leads to a memleak
      */
      // Convert to pdf if need
      String extension = DMSMimeTypeResolver.getInstance().getExtension(mimeType);
      if ("pdf".equals(extension)) {
        read(input, new BufferedOutputStream(new FileOutputStream(content)));
      } else {
        // create temp file to store original data of nt:file node
        File in = File.createTempFile(name + "_tmp", null);
        read(input, new BufferedOutputStream(new FileOutputStream(in)));
        try {
          boolean success = jodConverter_.convert(in, content, "pdf");
          // If the converting was failure then delete the content temporary file
          if (!success) {
            content.delete();
          }
        } catch (OfficeException connection) {
          content.delete();
          if (LOG.isErrorEnabled()) {
            LOG.error("Exception when using Office Service");
          }
        } finally {
          in.delete();
        }
      }
      if (content.exists()) {
        pdfCache.put(new ObjectKey(bd.toString()), content.getPath());
        contentNode.setProperty("jcr:lastModified", content.lastModified());
        contentNode.save();
        pdfCache.put(new ObjectKey(bd1.toString()), Utils.getJcrContentLastModified(currentNode));
      }
    }
    return content;
  }

  private String getJcrLastModified(Node node) throws Exception {
    Node checkedNode = node;
    if (node.isNodeType("nt:frozenNode")) {
      checkedNode = node.getSession().getNodeByUUID(node.getProperty("jcr:frozenUuid").getString());
    }
    return Utils.getJcrContentLastModified(checkedNode);
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
   * reduces the file name size. If the length is > 150, return the first 150 characters, else, return the original value
   * @param name the name
   * @return the reduced name 
   */
  private String reduceFileNameSize(String name) {
    return (name != null && name.length() > MAX_NAME_LENGTH) ? name.substring(0, MAX_NAME_LENGTH) : name;
  }


}
