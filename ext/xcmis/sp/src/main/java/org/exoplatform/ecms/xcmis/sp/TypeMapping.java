/**
 *  Copyright (C) 2003-2010 eXo Platform SAS.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.ecms.xcmis.sp;

import org.xcmis.spi.model.BaseType;

/**
 * TypeMapping may be used as part of configuration to show JCR nodes which has
 * node type which is not supported directly.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class TypeMapping
{

   private String nodeTypeName;

   private BaseType baseType;

   private String parentType;

   public TypeMapping(String nodeTypeName, BaseType baseType, String parentType)
   {
      this.nodeTypeName = nodeTypeName;
      this.baseType = baseType;
      this.parentType = parentType;
   }

   public TypeMapping()
   {
      // For using as object-param. Do not use directly.
   }

   public BaseType getBaseType()
   {
      return baseType;
   }

   public String getParentType()
   {
      if (parentType == null)
      {
         return baseType.value();
      }
      return parentType;
   }

   public String getNodeTypeName()
   {
      return nodeTypeName;
   }

}
