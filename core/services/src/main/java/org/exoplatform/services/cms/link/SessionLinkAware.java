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
package org.exoplatform.services.cms.link;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAResource;

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.NamespaceAccessor;
import org.exoplatform.services.jcr.core.SessionLifecycleListener;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 4 avr. 2009
 */
public class SessionLinkAware implements ExtendedSession, NamespaceAccessor {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger(SessionLinkAware.class.getName());

  private ItemLinkAware itemLA;

  private final String originalWorkspace;

  private volatile ExtendedSession[] sessions;

  SessionLinkAware (ItemLinkAware itemLA) throws RepositoryException {
    this.itemLA = itemLA;
    this.originalWorkspace = itemLA.originalWorkspaceName;
  }

  private ExtendedSession[] getSessions() throws RepositoryException {
    if (sessions == null) {
      synchronized (this) {
        if (sessions == null) {
          Set<ExtendedSession> sSessions = new HashSet<ExtendedSession>(3, 1f);
          sSessions.add(getOriginalSession());
          sSessions.add((ExtendedSession)itemLA.getItemSession());
          sSessions.add(getTargetSession());
          sessions = sSessions.toArray(new ExtendedSession[sSessions.size()]);
        }
      }
    }
    return sessions;
  }

  private ExtendedSession getSession() throws RepositoryException {
    return (ExtendedSession)itemLA.getItemSession();
  }

  private ExtendedSession getOriginalSession() throws RepositoryException {
    return (ExtendedSession)WCMCoreUtils.getUserSessionProvider().
                    getSession(originalWorkspace, WCMCoreUtils.getRepository());
  }

  private ExtendedSession getTargetSession() throws RepositoryException {
    return getTargetSession(itemLA);
  }

  private ExtendedSession getTargetSession(String absPath, Item item) throws RepositoryException {
    return getTargetSession(ItemLinkAware.newInstance(originalWorkspace, absPath, item));
  }

  private ExtendedSession getTargetSession(ItemLinkAware itemLA) throws RepositoryException {
    if (itemLA instanceof NodeLinkAware) {
      return (ExtendedSession) ((NodeLinkAware) itemLA).getTargetNode().getRealNode().getSession();
    }
    return (ExtendedSession) itemLA.getItemSession();
  }

  private ExtendedSession getTargetSession(String absPath) throws RepositoryException {
    Item item = getItem(absPath);
    return getTargetSession(absPath, item);
  }

