/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.utils;

import java.security.AccessControlException;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.exception.MessageException;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com May 8, 2008 3:18:06 PM
 */
public class JCRExceptionManager {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger("ecm.webui.utils.JCRExceptionManager");

  /**
   * Process.
   *
   * @param uiApp the ui app
   * @param e the e
   * @param messageKey the message key
   * @throws Exception the exception
   */
  public static void process(UIApplication uiApp,Exception e,String messageKey) throws Exception{
    if (LOG.isErrorEnabled()) {
      LOG.error("The following error occurs : " + e);
    }
    if(e instanceof LoginException) {
      if(messageKey == null) messageKey = "LoginException.msg";
    } else if(e instanceof AccessDeniedException) {
      if(messageKey == null) messageKey = "AccessDeniedException.msg";
    } else if(e instanceof NoSuchWorkspaceException) {
      if(messageKey == null) messageKey = "NoSuchWorkspaceException.msg";
    } else if(e instanceof ItemNotFoundException) {
      if(messageKey == null) messageKey = "ItemNotFoundException.msg";
    } else if(e instanceof ItemExistsException) {
      if(messageKey == null) messageKey = "ItemExistsException.msg";
    } else if(e instanceof ConstraintViolationException) {
      if(messageKey == null) messageKey = "ConstraintViolationException.msg";
    } else if(e instanceof InvalidItemStateException) {
      if(messageKey == null) messageKey = "InvalidItemStateException.msg";
    } else if(e instanceof ReferentialIntegrityException) {
      if(messageKey == null) messageKey = "ReferentialIntegrityException.msg";
    } else if(e instanceof LockException) {
      if(messageKey == null) messageKey = "LockException.msg";
    } else if(e instanceof NoSuchNodeTypeException) {
      if(messageKey == null) messageKey = "NoSuchNodeTypeException.msg";
    } else if(e instanceof VersionException) {
      if(messageKey == null) messageKey = "VersionException.msg";
    } else if(e instanceof PathNotFoundException) {
      if(messageKey == null) messageKey = "PathNotFoundException.msg";
    } else if(e instanceof ValueFormatException) {
      if(messageKey == null) messageKey = "ValueFormatException.msg";
    } else if(e instanceof InvalidSerializedDataException) {
      if(messageKey == null) messageKey = "InvalidSerializedDataException.msg";
    } else if(e instanceof RepositoryException) {
      if(messageKey == null) messageKey = "RepositoryException.msg";
    } else if(e instanceof AccessControlException) {
      if(messageKey == null) messageKey = "AccessControlException.msg";
    } else if(e instanceof UnsupportedRepositoryOperationException) {
      if(messageKey == null) messageKey = "UnsupportedRepositoryOperationException.msg";
    } else if(e instanceof MessageException) {
      if(messageKey == null) messageKey = ((MessageException)e).getDetailMessage().getMessageKey();
    } else {
      throw e;
    }
    uiApp.addMessage(new ApplicationMessage(Utils.getResourceBundle(Utils.LOCALE_WEBUI_DMS,
                                                                    messageKey,
                                                                    JCRExceptionManager.class.getClassLoader()),
                                            null,
                                            ApplicationMessage.WARNING));
  }

  /**
   * Process.
   *
   * @param uiApp the ui app
   * @param e the e
   * @throws Exception the exception
   */
  public static void process(UIApplication uiApp, Exception e) throws Exception {
    process(uiApp,e,null) ;
  }
}
