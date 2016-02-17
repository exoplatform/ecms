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
package org.exoplatform.services.ecm.publication;

/**
 * Created by The eXo Platform SAS
 * Author : Romain Dénarié
 *          romain.denarie@exoplatform.com
 * 7 mai 08
 */
@SuppressWarnings("serial")
public class NotInPublicationLifecycleException extends Exception {

  /**
   * Constructs an Exception without a message.
   */
  public NotInPublicationLifecycleException() {
    super();
  }

  /**
   * Constructs an Exception with a detailed message.
   *
   * @param message The message associated with the exception.
   */
  public NotInPublicationLifecycleException(String message) {
    super(message);
  }

  public NotInPublicationLifecycleException(String message, Throwable cause) {
    super(message, cause);
  }
}
