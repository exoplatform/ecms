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
package org.exoplatform.services.wcm.newsletter;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform
 * ngoc.tran@exoplatform.com May 27, 2009
 */
public class NewsletterSubscriptionConfig {

  /** The name. */
  private String name;

  /** The description. */
  private String description;

  /** The title. */
  private String title;

  /** The category name. */
  private String categoryName;

  private String redactor;

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
   * Gets the title.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title.
   *
   * @param title the new title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the new description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  public String getRedactor() {
    return redactor;
  }

  public void setRedactor(String redactor) {
    this.redactor = redactor;
  }
}
