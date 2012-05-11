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
package org.exoplatform.services.context;

import java.util.HashMap;


/**
 * This class acts as a context stores some auxiliary attributes of a document. 
 * It's useful with document listeners which are able to make decision based on these attributes, for example: skipping raising social activity,...  
 * 
 * Created by The eXo Platform SAS
 * Author : Lai Trung Hieu
 *          hieult@exoplatform.com
 * Apr 23, 2012  
 */
public class DocumentContext {

  /**
   * The attribute name is used to indicate is it need to skip raising social activity
   */
  public static final String                  IS_SKIP_RAISE_ACT = "isSkipRaiseActivity";
  
  /**
   * ThreadLocal keeper for DocumentContext.
   */
  private static ThreadLocal<DocumentContext> current = new ThreadLocal<DocumentContext>();
  
  /**
   * Additions attributes of DocumentContext.
   */
  private HashMap<String, Object> attributes = new HashMap<String, Object>();
  
  /**
   * @return current DocumentContext or null if it was not preset
   */
  public static DocumentContext getCurrent() {
    if (current.get() == null) {
      setCurrent(new DocumentContext());
    }
    return current.get();
  }

  /**
   * Preset current DocumentContext.
   * @param state DocumentContext
   */
  public static void setCurrent(DocumentContext state) {
    current.set(state);
  }

  /**
   * @return the attributes
   */
  public HashMap<String, Object> getAttributes() {
    return attributes;
  }

  /**
   * @param attributes the attributes to set
   */
  public void setAttributes(HashMap<String, Object> attributes) {
    this.attributes = attributes;
  }
}
