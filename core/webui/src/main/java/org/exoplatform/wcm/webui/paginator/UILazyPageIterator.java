/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.paginator;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPageIterator;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Apr 7, 2015  
 */
@ComponentConfig(
           template = "classpath:groovy/wcm/webui/paginator/UILazyPageIterator.gtmpl", 
           events = @EventConfig(listeners = UIPageIterator.ShowPageActionListener.class))
@Serialized
public class UILazyPageIterator extends UIPageIterator {
  
  public boolean loadedAllData() {
     if (getPageList() instanceof AbstractPageList) {
       return ((AbstractPageList)getPageList()).loadedAllData();
     } else {
       return true;
     }
    
  }
  
}
