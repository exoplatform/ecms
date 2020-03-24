/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
package org.exoplatform.wcm.connector.collaboration.editors;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class HypermediaSupport is used in HATEOAS REST services.
 */
public class HypermediaSupport {

  /** The links. */
  protected List<HypermediaLink> links;

  /**
   * Gets the links.
   *
   * @return the links
   */
  public List<HypermediaLink> getLinks() {
    return links;
  }

  /**
   * Sets the links.
   *
   * @param links the new links
   */
  public void setLinks(List<HypermediaLink> links) {
    this.links = links;
  }

  
  /**
   * Adds the link.
   *
   * @param link the link
   */
  public void addLink(HypermediaLink link) {
    if (links == null) {
      links = new ArrayList<>();
    }
    links.add(link);
  }

}
