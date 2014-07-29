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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.ecm.connector.fckeditor.FCKMessage;
import org.exoplatform.ecm.connector.fckeditor.FCKUtils;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.upload.UploadService.UploadLimit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com
 * Sep 4, 2009
 */
public class FileUploadHandler {

  /** Logger */  
  private static final Log LOG = ExoLogger.getLogger(FileUploadHandler.class.getName());

  /** The Constant UPLOAD_ACTION. */
  public final static String UPLOAD_ACTION = "upload";

  /** The Constant PROGRESS_ACTION. */
  public final static String PROGRESS_ACTION = "progress";

  /** The Constant ABORT_ACTION. */
  public final static String ABORT_ACTION = "abort";

  /** The Constant DELETE_ACTION. */
  public final static String DELETE_ACTION = "delete";

  /** The Constant SAVE_ACTION. */
  public final static String SAVE_ACTION = "save";
  
  /** The Constant CHECK_EXIST. */
  public final static String CHECK_EXIST= "exist";
  
  /** The Constant REPLACE. */
  public final static String REPLACE= "replace";

  /** The Constant KEEP_BOTH. */
  public final static String KEEP_BOTH= "keepBoth";

  /** The Constant LAST_MODIFIED_PROPERTY. */
  private static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  private static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
  
  public final static String POST_CREATE_CONTENT_EVENT = "CmsService.event.postCreate";
  
  /** The upload service. */
  private UploadService uploadService;
  
  /** The listener service. */
  ListenerService listenerService;
  
  private ActivityCommonService   activityService;

  /** The fck message. */
  private FCKMessage fckMessage;
  
  /** The uploadIds - time Map */
  private Map<String, Long> uploadIdTimeMap;
  
  /** The maximal life time for an upload */
  private long UPLOAD_LIFE_TIME;

  /**
   * Instantiates a new file upload handler.
   */
  public FileUploadHandler() {
    uploadService = WCMCoreUtils.getService(UploadService.class);
    listenerService = WCMCoreUtils.getService(ListenerService.class);
    activityService = WCMCoreUtils.getService(ActivityCommonService.class);
    fckMessage = new FCKMessage();
    uploadIdTimeMap = new Hashtable<String, Long>();
    UPLOAD_LIFE_TIME = System.getProperty("MULTI_UPLOAD_LIFE_TIME") == null ? 600 ://10 minutes
                                        Long.parseLong(System.getProperty("MULTI_UPLOAD_LIFE_TIME"));
  }

  /**
   * Upload
   * @param servletRequest The request to upload file
   * @param uploadId Upload Id
   * @param limit Limit size of upload file
   * @return
   * @throws Exception
   */
  public Response upload(HttpServletRequest servletRequest, String uploadId, Integer limit) throws Exception{
    uploadService.addUploadLimit(uploadId, limit);
    uploadService.createUploadResource(servletRequest);
    uploadIdTimeMap.put(uploadId, System.currentTimeMillis());
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    
    //create ret
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.newDocument();
    Element rootElement = doc.createElement("html");
    Element head = doc.createElement("head");
    Element body = doc.createElement("body");
    rootElement.appendChild(head);
    rootElement.appendChild(body);
    doc.appendChild(rootElement);
    
    return Response.ok(new DOMSource(doc), MediaType.TEXT_XML)
                   .cacheControl(cacheControl)
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }
  
