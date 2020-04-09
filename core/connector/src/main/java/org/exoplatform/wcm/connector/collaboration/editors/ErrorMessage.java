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
package org.exoplatform.wcm.connector.collaboration.editors;

/**
 * The Class ErrorMessage is used in REST services as response entity in error cases.
 */
public class ErrorMessage {

  /** The errorMessage. */
  private final String errorMessage;

  /** The errorCode. */
  private final String errorCode;

  /**
   * Instantiates a new error message.
   *
   * @param message the message
   * @param error the error
   */
  public ErrorMessage(String message, String error) {
    this.errorMessage = message;
    this.errorCode = error;
  }

  /**
   * Gets the error message.
   *
   * @return the error message
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Gets the error code.
   *
   * @return the error code
   */
  public String getErrorCode() {
    return errorCode;
  }
}