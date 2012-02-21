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

package org.exoplatform.ecms.xcmis.sp.index;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.search.content.Schema;
import org.xcmis.search.lucene.content.SchemaTableResolver;
import org.xcmis.search.value.NameConverter;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.TypeManager;
import org.xcmis.spi.TypeNotFoundException;
import org.xcmis.spi.model.TypeDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * Class override getSubTypes method.
 *
 */
public class CmisSchemaTableResolver extends SchemaTableResolver
{

   private final TypeManager typeManager;

   private static final Log LOG = ExoLogger.getLogger(CmisSchemaTableResolver.class);

   /**
    * Instantiates new instance of CmisSchemaTableResolver.
    *
    * @param nameConverter NameConverter
    * @param schema Schema
    * @param typeManager TypeManager
    */
   public CmisSchemaTableResolver(NameConverter nameConverter, Schema schema, TypeManager typeManager)
   {
      super(nameConverter, schema);
      this.typeManager = typeManager;
   }

   /**
    * @see org.xcmis.search.lucene.content.SchemaTableResolver#getSubTypes(java.lang.String)
    */
   @Override
   protected Set<String> getSubTypes(String tableName)
   {
      Set<String> subTypes = new HashSet<String>();

      try
      {
         ItemsIterator<TypeDefinition> typeChildren = typeManager.getTypeChildren(tableName, false);
         while (typeChildren.hasNext())
         {
            TypeDefinition typeDefinition = typeChildren.next();
            subTypes.add(typeDefinition.getQueryName());
         }
      }
      catch (TypeNotFoundException tnf)
      {
        if (LOG.isErrorEnabled()) {
          LOG.error(tnf.getMessage(), tnf);
        }
      }
      return subTypes;
   }
}
