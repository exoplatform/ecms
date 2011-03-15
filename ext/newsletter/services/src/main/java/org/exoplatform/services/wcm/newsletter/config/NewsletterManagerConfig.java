/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.wcm.newsletter.config;

import java.util.Date;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * ngoc.tran@exoplatform.com
 * Jun 9, 2009
 */
public class NewsletterManagerConfig {

  /** The newsletter name. */
  private String newsletterName;

  /** The newsletter title. */
  private String newsletterTitle;

  /** The newsletter sent date. */
  private Date newsletterSentDate;

  /** The status. */
  private String status;

  /** The subcription name. */
  private String subcriptionName;

  /** The category name. */
  private String categoryName;

  /**
   * Gets the category name.
   *
   * @return the category name
   */
  public String getCategoryName() {
    return categoryName;
  }

  /**
   * Sets the category name.
   *
   * @param categoryName the new category name
   */
  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
  }

  /**
   * Gets the subcription name.
   *
   * @return the subcription name
   */
  public String getSubcriptionName() {
    return subcriptionName;
  }

  /**
   * Sets the subcription name.
   *
   * @param subcriptionName the new subcription name
   */
  public void setSubcriptionName(String subcriptionName) {
    this.subcriptionName = subcriptionName;
  }

  /**
   * Gets the newsletter name.
   *
   * @return the newsletter name
   */
  public String getNewsletterName() {
    return newsletterName;
  }

  /**
   * Gets the newsletter sent date.
   *
   * @return the newsletter sent date
   */
  public Date getNewsletterSentDate() {
    return newsletterSentDate;
  }

  /**
   * Sets the newsletter sent date.
   *
   * @param newsletterSentDate the new newsletter sent date
   */
  public void setNewsletterSentDate(Date newsletterSentDate) {
    this.newsletterSentDate = newsletterSentDate;
  }

  /**
   * Sets the newsletter name.
   *
   * @param newsletterName the new newsletter name
   */
  public void setNewsletterName(String newsletterName) {
    this.newsletterName = newsletterName;
  }

  /**
   * Gets the newsletter title.
   *
   * @return the newsletter title
   */
  public String getNewsletterTitle() {
    return newsletterTitle;
  }

  /**
   * Sets the newsletter title.
   *
   * @param newsletterTitle the new newsletter title
   */
  public void setNewsletterTitle(String newsletterTitle) {
    this.newsletterTitle = newsletterTitle;
  }

  /**
   * Gets the status.
   *
   * @return the status
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets the status.
   *
   * @param status the new status
   */
  public void setStatus(String status) {
    this.status = status;
  }
}
