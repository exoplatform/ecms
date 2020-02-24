/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
package org.exoplatform.services.cms.documents.exception;


/**
 * The Class EditorProviderNotFoundException.
 */
public class EditorProviderNotFoundException extends Exception {
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -8981933520830552416L;

  /**
   * Instantiates a new editor provider not found exception.
   *
   */
  public EditorProviderNotFoundException() {
    super();
  }
  
  /**
   * Instantiates a new editor provider not found exception.
   *
   * @param message the message
   */
  public EditorProviderNotFoundException(String message) {
    super(message);
  }

  /**
   * Instantiates a new editor provider not found exception.
   *
   * @param cause the cause
   */
  public EditorProviderNotFoundException(Throwable cause) {
    super(cause);
  }

  /**
   * Instantiates a new editor provider not found exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public EditorProviderNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

}