  /**
   * {@inheritDoc}
   */
  public void addLockToken(String lt) {
    try {
      getTargetSession().addLockToken(lt);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void checkPermission(String absPath, String actions) throws AccessControlException,
                                                       RepositoryException {
    getTargetSession(absPath).checkPermission(absPath, actions);
  }

  /**
   * {@inheritDoc}
   */
  public void exportDocumentView(String absPath,
                                 ContentHandler contentHandler,
                                 boolean skipBinary,
                                 boolean noRecurse) throws PathNotFoundException,
                                                   SAXException,
                                                   RepositoryException {
    getTargetSession(absPath).exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
  }

  /**
   * {@inheritDoc}
   */
  public void exportDocumentView(String absPath,
                                 OutputStream out,
                                 boolean skipBinary,
                                 boolean noRecurse) throws IOException,
                                                   PathNotFoundException,
                                                   RepositoryException {
    getTargetSession(absPath).exportDocumentView(absPath, out, skipBinary, noRecurse);
  }

  /**
   * {@inheritDoc}
   */
  public void exportSystemView(String absPath,
                               ContentHandler contentHandler,
                               boolean skipBinary,
                               boolean noRecurse) throws PathNotFoundException,
                                                 SAXException,
                                                 RepositoryException {
    getTargetSession(absPath).exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
  }

  /**
   * {@inheritDoc}
   */
  public void exportSystemView(String absPath,
                               OutputStream out,
                               boolean skipBinary,
                               boolean noRecurse) throws IOException,
                                                 PathNotFoundException,
                                                 RepositoryException {
    getTargetSession(absPath).exportSystemView(absPath, out, skipBinary, noRecurse);
  }

  /**
   * {@inheritDoc}
   */
  public Object getAttribute(String name) {
    try {
      return getTargetSession().getAttribute(name);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String[] getAttributeNames() {
    try {
      return getTargetSession().getAttributeNames();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException,
                                                                                       ConstraintViolationException,
                                                                                       VersionException,
                                                                                       LockException,
                                                                                       RepositoryException {
    return getTargetSession(parentAbsPath).getImportContentHandler(parentAbsPath, uuidBehavior);
  }

  /**
   * {@inheritDoc}
   */
  public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
    NodeFinder nodeFinder = LinkUtils.getNodeFinder();
    return nodeFinder.getItem(getOriginalSession(), absPath);
  }

  /**
   * {@inheritDoc}
   */
  public String[] getLockTokens() {
    try {
      return getTargetSession().getLockTokens();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
    return getTargetSession().getNamespacePrefix(uri);
  }

  /**
   * {@inheritDoc}
   */
  public String[] getNamespacePrefixes() throws RepositoryException {
    return getTargetSession().getNamespacePrefixes();
  }

  /**
   * {@inheritDoc}
   */
  public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
    return getTargetSession().getNamespaceURI(prefix);
  }

  /**
   * {@inheritDoc}
   */
  public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
    ExtendedSession[] sessions = getSessions();
    for (int i = 0, length = sessions.length; i < length; i++) {
      Session session = sessions[i];
      try {
        return session.getNodeByUUID(uuid);
      } catch (ItemNotFoundException e) {
        continue;
      }
    }
    throw new ItemNotFoundException("No node with uuid ='" + uuid + "' can be found");
  }

  /**
   * {@inheritDoc}
   */
  public Repository getRepository() {
    try {
      return getSession().getRepository();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public Node getRootNode() throws RepositoryException {
    return getOriginalSession().getRootNode();
  }

  /**
   * {@inheritDoc}
   */
  public String getUserID() {
    try {
      return getSession().getUserID();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException,
                                       RepositoryException {
    return getOriginalSession().getValueFactory();
  }

  /**
   * {@inheritDoc}
   */
  public Workspace getWorkspace() {
    try {
      return getOriginalSession().getWorkspace();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasPendingChanges() throws RepositoryException {
    ExtendedSession[] sessions = getSessions();
    for (int i = 0, length = sessions.length; i < length; i++) {
      Session session = sessions[i];
      if (session.hasPendingChanges()) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
    return getOriginalSession().impersonate(credentials);
  }

  /**
   * {@inheritDoc}
   */
  public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException,
                                                                               PathNotFoundException,
                                                                               ItemExistsException,
                                                                               ConstraintViolationException,
                                                                               VersionException,
                                                                               InvalidSerializedDataException,
                                                                               LockException,
                                                                               RepositoryException {
    getTargetSession(parentAbsPath).importXML(parentAbsPath, in, uuidBehavior);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isLive() {
    ExtendedSession[] sessions;
    try {
      sessions = getSessions();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
      return false;
    }
    for (int i = 0, length = sessions.length; i < length; i++) {
      Session session = sessions[i];
      if (session.isLive()) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean itemExists(String absPath) throws RepositoryException {
    NodeFinder nodeFinder = LinkUtils.getNodeFinder();
    return nodeFinder.itemExists(getOriginalSession(), absPath);
  }

  /**
   * {@inheritDoc}
   */
  public void logout() {
    ExtendedSession[] sessions;
    try {
      sessions = getSessions();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
      return;
    }
    for (int i = 0, length = sessions.length; i < length; i++) {
      Session session = sessions[i];
      session.logout();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException,
                                                         PathNotFoundException,
                                                         VersionException,
                                                         ConstraintViolationException,
                                                         LockException,
                                                         RepositoryException {
    Item srcItem = getItem(srcAbsPath);
    Session srcSession = getTargetSession(srcAbsPath, srcItem);
    Session destParentSession = getTargetSession(LinkUtils.getParentPath(destAbsPath));
    if (srcSession.getWorkspace().equals(destParentSession.getWorkspace())) {
      srcSession.move(srcAbsPath, srcAbsPath);
    } else {
      destParentSession.getWorkspace().clone(srcSession.getWorkspace().getName(),
                                             srcAbsPath,
                                             destAbsPath,
                                             false);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void refresh(boolean keepChanges) throws RepositoryException {
    ExtendedSession[] sessions = getSessions();
    for (int i = 0, length = sessions.length; i < length; i++) {
      Session session = sessions[i];
      session.refresh(keepChanges);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeLockToken(String lt) {
    try {
      getTargetSession().removeLockToken(lt);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void save() throws AccessDeniedException,
                    ItemExistsException,
                    ConstraintViolationException,
                    InvalidItemStateException,
                    VersionException,
                    LockException,
                    NoSuchNodeTypeException,
                    RepositoryException {
    ExtendedSession[] sessions = getSessions();
    for (int i = 0, length = sessions.length; i < length; i++) {
      Session session = sessions[i];
      session.save();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void setNamespacePrefix(String prefix, String uri) throws NamespaceException,
                                                          RepositoryException {
    getTargetSession().setNamespacePrefix(prefix, uri);
  }

  /**
   * {@inheritDoc}
   */
  public String getId() {
    try {
      return getTargetSession().getId();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public LocationFactory getLocationFactory() {
    try {
      return getTargetSession().getLocationFactory();
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public void importXML(String parentAbsPath,
                        InputStream in,
                        int uuidBehavior,
                        Map<String, Object> context) throws IOException,
                                                    PathNotFoundException,
                                                    ItemExistsException,
                                                    ConstraintViolationException,
                                                    InvalidSerializedDataException,
                                                    RepositoryException {
    getTargetSession(parentAbsPath).importXML(parentAbsPath, in, uuidBehavior, context);
  }

  /**
   * {@inheritDoc}
   */
  public void registerLifecycleListener(SessionLifecycleListener listener) {
    try {
      getTargetSession().registerLifecycleListener(listener);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public String[] getAllNamespacePrefixes() throws RepositoryException {
    return ((NamespaceAccessor) getTargetSession()).getAllNamespacePrefixes();
  }

  /**
   * {@inheritDoc}
   */
  public String getNamespacePrefixByURI(String uri) throws NamespaceException, RepositoryException {
    return ((NamespaceAccessor) getTargetSession()).getNamespacePrefixByURI(uri);
  }

  /**
   * {@inheritDoc}
   */
  public String getNamespaceURIByPrefix(String prefix) throws NamespaceException,
                                                      RepositoryException {
    return ((NamespaceAccessor) getTargetSession()).getNamespaceURIByPrefix(prefix);
  }

  /**
   * {@inheritDoc}}
   */
  public Node getNodeByIdentifier(String identifier) throws ItemNotFoundException, RepositoryException {
    return getTargetSession().getNodeByIdentifier(identifier);
  }

  @Override
  public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse,
      boolean exportChildVersionHisotry) throws IOException, PathNotFoundException, RepositoryException {
    getTargetSession().exportSystemView(absPath, out, skipBinary, noRecurse, exportChildVersionHisotry);
  }

  @Override
  public XAResource getXAResource() {
    try {
      return getTargetSession().getXAResource();
    } catch (RepositoryException e) {
      return null;
    }
  }

  @Override
  public boolean hasExpired() {
    try {
      return getTargetSession().hasExpired();
    } catch (RepositoryException e) {
      return true;
    }
  }

  @Override
  public void setTimeout(long timeout) {
    try {
      getTargetSession().setTimeout(timeout);
    } catch (RepositoryException e) {
      LOG.warn(e.getMessage());
    }
  }

  @Override
  public long getTimeout() {
    try {
      return getTargetSession().getTimeout();
    } catch (RepositoryException e) {
      return 0;
    }
  }

  @Override
  public void move(String srcAbsPath,
                   String destAbsPath,
                   boolean triggerEventsForDescendentsOnRename) throws ItemExistsException,
                                                               PathNotFoundException,
                                                               VersionException,
                                                               ConstraintViolationException,
                                                               LockException,
                                                               RepositoryException {
    Item srcItem = getItem(srcAbsPath);
    Session srcSession = getTargetSession(srcAbsPath, srcItem);
    Session destParentSession = getTargetSession(LinkUtils.getParentPath(destAbsPath));
    if (srcSession.getWorkspace().equals(destParentSession.getWorkspace())) {
      ((SessionImpl)srcSession).move(srcAbsPath, srcAbsPath, triggerEventsForDescendentsOnRename);
    } else {
      destParentSession.getWorkspace().clone(srcSession.getWorkspace().getName(),
                                             srcAbsPath,
                                             destAbsPath,
                                             false);
    }
  }
}
