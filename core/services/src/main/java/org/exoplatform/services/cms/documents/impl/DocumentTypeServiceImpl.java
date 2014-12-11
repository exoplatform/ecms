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
package org.exoplatform.services.cms.documents.impl;

import java.util.*;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cms.documents.DocumentTypeService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com Oct 6, 2009 3:39:28 AM
 */
public class DocumentTypeServiceImpl implements DocumentTypeService, Startable {
  private static final Log    LOG               = ExoLogger.getLogger(DocumentTypeServiceImpl.class.getName());

  private final static String OWNER             = "exo:owner";

  private final static String QUERY             = " SELECT * FROM nt:resource WHERE";

  private final static String CONTENT_QUERY     = " SELECT * FROM nt:base WHERE ";

  private final static String EXO_DATE_MODIFIED = "exo:dateModified";

  private final static String JCR_MINE_TYPE     = "jcr:mimeType";

  private final static String JCR_PRIMARY_TYPE  = "jcr:primaryType";

  private final static String SQL               = "sql";

  private final static String AND               = " AND ";

  private final static String CONTAINS          = " contains";

  private final static String SINGLE_QUOTE      = "'";

  private final static String OR                = " OR ";

  private final static String BEGIN_BRANCH      = " ( ";

  private final static String END_BRANCH        = " ) ";

  private RepositoryService   repositoryService_;

  private TemplateService     templateService_;

  private InitParams          params_;

  private static final String OPEN_DESKTOP_PROVIDER_REGEX="^exo.remote-edit\\.([a-z]+)$";
  private static final String OPEN_PROVIDER_RESOURCEBUNDLE_SUFFIX = ".label";
  private static final String OPEN_PROVIDER_STYLE_SUFFIX = ".ico";
  private final String OPEN_DOCUMENT_ON_DESKTOP_ICO = "uiIcon16x16FileDefault";
  private final String OPEN_DOCUMENT_IN_DESKTOP_RESOURCE_KEY = "OpenInOfficeConnector.label.exo.remote-edit.desktop";

  private void init() {
    //load desktop application from system property to init-params
    Properties properties = System.getProperties();
    Enumeration keys = properties.keys();
    ObjectParameter _objectParameter=null;
    while (keys.hasMoreElements()){
      String key = (String)keys.nextElement();
      if(key.matches(OPEN_DESKTOP_PROVIDER_REGEX)) {
        List _mimetypes = Arrays.asList(properties.getProperty(key)!=null?properties.getProperty(key).split(","):null);
        String _resourceBundle = properties.getProperty(key + OPEN_PROVIDER_RESOURCEBUNDLE_SUFFIX);
        String _ico = properties.getProperty(key + OPEN_PROVIDER_STYLE_SUFFIX);

        _objectParameter = new ObjectParameter();
        _objectParameter.setName(key);
        _objectParameter.setObject(new DocumentType(_mimetypes, _resourceBundle, _ico));
        params_.addParam(_objectParameter);
      }
    }
  }

  public DocumentTypeServiceImpl(RepositoryService repoService,
                                 InitParams initParams,
                                 TemplateService templateService) {
    repositoryService_ = repoService;
    templateService_ = templateService;
    params_ = initParams;
  }

  public List<String> getAllSupportedType() {
    List<String> supportedType = new ArrayList<String>();
    Iterator iter = params_.getObjectParamIterator();
    ObjectParameter objectParam = null;
    while (iter.hasNext()) {
      objectParam = (ObjectParameter) iter.next();
      if (Boolean.parseBoolean(((DocumentType)objectParam.getObject()).getDisplayInFilter())) {
        supportedType.add(objectParam.getName());
      }
    }
    Collections.sort(supportedType);
    return supportedType;
  }
  
  public List<Node> getAllDocumentsByDocumentType(String documentType,
                                                  String workspace,
                                                  SessionProvider sessionProvider) throws Exception {
    return getAllDocumentsByType(workspace, sessionProvider, getMimeTypes(documentType));
  }

  public List<Node> getAllDocumentsByType(String workspace,
                                          SessionProvider sessionProvider,
                                          String mimeType) throws Exception {
    return getAllDocumentsByType(workspace, sessionProvider, new String[] { mimeType });
  }  

  public List<Node> getAllDocumentsByUser(String workspace,
                                          SessionProvider sessionProvider,
                                          String[] mimeTypes,
                                          String userName) throws Exception {
    Session session = sessionProvider.getSession(workspace,
                                                 repositoryService_.getCurrentRepository());
    List<Node> resultList = new ArrayList<Node>();
    QueryResult results = null;
    results = executeQuery(session, buildQueryByMimeTypes(mimeTypes, userName), SQL);
    NodeIterator iterator = results.getNodes();
    Node documentNode = null;
    while (iterator.hasNext()) {
      documentNode = iterator.nextNode();
      resultList.add(documentNode.getParent());
    }
    return resultList;
  }

  public List<Node> getAllDocumentsByType(String workspace,
                                          SessionProvider sessionProvider,
                                          String[] mimeTypes) throws Exception {
    return getAllDocumentsByUser(workspace, sessionProvider, mimeTypes, null);
  }

