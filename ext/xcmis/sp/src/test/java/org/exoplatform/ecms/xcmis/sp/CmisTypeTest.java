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

import org.exoplatform.services.jcr.util.IdGenerator;
import org.xcmis.spi.InvalidArgumentException;
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.model.ContentStreamAllowed;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.PropertyType;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.Updatability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey
 *         Kabashnyuk</a>
 * @version $Id$
 * 
 */
public class CmisTypeTest extends BaseTest
{
   //protected Storage storage;

   public static final Set<PropertyType> ALLPROPERTYTYPES = new HashSet<PropertyType>();
   static
   {
      ALLPROPERTYTYPES.add(PropertyType.BOOLEAN);
      ALLPROPERTYTYPES.add(PropertyType.DATETIME);
      ALLPROPERTYTYPES.add(PropertyType.DECIMAL);
      ALLPROPERTYTYPES.add(PropertyType.INTEGER);
      ALLPROPERTYTYPES.add(PropertyType.STRING);
      ALLPROPERTYTYPES.add(PropertyType.HTML);
      ALLPROPERTYTYPES.add(PropertyType.ID);
      ALLPROPERTYTYPES.add(PropertyType.URI);

   }

   private StorageImpl storageA;

   /**
    * @see org.exoplatform.ecms.xcmis.sp.BaseTest#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      storageA = (StorageImpl)registry.getConnection("driveA").getStorage();
   }

   public void testGetTypeRegisteredThrowXml() throws Exception
   {
      assertTypeDefinition(storageA.getTypeDefinition("cmis:type-test", true));

   }

   public void testRegisterTypeInRunTime() throws Exception
   {
      final String typeName = "cmis:type-test-runtime";

      //create new type
      TypeDefinition article = new TypeDefinition();
      article.setBaseId(BaseType.DOCUMENT);
      article.setControllableACL(false);
      article.setControllablePolicy(false);
      article.setCreatable(true);
      article.setDescription("addition type test");

      article.setDisplayName(typeName);
      article.setFileable(true);
      article.setFulltextIndexed(false);
      article.setId(typeName);
      article.setIncludedInSupertypeQuery(false);
      article.setLocalName(typeName);
      article.setParentId("cmis:document");
      article.setQueryable(false);
      article.setQueryName(typeName);
      article.setContentStreamAllowed(ContentStreamAllowed.ALLOWED);
      article.setVersionable(false);

      Map<String, PropertyDefinition<?>> mapPD = new HashMap<String, PropertyDefinition<?>>();

      for (PropertyType propertyType : ALLPROPERTYTYPES)
      {
         PropertyDefinition<String> pd = new PropertyDefinition<String>();
         pd.setMultivalued(false);
         pd.setUpdatability(Updatability.READWRITE);
         pd.setDisplayName("cmis:" + propertyType.toString() + "-type");
         pd.setId("cmis:" + propertyType.toString() + "-type");
         pd.setInherited(false);
         pd.setPropertyType(propertyType);

         mapPD.put(pd.getId(), pd);

      }
      article.setPropertyDefinitions(mapPD);
      //add to the storage
      storageA.addType(article);
      //check if type register correctly
      assertTypeDefinition(storageA.getTypeDefinition(typeName, true));
   }

   public void testRegisterTypeWithInvalidPropertyDefinitionName() throws Exception
   {
      final String typeName = "cmis:type-test-runtime" + IdGenerator.generate();

      //create new type
      TypeDefinition article = new TypeDefinition();
      article.setBaseId(BaseType.DOCUMENT);
      article.setControllableACL(false);
      article.setControllablePolicy(false);
      article.setCreatable(true);
      article.setDescription("addition type test");

      article.setDisplayName(typeName);
      article.setFileable(true);
      article.setFulltextIndexed(false);
      article.setId(typeName);
      article.setIncludedInSupertypeQuery(false);
      article.setLocalName(typeName);
      article.setParentId("cmis:document");
      article.setQueryable(false);
      article.setQueryName(typeName);
      article.setContentStreamAllowed(ContentStreamAllowed.ALLOWED);
      article.setVersionable(false);

      Map<String, PropertyDefinition<?>> mapPD = new HashMap<String, PropertyDefinition<?>>();

      PropertyDefinition<String> pd = new PropertyDefinition<String>();
      pd.setMultivalued(false);
      pd.setUpdatability(Updatability.READWRITE);
      pd.setDisplayName("cmis:def" + StorageImpl.XCMIS_PROPERTY_TYPE);
      pd.setId("cmis:def" + StorageImpl.XCMIS_PROPERTY_TYPE);
      pd.setInherited(false);
      pd.setPropertyType(PropertyType.STRING);

      mapPD.put(pd.getId(), pd);
      article.setPropertyDefinitions(mapPD);
      //add to the storage
      try
      {
         storageA.addType(article);
         fail();
      }
      catch (InvalidArgumentException e)
      {
         //ok
      }
   }

   private void assertTypeDefinition(TypeDefinition typeDefinition)
   {
      for (PropertyType propertyType : ALLPROPERTYTYPES)
      {
         assertPropertyType(propertyType, typeDefinition);
      }
   }

   private void assertPropertyType(PropertyType type, TypeDefinition typeDefinition)
   {
      assertEquals(type, typeDefinition.getPropertyDefinition("cmis:" + type.toString() + "-type").getPropertyType());
   }
}
