/*
 * Copyright (C) 2010 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.ecms.xcmis.sp.index;

import org.xcmis.search.content.Property;

import java.util.ArrayList;
import java.util.List;

class MockContentEntry
{
   /**
    * List of table names which identifies this content.
    */
   List<String> tableNames;

   /**
    * Name of the entry.
    */
   String name;

   /**
    * List of parent entry identifiers.
    */
   List<String> parentIdentifiers;

   /**
    * Entry identifier.
    */
   String identifier;

   /**
    * List of entry properties.
    */
   List<Property> properties;

   /**
    *
    */
   MockContentEntry()
   {
      tableNames = new ArrayList<String>();
      parentIdentifiers = new ArrayList<String>();
      properties = new ArrayList<Property>();
   }

   /**
    * @return the tableNames
    */
   public String[] getTableNames()
   {
      return tableNames.toArray(new String[tableNames.size()]);
   }

   /**
    * @return the parentIdentifiers
    */
   public String[] getParentIdentifiers()
   {
      return parentIdentifiers.toArray(new String[parentIdentifiers.size()]);
   }

   /**
    * @return the properties
    */
   public Property[] getProperties()
   {
      return properties.toArray(new Property[properties.size()]);
   }

}
