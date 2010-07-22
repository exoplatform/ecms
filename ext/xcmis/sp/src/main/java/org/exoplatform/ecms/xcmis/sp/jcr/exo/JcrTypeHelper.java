/**
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

package org.exoplatform.ecms.xcmis.sp.jcr.exo;

import static org.exoplatform.ecms.xcmis.sp.jcr.exo.StorageImpl.XCMIS_PROPERTY_TYPE_PATTERN;

import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.model.Choice;
import org.xcmis.spi.model.ContentStreamAllowed;
import org.xcmis.spi.model.DateResolution;
import org.xcmis.spi.model.Precision;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.PropertyType;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.Updatability;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: JcrTypeHelper.java 1262 2010-06-09 10:07:01Z andrew00x $
 */
class JcrTypeHelper
{

   static final Set<Pattern> ignoredProperties = new HashSet<Pattern>();
   static
   {
      ignoredProperties.add(Pattern.compile("jcr:created"));
      ignoredProperties.add(Pattern.compile("jcr:mixinTypes"));
      ignoredProperties.add(Pattern.compile("jcr:uuid"));
      ignoredProperties.add(Pattern.compile("jcr:primaryType"));
      ignoredProperties.add(Pattern.compile("exo:owner"));
      ignoredProperties.add(Pattern.compile("\\*"));
      ignoredProperties.add(XCMIS_PROPERTY_TYPE_PATTERN);
   }

   /**
    * Get object type definition.
    * 
    * @param nt JCR back-end node
    * @param includePropertyDefinition true if need include property definition
    *        false otherwise
    * @return object definition or <code>null</code> if specified JCR node-type
    *         has not corresponded CMIS type
    * @throws NotSupportedNodeTypeException if specified node-type is
    *         unsupported by xCMIS
    */
   public static TypeDefinition getTypeDefinition(NodeType nt, boolean includePropertyDefinition)
      throws NotSupportedNodeTypeException
   {
      if (nt.isNodeType(JcrCMIS.NT_FILE))
      {
         return getDocumentDefinition(nt, includePropertyDefinition);
      }
      else if (nt.isNodeType(JcrCMIS.NT_FOLDER) || nt.isNodeType(JcrCMIS.NT_UNSTRUCTURED))
      {
         return getFolderDefinition(nt, includePropertyDefinition);
      }
      else if (nt.isNodeType(JcrCMIS.CMIS_NT_RELATIONSHIP))
      {
         return getRelationshipDefinition(nt, includePropertyDefinition);
      }
      else if (nt.isNodeType(JcrCMIS.CMIS_NT_POLICY))
      {
         return getPolicyDefinition(nt, includePropertyDefinition);
      }
      else
      {
         throw new NotSupportedNodeTypeException("Type " + nt.getName() + " is unsupported for xCMIS.");
      }
   }

   /**
    * Get CMIS object type id by the JCR node type name.
    * 
    * @param ntName the JCR node type name
    * @return CMIS object type id
    */
   public static String getCmisTypeId(String ntName)
   {
      if (ntName.equals(JcrCMIS.NT_FILE))
      {
         return BaseType.DOCUMENT.value();
      }
      if (ntName.equals(JcrCMIS.NT_FOLDER) || ntName.equals(JcrCMIS.NT_UNSTRUCTURED))
      {
         return BaseType.FOLDER.value();
      }
      return ntName;
   }

   /**
    * Get JCR node type name by the CMIS object type id.
    * 
    * @param typeId the CMIS base object type id
    * @return JCR string node type
    */
   public static String getNodeTypeName(String typeId)
   {
      if (typeId.equals(BaseType.DOCUMENT.value()))
      {
         return JcrCMIS.NT_FILE;
      }
      if (typeId.equals(BaseType.FOLDER.value()))
      {
         return JcrCMIS.NT_FOLDER;
      }
      return typeId;
   }

