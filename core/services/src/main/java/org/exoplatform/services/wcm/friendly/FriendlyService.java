package org.exoplatform.services.wcm.friendly;
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

import java.util.Map;

import org.exoplatform.services.wcm.friendly.impl.FriendlyPlugin;

/**
 * Provides support for friendly URL in Content.
 * <p></p>
 * For example:
 * If URL is
 * http://mysite.com/portal/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents/news/news1
 * where the friendly key could be: acme
 * with unfriendly value as: /public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents,
 * the friendly URL becomes: http://mysite.com/portal/content/acme/news/news1.
 *
 * @LevelAPI Experimental
 *
 */
public interface FriendlyService {

  /**
   * Uses a servlet in the portal app to forward the friendly URL to its corresponding unfriendly one.
   * By default, the servelt name is "content".
   *
   * @return The servlet name.
   */
  public String getServletName();

  /**
   * Checks if the Friendly Service is active.
   *
   * @return "True" if the Friendly Service is active. Otherwise, it returns "false".
   */
  public boolean isEnabled();

  /**
   * Allows to add configuration to the Friendly Service.
   *
   * @param plugin The Friendly plugin.
   */
  public void addConfiguration(FriendlyPlugin plugin);

  /**
   * Gets the friendly URI corresponding to an unfriendly URI.
   * <p></p>
   * For example:<br>
   *
   * friendly = acme<br>
   * unfriendly = /public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents<br>
   *
   * @param unfriendlyUri The current unfriendly URI.
   * @return The friendly URI.
   */
  public String getFriendlyUri(String unfriendlyUri);

  /**
   * Gets the unfriendly URI corresponding to a friendly URI.
   * <br>
   * For example:<br>
   * 
   * friendly = acme<br>
   * unfriendly = /public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents<br>
   *
   * @param friendlyUri The current friendly URI.
   * @return The unfriendly URI.
   */
  public String getUnfriendlyUri(String friendlyUri);

  /**
   * Adds a new {friendly, unfriendly} couple.
   *
   * @param friendlyUri The friendly URI.
   * @param unfriendlyUri The unfriendly URI.
   */
  public void addFriendly(String friendlyUri, String unfriendlyUri);

  /**
   * Removes a friendly entry based on the friendly key.
   *
   * @param friendlyUri The friendly URI.
   */
  public void removeFriendly(String friendlyUri);


  /**
   * Gets all {friendly, unfriendly} entries.
   *
   * @return The map of entries.
   */
  public Map<String, String> getFriendlies();
}
