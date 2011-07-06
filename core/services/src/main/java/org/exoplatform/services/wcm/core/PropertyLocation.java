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
package org.exoplatform.services.wcm.core;

import javax.jcr.Item;
import javax.jcr.Property;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *              anhvurz90@gmail.com
 * 5 Jul. 2011
 */
public class PropertyLocation extends ItemLocation {

  /**
   * Instantiates a new property location.
   */
  public PropertyLocation() {
    super();
  }

  /**
   * Instantiates a new property location.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   * @param uuid the uuid
   * @param isSystem if the property session is system 
   */
  public PropertyLocation(final String repository, final String workspace, final String path, final String uuid, 
      final boolean isSystem) {
    super(repository, workspace, path, uuid, isSystem);
  }

  /**
   * Instantiates a new property location.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   * @param uuid the uuid
   */
  public PropertyLocation(final String repository, final String workspace, final String path, final String uuid ) {
    super(repository, workspace, path, uuid, false);
  }
  
  /**
   * Instantiates a new property location.
   *
   * @param repository the repository
   * @param workspace the workspace
   * @param path the path
   */
  public PropertyLocation(final String repository, final String workspace, final String path) {
    super(repository, workspace, path, null, false);
  }
  
  /**
   * Instantiates a new property location.
   *
   */
  public PropertyLocation(ItemLocation location) {
    super(location);
  }
  
  /**
   * Get an PropertyLocation object by a property.
   *
   * @param property the property
   *
   * @return a PropertyLocation object
   */
  public static final PropertyLocation getPropertyLocationByProperty(final Property property) {
    try {
      ItemLocation itemLocation = ItemLocation.getItemLocationByItem(property);
      return new PropertyLocation(itemLocation);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Get a property by a PropertyLocation object.
   *
   * @param propertyLocation the PropertyLocation object
   *
   * @return a property
   */
  public static final Property getPropertyByLocation(final PropertyLocation propertyLocation) {
    try {
      Item item = ItemLocation.getItemByLocation(propertyLocation);
      return (Property)item;
    } catch (Exception e) {
      return null;
    }
  }  
  
}
