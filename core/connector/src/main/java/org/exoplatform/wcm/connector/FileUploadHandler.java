/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.wcm.connector;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.GregorianCalendar;

import javax.jcr.Node;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.ecm.connector.fckeditor.FCKMessage;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com
 * Sep 4, 2009
 */
public class FileUploadHandler {

  /** The Constant UPLOAD_ACTION. */
  public final static String UPLOAD_ACTION = "upload".intern();

  /** The Constant PROGRESS_ACTION. */
  public final static String PROGRESS_ACTION = "progress".intern();

  /** The Constant ABORT_ACTION. */
  public final static String ABORT_ACTION = "abort".intern();

  /** The Constant DELETE_ACTION. */
  public final static String DELETE_ACTION = "delete".intern();

  /** The Constant SAVE_ACTION. */
  public final static String SAVE_ACTION = "save".intern();

  /** The upload service. */
  private UploadService uploadService;

  /** The fck message. */
  private FCKMessage fckMessage;
  
  /**
   * Instantiates a new file upload handler.
   * 
   * @param container the container
   */
  public FileUploadHandler() {  
    uploadService = WCMCoreUtils.getService(UploadService.class);
    fckMessage = new FCKMessage();
  }
  
  /**
   * Upload.
   * 
   * @param uploadId the upload id
   * @param contentType the content type
   * @param contentLength the content length
   * @param inputStream the input stream
   * @param currentNode the current node
   * @param language the language
   * @param limit the limit
   * 
   * @return the response
   * 
   * @throws Exception the exception
   */
  public Response upload(String uploadId, String contentType, double contentLength, InputStream inputStream, Node currentNode, String language, int limit) throws Exception {
    // Require from portal 2.5.5
    uploadService.addUploadLimit(uploadId, limit);
    uploadService.createUploadResource(uploadId,null,contentType,contentLength,inputStream);
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    return Response.ok(null, MediaType.TEXT_XML).cacheControl(cacheControl).build();            
  }

  /**
   * Control.
   * 
   * @param uploadId the upload id
   * @param action the action
   * 
   * @return the response
   * 
   * @throws Exception the exception
   */
  public Response control(String uploadId, String action) throws Exception {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    if(FileUploadHandler.PROGRESS_ACTION.equals(action)) {
      Document currentProgress = getProgress(uploadId);      
      return Response.ok(new DOMSource(currentProgress), MediaType.TEXT_XML).cacheControl(cacheControl).build();
    }else if(FileUploadHandler.ABORT_ACTION.equals(action)) {
      uploadService.removeUpload(uploadId);
      return Response.ok(null, MediaType.TEXT_XML).cacheControl(cacheControl).build();    
    }else if(FileUploadHandler.DELETE_ACTION.equals(action)) {
      uploadService.removeUpload(uploadId);
      return Response.ok(null, MediaType.TEXT_XML).cacheControl(cacheControl).build();    
    }
    return Response.status(HTTPStatus.BAD_REQUEST).cacheControl(cacheControl).build();
  }
  
  /**
   * Save as nt file.
   * 
   * @param parent the parent
   * @param uploadId the upload id
   * @param fileName the file name
   * @param language the language
   * 
   * @return the response
   * 
   * @throws Exception the exception
   */
  public Response saveAsNTFile(Node parent, String uploadId, String fileName, String language, String siteName, String userId) throws Exception {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    UploadResource resource = uploadService.getUploadResource(uploadId);
    if (parent == null) {
      Document fileNotUploaded = 
        fckMessage.createMessage(FCKMessage.FILE_NOT_UPLOADED, FCKMessage.ERROR, language, null);
      return Response.ok(new DOMSource(fileNotUploaded), MediaType.TEXT_XML).cacheControl(cacheControl).build();
    }
    if(!FCKUtils.hasAddNodePermission(parent)) {
      Object[] args = { parent.getPath() };
      Document message = 
        fckMessage.createMessage(FCKMessage.FILE_UPLOAD_RESTRICTION,FCKMessage.ERROR,language,args);
      return Response.ok(message, MediaType.TEXT_XML).cacheControl(cacheControl).build();
    }
    if((fileName == null) || (fileName.length() == 0)) {
      fileName = resource.getFileName();
    }
    if(parent.hasNode(fileName)) {
      Object args[] = { fileName, parent.getPath() };
      Document fileExisted = 
        fckMessage.createMessage(FCKMessage.FILE_EXISTED, FCKMessage.ERROR, language, args);
      return Response.ok(fileExisted, MediaType.TEXT_XML).cacheControl(cacheControl).build();
    }                
    String location = resource.getStoreLocation();
    byte[] uploadData = IOUtil.getFileContentAsBytes(location);
    Node file = parent.addNode(fileName,FCKUtils.NT_FILE);
    Node jcrContent = file.addNode("jcr:content","nt:resource");
    MimeTypeResolver mimeTypeResolver = new MimeTypeResolver();
    String mimetype = mimeTypeResolver.getMimeType(resource.getFileName());
    jcrContent.setProperty("jcr:data",new ByteArrayInputStream(uploadData));
    jcrContent.setProperty("jcr:lastModified",new GregorianCalendar());    
    jcrContent.setProperty("jcr:mimeType",mimetype);
    parent.getSession().save();
    parent.getSession().refresh(true);
    uploadService.removeUpload(uploadId);
    WCMPublicationService wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);
    wcmPublicationService.updateLifecyleOnChangeContent(file, siteName, userId);
    return Response.ok(null, MediaType.TEXT_XML).cacheControl(cacheControl).build();
  }
  
  /**
   * Gets the progress.
   * 
   * @param uploadId the upload id
   * 
   * @return the progress
   * 
   * @throws Exception the exception
   */
  private Document getProgress(String uploadId) throws Exception {    
    UploadResource resource = uploadService.getUploadResource(uploadId);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.newDocument();    
    if(resource == null) {
      return doc;
    }
    Double percent = 0.0;
    if (resource.getStatus() == UploadResource.UPLOADING_STATUS) {
      percent = (resource.getUploadedSize() * 100) / resource.getEstimatedSize();      
    } else {
      percent = 100.0;
    } 
    Element rootElement = doc.createElement("UploadProgress");
    rootElement.setAttribute("uploadId", uploadId);
    rootElement.setAttribute("fileName", resource.getFileName());
    rootElement.setAttribute("percent", percent.intValue() + "");
    doc.appendChild(rootElement);
    return doc;
  }
}