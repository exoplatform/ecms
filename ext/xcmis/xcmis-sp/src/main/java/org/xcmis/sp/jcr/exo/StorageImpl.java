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

package org.xcmis.sp.jcr.exo;

import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeValue;
import org.exoplatform.services.jcr.core.nodetype.PropertyDefinitionValue;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.xcmis.sp.jcr.exo.index.IndexListener;
import org.xcmis.spi.BaseItemsIterator;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ConstraintException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.InvalidArgumentException;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.NotSupportedException;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.PermissionService;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.RelationshipData;
import org.xcmis.spi.RenditionManager;
import org.xcmis.spi.Storage;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.TypeNotFoundException;
import org.xcmis.spi.UpdateConflictException;
import org.xcmis.spi.VersioningException;
import org.xcmis.spi.model.ACLCapability;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.AccessControlPropagation;
import org.xcmis.spi.model.AllowableActions;
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.model.CapabilityACL;
import org.xcmis.spi.model.CapabilityChanges;
import org.xcmis.spi.model.CapabilityContentStreamUpdatable;
import org.xcmis.spi.model.CapabilityJoin;
import org.xcmis.spi.model.CapabilityQuery;
import org.xcmis.spi.model.CapabilityRendition;
import org.xcmis.spi.model.ChangeEvent;
import org.xcmis.spi.model.Permission;
import org.xcmis.spi.model.PermissionMapping;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.PropertyType;
import org.xcmis.spi.model.Rendition;
import org.xcmis.spi.model.RepositoryCapabilities;
import org.xcmis.spi.model.RepositoryInfo;
import org.xcmis.spi.model.SupportedPermissions;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.UnfileObject;
import org.xcmis.spi.model.Updatability;
import org.xcmis.spi.model.VersioningState;
import org.xcmis.spi.model.Permission.BasicPermissions;
import org.xcmis.spi.query.Query;
import org.xcmis.spi.query.Result;
import org.xcmis.spi.utils.CmisUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: StorageImpl.java 804 2010-04-16 16:48:59Z
 *          alexey.zavizionov@gmail.com $
 */
public class StorageImpl implements Storage
{

   private class DeleteTreeLog
   {
      private final List<String> deleteObjects = new ArrayList<String>();

      private final List<String> deleteLinks = new ArrayList<String>();

      private final Map<String, String> moveMapping = new HashMap<String, String>();

      public List<String> getDeleteLinks()
      {
         return deleteLinks;
      }

      public List<String> getDeleteObjects()
      {
         return deleteObjects;
      }

      public Map<String, String> getMoveMapping()
      {
         return moveMapping;
      }

   }

   private class DeleteTreeVisitor implements ItemVisitor
   {

      private final String treePath;

      private final UnfileObject unfileObject;

      private final DeleteTreeLog deleteLog = new DeleteTreeLog();

      DeleteTreeVisitor(String path, UnfileObject unfileObject)
      {
         this.treePath = path;
         this.unfileObject = unfileObject != null ? unfileObject : UnfileObject.DELETE;
      }

      public DeleteTreeLog getDeleteLog()
      {
         return deleteLog;
      }

      /**
       * {@inheritDoc}
       */
      public void visit(javax.jcr.Property property) throws RepositoryException
      {
      }

      /**
       * {@inheritDoc}
       */
      public void visit(Node node) throws RepositoryException
      {
         NodeType nt = node.getPrimaryNodeType();
         String uuid = ((ExtendedNode)node).getIdentifier();
         String path = node.getPath();

         if (nt.isNodeType(JcrCMIS.NT_FOLDER) || nt.isNodeType(JcrCMIS.NT_UNSTRUCTURED))
         {
            for (NodeIterator children = node.getNodes(); children.hasNext();)
            {
               children.nextNode().accept(this);
            }
            deleteLog.getDeleteObjects().add(uuid);
         }

         if (nt.isNodeType("nt:linkedFile"))
         {
            // Met link in tree. Simply remove all links in current tree.
            if (!deleteLog.getDeleteLinks().contains(uuid))
            {
               deleteLog.getDeleteLinks().add(uuid);
            }

            // Check target of link only if need delete all fileable objects.
            if (unfileObject == UnfileObject.DELETE)
            {
               Node doc = node.getProperty("jcr:content").getNode();
               String targetPath = doc.getPath();
               String targetUuid = ((ExtendedNode)doc).getIdentifier();
               if (!targetPath.startsWith(treePath) && !deleteLog.getDeleteObjects().contains(targetUuid))
               {
                  deleteLog.getDeleteObjects().add(targetUuid);
               }
               // Otherwise will met target of link in tree.
            }
         }
         else if (nt.isNodeType(JcrCMIS.NT_FILE))
         {
            String moveTo = null;

            // Check all link to current node.
            // Need to find at least one that is not in deleted tree. It can be
            // used as destination for unfiling document which has parent-folders
            // outside of the current folder tree. If no link out of current tree then
            // document will be moved to special store for unfiled objects.
            for (PropertyIterator references = node.getReferences(); references.hasNext();)
            {
               Node link = references.nextProperty().getParent();

               String linkPath = link.getPath();
               String linkUuid = ((ExtendedNode)link).getIdentifier();

               if ((unfileObject == UnfileObject.DELETE || linkPath.startsWith(treePath))
                  && !deleteLog.getDeleteLinks().contains(linkUuid))
               {
                  deleteLog.getDeleteLinks().add(linkUuid);
               }
               else if (!linkPath.startsWith(treePath) && moveTo == null)
               {
                  moveTo = linkPath;
               }
            }

            if ((unfileObject == UnfileObject.UNFILE || (unfileObject == UnfileObject.DELETESINGLEFILED && moveTo != null)))
            {
               deleteLog.getMoveMapping().put(path, moveTo);
            }
            else if (!deleteLog.getDeleteObjects().contains(uuid))
            {
               deleteLog.getDeleteObjects().add(uuid);
            }

            String workingCopyPath =
               StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_WORKING_COPIES + "/" + uuid;
            try
            {
               Node workingCopy = (Node)session.getItem(workingCopyPath);
               String wcuuid = ((ExtendedNode)workingCopy).getIdentifier();
               if (!deleteLog.getDeleteLinks().contains(wcuuid))
               {
                  deleteLog.getDeleteLinks().add(wcuuid);
               }
            }
            catch (PathNotFoundException pnfe)
            {
               // if working copy does not exists
            }
         }
      }
   }

