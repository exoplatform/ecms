/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.clouddrive;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Abstract class for all locally connected {@link CloudFile} instances.
 */
public abstract class LocalCloudFile implements CloudFile {
  protected static final Log LOG = ExoLogger.getLogger(LocalCloudFile.class);

  private String             modifiedLocal;

  private String             modifiedRemote;

  /**
   * Inits the modified dates of the file using given locale.
   *
   * @param locale the locale to format the dates
   * @param drive the local drive, it may be used in need get a fresh file node from JCR
   */
  public void initModified(Locale locale, CloudDrive drive) {
    Node node = this.getNode();
    if (node != null) {
      try {
        try {
          // File node can be set in another thread or JCR session expired, check this
          node.getIndex();
        } catch (InvalidItemStateException e) {
          // Need get a fresh file node
          node = null;
          try {
            node = LocalCloudFile.class.cast(drive.getFile(getPath())).getNode();
          } catch (NotCloudFileException | NotCloudDriveException | DriveRemovedException | ClassCastException ncfe) {
            // Not a drive of this file or drive disconnected or removed
          }
        }
        if (node != null) {
          Calendar modifiedLocalDate = node.getProperty("exo:lastModifiedDate").getDate();
          this.modifiedLocal = formatLocalizedDate(modifiedLocalDate, locale);
          this.modifiedRemote = formatLocalizedDate(this.getModifiedDate(), locale);
        }
      } catch (RepositoryException e) {
        LOG.warn("Cannot initialize cloud file modified fields for {}", node, e);
      }
    }
  }

  /*
   * Implementation taken from UIDocumentNodeList.getDatePropertyValue 13/08/2019
   */
  private String formatLocalizedDate(Calendar date, Locale locale) {
    if (date != null && locale != null) {
      DateFormat dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, locale);
      return dateFormat.format(date.getTime());
    }
    return null;
  }

  /**
   * Gets the file modified date formatted in user locale (applied for current user who requests the file). Can be
   * <code>null</code> if current user was not initialized for the file.
   *
   * @return the modified date formatted in user locale or <code>null</code> if current user was not initialized for the file.
   */
  public String getModifiedRemote() {
    return modifiedRemote;
  }

  public String getModifiedLocal() {
    return modifiedLocal;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean isConnected() {
    return true;
  }

  public abstract Node getNode();
}
