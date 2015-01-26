/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.clouddrive;

/**
 * Lister for {@link CloudDrive} events, such as removal or synchronization with its provider.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveListener.java 00000 Dec 13, 2012 pnedonosko $
 * 
 */
public interface CloudDriveListener {

  /**
   * Will be fired after successful connection of a drive to remote provider.
   * 
   * @param event {@link CloudDriveEvent}
   */
  void onConnect(CloudDriveEvent event);

  /**
   * Will be fired after successful disconnection of a drive from remote provider.
   * 
   * @param event {@link CloudDriveEvent}
   */
  void onDisconnect(CloudDriveEvent event);

  /**
   * Will be fired just before a drive physical removal from JCR storage.
   * 
   * @param event {@link CloudDriveEvent}
   */
  void onRemove(CloudDriveEvent event);

  /**
   * Will be fired after the drive synchronization with its provider.
   * 
   * @param event {@link CloudDriveEvent}
   */
  void onSynchronized(CloudDriveEvent event);

  /**
   * Will be fired on an error happened during connect or synchronization of {@link CloudDrive}.<br>
   * This action can be useful for asynchronous calls or to invoke additional cleanup when regular try-catch
   * approach on mentioned operations not applicable.
   * 
   * @see {@link CloudDrive#connect(boolean)}
   * @see {@link CloudDrive#synchronize(boolean)}
   * 
   * @param event {@link CloudDriveEvent}
   * @param error {@link Throwable}, an error happened during connect or synchronization. Check type of the
   *          underlying error to handle a specific exceptions.
   * @param operationName {@link String} name of an operation when this error happened
   */
  void onError(CloudDriveEvent event, Throwable error, String operationName);
}
