/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.seo;

import java.io.Serializable;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 17, 2011  
 */
public class PageMetadataModel implements Serializable {
 
  private static final long serialVersionUID = -2765258183491567699L;

  private String uri = null;
  
  //private String pageParent = null;
 
  private String rbcontent = null;
 
  private String keywords = null;
 
  private String description = null;
  
  private String title = null;
 
  private float priority = -1;
 
  private String frequency = null;
 
  private boolean sitemap = true;
  
  private String fullStatus = null;
  
  private String pageReference = null;
 
  public String getUri() { 
    if(uri != null && uri.length() > 0)
      return uri.trim();
    return uri; 
  }
  public void setUri(String uri) { this.uri = uri; }
  
  /*public String getPageParent() { 
    if(pageParent != null && pageParent.length() > 0)
      return pageParent.trim();
    return pageParent; 
  }
  public void setPageParent(String pageParent) { this.pageParent = pageParent; }*/
 
  public String getRobotsContent() { 
    if(rbcontent != null && rbcontent.length() > 0)
      return rbcontent.trim();
    return rbcontent; 
  }
  public void setRobotsContent(String rbcontent) { this.rbcontent = rbcontent; }
 
  public String getDescription() { 
    if(description != null && description.length() > 0)
      return description.trim();
    return description; 
  }
  public void setDescription(String description) { this.description = description; }
 
  public String getKeywords() { 
    if(keywords != null && keywords.length() > 0)
      return keywords.trim();
    return keywords; 
  }
  public void setKeywords(String keywords) { this.keywords = keywords; }
  
  public String getTitle() { 
    if(title != null && title.length() > 0)
      return title.trim();
    return title; 
  }
  public void setTitle(String title) { this.title = title; }
 
  public float getPriority() { return priority; }
  public void setPriority(float priority) { this.priority= priority; }
 
  public String getFullStatus() { 
    if(fullStatus != null && fullStatus.length() > 0)
      return fullStatus.trim();
    return fullStatus; 
  }
  public void setFullStatus(String fullStatus) { this.fullStatus= fullStatus; }
 
  public boolean getSitemap() { return sitemap; }
  public void setSiteMap(boolean sitemap) { this.sitemap= sitemap; }
  
  public String getFrequency() { 
    if(frequency != null && frequency.length() > 0)
      return frequency.trim();
    return frequency; 
  }
  public void setFrequency(String frequency) { this.frequency= frequency; }
  
  public String getPageReference () { 
    if(pageReference != null && pageReference.length() > 0)
      return pageReference.trim();
    return pageReference; 
  }
  public void setPageReference(String pageReference) { this.pageReference = pageReference; }
}
