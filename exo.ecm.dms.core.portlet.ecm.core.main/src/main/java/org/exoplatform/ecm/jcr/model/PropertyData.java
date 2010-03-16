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
package org.exoplatform.ecm.jcr.model;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.PropertyDefinition;
/**
 * @author Tuan Nguyen (tuan08@users.sourceforge.net)
 * @since Feb 27, 2005
 * @version $Id$
 */
public class PropertyData {
  private String id_ ;
  private String name_ ;
  private Object value_ ;
  private Object stream_ ;
  private PropertyDefinition def_ ;
  private boolean residual_ ;
  private int type_ ;
  private boolean newDataRead_ = true ;
  
  public PropertyData(PropertyDefinition def) { 
    def_ =  def ;
    name_ = def.getName() ;
    id_ = Integer.toString(hashCode()) ;
    residual_ = "*".equals(name_) ;
    type_ = def.getRequiredType() ;
  }
  
  public String getId() { return id_ ; }
  
  public String getName() { return name_ ; }
  public void   setName(String s) { name_ = s ; }
  
  public int getPropertyType() { return type_ ; }
  public void setPropertyType(int type) { type_ = type ; }
  
  public PropertyDefinition getPropertyDef() { return def_ ; }
  
  public boolean isResidual() { return residual_ ; }
  
  public boolean isMultiple() { return def_.isMultiple() ; }
  
  public Object getValue() { return value_ ; }
  
  public boolean hasValue() { return value_ != null ; }
  
  public void setValue(byte[] value) throws Exception {
    value_ = "" ;
    stream_ = value ;
  }
  
  @SuppressWarnings("unchecked")
  public void setValue(String value) throws Exception {
    if(isMultiple()) {
      List<String> list = (List<String>) value_ ;
      if(list == null ) {
        list = new ArrayList<String>(3) ;
        value_  = list ;
      }
      if(newDataRead_) list.clear() ;
      newDataRead_ = false ;
      if(value != null && value.length() > 0)list.add(value) ;
    } else {
      value_ = value ;
    }
  }
  
  public void setValue(Node node) throws Exception {
    Property prop = node.getProperty(name_);
    if(isMultiple()) {
      Value[] values = prop.getValues() ;
      List<String> temp = new ArrayList<String>(3);
      for(Value value : values) {
        if(getPropertyType() == PropertyType.BINARY) temp.add("");
        else temp.add(value.getString()) ;
      }
      value_ = temp ;
    } else {
      if(getPropertyType() == PropertyType.BINARY) value_ = "" ;
      else value_ = prop.getValue().getString() ;
    }
  }
  
  public String getValueAsString() { return (String) value_ ; }
  
  public List getValuesAsString() {
    newDataRead_ = true ;
    return (List) value_ ; 
  }
  
//@TODO statement put in bracket when use if statement
  public Value getPropertyValue(ValueFactory factory) throws Exception {
  	// need to use session.getValueFactory().create... instead
  	if(getPropertyType() != PropertyType.BINARY)
  		return factory.createValue((String)value_, getPropertyType()) ;
  	return factory.createValue(new String((byte[]) stream_), getPropertyType()) ;
  	
    //if(getPropertyType() == PropertyType.STRING) return new StringValue((String)value_) ;
    //if(getPropertyType() == PropertyType.NAME) return NameValue.valueOf((String)value_) ;
    //if(getPropertyType() == PropertyType.BOOLEAN) return new BooleanValue("true".equals(value_)) ;
    //if(getPropertyType() == PropertyType.BINARY) return new BinaryValue((byte[]) stream_) ;
    //return null ;
  }
  
// @TODO statement put in bracket when use if statement
  public Value[] getPropertyValues(ValueFactory factory) throws Exception {
    newDataRead_ = true ;
    List values = (List) value_ ; 
    if(values == null || values.size() == 0) return null ;
    Value[] value = new Value[values.size()] ;
    for(int i = 0; i < values.size(); i++) {
      String  s = (String)values.get(i) ;
   	  if(getPropertyType() != PropertyType.BINARY)
   	  	value[i] = factory.createValue(s, getPropertyType()) ;
      else
      	value[i] = factory.createValue(new String((byte[]) stream_), getPropertyType()) ;
      
      //if(getPropertyType() == PropertyType.STRING) value[i] =  new StringValue(s) ;
      //if(getPropertyType() == PropertyType.NAME) value[i] = NameValue.valueOf(s) ;
      //if(getPropertyType() == PropertyType.BOOLEAN) value[i] = new BooleanValue("true".equals(s)) ;
      //if(getPropertyType() == PropertyType.BINARY) value[i] = new BinaryValue((byte[]) stream_) ;
    }
    return value ;
  }
}