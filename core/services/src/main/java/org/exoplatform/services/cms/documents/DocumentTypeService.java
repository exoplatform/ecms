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
package org.exoplatform.services.cms.documents;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.cms.documents.impl.DocumentType;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Get documents by mime types
 */
public interface DocumentTypeService {

  /**
   * Get all supported document type
   * @return List<String>
   */
  public List<String> getAllSupportedType();

  /**
   * Get all documents by kind of document type
   * @param documentType Kind of document(Images, Video,...)
   * @param workspace The name of workspace will be used to get documents
   * @param repository The name of repository will be used to get documents
   * @param sessionProvider
   * @return List<Node> all documents by kind of document type
   * @throws Exception
   */
  public List<Node> getAllDocumentsByDocumentType(String documentType,
                                                  String workspace,
                                                  SessionProvider sessionProvider) throws Exception;

  /**
   * Get all document by mimetype
   * @param workspace The name of workspace will be used to get documents
   * @param repository The name of repository will be used to get documents
   * @param sessionProvider
   * @param mimeType The mime type of node(For example: image/jpg)
   * @return List<Node> all documents by mime type
   * @throws Exception
   */
  public List<Node> getAllDocumentsByType(String workspace,
                                          SessionProvider sessionProvider,
                                          String mimeType) throws Exception;

  /**
   * Get all document by array of mimetype
   * @param workspace The name of workspace will be used to get documents
   * @param repository The name of repository will be used to get documents
   * @param sessionProvider
   * @param mimeTypes The array of mimetype(For example: ["image/jpg", "image/png"])
   * @return List<Node> all documents by mime type
   * @throws Exception
   */
  public List<Node> getAllDocumentsByType(String workspace,
                                          SessionProvider sessionProvider,
                                          String[] mimeTypes) throws Exception;

  /**
   * Get all document type by user
   * @param workspace The name of workspace will be used to get documents
   * @param sessionProvider
   * @param mimeTypes The array of mimetype(For example: ["image/jpg", "image/png"])
   * @param userName The name of current user
   * @return List<Node> all documents by mime type
   * @throws Exception
   */
  public List<Node> getAllDocumentsByUser(String workspace,
                                          SessionProvider sessionProvider,
                                          String[] mimeTypes,
                                          String userName) throws Exception;

  /**
   * Check the document is content type or not.
   *
   * @param documentType
   * @return
   */
  public boolean isContentsType(String documentType);

  /**
   * Get all contents type document
   * @param documentType Contents type
   * @param workspace The name of workspace will be used to get documents
   * @param sessionProvider
   * @param userName
   * @return List<Node> all contents type document
   * @throws Exception
   */
  public List<Node> getAllDocumentByContentsType(String documentType,
                                                 String workspace,
                                                 SessionProvider sessionProvider,
                                                 String userName) throws Exception;


  /**
   * Get mime types by document type
   * @param documentType
   * @return
   */
  public String[] getMimeTypes(String documentType);

  /**
   * Get documentType of extension string
   * @param mimeType
   * @return DocumentType
   */
  public DocumentType getDocumentType(String mimeType);
}
