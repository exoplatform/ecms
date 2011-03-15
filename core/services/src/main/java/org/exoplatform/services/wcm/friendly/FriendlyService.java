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
 * This service provides support for friendly Url in eXo Content.
 * example :
 * if url is
 * http://mysite.com/ecmdemo/public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents/news/news1
 * friendly key could be : acme
 * with unfriendly value as : /public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents
 *
 * friendly Url becomes : http://mysite.com/ecmdemo/content/acme/news/news1
 *
 * @author benjamin.paillereau@exoplatform.com
 *
 */
public interface FriendlyService {

  /**
   * We use a servlet in the portal app to forward the friendly to its corresponding unfriendly one.
   * By default, the servelt name is "content"
   *
   * @return the servlet name
   */
  public String getServletName();

  /**
   * Allow to know if service is active and if we should use it.
   *
   * @return true if service is active
   */
  public boolean isEnabled();

  /**
   * Allows to add configuration in the service after instanciation
   *
   * @param plugin
   */
  public void addConfiguration(FriendlyPlugin plugin);

  /**
   * return the friendly uri corresponding to the unfriendly uri
   * ex :
   * friendly = acme
   * unfriendly = /public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents
   *
   *
   * @param unfriendlyUri
   * @return friendly uri
   */
  public String getFriendlyUri(String unfriendlyUri);

  /**
   * return the unfriendly uri corresponding to the friendly uri
   * ex :
   * friendly = acme
   * unfriendly = /public/acme/detail?path=/repository/collaboration/sites content/live/acme/web contents
   *
   *
   * @param friendlyUri
   * @return unfriendly uri
   */
  public String getUnfriendlyUri(String friendlyUri);

  /**
   * Add a new <friendly, unfriendly> couple
   *
   * @param friendlyUri
   * @param unfriendlyUri
   */
  public void addFriendly(String friendlyUri, String unfriendlyUri);

  /**
   * Removes a friendly entry based on the friendly key
   *
   * @param friendlyUri
   */
  public void removeFriendly(String friendlyUri);


  /**
   * get all the <friendly, unfriendly> entries
   *
   * @return map of entries.
   */
  public Map<String, String> getFriendlies();
}
