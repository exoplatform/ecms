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
package org.exoplatform.services.cms.clouddrives;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
   */

  public void initModified(Locale locale) {
    this.modifiedLocal = formatLocalizedDate(getLocalModifiedDate(), locale);
    this.modifiedRemote = formatLocalizedDate(this.getModifiedDate(), locale);
  }

  /**
   * Gets the local modified date from the storage (not the same as actual modified date in remote provider).
   *
   * @return the local modified date
   */
  public abstract Calendar getLocalModifiedDate();

  private String formatLocalizedDate(Calendar date, Locale locale) {
    // Implementation taken from UIDocumentNodeList.getDatePropertyValue 13/08/2019
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

  /**
   * Gets the modified locally date formatted in user locale (applied for current user who requests the file).
   *
   * @return the modified local date
   */
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
}
