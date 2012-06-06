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
package org.exoplatform.ecm.REST.viewer;

import java.awt.image.BufferedImage;
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

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.artofsolving.jodconverter.office.OfficeException;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.jodconverter.JodConverterService;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 3, 2009  
 * 7:33:30 AM
 */
/**
 * Provide the request which will be used to display PDF file on web browser
 * {repoName} Repository name
 * {workspaceName} Workspace name
 * {nodePath} Node path
 * {pageNumber} Page number
 * {rotation} Page rotation, values are valid: 0.0f, 90.0f, 180.0f, 270.0f
 * {scale} Zoom factor to be applied to the rendered page 
 * Example: <img src="/portal/rest/pdfviewer/repository/collaboration/test.pdf/1/0.0f/1.0f">
 */
@Path("/pdfviewer/{repoName}/{workspaceName}/{pageNumber}/{rotation}/{scale}/{uuid}/")
public class PDFViewerRESTService implements ResourceContainer {

  private static final String LASTMODIFIED = "Last-Modified";
  private RepositoryService repositoryService_;
  private ExoCache<Serializable, Object> pdfCache;
  private JodConverterService jodConverter_;
  private static final Log LOG  = ExoLogger.getLogger(PDFViewerRESTService.class.getName());

  public PDFViewerRESTService(RepositoryService repositoryService, CacheService caService, JodConverterService jodConverter) throws Exception {
    repositoryService_ = repositoryService;
    jodConverter_ = jodConverter;
    pdfCache = caService.getCacheInstance(PDFViewerRESTService.class.getName());
    //System.setProperty("org.icepdf.core.awtFontLoading", "true");
  }

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
	  	File content = new File((String) pdfCache.get(new ObjectKey(bd.toString())));
	  	if (!content.exists()) {
				ManageableRepository repository = repositoryService_.getRepository(repoName);
				session = getSystemProvider().getSession(wsName, repository);
				Node currentNode = session.getNodeByUUID(uuid);
	  		initDocument(currentNode, repoName);
	  	}
      // capture the page image to file
			String lastModified = (String) pdfCache.get(new ObjectKey(bd1.append(bd.toString()).append("/jcr:lastModified").toString()));
      InputStream is = pushToCache(new File((String) pdfCache.get(new ObjectKey(bd.toString()))), repoName, wsName, uuid, 
                                   pageNumber, strRotation, strScale, lastModified);
      return Response.ok(is, "image").header(LASTMODIFIED, lastModified).build();
    } catch (Exception e) {
      LOG.error(e);
    } finally {
  			if (session != null)
  				session.logout();
  	}
    return Response.ok().build();
  }

  private SessionProvider getSystemProvider() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    SessionProviderService service = 
      (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);
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
 	  // capture the page image to file
    try {
      document.setInputStream(new BufferedInputStream(new FileInputStream(input)), name);
    } catch (PDFException ex) {
   	 LOG.error("Error parsing PDF document " + ex);
    } catch (PDFSecurityException ex) {
   	 LOG.error("Error encryption not supported " + ex);
    } catch (FileNotFoundException ex) {
   	 LOG.error("Error file not found " + ex);
    } catch (IOException ex) {
   	 LOG.error("Error handling PDF document " + ex);
    }
    return document;
  }
    
  private File buildFileImage(File input, String path, String pageNumber, String strRotation, String strScale) {
  	 //System.setProperty("org.icepdf.core.awtFontLoading", "true");
  	 //FontManager.getInstance().readSystemFonts(new String[] {"/usr/share/fonts/truetype/", "/usr/share/fonts/truetype/fonts1/", "/usr/share/fonts/truetype/fonts2"});
  	 Document document = buildDocumentImage(input, path);
     // save page capture to file.
     float scale = Float.parseFloat(strScale);
     float rotation = Float.parseFloat(strRotation);
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
     File file = null;
     try {
       file= File.createTempFile("imageCapture1_" + pageNum,".png");
       ImageIO.write(image, "png", file);
     } catch (IOException e) {
    	 LOG.error(e);
     } finally {
       image.flush();
       // clean up resources
       document.dispose();
     }
     return file;
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
  	String name = currentNode.getName();
    Node contentNode = currentNode.getNode("jcr:content");
    String lastModified = contentNode.getProperty("jcr:lastModified").getString();
  	if (path == null || !(content = new File(path)).exists() || !lastModified.equals(lastModifiedTime)) {
  		String mimeType = contentNode.getProperty("jcr:mimeType").getString();
  		InputStream input = new BufferedInputStream(contentNode.getProperty("jcr:data").getStream()); 
  		// Create temp file to store data of nt:file node 
  		if (name.indexOf(".") > 0) name = name.substring(0, name.lastIndexOf("."));
  		content = File.createTempFile(name, ".pdf");
  		content.deleteOnExit();
  		// Convert to pdf if need
  		String extension = DMSMimeTypeResolver.getInstance().getExtension(mimeType);
  		if ("pdf".equals(extension)) {
  			read(input, new BufferedOutputStream(new FileOutputStream(content)));
  		} else {
  			OutputStream out = new BufferedOutputStream((new FileOutputStream(content)));
        // create temp file to store original data of nt:file node
        File in = File.createTempFile(name + "_tmp", extension);
        read(input, new BufferedOutputStream(new FileOutputStream(in)));  			
  			try {
          boolean success = jodConverter_.convert(in, content, "pdf");
          // If the converting was failure then delete the content temporary file
          if (!success) {
            content.delete();
          }
  			} catch(OfficeException connection) {
  				content.delete();
  				LOG.error("Exception when using Office Service");
  			} finally {
  			  in.delete();
  				out.flush();
	  			out.close();
  			}
  		}
  		if (content.exists()) {
          if (contentNode.hasProperty("jcr:lastModified")) {
            pdfCache.put(new ObjectKey(bd.toString()), content.getPath());
            pdfCache.put(new ObjectKey(bd1.toString()), lastModified);
          }   
  		}
  	}
  	return content;
  }
 
	private void read(InputStream is, OutputStream os) throws Exception {
		int bufferLength = 1024; // TODO: Better to compute bufferLength in term of
		// -Xms, -Xmx properties
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
	 * Create key for cache. When key object is collected by GC, value (if is file) will be delete.
	 * @param key
	 * @return
	 * @throws IOException
	 */
	private class ObjectKey implements Serializable {
		String key;
		private ObjectKey(String key) {
			this.key = key;
		}
		@Override
		public String toString() {
			return key;
		}

		@Override
		public void finalize() {
			String path = (String) pdfCache.get(new ObjectKey(key));
			File f = new File(path);
			if (f.exists()) {
				f.delete();
			}
		}
		
		public String getKey() {
			return key;
		}
		
		@Override
		public int hashCode() {
			return key == null ? -1 : key.hashCode();
		}
		
		@Override
		public boolean equals(Object otherKey) {
			if (otherKey != null && ObjectKey.class.isInstance(otherKey)
					&& (key != null) && (key.equals(((ObjectKey) (otherKey)).getKey()))) {
				return true;
			}
			return false;
		}
	}
	
}
