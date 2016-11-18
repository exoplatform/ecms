/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.rest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base POJO for web-service errors.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ErrorEntiry.java 00000 Dec 21, 2014 pnedonosko $
 * 
 */
public class ErrorEntiry {

  /** The Constant DRIVE_REMOVED. */
  public static final String DRIVE_REMOVED      = "drive-removed";

  /** The Constant NODE_NOT_FOUND. */
  public static final String NODE_NOT_FOUND     = "node-not-found";

  /** The Constant NOT_CLOUD_DRIVE. */
  public static final String NOT_CLOUD_DRIVE    = "not-cloud-drive";

  /** The Constant NOT_CLOUD_FILE. */
  public static final String NOT_CLOUD_FILE     = "not-cloud-file";

  /** The Constant NOT_YET_CLOUD_FILE. */
  public static final String NOT_YET_CLOUD_FILE = "not-yet_cloud-file";
  
  /** The Constant ACCESS_DENIED. */
  public static final String ACCESS_DENIED = "access-denied";

  /**
   * Acess denied.
   *
   * @param message the message
   * @return the error entiry
   */
  public static ErrorEntiry acessDenied(String message) {
    ErrorEntiry err = new ErrorEntiry();
    err.error = ACCESS_DENIED;
    err.message = message;
    return err;
  }
  
  /**
   * Error.
   *
   * @param errorCode the error code
   * @return the error entiry
   */
  public static ErrorEntiry error(String errorCode) {
    ErrorEntiry err = new ErrorEntiry();
    err.error = errorCode;
    return err;
  }

  /**
   * Error.
   *
   * @param errorCode the error code
   * @param message the message
   * @return the error entiry
   */
  public static ErrorEntiry error(String errorCode, String message) {
    ErrorEntiry err = new ErrorEntiry();
    err.error = errorCode;
    err.message = message;
    return err;
  }

  /**
   * Error.
   *
   * @param errorCode the error code
   * @param message the message
   * @param workspace the workspace
   * @param path the path
   * @return the error entiry
   */
  public static ErrorEntiry error(String errorCode, String message, String workspace, String path) {
    ErrorEntiry err = new ErrorEntiry();
    err.error = errorCode;
    err.message = message;
    err.workspace = workspace;
    err.path = path;
    return err;
  }

  /**
   * Message.
   *
   * @param message the message
   * @return the error entiry
   */
  public static ErrorEntiry message(String message) {
    ErrorEntiry err = new ErrorEntiry();
    err.message = message;
    return err;
  }

  /**
   * Drive removed.
   *
   * @param message the message
   * @param workspace the workspace
   * @param path the path
   * @return the error entiry
   */
  public static ErrorEntiry driveRemoved(String message, String workspace, String path) {
    ErrorEntiry err = new ErrorEntiry();
    err.error = DRIVE_REMOVED;
    err.message = message;
    return err;
  }
  
  /**
   * Node not found.
   *
   * @param message the message
   * @param workspace the workspace
   * @param path the path
   * @return the error entiry
   */
  public static ErrorEntiry nodeNotFound(String message, String workspace, String path) {
    ErrorEntiry err = new ErrorEntiry();
    err.error = NODE_NOT_FOUND;
    err.message = message;
    return err;
  }

  /**
   * Not cloud drive.
   *
   * @param message the message
   * @param workspace the workspace
   * @param path the path
   * @return the error entiry
   */
  public static ErrorEntiry notCloudDrive(String message, String workspace, String path) {
    ErrorEntiry err = new ErrorEntiry();
    err.error = NOT_CLOUD_DRIVE;
    err.message = message;
    return err;
  }

  /**
   * Not cloud file.
   *
   * @param message the message
   * @param workspace the workspace
   * @param path the path
   * @return the error entiry
   */
  public static ErrorEntiry notCloudFile(String message, String workspace, String path) {
    ErrorEntiry err = new ErrorEntiry();
    err.error = NOT_CLOUD_FILE;
    err.message = message;
    return err;
  }

  /**
   * Not yet cloud file.
   *
   * @param message the message
   * @param workspace the workspace
   * @param path the path
   * @return the error entiry
   */
  public static ErrorEntiry notYetCloudFile(String message, String workspace, String path) {
    ErrorEntiry err = new ErrorEntiry();
    err.error = NOT_YET_CLOUD_FILE;
    err.message = message;
    return err;
  }

  // ************** instance members **************

  /**
   * Human-readable message.
   */
  protected String              message;

  /**
   * Context node workspace in JCR. Optional.
   */
  protected String              workspace;

  /**
   * Context node path in JCR. Optional.
   */
  protected String              path;

  /**
   * Error key for client software.
   */
  protected String              error;

  /**
   * Optional properties.
   */
  protected Map<String, Object> props = new LinkedHashMap<String, Object>();

  /**
   * Instantiates a new error entiry.
   */
  public ErrorEntiry() {
  }

  /**
   * Adds the property.
   *
   * @param key the key
   * @param value the value
   */
  public void addProperty(String key, Object value) {
    props.put(key, value);
  }

  /**
   * Gets the property.
   *
   * @param key the key
   * @return the property
   */
  public Object getProperty(String key) {
    return props.get(key);
  }

  /**
   * Gets the message.
   *
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the workspace.
   *
   * @return the workspace
   */
  public String getWorkspace() {
    return workspace;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * Gets the error.
   *
   * @return the error
   */
  public String getError() {
    return error;
  }

  /**
   * Gets the props.
   *
   * @return the properties
   */
  public Map<String, ?> getProps() {
    return props;
  }

}