   private class TreeVisitor implements ItemVisitor
   {

      private final Collection<String> allChildrenObjects = new HashSet<String>();

      TreeVisitor()
      {
      }

      public Collection<String> getDescendantsIds()
      {
         return allChildrenObjects;
      }

      /**
       * {@inheritDoc}
       */
      public void visit(javax.jcr.Property property) throws RepositoryException
      {
      }

      /**
       * {@inheritDoc}
       */
      public void visit(Node node) throws RepositoryException
      {
         NodeType nt = node.getPrimaryNodeType();
         String uuid = ((ExtendedNode)node).getIdentifier();

         if (nt.isNodeType(JcrCMIS.NT_FOLDER) || nt.isNodeType(JcrCMIS.NT_UNSTRUCTURED))
         {
            for (NodeIterator children = node.getNodes(); children.hasNext();)
            {
               children.nextNode().accept(this);
            }
            allChildrenObjects.add(uuid);
         }
         else
         {
            if (!allChildrenObjects.contains(uuid))
            {
               allChildrenObjects.add(uuid);
            }
         }
      }

   }

   /**
    * Iterator over set of object's renditions.
    */
   private class RenditionIterator implements ItemsIterator<Rendition>
   {

      /** Source NodeIterator. */
      protected final NodeIterator iter;

      /** Next rendition. */
      protected Rendition next;

      /**
       * Create RenditionIterator instance.
       *
       * @param iter the node iterator
       */
      public RenditionIterator(NodeIterator iter)
      {
         this.iter = iter;
         fetchNext();
      }

      /**
       * {@inheritDoc}
       */
      public boolean hasNext()
      {
         return next != null;
      }

      /**
       * {@inheritDoc}
       */
      public Rendition next()
      {
         if (next == null)
         {
            throw new NoSuchElementException();
         }
         Rendition n = next;
         fetchNext();
         return n;
      }

      /**
       * {@inheritDoc}
       */
      public void remove()
      {
         throw new UnsupportedOperationException("remove");
      }

      /**
       * {@inheritDoc}
       */
      public int size()
      {
         return -1;
      }

      /**
       * {@inheritDoc}
       */
      public void skip(int skip) throws NoSuchElementException
      {
         while (skip-- > 0)
         {
            fetchNext();
            if (next == null)
            {
               throw new NoSuchElementException();
            }
         }
      }

      /**
       * Fetching next rendition.
       */
      protected void fetchNext()
      {
         next = null;
         if (iter == null)
         {
            return;
         }
         while (next == null && iter.hasNext())
         {
            Node node = iter.nextNode();
            try
            {
               if (node.isNodeType(JcrCMIS.CMIS_NT_RENDITION))
               {
                  Rendition rendition = new Rendition();
                  rendition.setStreamId(node.getName());
                  rendition.setKind(node.getProperty(JcrCMIS.CMIS_RENDITION_KIND).getString());
                  rendition.setMimeType(node.getProperty(JcrCMIS.CMIS_RENDITION_MIME_TYPE).getString());
                  rendition.setLength(node.getProperty(JcrCMIS.CMIS_RENDITION_STREAM).getLength());
                  try
                  {
                     rendition.setHeight(Long.valueOf(node.getProperty(JcrCMIS.CMIS_RENDITION_HEIGHT).getLong())
                        .intValue());
                     rendition.setWidth(Long.valueOf(node.getProperty(JcrCMIS.CMIS_RENDITION_WIDTH).getLong())
                        .intValue());
                  }
                  catch (PathNotFoundException pnfe)
                  {
                     // Height & Width is optional
                  }
                  next = rendition;
               }
            }
            catch (javax.jcr.RepositoryException re)
            {
               String msg = "Unexpected error. Failed get next CMIS object. " + re.getMessage();
               LOG.warn(msg);
            }
         }
      }
   }

   private static final Log LOG = ExoLogger.getLogger(StorageImpl.class);

   public static final String XCMIS_SYSTEM_PATH = "/xcmis:system";

   public static final String XCMIS_UNFILED = "xcmis:unfileStore";

   public static final String XCMIS_WORKING_COPIES = "xcmis:workingCopyStore";

   public static final String XCMIS_RELATIONSHIPS = "xcmis:relationshipStore";

   public static final String XCMIS_POLICIES = "xcmis:policiesStore";

   public static final String XCMIS_PROPERTY_TYPE = "_xcmis_property_type";

   public static final Pattern XCMIS_PROPERTY_TYPE_PATTERN = Pattern.compile(".*" + StorageImpl.XCMIS_PROPERTY_TYPE);

   public static final String LATEST_LABEL = "latest";

   public static final String PWC_LABEL = "pwc";

   private static final String VENDOR_NAME = "eXo";

   private static final String PRODUCT_NAME = "xCMIS (eXo JCR SP)";

   private static final String PRODUCT_VERSION = "1.0";

   private static final String REPOSITORY_DESCRIPTION = "xCMIS (eXo JCR SP)";

   protected final Session session;

   /** The storage configuration. */
   private final StorageConfiguration configuration;