  /**
   * Check status of uploaded file.
   * If any problem while uploading, error message is returned.
   * Returning null means no problem happen.
   * 
   * @param uploadId upload ID
   * @param language language for getting message
   * @return Response message is returned if any problem while uploading.
   * @throws Exception
   */
  public Response checkStatus(String uploadId, String language) throws Exception {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    
    if ((StringUtils.isEmpty(uploadId)) || (uploadService.getUploadResource(uploadId) == null)) return null;
    
    // If file size exceed limit, return message
    if (UploadResource.FAILED_STATUS == uploadService.getUploadResource(uploadId).getStatus()) {
      
      // Remove upload Id
      uploadService.removeUploadResource(uploadId);
      uploadIdTimeMap.remove(uploadId);
      // Get message warning upload exceed limit
      String uploadLimit = String.valueOf(uploadService.getUploadLimits().get(uploadId).getLimit());
      Document fileExceedLimit =
          fckMessage.createMessage(FCKMessage.FILE_EXCEED_LIMIT,
                                   FCKMessage.ERROR,
                                   language,
                                   new String[]{uploadLimit});
      
      return Response.ok(new DOMSource(fileExceedLimit), MediaType.TEXT_XML)
                      .cacheControl(cacheControl)
                      .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                      .build();
    }
    
    return null;
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
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    
    if (FileUploadHandler.PROGRESS_ACTION.equals(action)) {
      Document currentProgress = getProgress(uploadId);
      return Response.ok(new DOMSource(currentProgress), MediaType.TEXT_XML)
                     .cacheControl(cacheControl)
                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .build();
    } else if (FileUploadHandler.ABORT_ACTION.equals(action)) {
      uploadService.removeUploadResource(uploadId);
      uploadIdTimeMap.remove(uploadId);
      return Response.ok(null, MediaType.TEXT_XML)
                     .cacheControl(cacheControl)
                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .build();
    } else if (FileUploadHandler.DELETE_ACTION.equals(action)) {
      uploadService.removeUploadResource(uploadId);
      uploadIdTimeMap.remove(uploadId);
      return Response.ok(null, MediaType.TEXT_XML)
                     .cacheControl(cacheControl)
                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .build();
    }
    return Response.status(HTTPStatus.BAD_REQUEST)
                   .cacheControl(cacheControl)
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }

  /**
   * checks if file already existed in parent folder
   *
   * @param parent the parent
   * @param fileName the file name
   * @return the response
   *
   * @throws Exception the exception
   */
  public Response checkExistence(Node parent, String fileName) throws Exception {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    
    //create ret
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document fileExistence = builder.newDocument();
    fileName = Text.escapeIllegalJcrChars(fileName);
    fileName = cleanNameUtil(fileName);
    Element rootElement = fileExistence.createElement(
                              parent.hasNode(fileName) ? "Existed" : "NotExisted");
    fileExistence.appendChild(rootElement);
    //return ret;
    return Response.ok(new DOMSource(fileExistence), MediaType.TEXT_XML)
                   .cacheControl(cacheControl)
                   .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                   .build();
  }
  
