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

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.xcmis.spi.BaseItemsIterator;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.InvalidArgumentException;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.TypeManager;
import org.xcmis.spi.TypeNotFoundException;
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.model.Choice;
import org.xcmis.spi.model.ContentStreamAllowed;
import org.xcmis.spi.model.DateResolution;
import org.xcmis.spi.model.Precision;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.PropertyType;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.Updatability;
import org.xcmis.spi.model.VersioningState;
import org.xcmis.spi.utils.CmisUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
abstract class BaseJcrStorage implements TypeManager
{

   private static final Log LOG = ExoLogger.getExoLogger(BaseJcrStorage.class.getName());

   public static final String XCMIS_PROPERTY_TYPE = "_xcmis_property_type";

   public static final Pattern XCMIS_PROPERTY_TYPE_PATTERN = Pattern.compile(".*" + StorageImpl.XCMIS_PROPERTY_TYPE);

   public static final String XCMIS_SYSTEM_PATH = "/xcmis:system";

   public static final String XCMIS_WORKING_COPIES = "xcmis:workingCopyStore";

   public static final String XCMIS_RELATIONSHIPS = "xcmis:relationshipStore";

   public static final String XCMIS_POLICIES = "xcmis:policiesStore";

   public static final String LATEST_LABEL = "latest";

   public static final String PWC_LABEL = "pwc";

   public static final String VENDOR_NAME = "eXo";

   public static final String PRODUCT_NAME = "xCMIS (eXo SP)";

   public static final String PRODUCT_VERSION = "1.1";

   public static final String REPOSITORY_DESCRIPTION = "xCMIS (eXo SP)";

   static final Set<Pattern> IGNORED_PROPERTIES = new HashSet<Pattern>();
   static
   {
      IGNORED_PROPERTIES.add(Pattern.compile("jcr:created"));
      IGNORED_PROPERTIES.add(Pattern.compile("jcr:mixinTypes"));
      IGNORED_PROPERTIES.add(Pattern.compile("jcr:uuid"));
      IGNORED_PROPERTIES.add(Pattern.compile("jcr:primaryType"));
      IGNORED_PROPERTIES.add(Pattern.compile("exo:owner"));
      IGNORED_PROPERTIES.add(Pattern.compile("\\*"));
      IGNORED_PROPERTIES.add(XCMIS_PROPERTY_TYPE_PATTERN);
   }

   protected StorageConfiguration storageConfiguration;

   protected Map<String, TypeMapping> nodeTypeMapping;

   protected final String rootPath;

  public BaseJcrStorage(StorageConfiguration storageConfiguration, Map<String, TypeMapping> nodeTypeMapping) {
    this.storageConfiguration = storageConfiguration;
    this.nodeTypeMapping = nodeTypeMapping;
    String rootNodePath = storageConfiguration.getRootNodePath();
    if (rootNodePath.contains("${userId}")) {
      // process root path template
      String userPath = null;
      try {
        // get user node path
        userPath = WCMCoreUtils.getService(NodeHierarchyCreator.class)
                               .getUserNode(SessionProvider.createSystemProvider(),
                                            getSession().getUserID())
                               .getPath();
      } catch (Exception e) {
        // nothing
      }
      if (userPath != null) {
        // add to the path the /Private or /Public path segment from the template
        StringBuffer sb = new StringBuffer();
        sb.append(userPath).append(rootNodePath.substring(rootNodePath.indexOf("${userId}") + "${userId}".length()));
        userPath = sb.toString();

        storageConfiguration.setRootNodePath(userPath);
        rootNodePath = userPath;
      }
    }
    this.rootPath = rootNodePath;
  }

   // ================= TypeManager =================

   /**
    * @return the storageConfiguration
    */
   public StorageConfiguration getStorageConfiguration()
   {
      return storageConfiguration;
   }

