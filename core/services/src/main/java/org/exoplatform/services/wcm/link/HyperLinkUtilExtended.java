/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.link;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.services.html.HTMLNode;
import org.exoplatform.services.html.util.HyperLinkUtil;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong_phan@exoplatform.com
 * Sep 5, 2008  
 */
public class HyperLinkUtilExtended extends HyperLinkUtil {  
  public synchronized List<String> getSiteLink(HTMLNode node) {
    Map<String, String> map = new HashMap<String, String>(4); 
    map.put("a", "href");
    map.put("iframe", "src");
    map.put("frame", "src");
    map.put("meta", "url");
    
    return getAttributes(node, null, map, null);
  }
}
