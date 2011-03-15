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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * May 22, 2009
 */
public class NewsletterCategoryConfig {

  /** The name. */
  private String name;

  /** The title. */
  private String title;

  /** The description. */
  private String description;

  /** The moderator. */
  private String moderator;

  /** The subscriptions. */
  private List<NewsletterSubscriptionConfig> subscriptions = new ArrayList<NewsletterSubscriptionConfig>();

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
  public String getName() { return name; }

  /**
   * Sets the name.
   *
   * @param name the new name
   */
  public void setName(String name) { this.name = name; }

  /**
   * Gets the description.
   *
   * @return the description
   */
  public String getDescription() { return description; }

  /**
   * Sets the description.
   *
   * @param description the new description
   */
  public void setDescription(String description) { this.description = description; }

  /**
   * Gets the moderator.
   *
   * @return the moderator
   */
  public String getModerator() { return moderator; }

  /**
   * Sets the moderator.
   *
   * @param moderator the new moderator
   */
  public void setModerator(String moderator) { this.moderator = moderator; }

  /**
   * Gets the subscriptions.
   *
   * @return the subscriptions
   */
  public List<NewsletterSubscriptionConfig> getSubscriptions() { return subscriptions; }

  /**
   * Sets the subscriptions.
   *
   * @param subscriptions the new subscriptions
   */
  public void setSubscriptions(List<NewsletterSubscriptionConfig> subscriptions) { this.subscriptions = subscriptions; }
}
