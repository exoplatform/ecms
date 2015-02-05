/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
 * Empty implementation of {@link CloudDriveListener}.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: BaseCloudDriveListener.java 00000 Jan 8, 2013 pnedonosko $
 * 
 */
public class BaseCloudDriveListener implements CloudDriveListener {

  /**
   * {@inheritDoc}
   */
  @Override
  public void onConnect(CloudDriveEvent event) {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDisconnect(CloudDriveEvent event) {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRemove(CloudDriveEvent event) {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onSynchronized(CloudDriveEvent event) {
    // nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onError(CloudDriveEvent event, Throwable error, String operationName) {
    // nothing
  }
}
