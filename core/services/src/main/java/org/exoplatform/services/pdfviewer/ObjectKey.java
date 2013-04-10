
/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.pdfviewer;

import java.io.Serializable;

/**
 * Create key for cache. When key object is collected by GC, value (if is file) will be delete.
 */
public class ObjectKey implements Serializable{
  private static final long serialVersionUID = -1075842770773918554L;
  String key;
  public ObjectKey(String key) {
    this.key = key;
  }
  @Override
  public String toString() {
    return key;
  }
  
  public String getKey() {
    return key;
  }

  @Override
  public int hashCode() {
    return key == null ? -1 : key.hashCode();
  }

  @Override
  public boolean equals(Object otherKey) {
    if (otherKey != null && ObjectKey.class.isInstance(otherKey)
        && (key != null) && (key.equals(((ObjectKey) (otherKey)).getKey()))) {
      return true;
    }
    return false;
  }
}