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
package org.exoplatform.services.cms.views;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Quang Hung
 *          nguyenkequanghung@yahoo.com
 * Feb 27, 2006
 *
 */
public class TemplateConfig {
  private String name ;
  private String warPath ;
  private String type ;

  public TemplateConfig(){}

  public String getName() { return this.name ; }
  public void setName(String name) { this.name = name ; }

  public String getWarPath() { return this.warPath ; }
  public void setWarPath(String warPath) { this.warPath = warPath ; }

  public String getTemplateType() {return this.type ; }
  public void setTemplateType(String type) { this.type = type ; }

}