   /**
    * Document type definition.
    * 
    * @param nt node type
    * @param includePropertyDefinition true if need include property definition
    *        false otherwise
    * @return document type definition
    */
   protected static TypeDefinition getDocumentDefinition(NodeType nt, boolean includePropertyDefinition)
   {
      TypeDefinition def = new TypeDefinition();
      String localTypeName = nt.getName();
      String typeId = getCmisTypeId(localTypeName);
      def.setBaseId(BaseType.DOCUMENT);
      def.setContentStreamAllowed(ContentStreamAllowed.ALLOWED);
      def.setControllableACL(true);
      def.setControllablePolicy(true);
      def.setCreatable(true);
      def.setDescription("Cmis Document Type");
      def.setDisplayName(typeId);
      def.setFileable(true);
      def.setFulltextIndexed(true);
      def.setId(typeId);
      def.setIncludedInSupertypeQuery(true);
      def.setLocalName(localTypeName);
      def.setLocalNamespace(JcrCMIS.EXO_CMIS_NS_URI);
      if (typeId.equals(BaseType.DOCUMENT.value()))
      {
         def.setParentId(null); // no parents for root type
      }
      else
      {
         // Try determine parent type.
         NodeType[] superTypes = nt.getDeclaredSupertypes();
         for (NodeType superType : superTypes)
         {
            if (superType.isNodeType(JcrCMIS.NT_FILE))
            {
               // Take first type that is super for cmis:document or is cmis:document.
               def.setParentId(getCmisTypeId(superType.getName()));
               break;
            }
         }
      }
      def.setQueryable(true);
      def.setQueryName(typeId);
      def.setVersionable(true);
      if (includePropertyDefinition)
      {
         addPropertyDefinitions(def, nt);
      }
      return def;
   }

   /**
    * Folder type definition.
    * 
    * @param nt node type
    * @param includePropertyDefinition true if need include property definition
    *        false otherwise
    * @return folder type definition
    */
   protected static TypeDefinition getFolderDefinition(NodeType nt, boolean includePropertyDefinition)
   {
      TypeDefinition def = new TypeDefinition();
      String localTypeName = nt.getName();
      String typeId = getCmisTypeId(localTypeName);
      def.setBaseId(BaseType.FOLDER);
      def.setControllableACL(true);
      def.setControllablePolicy(true);
      def.setCreatable(true);
      def.setDescription("Cmis Folder Type");
      def.setDisplayName(typeId);
      def.setFileable(true);
      def.setFulltextIndexed(false);
      def.setId(typeId);
      def.setIncludedInSupertypeQuery(true);
      def.setLocalName(localTypeName);
      def.setLocalNamespace(JcrCMIS.EXO_CMIS_NS_URI);
      if (typeId.equals(BaseType.FOLDER.value()))
      {
         def.setParentId(null); // no parents for root type
      }
      else
      {
         // Try determine parent type.
         NodeType[] superTypes = nt.getDeclaredSupertypes();
         for (NodeType superType : superTypes)
         {
            if (superType.isNodeType(JcrCMIS.NT_FOLDER))
            {
               // Take first type that is super for cmis:folder or is cmis:folder.
               def.setParentId(getCmisTypeId(superType.getName()));
               break;
            }
         }
      }
      def.setQueryable(true);
      def.setQueryName(typeId);
      if (includePropertyDefinition)
      {
         addPropertyDefinitions(def, nt);
      }
      return def;
   }

   /**
    * Get policy type definition.
    * 
    * @param nt node type
    * @param includePropertyDefinition true if need include property definition
    *        false otherwise
    * @return type policy definition
    */
   protected static TypeDefinition getPolicyDefinition(NodeType nt, boolean includePropertyDefinition)
   {
      TypeDefinition def = new TypeDefinition();
      String localTypeName = nt.getName();
      String typeId = getCmisTypeId(localTypeName);
      def.setBaseId(BaseType.POLICY);
      def.setControllableACL(true);
      def.setControllablePolicy(true);
      def.setCreatable(true);
      def.setDescription("Cmis Policy Type");
      def.setDisplayName(typeId);
      def.setFileable(false);
      def.setFulltextIndexed(false);
      def.setId(typeId);
      def.setIncludedInSupertypeQuery(false);
      def.setLocalName(localTypeName);
      def.setLocalNamespace(JcrCMIS.EXO_CMIS_NS_URI);
      if (typeId.equals(BaseType.POLICY.value()))
      {
         def.setParentId(null); // no parents for root type
      }
      else
      {
         // Try determine parent type.
         NodeType[] superTypes = nt.getDeclaredSupertypes();
         for (NodeType superType : superTypes)
         {
            if (superType.isNodeType(JcrCMIS.CMIS_NT_POLICY))
            {
               // Take first type that is super for cmis:policy or is cmis:policy.
               def.setParentId(getCmisTypeId(superType.getName()));
               break;
            }
         }
      }
      def.setQueryable(false);
      def.setQueryName(typeId);
      if (includePropertyDefinition)
      {
         addPropertyDefinitions(def, nt);
      }
      return def;
   }

