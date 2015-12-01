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
package org.exoplatform.services.pdfviewer;

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

import javax.jcr.Node;

import org.artofsolving.jodconverter.office.OfficeException;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.jodconverter.JodConverterService;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 6 Jul 2012  
 */

public class PDFViewerService {
  private static final int MAX_NAME_LENGTH= 150;
  private static final Log LOG  = ExoLogger.getLogger(PDFViewerService.class.getName());
  private JodConverterService jodConverter_;
  private ExoCache<Serializable, Object> pdfCache;
  
  public PDFViewerService(RepositoryService repositoryService,
                          CacheService caService,
                          JodConverterService jodConverter) throws Exception {
    jodConverter_ = jodConverter;
    pdfCache = caService.getCacheInstance(PDFViewerService.class.getName());
  }
  public ExoCache<Serializable, Object> getCache() {
    return pdfCache;
  }
  /**
   * Init pdf document from InputStream in nt:file node
   * @param currentNode
   * @param repoName
   * @return
   * @throws Exception
   */
  public Document initDocument(Node currentNode, String repoName) throws Exception {
    return buildDocumentImage(getPDFDocumentFile(currentNode, repoName), currentNode.getName());
  }

  public Document buildDocumentImage(File input, String name) {
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
  /**
   * Write PDF data to file
   * @param currentNode
   * @param repoName
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
    
    String lastModified = Utils.getJcrContentLastModified(currentNode);
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
        if (contentNode.hasProperty("jcr:lastModified")) {
          pdfCache.put(new ObjectKey(bd.toString()), content.getPath());
          contentNode.setProperty("jcr:lastModified", content.lastModified());
          contentNode.save();
          pdfCache.put(new ObjectKey(bd1.toString()), Utils.getJcrContentLastModified(currentNode));
        }
      }
    }
    return content;
  }
  
  /**
   * reduces the file name size. If the length is > 150, return the first 150 characters, else, return the original value
   * @param name the name
   * @return the reduced name 
   */
  private String reduceFileNameSize(String name) {
    return (name != null && name.length() > MAX_NAME_LENGTH) ? name.substring(0, MAX_NAME_LENGTH) : name;
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
}