   /** The rendition manager. */
   private RenditionManager renditionManager;

   private IndexListener indexListener;

   private RepositoryInfo repositoryInfo;

   private PermissionService permissionService;

   public StorageImpl(Session session, IndexListener indexListener, StorageConfiguration configuration,
      RenditionManager renditionManager, PermissionService permissionService)
   {
      this.session = session;
      this.indexListener = indexListener;
      this.configuration = configuration;
      this.renditionManager = renditionManager;
      this.permissionService = permissionService;
   }

   public StorageImpl(Session session, StorageConfiguration configuration, PermissionService permissionService)
   {
      this.session = session;
      this.configuration = configuration;
      this.permissionService = permissionService;
   }

   public StorageImpl(Session session, StorageConfiguration configuration, RenditionManager renditionManager,
      PermissionService permissionService)
   {
      this.session = session;
      this.configuration = configuration;
      this.renditionManager = renditionManager;
      this.permissionService = permissionService;
   }

   /**
    * {@inheritDoc}
    */
   @SuppressWarnings("unchecked")
   public String addType(TypeDefinition type) throws ConstraintException, StorageException
   {
      try
      {
         ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager();

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
         declaredSupertypeNames.add(JcrTypeHelper.getNodeTypeName(parentId));
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

         NodeType nodeType = nodeTypeManager.registerNodeType(nodeTypeValue, ExtendedNodeTypeManager.FAIL_IF_EXISTS);

         return nodeType.getName();

      }
      catch (javax.jcr.RepositoryException re)
      {
         throw new StorageException("Unable add new CMIS type. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public AllowableActions calculateAllowableActions(ObjectData object)
   {
      ConversationState state = ConversationState.getCurrent();
      AllowableActions actions =
         permissionService.calculateAllowableActions(object, state != null ? state.getIdentity() : null,
            getRepositoryInfo());

      return actions;
   }

   /**
    * {@inheritDoc}
    */
   public DocumentData copyDocument(DocumentData source, FolderData parent, Map<String, Property<?>> properties,
      List<AccessControlEntry> acl, Collection<PolicyData> policies, VersioningState versioningState)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      try
      {
         String name = null;
         Property<?> nameProperty = properties.get(CmisConstants.NAME);
         if (nameProperty != null && nameProperty.getValues().size() > 0)
         {
            name = (String)nameProperty.getValues().get(0);
         }
         if (name == null || name.length() == 0)
         {
            name = source.getName();
         }

         TypeDefinition typeDefinition = source.getTypeDefinition();
         Node copyNode = null;
         if (parent != null)
         {
            Node parentNode = ((FolderDataImpl)parent).getNode();
            if (parentNode.hasNode(name))
            {
               throw new NameConstraintViolationException("Object with name " + name
                  + " already exists in specified folder.");
            }
            copyNode = parentNode.addNode(name, typeDefinition.getLocalName());
         }
         else
         {
            Node unfiledStore = (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_UNFILED);
            // wrapper around Document node with unique name.
            Node unfiled = unfiledStore.addNode(IdGenerator.generate(), "xcmis:unfiledObject");
            copyNode = unfiled.addNode(name, typeDefinition.getLocalName());
         }

         if (!copyNode.isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
         {
            copyNode.addMixin(JcrCMIS.CMIS_MIX_DOCUMENT);
         }
         if (copyNode.canAddMixin(JcrCMIS.MIX_VERSIONABLE))
         {
            copyNode.addMixin(JcrCMIS.MIX_VERSIONABLE);
         }

         JcrNodeEntry copyNodeEntry = new JcrNodeEntry(copyNode, typeDefinition);

         copyNodeEntry.setValue(CmisConstants.OBJECT_TYPE_ID, typeDefinition.getId());
         copyNodeEntry.setValue(CmisConstants.BASE_TYPE_ID, typeDefinition.getBaseId().value());
         String userId = session.getUserID();
         copyNodeEntry.setValue(CmisConstants.CREATED_BY, userId);
         Calendar cal = Calendar.getInstance();
         copyNodeEntry.setValue(CmisConstants.CREATION_DATE, cal);
         copyNodeEntry.setValue(CmisConstants.VERSION_SERIES_ID, copyNode.getProperty(JcrCMIS.JCR_VERSION_HISTORY)
            .getString());
         copyNodeEntry.setValue(CmisConstants.IS_LATEST_VERSION, true);
         copyNodeEntry.setValue(CmisConstants.IS_MAJOR_VERSION, versioningState == VersioningState.MAJOR);

         // TODO : support for checked-out initial state
         copyNodeEntry.setValue(CmisConstants.VERSION_LABEL, LATEST_LABEL);

         for (Property<?> property : properties.values())
         {
            PropertyDefinition<?> definition = typeDefinition.getPropertyDefinition(property.getId());
            Updatability updatability = definition.getUpdatability();
            if (updatability == Updatability.READWRITE || updatability == Updatability.ONCREATE)
            {
               copyNodeEntry.setProperty(property);
            }
         }

         try
         {
            copyNodeEntry.setContentStream(source.getContentStream());
         }
         catch (IOException ioe)
         {
            throw new CmisRuntimeException("Unable copy content for new document. " + ioe.getMessage(), ioe);
         }

         if (acl != null && acl.size() > 0)
         {
            copyNodeEntry.setACL(acl);
         }

         if (policies != null && policies.size() > 0)
         {
            for (PolicyData policy : policies)
            {
               copyNodeEntry.applyPolicy(policy);
            }
         }

         DocumentDataImpl copy = new DocumentDataImpl(copyNodeEntry, indexListener, renditionManager);
         copy.save();
         return copy;
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable to create a copy of document. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public DocumentData createDocument(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, ContentStream content, List<AccessControlEntry> acl,
      Collection<PolicyData> policies, VersioningState versioningState) throws ConstraintException,
      NameConstraintViolationException, IOException, StorageException
   {
      try
      {
         String name = null;
         Property<?> nameProperty = properties.get(CmisConstants.NAME);
         if (nameProperty != null && nameProperty.getValues().size() > 0)
         {
            name = (String)nameProperty.getValues().get(0);
         }
         if (name == null && content != null)
         {
            name = content.getFileName();
         }
         if (name == null || name.length() == 0)
         {
            throw new NameConstraintViolationException("Name for new document must be provided.");
         }

         Node documentNode = null;
         if (parent != null)
         {
            Node parentNode = ((FolderDataImpl)parent).getNode();
            if (parentNode.hasNode(name))
            {
               throw new NameConstraintViolationException("Object with name " + name
                  + " already exists in specified folder.");
            }
            documentNode = parentNode.addNode(name, typeDefinition.getLocalName());
         }
         else
         {
            Node unfiledStore = (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_UNFILED);
            // wrapper around Document node with unique name.
            Node unfiled = unfiledStore.addNode(IdGenerator.generate(), "xcmis:unfiledObject");
            documentNode = unfiled.addNode(name, typeDefinition.getLocalName());
         }

         if (!documentNode.isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
         {
            documentNode.addMixin(JcrCMIS.CMIS_MIX_DOCUMENT);
         }
         if (documentNode.canAddMixin(JcrCMIS.MIX_VERSIONABLE))
         {
            documentNode.addMixin(JcrCMIS.MIX_VERSIONABLE);
         }

         JcrNodeEntry documentNodeEntry = new JcrNodeEntry(documentNode, typeDefinition);

         documentNodeEntry.setValue(CmisConstants.OBJECT_TYPE_ID, typeDefinition.getId());
         documentNodeEntry.setValue(CmisConstants.BASE_TYPE_ID, typeDefinition.getBaseId().value());
         String userId = session.getUserID();
         documentNodeEntry.setValue(CmisConstants.CREATED_BY, userId);
         Calendar cal = Calendar.getInstance();
         documentNodeEntry.setValue(CmisConstants.CREATION_DATE, cal);
         documentNodeEntry.setValue(CmisConstants.VERSION_SERIES_ID, documentNode.getProperty(
            JcrCMIS.JCR_VERSION_HISTORY).getString());
         documentNodeEntry.setValue(CmisConstants.IS_LATEST_VERSION, true);
         documentNodeEntry.setValue(CmisConstants.IS_MAJOR_VERSION, versioningState == VersioningState.MAJOR);

         // TODO : support for checked-out initial state
         documentNodeEntry.setValue(CmisConstants.VERSION_LABEL, LATEST_LABEL);

         for (Property<?> property : properties.values())
         {
            PropertyDefinition<?> definition = typeDefinition.getPropertyDefinition(property.getId());
            Updatability updatability = definition.getUpdatability();
            if (updatability == Updatability.READWRITE || updatability == Updatability.ONCREATE)
            {
               documentNodeEntry.setProperty(property);
            }
         }

         documentNodeEntry.setContentStream(content);

         if (acl != null && acl.size() > 0)
         {
            documentNodeEntry.setACL(acl);
         }

         if (policies != null && policies.size() > 0)
         {
            for (PolicyData policy : policies)
            {
               documentNodeEntry.applyPolicy(policy);
            }
         }

         DocumentDataImpl document = new DocumentDataImpl(documentNodeEntry, indexListener, renditionManager);
         document.save();
         return document;
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable to create Document. " + re.getMessage(), re);
      }

   }

   /**
    * {@inheritDoc}
    */
   public FolderData createFolder(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      if (parent == null)
      {
         throw new ConstraintException("Parent folder must be provided.");
      }

      try
      {
         String name = null;
         Property<?> nameProperty = properties.get(CmisConstants.NAME);
         if (nameProperty != null && nameProperty.getValues().size() > 0)
         {
            name = (String)nameProperty.getValues().get(0);
         }
         if (name == null || name.length() == 0)
         {
            throw new NameConstraintViolationException("Name for new folder must be provided.");
         }

         Node parentNode = ((FolderDataImpl)parent).getNode();
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

         JcrNodeEntry folderNodeEntry = new JcrNodeEntry(folderNode, typeDefinition);

         folderNodeEntry.setValue(CmisConstants.OBJECT_TYPE_ID, typeDefinition.getId());
         folderNodeEntry.setValue(CmisConstants.BASE_TYPE_ID, typeDefinition.getBaseId().value());
         String userId = session.getUserID();
         folderNodeEntry.setValue(CmisConstants.CREATED_BY, userId);
         Calendar cal = Calendar.getInstance();
         folderNodeEntry.setValue(CmisConstants.CREATION_DATE, cal);

         for (Property<?> property : properties.values())
         {
            PropertyDefinition<?> definition = typeDefinition.getPropertyDefinition(property.getId());
            Updatability updatability = definition.getUpdatability();
            if (updatability == Updatability.READWRITE || updatability == Updatability.ONCREATE)
            {
               folderNodeEntry.setProperty(property);
            }
         }

         if (acl != null && acl.size() > 0)
         {
            folderNodeEntry.setACL(acl);
         }

         if (policies != null && policies.size() > 0)
         {
            for (PolicyData policy : policies)
            {
               folderNodeEntry.applyPolicy(policy);
            }
         }

         FolderDataImpl folder = new FolderDataImpl(folderNodeEntry, indexListener, renditionManager);
         folder.save();
         return folder;
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable to create Folder. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public PolicyData createPolicy(FolderData parent, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws ConstraintException, NameConstraintViolationException, StorageException
   {
      // TODO : need raise exception if parent folder is provided ??
      // Do not use parent folder, policy is not fileable.
      try
      {
         String name = null;
         Property<?> nameProperty = properties.get(CmisConstants.NAME);
         if (nameProperty != null && nameProperty.getValues().size() > 0)
         {
            name = (String)nameProperty.getValues().get(0);
         }
         if (name == null || name.length() == 0)
         {
            throw new NameConstraintViolationException("Name for new policy must be provided.");
         }

         Node policiesStore = (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_POLICIES);
         if (policiesStore.hasNode(name))
         {
            throw new NameConstraintViolationException("Policy with name " + name + " already exists.");
         }

         Node policyNode = policiesStore.addNode(name, typeDefinition.getLocalName());

         JcrNodeEntry policyNodeEntry = new JcrNodeEntry(policyNode, typeDefinition);

         policyNodeEntry.setValue(CmisConstants.OBJECT_TYPE_ID, typeDefinition.getId());
         policyNodeEntry.setValue(CmisConstants.BASE_TYPE_ID, typeDefinition.getBaseId().value());
         String userId = session.getUserID();
         policyNodeEntry.setValue(CmisConstants.CREATED_BY, userId);
         Calendar cal = Calendar.getInstance();
         policyNodeEntry.setValue(CmisConstants.CREATION_DATE, cal);

         for (Property<?> property : properties.values())
         {
            PropertyDefinition<?> definition = typeDefinition.getPropertyDefinition(property.getId());
            Updatability updatability = definition.getUpdatability();
            if (updatability == Updatability.READWRITE || updatability == Updatability.ONCREATE)
            {
               policyNodeEntry.setProperty(property);
            }
         }

         if (acl != null && acl.size() > 0)
         {
            policyNodeEntry.setACL(acl);
         }

         if (policies != null && policies.size() > 0)
         {
            for (PolicyData policy : policies)
            {
               policyNodeEntry.applyPolicy(policy);
            }
         }

         PolicyDataImpl policy = new PolicyDataImpl(policyNodeEntry, indexListener);
         policy.save();
         return policy;
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable create new policy. " + re.getMessage(), re);
      }

   }

   /**
    * {@inheritDoc}
    */
   public RelationshipData createRelationship(ObjectData source, ObjectData target, TypeDefinition typeDefinition,
      Map<String, Property<?>> properties, List<AccessControlEntry> acl, Collection<PolicyData> policies)
      throws NameConstraintViolationException, StorageException
   {
      try
      {
         String name = null;
         Property<?> nameProperty = properties.get(CmisConstants.NAME);
         if (nameProperty != null)
         {
            name = (String)nameProperty.getValues().get(0);
         }
         if (name == null || name.length() == 0)
         {
            throw new NameConstraintViolationException("Name for new relationship must be provided.");
         }

         Node relationships =
            (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_RELATIONSHIPS);
         if (relationships.hasNode(name))
         {
            throw new NameConstraintViolationException("Relationship with name " + name + " already exists.");
         }

         Node relationshipNode = relationships.addNode(name, typeDefinition.getLocalName());
         relationshipNode.setProperty(CmisConstants.SOURCE_ID, ((BaseObjectData)source).getNode());
         relationshipNode.setProperty(CmisConstants.TARGET_ID, ((BaseObjectData)target).getNode());

         JcrNodeEntry relationshipNodeEntry = new JcrNodeEntry(relationshipNode);

         relationshipNodeEntry.setValue(CmisConstants.OBJECT_TYPE_ID, typeDefinition.getId());
         relationshipNodeEntry.setValue(CmisConstants.BASE_TYPE_ID, typeDefinition.getBaseId().value());
         String userId = session.getUserID();
         relationshipNodeEntry.setValue(CmisConstants.CREATED_BY, userId);
         Calendar cal = Calendar.getInstance();
         relationshipNodeEntry.setValue(CmisConstants.CREATION_DATE, cal);

         for (Property<?> property : properties.values())
         {
            PropertyDefinition<?> definition = typeDefinition.getPropertyDefinition(property.getId());
            Updatability updatability = definition.getUpdatability();
            if (updatability == Updatability.READWRITE || updatability == Updatability.ONCREATE)
            {
               relationshipNodeEntry.setProperty(property);
            }
         }

         if (acl != null && acl.size() > 0)
         {
            relationshipNodeEntry.setACL(acl);
         }

         if (policies != null && policies.size() > 0)
         {
            for (PolicyData policy : policies)
            {
               relationshipNodeEntry.applyPolicy(policy);
            }
         }

         RelationshipDataImpl relationship = new RelationshipDataImpl(relationshipNodeEntry, indexListener);
         relationship.save();
         return relationship;
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable create new policy. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void deleteObject(ObjectData object, boolean deleteAllVersions) throws UpdateConflictException,
      VersioningException, StorageException
   {
      if (object.getBaseType() == BaseType.DOCUMENT)
      {
         // Throw exception to avoid unexpected removing data.
         // Any way at the moment we are not able remove 'base version' of
         // versionable node, so have not common behavior.
         if (object.getTypeDefinition().isVersionable() && !deleteAllVersions)
         {
            throw new VersioningException("Unable delete only specified version.");
         }
      }

      //      String objectId = object.getObjectId();

      ((BaseObjectData)object).delete();
      //
      //      if (indexListener != null)
      //      {
      //         Set<String> removed = new HashSet<String>();
      //         removed.add(objectId);
      //         indexListener.removed(removed);
      //      }
   }

   /**
    * {@inheritDoc}
    */
   public Collection<String> deleteTree(FolderData folder, boolean deleteAllVersions, UnfileObject unfileObject,
      boolean continueOnFailure) throws UpdateConflictException
   {
      if (!deleteAllVersions)
      {
         // Throw exception to avoid unexpected removing data.
         // Any way at the moment we are not able remove 'base version' of
         // versionable node, so have not common behavior.
         throw new CmisRuntimeException("Unable delete only specified version.");
      }

      Collection<String> failedToDelete = new ArrayList<String>();

      try
      {
         DeleteTreeVisitor v = new DeleteTreeVisitor(folder.getPath(), unfileObject);
         v.visit(((FolderDataImpl)folder).getNode());

         DeleteTreeLog deleteLog = v.getDeleteLog();

         for (String id : deleteLog.getDeleteLinks())
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug("Delete link " + id);
            }
            ((ExtendedSession)session).getNodeByIdentifier(id).remove();
         }

         for (Map.Entry<String, String> e : deleteLog.getMoveMapping().entrySet())
         {
            String scrPath = e.getKey();
            String destPath = e.getValue();

            if (destPath == null)
            {
               // No found links outside of current tree, then will move node in
               // special store for unfiled objects.
               ExtendedNode unfiledStore = (ExtendedNode)session.getItem(XCMIS_SYSTEM_PATH + "/" + XCMIS_UNFILED);
               ExtendedNode src = (ExtendedNode)session.getItem(scrPath);
               Node unfiled = unfiledStore.addNode(src.getIdentifier(), "xcmis:unfiledObject");
               destPath = unfiled.getPath() + "/" + src.getName();
            }
            else
            {
               // Remove link, it will be replaced by real node.
               session.getItem(destPath).remove();
            }

            if (LOG.isDebugEnabled())
            {
               LOG.debug("Move " + scrPath + " to " + destPath);
            }
            session.move(scrPath, destPath);
         }

         for (String e : deleteLog.getDeleteObjects())
         {
            if (LOG.isDebugEnabled())
            {
               LOG.debug("Delete: " + e);
            }
            ((ExtendedSession)session).getNodeByIdentifier(e).remove();
         }

         session.save();

         // FIXME : need update indexer for moved objects
         if (indexListener != null)
         {
            indexListener.removed(new HashSet<String>(deleteLog.getDeleteObjects()));
         }
      }
      catch (RepositoryException re)
      {
         try
         {
            TreeVisitor v = new TreeVisitor();
            v.visit(((FolderDataImpl)folder).getNode());
            failedToDelete.addAll(v.getDescendantsIds());
         }
         catch (RepositoryException e)
         {
            throw new CmisRuntimeException(re.getMessage(), re);
         }
      }
      return failedToDelete;
   }

   /**
    * {@inheritDoc}
    */
   public Collection<DocumentData> getAllVersions(String versionSeriesId) throws ObjectNotFoundException
   {
      try
      {
         Node node = ((ExtendedSession)session).getNodeByIdentifier(versionSeriesId);
         VersionHistory vh = ((VersionHistory)node);
         LinkedList<DocumentData> versions = new LinkedList<DocumentData>();
         VersionIterator iterator = vh.getAllVersions();
         iterator.next(); // skip jcr:rootVersion
         while (iterator.hasNext())
         {
            Version v = iterator.nextVersion();
            versions.addFirst(getDocumentVersion(v.getNode(JcrCMIS.JCR_FROZEN_NODE)));
         }
         DocumentData latest = (DocumentData)getObjectById(vh.getVersionableUUID());
         versions.addFirst(latest);
         String pwcId = latest.getVersionSeriesCheckedOutId();
         if (pwcId != null)
         {
            PWC pwc = (PWC)getObjectById(pwcId);
            versions.addFirst(pwc);
         }
         return versions;
      }
      catch (ItemNotFoundException infe)
      {
         throw new ObjectNotFoundException("Version series '" + versionSeriesId + "' does not exist.");
      }
      catch (javax.jcr.RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get version series " + versionSeriesId + ". " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<ChangeEvent> getChangeLog(String changeLogToken) throws ConstraintException
   {
      throw new NotSupportedException("Changes log feature is not supported.");
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<DocumentData> getCheckedOutDocuments(FolderData folder, String orderBy)
   {
      try
      {
         Node workingCopies =
            (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_WORKING_COPIES);

         List<DocumentData> checkedOut = new ArrayList<DocumentData>();

         for (NodeIterator iterator = workingCopies.getNodes(); iterator.hasNext();)
         {
            Node wc = iterator.nextNode();
            if (!wc.hasNodes())
            {
               // Must not happen.
               LOG.error("PWC node not fould.");
               continue;
            }
            Node node = wc.getNodes().nextNode();
            PWC pwc = getPWC(node);
            if (folder != null)
            {
               for (FolderData parent : pwc.getParents())
               {
                  if (parent.getObjectId().equals(folder.getObjectId()))
                  {
                     checkedOut.add(pwc);
                     break;
                  }
               }
            }
            else
            {
               checkedOut.add(pwc);
            }
         }
         return new BaseItemsIterator<DocumentData>(checkedOut);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get checked-out documents. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public String getId()
   {
      return configuration.getId();
   }

   /**
    * @return the indexListener
    */
   public IndexListener getIndexListener()
   {
      return indexListener;
   }

   /**
    * {@inheritDoc}
    */
   public ObjectData getObjectById(String objectId) throws ObjectNotFoundException
   {
      if (objectId == null)
      {
         throw new CmisRuntimeException("Object id may not be null.");
      }
      try
      {
         Node node = ((ExtendedSession)session).getNodeByIdentifier(objectId);
         if (node.isNodeType(JcrCMIS.NT_FROZEN_NODE))
         {
            return getDocumentVersion(node);
         }
         return getObject(node);
      }
      catch (ItemNotFoundException nfe)
      {
         throw new ObjectNotFoundException("Object '" + objectId + "' does not exist.");
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException(re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ObjectData getObjectByPath(String path) throws ObjectNotFoundException
   {
      if (path == null)
      {
         throw new CmisRuntimeException("Object path may not be null.");
      }
      try
      {
         Item item = session.getItem(path);
         if (!item.isNode())
         {
            throw new ObjectNotFoundException("Object '" + path + "' does not exist.");
         }
         Node node = (Node)item;
         return getObject(node);
      }
      catch (ItemNotFoundException nfe)
      {
         throw new ObjectNotFoundException("Object  '" + path + "' does not exist.");
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException(re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<Rendition> getRenditions(ObjectData object)
   {
      try
      {
         RenditionIterator it = new RenditionIterator(((BaseObjectData)object).getNode().getNodes());
         if (it.hasNext())
         {
            // if renditions persisted then use it.
            // Observation listener must be configured.
            return it;
         }
         else
         {
            if (renditionManager != null)
            {
               return renditionManager.getRenditions(object);
            }
         }
      }
      catch (javax.jcr.RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get renditions for object " + object.getObjectId()
            + ". Unexpected error " + re.getMessage(), re);
      }
      return CmisUtils.emptyItemsIterator();
   }

   /**
    * {@inheritDoc}
    */
   public RepositoryInfo getRepositoryInfo()
   {
      if (repositoryInfo == null)
      {
         PermissionMapping permissionMapping = new PermissionMapping();

         permissionMapping.put(PermissionMapping.CAN_GET_DESCENDENTS_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_FOLDER_TREE_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_CHILDREN_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_OBJECT_PARENTS_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_FOLDER_PARENT_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_CREATE_DOCUMENT_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_CREATE_FOLDER_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_CREATE_RELATIONSHIP_SOURCE, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_CREATE_RELATIONSHIP_TARGET, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_PROPERTIES_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_CONTENT_STREAM_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_RENDITIONS_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_UPDATE_PROPERTIES_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_MOVE_OBJECT_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_MOVE_OBJECT_TARGET, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_MOVE_OBJECT_SOURCE, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_DELETE_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_DELETE_TREE_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_SET_CONTENT_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_DELETE_CONTENT_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_ADD_TO_FOLDER_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_ADD_TO_FOLDER_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_REMOVE_OBJECT_FROM_FOLDER_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_REMOVE_OBJECT_FROM_FOLDER_FOLDER, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_CHECKOUT_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_CANCEL_CHECKOUT_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_CHECKIN_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_ALL_VERSIONS_DOCUMENT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_OBJECT_RELATIONSHIPS_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_ADD_POLICY_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_ADD_POLICY_POLICY, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_REMOVE_POLICY_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_WRITE.value()));
         permissionMapping.put(PermissionMapping.CAN_REMOVE_POLICY_POLICY, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_APPLIED_POLICIES_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_GET_ACL_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value()));
         permissionMapping.put(PermissionMapping.CAN_APPLY_ACL_OBJECT, //
            Arrays.asList(BasicPermissions.CMIS_READ.value(), BasicPermissions.CMIS_WRITE.value()));

         List<Permission> permissions = new ArrayList<Permission>(4);
         for (BasicPermissions b : BasicPermissions.values())
         {
            permissions.add(new Permission(b.value(), ""));
         }

         repositoryInfo =
            new RepositoryInfo(getId(), getId(), JcrCMIS.ROOT_FOLDER_ID, CmisConstants.SUPPORTED_VERSION,
               new RepositoryCapabilities(CapabilityACL.MANAGE, CapabilityChanges.NONE,
                  CapabilityContentStreamUpdatable.ANYTIME, CapabilityJoin.NONE, CapabilityQuery.BOTHCOMBINED,
                  CapabilityRendition.READ, false, true, true, true, false, true, true, false), new ACLCapability(
                  permissionMapping, Collections.unmodifiableList(permissions), AccessControlPropagation.OBJECTONLY,
                  SupportedPermissions.BASIC), SystemIdentity.ANONIM, SystemIdentity.ANY, null, null, true,
               REPOSITORY_DESCRIPTION, VENDOR_NAME, PRODUCT_NAME, PRODUCT_VERSION, null);
      }

      // TODO update latestChangeLogToken when ChangeLogToken feature will be implemented
      return repositoryInfo;
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
            String nodeTypeName = JcrTypeHelper.getNodeTypeName(typeId);
            NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
            try
            {
               nodeTypeManager.getNodeType(nodeTypeName);
            }
            catch (NoSuchNodeTypeException nsne)
            {
               throw new TypeNotFoundException("Type " + typeId + " is not supported.");
            }

            for (NodeTypeIterator iter = nodeTypeManager.getPrimaryNodeTypes(); iter.hasNext();)
            {
               NodeType nt = iter.nextNodeType();
               // Get only direct children of specified type.
               if (nt.isNodeType(nodeTypeName) && getTypeLevelHierarchy(nt, nodeTypeName) == 1)
               {
                  types.add(JcrTypeHelper.getTypeDefinition(nt, includePropertyDefinitions));
               }
            }
         }
         return new BaseItemsIterator<TypeDefinition>(types);
      }
      catch (javax.jcr.RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get type children. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public TypeDefinition getTypeDefinition(String typeId, boolean includePropertyDefinition)
      throws TypeNotFoundException, CmisRuntimeException
   {
      try
      {
         return JcrTypeHelper.getTypeDefinition(getNodeType(JcrTypeHelper.getNodeTypeName(typeId)),
            includePropertyDefinition);
      }
      catch (NoSuchNodeTypeException e)
      {
         throw new TypeNotFoundException("Type with id " + typeId + " not found in repository.");
      }
      catch (javax.jcr.RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get object type " + typeId, re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Iterator<String> getUnfiledObjectsId() throws StorageException
   {
      try
      {
         Node unfiledStore = (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_UNFILED);
         final NodeIterator nodes = unfiledStore.getNodes();

         return new Iterator<String>()
         {

            public boolean hasNext()
            {
               return nodes.hasNext();
            }

            public String next()
            {
               if (nodes.hasNext())
               {
                  //get wrapper
                  Node nextNode = nodes.nextNode();
                  NodeIterator etries;
                  try
                  {
                     etries = nextNode.getNodes();
                     if (etries.hasNext())
                     {
                        return ((ExtendedNode)etries.nextNode()).getIdentifier();
                     }
                     throw new CmisRuntimeException("Unfiled node object not found for wrapper object "
                        + nextNode.getPath());
                  }
                  catch (RepositoryException e)
                  {
                     throw new CmisRuntimeException(e.getLocalizedMessage(), e);
                  }
               }
               return null;
            }

            public void remove()
            {
               throw new UnsupportedOperationException();

            }
         };

      }
      catch (RepositoryException e)
      {
         throw new StorageException("Unable unfiled objects. " + e.getMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ObjectData moveObject(ObjectData object, FolderData target, FolderData source)
      throws UpdateConflictException, VersioningException, NameConstraintViolationException, StorageException
   {
      try
      {
         String objectPath = ((BaseObjectData)object).getNode().getPath();
         String destinationPath = ((BaseObjectData)target).getNode().getPath();
         destinationPath += destinationPath.equals("/") ? object.getName() : ("/" + object.getName());
         session.getWorkspace().move(objectPath, destinationPath);
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Object moved in " + destinationPath);
         }
         return getObject((Node)session.getItem(destinationPath));
      }
      catch (ItemExistsException ie)
      {
         throw new NameConstraintViolationException("Object with the same name already exists in target folder.");
      }
      catch (javax.jcr.RepositoryException re)
      {
         throw new StorageException("Unable to move object. " + re.getMessage(), re);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ItemsIterator<Result> query(Query query) throws InvalidArgumentException
   {
      // will be overridden
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    */
   public void removeType(String typeId) throws TypeNotFoundException, StorageException, CmisRuntimeException
   {
      // Throws exceptions if type with specified 'typeId' does not exist or is unsupported by CMIS.
      getTypeDefinition(typeId, false);
      try
      {
         ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager)session.getWorkspace().getNodeTypeManager();
         nodeTypeManager.unregisterNodeType(typeId);
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable remove CMIS type " + typeId + ". " + re.getMessage(), re);
      }
   }

   /**
    * @param indexListener the indexListener to set
    */
   public void setIndexListener(IndexListener indexListener)
   {
      this.indexListener = indexListener;
   }

   /**
    * {@inheritDoc}
    */
   public void unfileObject(ObjectData object)
   {
      ((DocumentDataImpl)object).unfile();
   }

   private DocumentVersion getDocumentVersion(Node node) throws RepositoryException
   {
      TypeDefinition typeDefinition =
         JcrTypeHelper.getTypeDefinition(getNodeType(node.getProperty(JcrCMIS.JCR_FROZEN_PRIMARY_TYPE).getString()),
            true);

      return new DocumentVersion(new JcrNodeEntry(node, typeDefinition), indexListener, renditionManager);
   }

   private PWC getPWC(Node node) throws RepositoryException
   {
      return new PWC(new JcrNodeEntry(node), indexListener, renditionManager);
   }

   private ObjectData getObject(Node node) throws RepositoryException
   {
      TypeDefinition typeDefinition = JcrTypeHelper.getTypeDefinition(node.getPrimaryNodeType(), true);

      if (typeDefinition.getBaseId() == BaseType.DOCUMENT)
      {
         if (!node.isCheckedOut())
         {
            node.checkout();
         }
         if (node.getParent().isNodeType("xcmis:workingCopy"))
         {
            return getPWC(node);
         }
         if (!node.isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
         {
            return new JcrFile(new JcrNodeEntry(node, typeDefinition), indexListener, renditionManager);
         }
         return new DocumentDataImpl(new JcrNodeEntry(node, typeDefinition), indexListener, renditionManager);
      }
      else if (typeDefinition.getBaseId() == BaseType.FOLDER)
      {
         if (!node.isNodeType(JcrCMIS.CMIS_MIX_FOLDER))
         {
            return new JcrFolder(new JcrNodeEntry(node, typeDefinition), indexListener, renditionManager);
         }
         return new FolderDataImpl(new JcrNodeEntry(node, typeDefinition), indexListener, renditionManager);
      }
      else if (typeDefinition.getBaseId() == BaseType.POLICY)
      {
         return new PolicyDataImpl(new JcrNodeEntry(node, typeDefinition), indexListener);
      }
      else if (typeDefinition.getBaseId() == BaseType.RELATIONSHIP)
      {
         return new RelationshipDataImpl(new JcrNodeEntry(node, typeDefinition), indexListener);
      }

      // Must never happen.
      throw new CmisRuntimeException("Unknown base type. ");
   }

   /**
    * Get the level of hierarchy.
    *
    * @param discovered the node type
    * @param match the name of the node type
    * @return hierarchical level for node type
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
    * Create String representation of date in format required by JCR.
    *
    * @param c Calendar
    * @return formated string date
    */
   // TODO : Add in common utils ??
   protected String createJcrDate(Calendar c)
   {
      return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%03dZ", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c
         .get(Calendar.DAY_OF_MONTH), c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c
         .get(Calendar.MILLISECOND));
   }

   protected NodeType getNodeType(String name) throws NoSuchNodeTypeException, javax.jcr.RepositoryException
   {
      NodeType nt = session.getWorkspace().getNodeTypeManager().getNodeType(name);
      return nt;
   }
}