   /**
    * Get relationship type definition.
    * 
    * @param nt node type
    * @param includePropertyDefinition true if need include property definition
    *        false otherwise
    * @return type relationship definition
    */
   protected static TypeDefinition getRelationshipDefinition(NodeType nt, boolean includePropertyDefinition)
   {
      TypeDefinition def = new TypeDefinition();
      String localTypeName = nt.getName();
      String typeId = getCmisTypeId(localTypeName);
      def.setBaseId(BaseType.RELATIONSHIP);
      def.setControllableACL(false);
      def.setControllablePolicy(false);
      def.setCreatable(true);
      def.setDescription("Cmis Relationship Type");
      def.setDisplayName(typeId);
      def.setFileable(false);
      def.setFulltextIndexed(false);
      def.setId(typeId);
      def.setIncludedInSupertypeQuery(false);
      def.setLocalName(localTypeName);
      def.setLocalNamespace(JcrCMIS.EXO_CMIS_NS_URI);
      if (typeId.equals(BaseType.RELATIONSHIP.value()))
      {
         def.setParentId(null); // no parents for root type
      }
      else
      {
         // Try determine parent type.
         NodeType[] superTypes = nt.getDeclaredSupertypes();
         for (NodeType superType : superTypes)
         {
            if (superType.isNodeType(JcrCMIS.CMIS_NT_RELATIONSHIP))
            {
               // Take first type that is super for cmis:relationship or is cmis:relationship.
               def.setParentId(getCmisTypeId(superType.getName()));
               break;
            }
         }
      }
      def.setQueryable(false);
      def.setQueryName(typeId);
      if (includePropertyDefinition)
      {
         addPropertyDefinitions(def, nt);
      }
      return def;
   }

