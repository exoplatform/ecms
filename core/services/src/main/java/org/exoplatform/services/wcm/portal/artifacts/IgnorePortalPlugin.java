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
package org.exoplatform.services.wcm.portal.artifacts;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;

import java.util.ArrayList;
import java.util.List;




/**
 * Created by The eXo Platform SAS
 * Author : Dang Viet Ha
 *          hadv@exoplatform.com
 * Nov 22, 2011  
 */
public class IgnorePortalPlugin implements ComponentPlugin {

  private String name;

  private String desciption;
  
  private List<String> ignorePortals = new ArrayList<String>();

  public IgnorePortalPlugin(InitParams initParams) {
    ValuesParam valuesParam = initParams.getValuesParam("ignored.portals");
    if(valuesParam != null) {
      ignorePortals = valuesParam.getValues();
    }    
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String s) {
    this.name = s;
  }

  @Override
  public String getDescription() {
    return desciption;
  }

  @Override
  public void setDescription(String s) {
    this.desciption = s;

  }
  
  public List<String> getIgnorePortals() {
    return ignorePortals;
  }

}
