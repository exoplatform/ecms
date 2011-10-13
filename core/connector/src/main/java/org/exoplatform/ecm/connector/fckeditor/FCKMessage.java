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
package org.exoplatform.ecm.connector.fckeditor;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
 * Created by The eXo Platform SAS @author : Hoa.Pham hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
/**
 * The Class FCKMessage.
 */
public class FCKMessage {

  /** The Constant ERROR. */
  public static final String ERROR                      = "Error";

  /** The Constant INFO. */
  public static final String INFO                       = "Info";

  /** The Constant FOLDER_CREATED. */
  public static final int    FOLDER_CREATED             = 100;

  /** The Constant FOLDER_EXISTED. */
  public static final int    FOLDER_EXISTED             = 101;

  /** The Constant FOLDER_INVALID_NAME. */
  public static final int    FOLDER_INVALID_NAME        = 102;

  /** The Constant FOLDER_PERMISSION_CREATING. */
  public static final int    FOLDER_PERMISSION_CREATING = 103;

  /** The Constant FOLDER_NOT_CREATED. */
  public static final int    FOLDER_NOT_CREATED         = 104;

  /** The Constant UNKNOWN_ERROR. */
  public static final int    UNKNOWN_ERROR              = 110;

  /** The Constant FILE_EXISTED. */
  public static final int    FILE_EXISTED               = 201;

  /** The Constant FILE_NOT_FOUND. */
  public static final int    FILE_NOT_FOUND             = 202;

  /** The Constant FILE_UPLOAD_RESTRICTION. */
  public static final int    FILE_UPLOAD_RESTRICTION    = 203;

  /** The Constant FILE_NOT_UPLOADED. */
  public static final int FILE_NOT_UPLOADED = 204;

  /** The Constant FCK_RESOURCE_BUNDLE. */
  public static final String FCK_RESOURCE_BUNDLE_FILE   = "locale.services.fckeditor.FCKConnector"
                                                            ;

  /**
   * Instantiates a new fCK message.
   *
   * @param bundleService the bundle service
   */
  public FCKMessage() {
  }

  public Document createMessage(int messageCode, String messageType, String language, Object[] args)
      throws Exception {
    String message = getMessage(messageCode, args, language);
    return createMessage(messageCode, message, messageType);
  }

  public Document createMessage(int messageCode, String message, String messageType)
      throws Exception {
    DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document document = documentBuilder.newDocument();
    Element element = document.createElement("Message");
    element.setAttribute("number", Integer.toString(messageCode));
    element.setAttribute("text", message);
    element.setAttribute("type", messageType);
    document.appendChild(element);
    return document;
  }

  /**
   * Gets the message.
   *
   * @param messageNum the message num
   * @param args the args
   * @param language the language
   * @return the message
   * @throws Exception the exception
   */
  public String getMessage(int messageNum, Object[] args, String language) throws Exception {
    String messageKey = getMessageKey(messageNum);
    return getMessage(messageKey, args, language);
  }

  /**
   * Gets the message.
   *
   * @param messageKey the message key
   * @param args the args
   * @param language the language
   * @return the message
   * @throws Exception the exception
   */
  public String getMessage(String messageKey, Object[] args, String language) throws Exception {
    Locale locale = null;
    if (language == null) {
      locale = Locale.ENGLISH;
    } else {
      locale = new Locale(language);
    }
    ResourceBundle resourceBundle = ResourceBundle.getBundle(FCK_RESOURCE_BUNDLE_FILE, locale);
    String message = resourceBundle.getString(messageKey);
    if (args == null) {
      return message;
    }
    return MessageFormat.format(message, args);
  }

  protected String getMessageKey(int number) {
    String messageKey = null;
    switch (number) {
    case FOLDER_CREATED:
      messageKey = "fckeditor.folder-created";
      break;
    case FOLDER_NOT_CREATED:
      messageKey = "fckeditor.folder-not-created";
      break;
    case FOLDER_INVALID_NAME:
      messageKey = "fckeditor.folder-invalid-name";
      break;
    case FOLDER_EXISTED:
      messageKey = "fckeditor.folder-existed";
      break;
    case FOLDER_PERMISSION_CREATING:
      messageKey = "fckeditor.folder-permission-denied";
      break;
    case FILE_EXISTED:
      messageKey = "fckeditor.file-existed";
      break;
    case FILE_NOT_FOUND:
      messageKey = "fckeditor.file-not-found";
      break;
    case FILE_NOT_UPLOADED:
      messageKey = "fckeditor.file-not-uploaded";
      break;
    case FILE_UPLOAD_RESTRICTION:
      messageKey = "fckeditor.file-uploaded-restriction";
      break;
    default:
      messageKey = "connector.fckeditor.unknowm-message";
      break;
    }
    return messageKey;
  }
}