   /**
    * {@inheritDoc}
    */
   @SuppressWarnings("unchecked")
   public String addType(TypeDefinition type) throws ConstraintException, StorageException
   {
      try
      {
         NodeTypeValue nodeTypeValue = new NodeTypeValue();
         String parentId = type.getParentId();
         if (parentId == null)
         {
            String msg = "Unable add root type. Parent Type Id must be specified.";
            throw new InvalidArgumentException(msg);
         }

         TypeDefinition parentType = null;
         try
         {
            // May throw exception if parent type is unknown or unsupported.
            parentType = getTypeDefinition(parentId, false);
         }
         catch (TypeNotFoundException tnfe)
         {
            throw new ConstraintException("Parent type " + parentId + " does not exist");
         }

         List<String> declaredSupertypeNames = new ArrayList<String>();
         declaredSupertypeNames.add(parentType.getLocalName());
         if (parentType.getBaseId() == BaseType.DOCUMENT)
         {
            declaredSupertypeNames.add(JcrCMIS.CMIS_MIX_DOCUMENT);
         }
         else if (parentType.getBaseId() == BaseType.FOLDER)
         {
            declaredSupertypeNames.add(JcrCMIS.CMIS_MIX_FOLDER);
         }

         nodeTypeValue.setDeclaredSupertypeNames(declaredSupertypeNames);
         nodeTypeValue.setMixin(false);
         nodeTypeValue.setName(type.getId());
         nodeTypeValue.setOrderableChild(false);
         nodeTypeValue.setPrimaryItemName("");

         List<PropertyDefinitionValue> jcrPropDefintions = null;

         if (type.getPropertyDefinitions() != null && type.getPropertyDefinitions().size() > 0)
         {
            jcrPropDefintions = new ArrayList<PropertyDefinitionValue>();

            for (PropertyDefinition<?> propDef : type.getPropertyDefinitions())
            {
               if (propDef.getPropertyType() == null)
               {
                  String msg = "Property Type required.";
                  throw new InvalidArgumentException(msg);
               }

               if (parentType.getPropertyDefinition(propDef.getId()) != null)
               {
                  throw new InvalidArgumentException("Property " + propDef.getId() + " already defined");
               }

               if (XCMIS_PROPERTY_TYPE_PATTERN.matcher(propDef.getId()).matches())
               {
                  throw new InvalidArgumentException("Unacceptable property definition name " + propDef.getId()
                     + " type " + type.getId());
               }

               PropertyDefinitionValue jcrPropDef = new PropertyDefinitionValue();

               List<String> defaultValues = null;

               //calculate default values
               switch (propDef.getPropertyType())
               {
                  case BOOLEAN :
                     jcrPropDef.setRequiredType(javax.jcr.PropertyType.BOOLEAN);
                     Boolean[] booleans = ((PropertyDefinition<Boolean>)propDef).getDefaultValue();
                     if (booleans != null && booleans.length > 0)
                     {
                        defaultValues = new ArrayList<String>(booleans.length);
                        for (Boolean v : booleans)
                        {
                           defaultValues.add(v.toString());
                        }
                     }
                     break;

                  case DATETIME :
                     jcrPropDef.setRequiredType(javax.jcr.PropertyType.DATE);
                     Calendar[] dates = ((PropertyDefinition<Calendar>)propDef).getDefaultValue();
                     if (dates != null && dates.length > 0)
                     {
                        defaultValues = new ArrayList<String>(dates.length);
                        for (Calendar v : dates)
                        {
                           defaultValues.add(createJcrDate(v));
                        }
                     }
                     break;

                  case DECIMAL :
                     jcrPropDef.setRequiredType(javax.jcr.PropertyType.DOUBLE);
                     BigDecimal[] decimals = ((PropertyDefinition<BigDecimal>)propDef).getDefaultValue();
                     if (decimals != null && decimals.length > 0)
                     {
                        defaultValues = new ArrayList<String>(decimals.length);
                        for (BigDecimal v : decimals)
                        {
                           defaultValues.add(Double.toString(v.doubleValue()));
                        }
                     }
                     break;

                  case INTEGER :
                     jcrPropDef.setRequiredType(javax.jcr.PropertyType.LONG);
                     BigInteger[] ints = ((PropertyDefinition<BigInteger>)propDef).getDefaultValue();
                     if (ints != null && ints.length > 0)
                     {
                        defaultValues = new ArrayList<String>(ints.length);
                        for (BigInteger v : ints)
                        {
                           defaultValues.add(Long.toString(v.longValue()));
                        }
                     }
                     break;

                  case ID :
                  case HTML :
                  case URI :
                  case STRING :
                     jcrPropDef.setRequiredType(javax.jcr.PropertyType.STRING);
                     String[] str = ((PropertyDefinition<String>)propDef).getDefaultValue();
                     if (str != null && str.length > 0)
                     {
                        defaultValues = new ArrayList<String>(str.length);
                        for (String v : str)
                        {
                           defaultValues.add(v);
                        }
                     }
                     break;
               }

               if (defaultValues != null)
               {
                  jcrPropDef.setDefaultValueStrings(defaultValues);
                  jcrPropDef.setAutoCreate(true);
               }
               else
               {
                  jcrPropDef.setAutoCreate(false);
               }

               jcrPropDef.setMandatory(propDef.isRequired());
               jcrPropDef.setMultiple(propDef.isMultivalued());
               jcrPropDef.setName(propDef.getId());
               jcrPropDef.setOnVersion(OnParentVersionAction.COPY);

               //               jcrPropDef.setReadOnly(propDef.getUpdatability() != null
               //                  && propDef.getUpdatability() == Updatability.READONLY);

               // TODO May not set read-only for property definition at JCR level.
               // In this case can't update property through JCR API.
               jcrPropDef.setReadOnly(false);

               jcrPropDefintions.add(jcrPropDef);

               // TODO replace with native types in JCR 2.x
               // add type definition for ID, HTML, URI
               if (propDef.getPropertyType() == PropertyType.ID || propDef.getPropertyType() == PropertyType.HTML
                  || propDef.getPropertyType() == PropertyType.URI)
               {
                  List<String> actualTypeStorage = new ArrayList<String>();
                  actualTypeStorage.add(propDef.getPropertyType().toString());
                  jcrPropDefintions.add(new PropertyDefinitionValue(propDef.getId() + XCMIS_PROPERTY_TYPE, false,
                     false, OnParentVersionAction.COPY, false, actualTypeStorage, false, 0, new ArrayList<String>()));
               }

            }

            nodeTypeValue.setDeclaredPropertyDefinitionValues(jcrPropDefintions);
         }

         ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager)getSession().getWorkspace().getNodeTypeManager();
         NodeType nodeType = nodeTypeManager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);
         return nodeType.getName();
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable add new CMIS type. " + re.getMessage(), re);
      }
   }

   /**
    * Create String representation of date in format required by JCR.
    *
    * @param c Calendar
    * @return formated string date
    */
   private String createJcrDate(Calendar c)
   {
      return CmisUtils.convertToString(c);
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<TypeDefinition> getTypeChildren(String typeId, boolean includePropertyDefinitions)
      throws TypeNotFoundException
   {
      try
      {
         List<TypeDefinition> types = new ArrayList<TypeDefinition>();
         if (typeId == null)
         {
            for (String t : new String[]{"cmis:document", "cmis:folder", "cmis:policy", "cmis:relationship"})
            {
               types.add(getTypeDefinition(t, includePropertyDefinitions));
            }
         }
         else
         {
            TypeDefinition typeDefinition = getTypeDefinition(typeId, includePropertyDefinitions);
            String nodeTypeName = typeDefinition.getLocalName();
            NodeTypeManager nodeTypeManager = getSession().getWorkspace().getNodeTypeManager();
            for (NodeTypeIterator iter = nodeTypeManager.getPrimaryNodeTypes(); iter.hasNext();)
            {
               NodeType nt = iter.nextNodeType();
               TypeMapping mapping = getTypeMapping(nt);
               // Get only direct children of specified type.
               if (mapping == null && nt.isNodeType(nodeTypeName) && getTypeLevelHierarchy(nt, nodeTypeName) == 1)
               {
                  types.add(getTypeDefinition(nt, includePropertyDefinitions));
               }
               else if (mapping != null && mapping.getParentType().equals(typeId))
               {
                  types.add(getTypeDefinition(mapping, nt, includePropertyDefinitions));
               }
            }
         }
         return new BaseItemsIterator<TypeDefinition>(types);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get type children. " + re.getMessage(), re);
      }
   }

   /**
    * Get the level of hierarchy.
    *
    * @param discovered the node type
    * @param match the name of the node type
    * @return hierarchical level for node type <code>discovered</code>
    */
   private int getTypeLevelHierarchy(NodeType discovered, String match)
   {
      // determine level of hierarchy
      int level = 0;
      for (NodeType sup : discovered.getSupertypes())
      {
         if (sup.isNodeType(match))
         {
            level++;
         }
      }
      return level;
   }

   /**
    * {@inheritDoc}
    */
   public TypeDefinition getTypeDefinition(String typeId, boolean includePropertyDefinition)
      throws TypeNotFoundException
   {
      typeId = getNodeType(typeId);
      try
      {
         NodeType nodeType = getSession().getWorkspace().getNodeTypeManager().getNodeType(typeId);
         return getTypeDefinition(nodeType, includePropertyDefinition);
      }
      catch (NoSuchNodeTypeException e)
      {
         throw new TypeNotFoundException("Type with id " + typeId + " not found in repository.");
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get object type " + typeId, re);
      }
   }

   /**
    * Get JCR node type name reflected CMIS type <code>typeId</code>. This is
    * reverse operation to {@link #getCmisTypeId(String)}.
    *
    * @param cmisTypeId CMIS type
    * @return corresponded JCR node type name
    */
   private String getNodeType(String cmisTypeId)
   {
      if (cmisTypeId.equals(CmisConstants.DOCUMENT))
      {
         return JcrCMIS.NT_FILE;
      }
      else if (cmisTypeId.equals(CmisConstants.FOLDER))
      {
         return JcrCMIS.NT_FOLDER;
      }
      return cmisTypeId;
   }

   /**
    * {@inheritDoc}
    */
   public void removeType(String typeId) throws ConstraintException, TypeNotFoundException, StorageException
   {
      // Throws exceptions if type with specified 'typeId' does not exist or is unsupported by CMIS.
      TypeDefinition typeDefinition = getTypeDefinition(typeId, false);
      if (typeDefinition.getParentId() == null)
      {
         throw new ConstraintException("Unable remove root type " + typeId);
      }
      try
      {
         ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager)getSession().getWorkspace().getNodeTypeManager();
         nodeTypeManager.unregisterNodeType(typeDefinition.getLocalName());
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable remove CMIS type " + typeId + ". " + re.getMessage(), re);
      }
   }

   // ===============================================

   /**
    * Get all sub-types of type <code>typeId</code>.
    *
    * @param typeId type Id
    * @param includePropertyDefinitions if <code>true</code> property definition
    *        should be included
    * @return set of type definitions which are sub-type of specified type. If
    *         type has not any sub-types that empty collection will be returned.
    * @throws TypeNotFoundException if type <code>typeId</code> does not exist
    */
   public Collection<TypeDefinition> getSubTypes(String typeId, boolean includePropertyDefinitions)
      throws TypeNotFoundException
   {
      List<TypeDefinition> subTypes = new ArrayList<TypeDefinition>();
      for (ItemsIterator<TypeDefinition> children = getTypeChildren(typeId, includePropertyDefinitions); children
         .hasNext();)
      {
         TypeDefinition type = children.next();
         subTypes.add(type);
         Collection<TypeDefinition> cchildren = getSubTypes(type.getId(), includePropertyDefinitions);
         if (cchildren.size() > 0)
         {
            subTypes.addAll(cchildren);
         }
      }
      return subTypes;
   }

   /**
    * Get CMIS type definition which is front-end of JCR node type.
    *
    * @param nodeType JCR node type
    * @param includePropertyDefinition if true property definition should be
    *        included
    * @return CMIS type definition
    */
   public TypeDefinition getTypeDefinition(NodeType nodeType, boolean includePropertyDefinition)
   {
      TypeMapping mapping = getTypeMapping(nodeType);
      return getTypeDefinition(mapping, nodeType, includePropertyDefinition);
   }

   private TypeDefinition getTypeDefinition(TypeMapping mapping, NodeType nodeType, boolean includePropertyDefinition)
   {
      if (nodeType.isNodeType(JcrCMIS.NT_FILE))
      {
         // Mapping not supported for documents at the moment.
         // Need pluggable mechanism for reading content first.
         // Before this no sense to use any mapping.
         return getDocumentDefinition(nodeType, includePropertyDefinition);
      }
      else if (nodeType.isNodeType(JcrCMIS.NT_FOLDER) || mapping != null && mapping.getBaseType() == BaseType.FOLDER)
      {
         return getFolderDefinition(nodeType, includePropertyDefinition, mapping);
      }
      else if (nodeType.isNodeType(JcrCMIS.CMIS_NT_RELATIONSHIP))
      {
         return getRelationshipDefinition(nodeType, includePropertyDefinition);
      }
      else if (nodeType.isNodeType(JcrCMIS.CMIS_NT_POLICY))
      {
         return getPolicyDefinition(nodeType, includePropertyDefinition);
      }
      throw new NotSupportedNodeTypeException("Type " + nodeType.getName() + " is unsupported for xCMIS.");
   }

   /**
    * Document type definition.
    *
    * @param nt node type
    * @param includePropertyDefinition true if need include property definition
    *        false otherwise
    * @return document type definition
    */
   private TypeDefinition getDocumentDefinition(NodeType nt, boolean includePropertyDefinition)
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

      if (typeId.equals(CmisConstants.DOCUMENT))
      {
         def.setParentId(null); // no parents for root type
      }
      else if (nt.isNodeType(JcrCMIS.NT_FILE))
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
    * @param typeMapping TypeMapping
    * @return folder type definition
    */
   private TypeDefinition getFolderDefinition(NodeType nt, boolean includePropertyDefinition, TypeMapping typeMapping)
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
      if (typeId.equals(CmisConstants.FOLDER))
      {
         def.setParentId(null); // no parents for root type
      }
      else if (nt.isNodeType(JcrCMIS.NT_FOLDER))
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
      else
      {
         // Since reach this method node type is supported BUT with mapping.
         // Node type is not nt:folder and does not extend nt:folder.
         def.setParentId(typeMapping.getParentType());
      }
      def.setQueryable(true);
      def.setQueryName(typeId);
      if (includePropertyDefinition)
      {
         addPropertyDefinitions(def, nt);
      }
      return def;
   }

   TypeMapping getTypeMapping(NodeType nodeType)
   {
      if (nodeTypeMapping == null)
      {
         return null;
      }
      String nodeTypeName = nodeType.getName();
      TypeMapping tm = nodeTypeMapping.get(nodeTypeName);
      if (tm == null)
      {
         NodeType[] supertypes = nodeType.getSupertypes();
         for (int i = 0; i < supertypes.length && tm == null; i++)
         {
            tm = nodeTypeMapping.get(supertypes[i].getName());
         }
      }
      return tm;
   }

   /**
    * Get policy type definition.
    *
    * @param nt node type
    * @param includePropertyDefinition true if need include property definition
    *        false otherwise
    * @return type policy definition
    */
   private TypeDefinition getPolicyDefinition(NodeType nt, boolean includePropertyDefinition)
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
   private TypeDefinition getRelationshipDefinition(NodeType nt, boolean includePropertyDefinition)
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
    * Get CMIS object type id by the JCR node type name.
    *
    * @param ntName JCR node type name
    * @return CMIS object type id
    */
   private String getCmisTypeId(String ntName)
   {
      if (ntName.equals(JcrCMIS.NT_FILE))
      {
         // nt:file consider as root type of all documents and mapped to cmis:document type.
         return CmisConstants.DOCUMENT;
      }
      if (ntName.equals(JcrCMIS.NT_FOLDER))
      {
         // nt:folder consider as root type of all folders and mapped to cmis:folder type.
         return CmisConstants.FOLDER;
      }
      // Do nothing about other types!
      return ntName;
   }

   /**
    * Add property definitions.
    *
    * @param typeDefinition the object type definition
    * @param nt the JCR node type.
    */
   private void addPropertyDefinitions(TypeDefinition typeDefinition, NodeType nt)
   {
      // Known described in spec. property definitions
      Map<String, PropertyDefinition<?>> pd =
         new HashMap<String, PropertyDefinition<?>>(PropertyDefinitions.getAll(typeDefinition.getBaseId().value()));

      Set<String> knownIds = PropertyDefinitions.getPropertyIds(typeDefinition.getBaseId().value());

      final javax.jcr.nodetype.PropertyDefinition[] propertyDefinitions = nt.getPropertyDefinitions();
      //map for quick string properties lookup
      Map<String, javax.jcr.nodetype.PropertyDefinition> propertyDefinitionsMap =
         new HashMap<String, javax.jcr.nodetype.PropertyDefinition>(propertyDefinitions.length);
      for (javax.jcr.nodetype.PropertyDefinition propertyDefinition : propertyDefinitions)
      {
         propertyDefinitionsMap.put(propertyDefinition.getName(), propertyDefinition);
      }

      for (javax.jcr.nodetype.PropertyDefinition jcrPropertyDef : propertyDefinitions)
      {
         String pdName = jcrPropertyDef.getName();
         // At the moment just hide JCR properties
         boolean shouldBeIgnored = false;
         for (Pattern ignoredPattern : IGNORED_PROPERTIES)
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

               case javax.jcr.PropertyType.NAME :
               case javax.jcr.PropertyType.REFERENCE :
               case javax.jcr.PropertyType.STRING :
               case javax.jcr.PropertyType.PATH : {
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
               default :
                  // If binary or undefined.
                  continue;
            }
            pd.put(cmisPropDef.getId(), cmisPropDef);
         }
      }

      typeDefinition.setPropertyDefinitions(pd);
   }

   private <T> T[] createDefaultValues(Value[] jcrValues, T[] a)
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

   public JcrNodeEntry createDocumentEntry(JcrNodeEntry parent, String name, TypeDefinition typeDefinition,
      VersioningState versioningState) throws NameConstraintViolationException, StorageException
   {
      try
      {
         Node parentNode = parent.getNode();
         if (parentNode.hasNode(name))
         {
            throw new NameConstraintViolationException("Object with name " + name
               + " already exists in specified folder.");
         }
         Node document = parentNode.addNode(name, typeDefinition.getLocalName());
         if (!document.isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
         {
            document.addMixin(JcrCMIS.CMIS_MIX_DOCUMENT);
         }
         if (document.canAddMixin(JcrCMIS.MIX_VERSIONABLE))
         {
            document.addMixin(JcrCMIS.MIX_VERSIONABLE);
         }
         return fromNode(document);
      }
      catch (ObjectNotFoundException onfe)
      {
         throw new StorageException(onfe.getMessage(), onfe);
      }
      catch (RepositoryException re)
      {
         throw new StorageException(re.getMessage(), re);
      }
   }

   public JcrNodeEntry createFolderEntry(JcrNodeEntry parent, String name, TypeDefinition typeDefinition)
      throws NameConstraintViolationException, StorageException
   {
      try
      {
         Node parentNode = parent.getNode();
         if (parentNode.hasNode(name))
         {
            throw new NameConstraintViolationException("Object with name " + name
               + " already exists in specified folder.");
         }
         Node folderNode = parentNode.addNode(name, typeDefinition.getLocalName());
         if (!folderNode.isNodeType(JcrCMIS.CMIS_MIX_FOLDER))
         {
            folderNode.addMixin(JcrCMIS.CMIS_MIX_FOLDER);
         }
         return fromNode(folderNode);
      }
      catch (ObjectNotFoundException onfe)
      {
         throw new StorageException(onfe.getMessage(), onfe);
      }
      catch (RepositoryException re)
      {
         throw new StorageException(re.getMessage(), re);
      }
   }

   public JcrNodeEntry createPolicyEntry(String name, TypeDefinition typeDefinition)
      throws NameConstraintViolationException, StorageException
   {
      try
      {
         Node policiesStore = (Node)getSession().getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_POLICIES);
         if (policiesStore.hasNode(name))
         {
            throw new NameConstraintViolationException("Policy with name " + name + " already exists.");
         }
         Node policyNode = policiesStore.addNode(name, typeDefinition.getLocalName());
         return fromNode(policyNode);
      }
      catch (ObjectNotFoundException onfe)
      {
         throw new StorageException(onfe.getMessage(), onfe);
      }
      catch (RepositoryException re)
      {
         throw new StorageException(re.getMessage(), re);
      }
   }

   public JcrNodeEntry createRelationshipEntry(String name, TypeDefinition typeDefinition, JcrNodeEntry source,
      JcrNodeEntry target) throws NameConstraintViolationException, StorageException
   {
      try
      {
         Node relationships =
            (Node)getSession().getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_RELATIONSHIPS);
         if (relationships.hasNode(name))
         {
            throw new NameConstraintViolationException("Relationship with name " + name + " already exists.");
         }
         Node relationshipNode = relationships.addNode(name, typeDefinition.getLocalName());
         relationshipNode.setProperty(CmisConstants.SOURCE_ID, source.getNode());
         relationshipNode.setProperty(CmisConstants.TARGET_ID, target.getNode());
         return fromNode(relationshipNode);
      }
      catch (ObjectNotFoundException onfe)
      {
         throw new StorageException(onfe.getMessage(), onfe);
      }
      catch (RepositoryException re)
      {
         throw new StorageException(re.getMessage(), re);
      }
   }

  public JcrNodeEntry getEntry(String id) throws ObjectNotFoundException {
    if (id == null) {
      throw new CmisRuntimeException("Id may not be null.");
    }
    String internal;
    String v = null;

    if (id.contains(JcrCMIS.ID_SEPARATOR)) {
      String[] tmp = id.split(JcrCMIS.ID_SEPARATOR);
      internal = tmp[0];
      v = tmp[1];
    } else {
      internal = id;
    }
    try {
      Node node = ((ExtendedSession) getSession()).getNodeByIdentifier(internal);
      if (node.isNodeType(JcrCMIS.NT_VERSION_HISTORY) && v != null) {
        VersionHistory vh = (VersionHistory) node;
        try {
          node = vh.getVersion(v).getNode(JcrCMIS.JCR_FROZEN_NODE);
        } catch (VersionException ve) {

          int lastVersion = -1;
          try {
            long allVersionSize = vh.getAllVersions().getSize();
            Version lastVersionNode = vh.getVersion(String.valueOf(allVersionSize));
            String propertyObjectId = lastVersionNode.getNode(JcrCMIS.JCR_FROZEN_NODE)
                                                     .getProperty(JcrCMIS.OBJECT_ID)
                                                     .getString();
            lastVersion = Integer.parseInt(propertyObjectId.split(JcrCMIS.ID_SEPARATOR)[1]) + 1;
            // it works since last version cannot be deleted
          } catch (RepositoryException e) {
            // There are no version with provided name (the size of version
            // history with no deleted nodes) in the version history
            lastVersion = Integer.parseInt(String.valueOf(vh.getAllVersions().getSize()));
          }

          if ("1".equals(v) || lastVersion == Integer.parseInt(v)) {
            node = ((ExtendedSession) getSession()).getNodeByIdentifier(vh.getVersionableUUID());
          } else {
            throw new ObjectNotFoundException("Object '" + id + "' does not exist.");
          }
        }
      }
      return fromNode(node);
    } catch (NotSupportedNodeTypeException unsupp) {
      throw new ObjectNotFoundException("Object '" + id + "' does not exist. "
          + unsupp.getMessage());
    } catch (ItemNotFoundException nfe) {
      throw new ObjectNotFoundException("Object '" + id + "' does not exist.");
    } catch (RepositoryException re) {
      throw new CmisRuntimeException(re.getMessage(), re);
    }
  }

  public JcrNodeEntry fromNode(Node node) throws ObjectNotFoundException {
    try {
      // Need for set checkedOut state after WebDAV
      if (!node.isCheckedOut()) {
        node.checkout();
      }
      if (node.isNodeType("exo:symlink")) {
        Node link = node;
        try {
          node = ((ExtendedSession) getSession()).getNodeByIdentifier(node.getProperty("exo:uuid")
                                                                     .getString());
        } catch (ItemNotFoundException e) {
          throw new ObjectNotFoundException("Target of exo:symlink " + link.getPath()
              + " is not exist any more. ");
        }
        return new SymLinkNodeEntry(link, node, this);
      } else if (node.isNodeType(JcrCMIS.JCR_XCMIS_LINKEDFILE)) {
        javax.jcr.Property propertyWithId = null;
        for (PropertyIterator iter = node.getProperties(); iter.hasNext() && propertyWithId == null;) {
          javax.jcr.Property nextProperty = iter.nextProperty();
          // iterate while don't get the property with CMIS Object Id in the
          // name.
          // xcmis:linkedFile extends nt:base which has two properties by
          // default: jcr:primaryType and jcr:mixinTypes
          if (!nextProperty.getName().equalsIgnoreCase(JcrCMIS.JCR_PRIMARYTYPE)
              && !nextProperty.getName().equalsIgnoreCase(JcrCMIS.JCR_MIXINTYPES)) {
            propertyWithId = nextProperty;
          }
        }
        Node target = propertyWithId.getNode();

        return new JcrNodeEntry(target.getPath(), target.getSession().getWorkspace().getName(), this);
      }
      return new JcrNodeEntry(node.getPath(), node.getSession().getWorkspace().getName(), this);
    } catch (RepositoryException re) {
      throw new CmisRuntimeException(re.getMessage(), re);
    }
  }

   public String getJcrRootPath()
   {
      return rootPath;
   }

   public boolean isSupportedNodeType(String nodeTypeName)
   {
      try
      {
         NodeType nodeType = getSession().getWorkspace().getNodeTypeManager().getNodeType(nodeTypeName);
         return nodeType.isNodeType(JcrCMIS.NT_FILE) || nodeType.isNodeType(JcrCMIS.NT_FOLDER)
            || nodeType.isNodeType(JcrCMIS.CMIS_NT_POLICY) || nodeType.isNodeType(JcrCMIS.CMIS_NT_RELATIONSHIP)
            || getTypeMapping(nodeType) != null;
      }
      catch (NoSuchNodeTypeException e)
      {
        if (LOG.isErrorEnabled()) {
          LOG.error(e.getMessage(), e);
        }
      }
      catch (RepositoryException e)
      {
        if (LOG.isErrorEnabled()) {
          LOG.error(e.getMessage(), e);
        }
      }
      return false;
   }

   /**
    * @return the nodeTypeMapping
    */
   public Map<String, TypeMapping> getNodeTypeMapping()
   {
      return nodeTypeMapping;
   }
   
   public Session getSession() throws RepositoryException {
     return WCMCoreUtils.getUserSessionProvider().getSession(storageConfiguration.getWorkspace(), WCMCoreUtils.getRepository());
   }
}
