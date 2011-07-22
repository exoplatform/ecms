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

import javax.jcr.RangeIterator;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 1 avr. 2009
 */
public abstract class RangeIteratorLinkAware implements RangeIterator {

  protected final String originalWorkspace;
  protected final String virtualPath;
  protected final RangeIterator iterator;

  public RangeIteratorLinkAware(String originalWorkspace, String virtualPath, RangeIterator iterator) {
    this.iterator = iterator;
    this.originalWorkspace = originalWorkspace;
    if (!virtualPath.startsWith("/")) {
      throw new IllegalArgumentException("The path '" + virtualPath +  "' must be an absolute path");
    }
    this.virtualPath = virtualPath;
  }

  /**
   * {@inheritDoc}
   */
  public long getPosition() {
    return iterator.getPosition();
  }

  /**
   * {@inheritDoc}
   */
  public long getSize() {
    return iterator.getSize();
  }

  /**
   * {@inheritDoc}
   */
  public void skip(long skipNum) {
    iterator.skip(skipNum);
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasNext() {
    return iterator.hasNext();
  }

  /**
   * {@inheritDoc}
   */
  public void remove() {
    iterator.remove();
  }
}