   /**
    * Add property definitions.
    * 
    * @param typeDefinition the object type definition
    * @param nt the JCR node type.
    */
   private static void addPropertyDefinitions(TypeDefinition typeDefinition, NodeType nt)
   {
      // Known described in spec. property definitions
      //      for (PropertyDefinition<?> propDef : PropertyDefinitionsMap.getAll(typeDefinition.getBaseId().value()))
      //         typeDefinition.getPropertyDefinitions().add(propDef);

      Map<String, PropertyDefinition<?>> pd =
         new HashMap<String, PropertyDefinition<?>>(PropertyDefinitions.getAll(typeDefinition.getBaseId().value()));

      Set<String> knownIds = PropertyDefinitions.getPropertyIds(typeDefinition.getBaseId().value());

      final javax.jcr.nodetype.PropertyDefinition[] propertyDefinitions = nt.getPropertyDefinitions();
      //map for quick string properties lookup 
      Map<String, javax.jcr.nodetype.PropertyDefinition> propertyDefinitionsMap =
         new HashMap<String, javax.jcr.nodetype.PropertyDefinition>(propertyDefinitions.length);
      for (int i = 0; i < propertyDefinitions.length; i++)
      {
         propertyDefinitionsMap.put(propertyDefinitions[i].getName(), propertyDefinitions[i]);
      }

      for (javax.jcr.nodetype.PropertyDefinition jcrPropertyDef : propertyDefinitions)
      {
         String pdName = jcrPropertyDef.getName();
         // At the moment just hide JCR properties
         boolean shouldBeIgnored = false;
         for (Pattern ignoredPattern : ignoredProperties)
         {
            if (ignoredPattern.matcher(pdName).matches())
            {
               shouldBeIgnored = true;
               break;
            }
         }
         if (shouldBeIgnored)
         {
            continue;
         }
         // Do not process known properties
         if (!knownIds.contains(pdName))
         {
            PropertyDefinition<?> cmisPropDef = null;
            switch (jcrPropertyDef.getRequiredType())
            {

               case javax.jcr.PropertyType.BOOLEAN : {
                  Value[] jcrDefaultValues = jcrPropertyDef.getDefaultValues();
                  PropertyDefinition<Boolean> boolDef =
                     new PropertyDefinition<Boolean>(pdName, pdName, pdName, null, pdName, null, PropertyType.BOOLEAN,
                        jcrPropertyDef.isProtected() ? Updatability.READONLY : Updatability.READWRITE, false,
                        jcrPropertyDef.isMandatory(), true, true, null, jcrPropertyDef.isMultiple(), null,
                        jcrDefaultValues != null ? createDefaultValues(jcrDefaultValues,
                           new Boolean[jcrDefaultValues.length]) : null);

                  cmisPropDef = boolDef;
                  break;
               }

               case javax.jcr.PropertyType.DATE : {
                  Value[] jcrDefaultValues = jcrPropertyDef.getDefaultValues();
                  PropertyDefinition<Calendar> dateDef =
                     new PropertyDefinition<Calendar>(pdName, pdName, pdName, null, pdName, null,
                        PropertyType.DATETIME, jcrPropertyDef.isProtected() ? Updatability.READONLY
                           : Updatability.READWRITE, false, jcrPropertyDef.isMandatory(), true, true, null,
                        jcrPropertyDef.isMultiple(), null, jcrDefaultValues != null ? createDefaultValues(
                           jcrDefaultValues, new Calendar[jcrDefaultValues.length]) : null);

                  dateDef.setDateResolution(DateResolution.TIME);
                  cmisPropDef = dateDef;
                  break;
               }
               case javax.jcr.PropertyType.DOUBLE : {
                  Value[] jcrDefaultValues = jcrPropertyDef.getDefaultValues();
                  PropertyDefinition<BigDecimal> decimalDef =
                     new PropertyDefinition<BigDecimal>(pdName, pdName, pdName, null, pdName, null,
                        PropertyType.DECIMAL, jcrPropertyDef.isProtected() ? Updatability.READONLY
                           : Updatability.READWRITE, false, jcrPropertyDef.isMandatory(), true, true, null,
                        jcrPropertyDef.isMultiple(), null, jcrDefaultValues != null ? createDefaultValues(
                           jcrDefaultValues, new BigDecimal[jcrDefaultValues.length]) : null);

                  decimalDef.setDecimalPrecision(Precision.Bit32);
                  decimalDef.setMaxDecimal(CmisConstants.MAX_DECIMAL_VALUE);
                  decimalDef.setMinDecimal(CmisConstants.MIN_DECIMAL_VALUE);
                  cmisPropDef = decimalDef;
                  break;
               }

               case javax.jcr.PropertyType.LONG : {
                  Value[] jcrDefaultValues = jcrPropertyDef.getDefaultValues();
                  PropertyDefinition<BigInteger> integerDef =
                     new PropertyDefinition<BigInteger>(pdName, pdName, pdName, null, pdName, null,
                        PropertyType.INTEGER, jcrPropertyDef.isProtected() ? Updatability.READONLY
                           : Updatability.READWRITE, false, jcrPropertyDef.isMandatory(), true, true, null,
                        jcrPropertyDef.isMultiple(), null, jcrDefaultValues != null ? createDefaultValues(
                           jcrDefaultValues, new BigInteger[jcrDefaultValues.length]) : null);

                  integerDef.setMaxInteger(CmisConstants.MAX_INTEGER_VALUE);
                  integerDef.setMinInteger(CmisConstants.MIN_INTEGER_VALUE);
                  cmisPropDef = integerDef;
                  break;
               }

               case javax.jcr.PropertyType.NAME : // TODO
               case javax.jcr.PropertyType.REFERENCE :
               case javax.jcr.PropertyType.STRING :
               case javax.jcr.PropertyType.PATH :
               case javax.jcr.PropertyType.BINARY :
               case javax.jcr.PropertyType.UNDEFINED : {
                  Value[] jcrDefaultValues = jcrPropertyDef.getDefaultValues();
                  List<Choice<String>> choices = null;
                  Boolean openChoice = null;
                  if (javax.jcr.PropertyType.STRING == jcrPropertyDef.getRequiredType())
                  {
                     String[] vc = jcrPropertyDef.getValueConstraints();
                     if (vc != null && vc.length > 0)
                     {
                        openChoice = false;
                        choices = new ArrayList<Choice<String>>();
                        if (jcrPropertyDef.isMultiple())
                        {
                           List<String> vals = new ArrayList<String>();
                           for (String chVal : vc)
                           {
                              if (".*".equals(chVal))
                              {
                                 openChoice = true;
                                 continue;
                              }
                              vals.add(chVal);
                           }
                           choices.add(new Choice<String>(vals.toArray(new String[vals.size()]), ""));
                        }
                        else
                        {
                           for (String chVal : vc)
                           {
                              if (".*".equals(chVal))
                              {
                                 openChoice = true;
                                 continue;
                              }
                              choices.add(new Choice<String>(new String[]{chVal}, ""));
                           }
                        }
                     }
                  }
                  PropertyType propertyType = PropertyType.STRING;
                  //Let's find out the actual type
                  if (propertyDefinitionsMap.containsKey(pdName + StorageImpl.XCMIS_PROPERTY_TYPE))
                  {
                     try
                     {
                        final Value[] defaultValues =
                           propertyDefinitionsMap.get(pdName + StorageImpl.XCMIS_PROPERTY_TYPE).getDefaultValues();
                        if (defaultValues.length > 0)
                        {
                           propertyType = PropertyType.fromValue(defaultValues[0].getString());
                        }
                     }
                     catch (IllegalStateException e)
                     {
                        throw new CmisRuntimeException(e.getLocalizedMessage(), e);
                     }
                     catch (RepositoryException e)
                     {
                        throw new CmisRuntimeException(e.getLocalizedMessage(), e);
                     }
                  }
                  PropertyDefinition<String> stringDef =
                     new PropertyDefinition<String>(pdName, pdName, pdName, null, pdName, null, propertyType,
                        jcrPropertyDef.isProtected() ? Updatability.READONLY : Updatability.READWRITE, false,
                        jcrPropertyDef.isMandatory(), true, true, openChoice, jcrPropertyDef.isMultiple(), choices,
                        jcrDefaultValues != null ? createDefaultValues(jcrDefaultValues,
                           new String[jcrDefaultValues.length]) : null);
                  stringDef.setMaxLength(CmisConstants.MAX_STRING_LENGTH);
                  cmisPropDef = stringDef;
                  break;
               }

            }
            pd.put(cmisPropDef.getId(), cmisPropDef);
         }
      }

      typeDefinition.setPropertyDefinitions(pd);
   }

