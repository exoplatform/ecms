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

package org.exoplatform.ecms.xcmis.sp.jcr.exo;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.xcmis.spi.model.BaseType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class TypeMappingPlugin extends BaseComponentPlugin
{

   private InitParams params;

   public TypeMappingPlugin(InitParams params)
   {
      this.params = params;
   }

   @SuppressWarnings("unchecked")
   public Map<String, TypeMapping> getTypeMapping()
   {
      Map<String, TypeMapping> mapping = new HashMap<String, TypeMapping>();
      if (params != null)
      {
         for (Iterator<PropertiesParam> iterator = params.getPropertiesParamIterator(); iterator.hasNext();)
         {
            PropertiesParam param = iterator.next();
            TypeMapping me =
               new TypeMapping(param.getName(), BaseType.fromValue(param.getProperty("baseType")), param
                  .getProperty("parentType"));
            mapping.put(me.getNodeTypeName(), me);
         }
      }
      return mapping;
   }

}