  /**
   * Clean name using Transliterator
   * @param fileName original file name
   * 
   * @return Response
   */
  public Response cleanName(String fileName) throws Exception {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document cleanedFilename = builder.newDocument(); 
    fileName = cleanNameUtil(fileName);
    Element rootElement = cleanedFilename.createElement("name");
    cleanedFilename.appendChild(rootElement);
    rootElement.setTextContent(fileName);
    return Response.ok(new DOMSource(cleanedFilename), MediaType.TEXT_XML)
            .cacheControl(cacheControl)
            .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
            .build();
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
  public Response saveAsNTFile(Node parent,
                               String uploadId,
                               String fileName,
                               String language,
                               String siteName,
                               String userId) throws Exception {
    return saveAsNTFile(parent, uploadId, fileName, language, siteName, userId, KEEP_BOTH); 
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
  public Response saveAsNTFile(Node parent,
                               String uploadId,
                               String fileName,
                               String language,
                               String siteName,
                               String userId,
                               String existenceAction) throws Exception {
    try {
      CacheControl cacheControl = new CacheControl();
      cacheControl.setNoCache(true);
      UploadResource resource = uploadService.getUploadResource(uploadId);
      DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
      if (parent == null) {
        Document fileNotUploaded = fckMessage.createMessage(FCKMessage.FILE_NOT_UPLOADED,
                                                            FCKMessage.ERROR,
                                                            language,
                                                            null);
        return Response.ok(new DOMSource(fileNotUploaded), MediaType.TEXT_XML)
                       .cacheControl(cacheControl)
                       .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                       .build();
      }
      if (!FCKUtils.hasAddNodePermission(parent)) {
        Object[] args = { parent.getPath() };
        Document message = fckMessage.createMessage(FCKMessage.FILE_UPLOAD_RESTRICTION,
                                                    FCKMessage.ERROR,
                                                    language,
                                                    args);
        return Response.ok(new DOMSource(message), MediaType.TEXT_XML)
                       .cacheControl(cacheControl)
                       .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                       .build();
      }
      if ((fileName == null) || (fileName.length() == 0)) {
        fileName = resource.getFileName();
      }
      //add lock token
      if(parent.isLocked()) {
        parent.getSession().addLockToken(LockUtil.getLockToken(parent));
      }
      if (parent.hasNode(fileName)) {
  //      Object args[] = { fileName, parent.getPath() };
  //      Document fileExisted = fckMessage.createMessage(FCKMessage.FILE_EXISTED,
  //                                                      FCKMessage.ERROR,
  //                                                      language,
  //                                                      args);
  //      return Response.ok(new DOMSource(fileExisted), MediaType.TEXT_XML)
  //                     .cacheControl(cacheControl)
  //                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
  //                     .build();
        if (REPLACE.equals(existenceAction)) {
          //Broadcast the event when user move node to Trash
          ListenerService listenerService =  WCMCoreUtils.getService(ListenerService.class);
          listenerService.broadcast(ActivityCommonService.FILE_REMOVE_ACTIVITY, parent, parent.getNode(fileName));
          parent.getNode(fileName).remove();
          parent.save();        
        }
      }
      String location = resource.getStoreLocation();
      //save node with name=fileName
      Node file = null;
      boolean fileCreated = false;
      String exoTitle = fileName;
      
      fileName = cleanNameUtil(fileName);
      
      String nodeName = fileName;
      int count = 0;
      do {
        try {
          file = parent.addNode(nodeName,FCKUtils.NT_FILE);
          fileCreated = true;
        } catch (ItemExistsException e) {//sameNameSibling is not allowed
          nodeName = increaseName(fileName, ++count);
        }      
      } while (!fileCreated);
      //--------------------------------------------------------
      if(!file.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) {
        file.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
      }
      
      if(!file.isNodeType(NodetypeConstant.MIX_COMMENTABLE))
        file.addMixin(NodetypeConstant.MIX_COMMENTABLE);
      
      if(!file.isNodeType(NodetypeConstant.MIX_VOTABLE))
        file.addMixin(NodetypeConstant.MIX_VOTABLE);
      
      if(!file.isNodeType(NodetypeConstant.MIX_I18N))
        file.addMixin(NodetypeConstant.MIX_I18N);
      
      if(!file.hasProperty(NodetypeConstant.EXO_TITLE)) {
        file.setProperty(NodetypeConstant.EXO_TITLE, exoTitle);
      }
      Node jcrContent = file.addNode("jcr:content","nt:resource");
      //MimeTypeResolver mimeTypeResolver = new MimeTypeResolver();
      DMSMimeTypeResolver mimeTypeResolver = DMSMimeTypeResolver.getInstance();
      String mimetype = mimeTypeResolver.getMimeType(resource.getFileName());
      jcrContent.setProperty("jcr:data",new BufferedInputStream(new FileInputStream(new File(location))));
      jcrContent.setProperty("jcr:lastModified",new GregorianCalendar());
      jcrContent.setProperty("jcr:mimeType",mimetype);
      
      parent.getSession().refresh(true); // Make refreshing data
      uploadService.removeUploadResource(uploadId);
      uploadIdTimeMap.remove(uploadId);
      WCMPublicationService wcmPublicationService = WCMCoreUtils.getService(WCMPublicationService.class);    
      wcmPublicationService.updateLifecyleOnChangeContent(file, siteName, userId);
     
      if (activityService.isBroadcastNTFileEvents(file)) {
        listenerService.broadcast(ActivityCommonService.FILE_CREATED_ACTIVITY, null, file);
      }
      file.getSession().save();
      return Response.ok(createDOMResponse("Result", mimetype), MediaType.TEXT_XML)
          .cacheControl(cacheControl)
          .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
          .build();
    } catch (Exception exc) {
      LOG.error(exc.getMessage(), exc);
      return Response.serverError().entity(exc.getMessage()).build();
    }
  }
  
  public boolean isDocumentNodeType(Node node) throws Exception {
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    return templateService.isManagedNodeType(node.getPrimaryNodeType().getName());
  }
  
  /**
   * increase the file name (not extension).
   * @param origin the original name
   * @param count the number add to file name
   * @return the new increased file name 
   */
  private String increaseName(String origin, int count) {
    int index = origin.indexOf('.');
    if (index == -1) return origin + count;
    return origin.substring(0, index) + count + origin.substring(index);
  }
  
  /**
   * get number of files uploading 
   * @return number of files uploading
   */
  public long getUploadingFileCount() {
    removeDeadUploads();
    return uploadIdTimeMap.size();
  }

  /**
   * removes dead uploads
   */
  private void removeDeadUploads() {
    Set<String> removedIds = new HashSet<String>();
    for (String id : uploadIdTimeMap.keySet()) {
      if ((System.currentTimeMillis() - uploadIdTimeMap.get(id)) > UPLOAD_LIFE_TIME * 1000) {
        removedIds.add(id);
      }
    }
    for (String id : removedIds) {
      uploadIdTimeMap.remove(id);
    }
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
    Double percent = 0.0;
    if(resource != null) {
      if (resource.getStatus() == UploadResource.UPLOADING_STATUS) {
        percent = (resource.getUploadedSize() * 100) / resource.getEstimatedSize();
      } else {
        percent = 100.0;
      }
    }
    Element rootElement = doc.createElement("UploadProgress");
    rootElement.setAttribute("uploadId", uploadId);
    rootElement.setAttribute("fileName", resource == null ? "" : resource.getFileName());
    rootElement.setAttribute("percent", percent.intValue() + "");
    rootElement.setAttribute("uploadedSize", resource == null ? "0" : resource.getUploadedSize() + "");
    rootElement.setAttribute("totalSize", resource == null ? "0" : resource.getEstimatedSize() + "");
    rootElement.setAttribute("fileType", resource == null ? "null" : resource.getMimeType() + "");
    UploadLimit limit = uploadService.getUploadLimits().get(uploadId);
    if (limit != null) {
      rootElement.setAttribute("limit", limit.getLimit() + "");
      rootElement.setAttribute("unit", limit.getUnit() + "");
    }
    doc.appendChild(rootElement);
    return doc;
  }
  
  /**
   * returns a DOMSource object containing given message
   * @param message the message
   * @return DOMSource object
   * @throws Exception
   */
  private DOMSource createDOMResponse(String name, String mimeType) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.newDocument();
    Element rootElement = doc.createElement(name);
    rootElement.setAttribute("mimetype", mimeType);
    doc.appendChild(rootElement);
    return new DOMSource(doc);
  }

  /** Return name after cleaning
   * @param fileName file name
   * @return cleaned name
   */
  private String cleanNameUtil(String fileName) {
    if (fileName.indexOf('.') > 0) {
      String ext = fileName.substring(fileName.lastIndexOf('.'));
      fileName = Utils.cleanString(fileName.substring(0, fileName.lastIndexOf('.'))).concat(ext);
    } else {
      fileName = Utils.cleanString(fileName);
    }
    return fileName;

  }
  
}