   private static <T> T[] createDefaultValues(Value[] jcrValues, T[] a)
   {
      try
      {
         Object[] tmp = new Object[jcrValues.length];
         for (int i = 0; i < jcrValues.length; i++)
         {
            Value v = jcrValues[i];
            switch (v.getType())
            {
               case javax.jcr.PropertyType.BOOLEAN :
                  tmp[i] = v.getBoolean();
                  break;
               case javax.jcr.PropertyType.DATE :
                  tmp[i] = v.getDate();
                  break;
               case javax.jcr.PropertyType.DOUBLE :
                  tmp[i] = BigDecimal.valueOf(v.getDouble());
                  break;
               case javax.jcr.PropertyType.LONG :
                  tmp[i] = BigInteger.valueOf(v.getLong());
                  break;
               case javax.jcr.PropertyType.NAME :
               case javax.jcr.PropertyType.REFERENCE :
               case javax.jcr.PropertyType.STRING :
               case javax.jcr.PropertyType.PATH :
               case javax.jcr.PropertyType.BINARY :
               case javax.jcr.PropertyType.UNDEFINED :
                  tmp[i] = v.getString();
                  break;
            }
         }
         System.arraycopy(tmp, 0, a, 0, tmp.length);
         return a;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property definition. " + re.getMessage(), re);
      }

   }

}
