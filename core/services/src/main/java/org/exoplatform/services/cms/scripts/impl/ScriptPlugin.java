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
package org.exoplatform.services.cms.scripts.impl;

import java.util.Iterator;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;


public class ScriptPlugin extends BaseComponentPlugin{

  private String name;
  private String description;
  private InitParams params_ ;

  public ScriptPlugin(InitParams params) throws Exception {
    params_ = params;
  }

  @SuppressWarnings("unchecked")
  public Iterator<ObjectParameter> getScriptIterator() { return params_.getObjectParamIterator()  ; }

  public boolean getAutoCreateInNewRepository() {
    ValueParam param = params_.getValueParam("autoInitInNewRepository") ;
    if(param == null) return true ;
    return Boolean.parseBoolean(param.getValue()) ;
  }

  @Deprecated
  public String getInitRepository() {
    ValueParam param = params_.getValueParam("repository") ;
    if(param == null) return null ;
    return param.getValue() ;
  }

  public String getPredefineScriptsLocation() {
    ValueParam param = params_.getValueParam("predefinedScriptsLocation") ;
    if(param == null) return null ;
    return param.getValue() ;
  }

  public String getName() {   return name; }
  public void setName(String s) { name = s ; }

  public String getDescription() {   return description ; }
  public void setDescription(String s) { description = s ;  }
}
