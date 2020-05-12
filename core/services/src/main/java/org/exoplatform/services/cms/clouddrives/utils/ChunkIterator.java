/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
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
package org.exoplatform.services.cms.clouddrives.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import org.exoplatform.services.cms.clouddrives.CloudDriveException;

/**
 * Iterator over whole set of items possibly split on chunks. This iterator
 * hides next-chunk logic for consumer code. <br>
 * Iterator methods can throw {@link CloudDriveException} in case of remote or
 * communication errors.<br>
 * Created by The eXo Platform SAS
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ChunkIterator.java 00000 Dec 3, 2013 pnedonosko $
 * @param <I> the generic type
 */
public abstract class ChunkIterator<I> {

  /** The iter. */
  protected Iterator<I> iter;

  /** The next. */
  protected I           next;

  /**
   * Forecast of available items in the iterator. Calculated on each
   * {@link #nextChunk()}. Used for progress indicator.
   */
  protected AtomicLong  available = new AtomicLong();

  /**
   * Totally fetched items. Changes on each {@link #next()}. Used for progress
   * indicator.
   */
  protected AtomicLong  fetched   = new AtomicLong();

  /**
   * Next chunk.
   *
   * @return the iterator
   * @throws CloudDriveException the cloud drive exception
   */
  protected abstract Iterator<I> nextChunk() throws CloudDriveException;

  /**
   * Checks for next chunk.
   *
   * @return true, if successful
   */
  protected abstract boolean hasNextChunk();

  /**
   * Checks for next.
   *
   * @return true, if successful
   * @throws CloudDriveException the cloud drive exception
   */
  public boolean hasNext() throws CloudDriveException {
    if (next == null) {
      if (iter.hasNext()) {
        next = iter.next();
      } else {
        // try to fetch next portion of changes
        while (hasNextChunk()) {
          iter = nextChunk();
          if (iter.hasNext()) {
            next = iter.next();
            break;
          }
        }
      }
      return next != null;
    } else {
      return true;
    }
  }

  /**
   * Next.
   *
   * @return the i
   * @throws NoSuchElementException the no such element exception
   * @throws CloudDriveException the cloud drive exception
   */
  public I next() throws NoSuchElementException, CloudDriveException {
    if (next == null && !hasNext()) {
      throw new NoSuchElementException("No more data.");
    }

    I i = next;
    next = null;
    fetched.incrementAndGet();
    return i;
  }

  /**
   * Calculate a forecast of items available to fetch. Call it on each
   * {@link #nextChunk()}.
   * 
   * @param newValue long
   */
  public void available(long newValue) {
    if (available.get() == 0) {
      // magic here as we're in indeterminate progress during the fetching
      // logic based on page bundles we're getting from the drive
      // first page it's 100%, assume the second is filled on 25%
      available.set(hasNextChunk() ? Math.round(newValue * 1.25f) : newValue);
    } else {
      // All previously set newValue was fetched.
      // Assuming the next page is filled on 25%.
      long newFetched = available.getAndAdd(hasNextChunk() ? Math.round(newValue * 1.25f) : newValue);
      fetched.set(newFetched);
    }
  }

  /**
   * Return currently available items to fetch. Value can be inaccurate and for
   * information only.
   * 
   * @return long
   */
  public long getAvailable() {
    return available.get();
  }

  /**
   * Return number of already fetched items. Value for information only.
   * 
   * @return long
   */
  public long getFetched() {
    return fetched.get();
  }

}
