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
package org.exoplatform.services.cms.link;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 1 avr. 2009
 */
public class PropertyIteratorLinkAware extends RangeIteratorLinkAware implements PropertyIterator {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger("cms.PropertyIteratorLinkAware");

  public PropertyIteratorLinkAware(
                                   String originalWorkspace,
                                   String virtualPath,
                                   PropertyIterator propertyIterator) {
    super(originalWorkspace, virtualPath, propertyIterator);
  }

  /**
   * {@inheritDoc}
   */
  public Property nextProperty() {
    Property property = (Property) iterator.next();
    try {
      return new PropertyLinkAware(originalWorkspace, LinkUtils.createPath(virtualPath, property.getName()), property);
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Cannot create an instance of PropertyLinkAware", e);
      }
    }
    return property;
  }


  /**
   * {@inheritDoc}
   */
  public Object next() {
    return nextProperty();
  }
}