  public String[] getMimeTypes(String documentType) {
    Iterator iter = params_.getObjectParamIterator();
    ObjectParameter objectParam = null;
    List<String> mimeTypes = new ArrayList<String>();
    while (iter.hasNext()) {
      objectParam = (ObjectParameter) iter.next();
      if (objectParam.getName().equals(documentType)) {
        mimeTypes = ((DocumentType) objectParam.getObject()).getMimeTypes();
        break;
      }
    }
    return mimeTypes.toArray(new String[mimeTypes.size()]);
  }

  public boolean isContentsType(String documentType) {
    Iterator iter = params_.getObjectParamIterator();
    ObjectParameter objectParam = null;
    while (iter.hasNext()) {
      objectParam = (ObjectParameter) iter.next();
      if (objectParam.getName().equals(documentType)) {
        return Boolean.parseBoolean(((DocumentType) objectParam.getObject()).getContentsType());
      }
    }
    return false;
  }

  public List<Node> getAllDocumentByContentsType(String documentType,
                                                 String workspace,
                                                 SessionProvider sessionProvider,
                                                 String userName) throws Exception {
    if (isContentsType(documentType)) {
      Session session = sessionProvider.getSession(workspace,
                                                   repositoryService_.getCurrentRepository());
      List<Node> resultList = new ArrayList<Node>();
      QueryResult results = null;
      try {
        // Execute sql query and return a results
        results = executeQuery(session, buildQueryByContentsType(userName), SQL);
      } catch (PathNotFoundException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("An unexpected exception appear", e);
        }
      } catch (RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("An unexpected exception appear", e);
        }
      }
      NodeIterator iterator = results.getNodes();
      while (iterator.hasNext()) {
        resultList.add(iterator.nextNode());
      }
      return resultList;
    }
    return null;
  }

  private QueryResult executeQuery(Session session, String statement, String language) {
    try {
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(statement, language);
      return query.execute();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("SQL query fail", e);
      }
      return null;
    }
  }

  private String buildQueryByMimeTypes(String[] mimeTypes, String userName) {
    StringBuilder query = new StringBuilder();
    if (userName == null) {
      for (String mimeType : mimeTypes) {
        if (query.length() > 0)
          query.append(OR);
        query.append(CONTAINS)
             .append("(")
             .append(JCR_MINE_TYPE)
             .append(",")
             .append(SINGLE_QUOTE)
             .append(mimeType.trim())
             .append(SINGLE_QUOTE)
             .append(")");
      }
    } else {
      query.append(BEGIN_BRANCH);
      for (String mimeType : mimeTypes) {
        if (query.length() > BEGIN_BRANCH.length())
          query.append(OR);
        query.append(CONTAINS)
             .append("(")
             .append(JCR_MINE_TYPE)
             .append(",")
             .append(SINGLE_QUOTE)
             .append(mimeType.trim())
             .append(SINGLE_QUOTE)
             .append(")");
      }
      query.append(END_BRANCH);
      query.append(AND);
      query.append("(")
           .append(OWNER)
           .append("=")
           .append(SINGLE_QUOTE)
           .append(userName)
           .append(SINGLE_QUOTE)
           .append(")");
    }
    query.append(" ORDER  BY " + EXO_DATE_MODIFIED);
    return QUERY + query.toString();
  }

  private String buildQueryByContentsType(String userName) throws PathNotFoundException, RepositoryException {
    List<String> contentsType = templateService_.getAllDocumentNodeTypes();
    StringBuilder constraint = new StringBuilder();
    if (userName == null) {
      for (String contentType : contentsType) {
        if (constraint.length() > 0)
          constraint.append(OR);
        constraint.append("(")
                  .append(JCR_PRIMARY_TYPE)
                  .append("=")
                  .append(SINGLE_QUOTE)
                  .append(contentType)
                  .append(SINGLE_QUOTE)
                  .append(")");
      }
    } else {
      constraint.append(BEGIN_BRANCH);
      for (String contentType : contentsType) {
        if (constraint.length() > BEGIN_BRANCH.length())
          constraint.append(OR);
        constraint.append("(")
                  .append(JCR_PRIMARY_TYPE)
                  .append("=")
                  .append(SINGLE_QUOTE)
                  .append(contentType)
                  .append(SINGLE_QUOTE)
                  .append(")");
      }
      constraint.append(END_BRANCH);
      constraint.append(AND);
      constraint.append("(")
                .append(OWNER)
                .append("=")
                .append(SINGLE_QUOTE)
                .append(userName)
                .append(SINGLE_QUOTE)
                .append(")");
    }
    constraint.append(" ORDER  BY " + EXO_DATE_MODIFIED);
    return CONTENT_QUERY + constraint.toString();
  }

  @Override
  public DocumentType getDocumentType(String mimeType) {
    for(DocumentType documentType: params_.getObjectParamValues(DocumentType.class)){
      if(documentType.getMimeTypes().contains(mimeType)){
        return documentType;
      }
    }
    return new DocumentType(Arrays.asList(new String[] {mimeType}), OPEN_DOCUMENT_IN_DESKTOP_RESOURCE_KEY, OPEN_DOCUMENT_ON_DESKTOP_ICO);
  }

  @Override
  public void start() {
    init();
  }

  @Override
  public void stop() {

  }
}
