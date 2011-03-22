/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.cms.folksonomy.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : Xuan Hoa Pham
 *          hoapham@exoplatform.com
 *          phamvuxuanhoa@gmail.com
 * Dec 8, 2006
 */
public class TagStyleConfig {

  private List<HtmlTagStyle> tagStyleList = new ArrayList<HtmlTagStyle>() ;
  private boolean autoCreatedInNewRepository ;
  private String repository ;

  public List<HtmlTagStyle> getTagStyleList() { return this.tagStyleList ; }
  public void setTagStyleList(List<HtmlTagStyle> list) { this.tagStyleList = list ; }

  public boolean getAutoCreatedInNewRepository(){ return autoCreatedInNewRepository ; }
  public void setAutoCreatedInNewRepository(boolean isAuto) { autoCreatedInNewRepository = isAuto ; }

  public void setRepository(String repo) { repository = repo ; }
  public String getRepository() { return repository ; }

  static public class HtmlTagStyle {
    String name ;
    String tagRate ;
    String htmlStyle ;
    String description ;

    public String getDescription() { return description; }
    public void setDescription(String description) {
      this.description = description;
    }
    public String getHtmlStyle() { return htmlStyle; }
    public void setHtmlStyle(String htmlStyle) {
      this.htmlStyle = htmlStyle;
    }

    public String getName() { return name; }
    public void setName(String name) {
      this.name = name;
    }

    public String getTagRate() { return tagRate; }
    public void setTagRate(String tagRate) {
      this.tagRate = tagRate;
    }
  }

}
