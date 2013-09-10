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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.value.BooleanValue;
import org.exoplatform.services.jcr.impl.core.value.DateValue;
import org.exoplatform.services.jcr.impl.core.value.DoubleValue;
import org.exoplatform.services.jcr.impl.core.value.LongValue;
import org.exoplatform.services.jcr.impl.core.value.StringValue;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.xcmis.spi.BaseContentStream;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.LazyIterator;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.ObjectNotFoundException;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.TypeNotFoundException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.model.Permission.BasicPermissions;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.PropertyType;
import org.xcmis.spi.model.RelationshipDirection;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.impl.BooleanProperty;
import org.xcmis.spi.model.impl.DateTimeProperty;
import org.xcmis.spi.model.impl.DecimalProperty;
import org.xcmis.spi.model.impl.HtmlProperty;
import org.xcmis.spi.model.impl.IdProperty;
import org.xcmis.spi.model.impl.IntegerProperty;
import org.xcmis.spi.model.impl.StringProperty;
import org.xcmis.spi.model.impl.UriProperty;
import org.xcmis.spi.utils.MimeType;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
class JcrNodeEntry
{

  private class ChildrenIterator extends LazyIterator<JcrNodeEntry> {
    private final NodeIterator iter;

    public ChildrenIterator(NodeIterator iter) {
      this.iter = iter;
      fetchNext();
    }

    @Override
    protected void fetchNext() {
      next = null;
      while (next == null && iter.hasNext()) {
        Node fetchNode = iter.nextNode();
        try {
          if (SKIP_CHILD_ITEMS.contains(getNode().getName())) {
            continue;
          }

          if (!((NodeImpl) fetchNode).isValid()) {
            continue; // TODO temporary. Be sure it fixed in JCR back-end.
          }

          if (getNode().isNodeType(JcrCMIS.JCR_XCMIS_LINKEDFILE)) {
            javax.jcr.Property propertyWithId = null;
            for (PropertyIterator iterPro = getNode().getProperties(); iterPro.hasNext()
                && propertyWithId == null;) {
              javax.jcr.Property nextProperty = iterPro.nextProperty();
              // iterate while don't get the property with CMIS Object Id in the
              // name.
              // xcmis:linkedFile extends nt:base which has two properties by
              // default: jcr:primaryType and jcr:mixinTypes
              if (!nextProperty.getName().equalsIgnoreCase(JcrCMIS.JCR_PRIMARYTYPE)
                  && !nextProperty.getName().equalsIgnoreCase(JcrCMIS.JCR_MIXINTYPES)) {
                propertyWithId = nextProperty;
              }
            }
            fetchNode = propertyWithId.getNode();
            try {
              next = storage.fromNode(fetchNode);
            } catch (ObjectNotFoundException e) {
              continue;
            }
          } else if (getNode().isNodeType("exo:symlink")) {
            try {
              // May be sub-types of exo:symlink
              next = storage.fromNode(fetchNode);
            } catch (ObjectNotFoundException e) {
              continue;
            }
          } else {
            try {
              next = storage.fromNode(fetchNode);
            } catch (ObjectNotFoundException e) {
              continue;
            }
          }
        } catch (NotSupportedNodeTypeException iae) {
          if (LOG.isDebugEnabled()) {
            // Show only in debug mode. It may cause a lot of warn when
            // unsupported by xCMIS nodes met.
            LOG.debug("Unable get next object . " + iae.getMessage());
          }
        } catch (javax.jcr.RepositoryException re) {
          if (LOG.isWarnEnabled()) {
            LOG.warn("Unexpected error. Failed get next CMIS object. " + re.getMessage());
          }
        }
      }
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
      // Size is unknown since may met nodes with unsupported node type.
      return -1;
    }
  }

   static final Set<String> SKIP_CHILD_ITEMS = new HashSet<String>();

   static
   {
      SKIP_CHILD_ITEMS.add("jcr:system");
      SKIP_CHILD_ITEMS.add("xcmis:system");
   }

   private static final Log LOG = ExoLogger.getLogger(JcrNodeEntry.class.getName());

   protected final TypeDefinition type;

   protected BaseJcrStorage storage;

   private String id = null;
   
   protected String path;
   
   protected String workspace;

   /**
    * @param path back-end JCR path
    * @param workspace The workspace
    * @param storage CMIS storage
    * @throws RepositoryException if any JCR repository error occurs
    * @see Node#getPrimaryNodeType()
    * @see Workspace#getNodeTypeManager()
    * @see RepositoryException
    */
   JcrNodeEntry(String path, String workspace,BaseJcrStorage storage) throws RepositoryException
   {
      this.path = path;
      this.workspace = workspace;
      NodeType nodeType = null;
      if (getNode().isNodeType(JcrCMIS.NT_FROZEN_NODE))
      {
         nodeType =
            getNode().getSession().getWorkspace().getNodeTypeManager()
               .getNodeType(getNode().getProperty(JcrCMIS.JCR_FROZEN_PRIMARY_TYPE).getString());
      }
      else
      {
         nodeType = getNode().getPrimaryNodeType();
      }
      this.type = storage.getTypeDefinition(nodeType, true);
      this.storage = storage;
   }

   String getId()
   {
      if (id != null)
         return id;

      if (StorageImpl.PWC_LABEL.equalsIgnoreCase(getString(CmisConstants.VERSION_LABEL))) {
         try {
            id = ((ExtendedNode)getNode()).getIdentifier();
         } catch (RepositoryException e) {
            throw new CmisRuntimeException("Unable get objects's id ." + e.getMessage(), e);
         }
      } else {
         // not PWC
         id = getString(CmisConstants.OBJECT_ID);
      }

      if (id == null)
      {
         // if not PWC and not Document (with stored objectId)
         try {
             id = ((ExtendedNode)getNode()).getIdentifier();
          } catch (RepositoryException e) {
             throw new CmisRuntimeException("Unable get objects's id ." + e.getMessage(), e);
          }
      }
      return id;
   }

   String getName()
   {
      try
      {
         return getNode().getName();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable object's name. " + re.getMessage(), re);
      }
   }

   void setName(String name) throws NameConstraintViolationException
   {
      if (name == null || name.length() == 0)
      {
         throw new NameConstraintViolationException("Name can't be null or empty string.");
      }
      if (name.equals(getName()))
      {
         return;
      }
      try
      {
         if (getNode().getParent().hasNode(name))
         {
            throw new NameConstraintViolationException("Object with name " + name + " already exists.");
         }
         if (name != null)
         {
            Session session = getNode().getSession();
            String srcPath = path();
            String destPath = srcPath.substring(0, srcPath.lastIndexOf('/') + 1) + name;
            session.move(srcPath, destPath);
            path = destPath;
            // 'cmis:name' is not stored as property. This is virtual property in xcmis-jcr.
         }
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set object name. " + re.getMessage(), re);
      }
   }

   TypeDefinition getType()
   {
      return type;
   }

   BaseType getBaseType()
   {
      return type.getBaseId();
   }

   String getPath()
   {
      String rootPath = storage.getJcrRootPath();
      String nodePath = path();
      if (rootPath.length() > 1 && rootPath.endsWith("/"))
      {
         rootPath = rootPath.substring(0, rootPath.length() - 1);
      }
      if (rootPath.equals(nodePath))
      {
         return "/";
      }
      if (rootPath.length() > 1)
      {
         nodePath = nodePath.substring(rootPath.length());
      }
      return nodePath;
   }

   String path()
   {
     return path;
   }

   boolean isRoot()
   {
      return "/".equals(getPath());
   }

   boolean isNew() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {
      return getNode().isNew();
   }

   Collection<JcrNodeEntry> getPolicies()
   {
      Set<JcrNodeEntry> policies = new HashSet<JcrNodeEntry>();
      try
      {
         for (PropertyIterator iter = getNode().getProperties(); iter.hasNext();)
         {
            javax.jcr.Property jcrProperty = iter.nextProperty();
            if (jcrProperty.getType() == javax.jcr.PropertyType.REFERENCE)
            {
               try
               {
            	   Node n = jcrProperty.getNode();
            	   if (n.getPrimaryNodeType().isNodeType(JcrCMIS.CMIS_NT_POLICY))
            	   {
            		   try
            		   {
            			   policies.add(storage.fromNode(n));
            		   }
            		   catch (ObjectNotFoundException onfe)
            		   {
            			   // Ignore nodes with object not found.
            		   }
            	   }
               }
               catch (ValueFormatException ignored)
               {
                  // Can be thrown id met multi-valued property. Not care about
                  // it cause policy reference may not be multi-valued.
               }
               catch(ItemNotFoundException infe) {
            	   if(LOG.isDebugEnabled()) {
            		   LOG.debug("Cannot find the reference node", infe);
            	   }
                   // it cause of reference property does not found and should be ignored
               }
            }
         }
         return policies;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get object's policies. " + re.getMessage(), re);
      }
   }

   void applyPolicy(JcrNodeEntry policy)
   {
      try
      {
         String policyId = policy.getId();
         if (!getNode().hasProperty(policyId))
         {
            getNode().setProperty(policyId, policy.getNode());
         }
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable apply policy. " + re.getMessage(), re);
      }
   }

   void removePolicy(JcrNodeEntry policy)
   {
      try
      {
         String policyId = policy.getId();
         getNode().setProperty(policyId, (Node)null);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable remove policy. " + re.getMessage(), re);
      }
   }

   List<AccessControlEntry> getACL()
   {
      try
      {
         if (getNode().isNodeType(JcrCMIS.EXO_PRIVILEGABLE))
         {
            AccessControlList jcrACL = ((ExtendedNode)getNode()).getACL();

            Map<String, Set<String>> cache = new HashMap<String, Set<String>>();

            // Merge JCR ACEs
            List<org.exoplatform.services.jcr.access.AccessControlEntry> jcrACEs = jcrACL.getPermissionEntries();
            for (org.exoplatform.services.jcr.access.AccessControlEntry ace : jcrACEs)
            {
               String identity = ace.getIdentity();

               Set<String> permissions = cache.get(identity);
               if (permissions == null)
               {
                  permissions = new HashSet<String>();
                  cache.put(identity, permissions);
               }

               permissions.add(ace.getPermission());
            }

            List<AccessControlEntry> cmisACL = new ArrayList<AccessControlEntry>(cache.size());

            for (String principal : cache.keySet())
            {
               AccessControlEntry cmisACE = new AccessControlEntry();
               cmisACE.setPrincipal(principal);

               Set<String> values = cache.get(principal);
               // Represent JCR ACEs as CMIS ACEs.
               if (values.size() == PermissionType.ALL.length)
               {
                  cmisACE.getPermissions().add(BasicPermissions.CMIS_ALL.value());
               }
               else if (values.contains(PermissionType.READ) && values.contains(PermissionType.ADD_NODE))
               {
                  cmisACE.getPermissions().add(BasicPermissions.CMIS_READ.value());
               }
               else if (values.contains(PermissionType.SET_PROPERTY) && values.contains(PermissionType.REMOVE))
               {
                  cmisACE.getPermissions().add(BasicPermissions.CMIS_WRITE.value());
               }
               cmisACE.setDirect(true);
               cmisACL.add(cmisACE);
            }
            return Collections.unmodifiableList(cmisACL);
         }
         // Node has not "exo:privilegeable" mixin.
         return Collections.emptyList();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get objects's ACL. " + re.getMessage(), re);
      }

   }

   void setACL(List<AccessControlEntry> aces)
   {
      try
      {
         if (!getNode().isNodeType(JcrCMIS.EXO_PRIVILEGABLE))
         {
            getNode().addMixin(JcrCMIS.EXO_PRIVILEGABLE);
         }
         ExtendedNode extNode = (ExtendedNode)getNode();
         // Not merge ACL overwrite it.
         extNode.clearACL();
         if (aces != null && aces.size() > 0)
         {
            extNode.setPermissions(createPermissionMap(aces));
         }
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable apply ACL. " + re.getMessage(), re);
      }
   }

   /**
    * Create permission map which can be passed to JCR getNode().
    *
    * @param source source ACL
    * @return permission map
    */
   private Map<String, String[]> createPermissionMap(List<AccessControlEntry> source)
   {
      Map<String, Set<String>> cache = new HashMap<String, Set<String>>();
      for (AccessControlEntry ace : source)
      {
         String principal = ace.getPrincipal();
         Set<String> permissions = cache.get(principal);
         if (permissions == null)
         {
            permissions = new HashSet<String>();
            cache.put(principal, permissions);
         }
         for (String perm : ace.getPermissions())
         {
            if (BasicPermissions.CMIS_READ.value().equals(perm))
            {
               permissions.add(PermissionType.READ);
               // In CMIS child may be add without write permission for parent.
               permissions.add(PermissionType.ADD_NODE);
            }
            else if (BasicPermissions.CMIS_WRITE.value().equals(perm))
            {
               permissions.add(PermissionType.SET_PROPERTY);
               permissions.add(PermissionType.REMOVE);
            }
            else if (BasicPermissions.CMIS_ALL.value().equals(perm))
            {
               permissions.add(PermissionType.READ);
               permissions.add(PermissionType.ADD_NODE);
               permissions.add(PermissionType.SET_PROPERTY);
               permissions.add(PermissionType.REMOVE);
            }
         }
      }
      Map<String, String[]> aces = new HashMap<String, String[]>();

      for (Map.Entry<String, Set<String>> e : cache.entrySet())
      {
         aces.put(e.getKey(), e.getValue().toArray(new String[e.getValue().size()]));
      }

      return aces;
   }

   Collection<JcrNodeEntry> getRelationships(RelationshipDirection direction, TypeDefinition typeDef,
      boolean includeSubRelationshipTypes)
   {
      Collection<JcrNodeEntry> relationships = new HashSet<JcrNodeEntry>();
      Collection<String> typeFilter = new HashSet<String>();
      typeFilter.add(typeDef.getId());

      if (includeSubRelationshipTypes)
      {
         Collection<TypeDefinition> subTypes = null;
         try
         {
            subTypes = storage.getSubTypes(typeDef.getId(), false);
         }
         catch (TypeNotFoundException e)
         {
            // Should never happen.
            throw new CmisRuntimeException(e.getMessage(), e);
         }
         for (TypeDefinition t : subTypes)
         {
            typeFilter.add(t.getId());
         }
      }
      try
      {
         for (PropertyIterator references = getNode().getReferences(); references.hasNext();)
         {

            javax.jcr.Property prop = references.nextProperty();
            String propName = prop.getName();
            if ((direction == RelationshipDirection.EITHER && (propName.equals(CmisConstants.SOURCE_ID) || propName
               .equals(CmisConstants.TARGET_ID)))
               || (direction == RelationshipDirection.SOURCE && propName.equals(CmisConstants.SOURCE_ID))
               || (direction == RelationshipDirection.TARGET && propName.equals(CmisConstants.TARGET_ID)))
            {
               JcrNodeEntry relationshipEntry = null;
               try
               {
                  relationshipEntry = storage.fromNode(prop.getParent());

               }
               catch (ObjectNotFoundException onfe)
               {
                  // Ignore nodes with object not found.
               }
               catch (NotSupportedNodeTypeException ignored)
               {
                  // Ignore nodes with not supported node-type.
               }
               if (relationshipEntry != null && typeFilter.contains(relationshipEntry.getType().getId()))
               {
                  relationships.add(relationshipEntry);
               }
            }
         }
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get relationships. " + re.getMessage(), re);
      }
      return relationships;
   }

   String getContentStreamId()
   {
      if (getBaseType() == BaseType.DOCUMENT)
      {
         String streamId = getString(CmisConstants.CONTENT_STREAM_ID);
         if (streamId == null)
         {
            try
            {
               Node contentNode = getNode().getNode(JcrCMIS.JCR_CONTENT);
               long contentLength = contentNode.getProperty(JcrCMIS.JCR_DATA).getLength();
               if (contentLength > 0)
               {
                  streamId = ((ExtendedNode)contentNode).getIdentifier();
               }
            }
            catch (RepositoryException re)
            {
               throw new CmisRuntimeException(re.getMessage(), re);
            }
         }
         return streamId;
      }
      return null;
   }

   String getContentStreamFileName()
   {
      if (getBaseType() == BaseType.DOCUMENT)
      {
         String contentFileName = getString(CmisConstants.CONTENT_STREAM_FILE_NAME);
         if (contentFileName == null)
         {
            // Use name of Document if content not empty.
            contentFileName = getName();
         }
         return contentFileName;
      }
      return null;
   }

   ContentStream getContentStream(String streamId)
   {
      try
      {
         if (streamId == null || streamId.equals(getContentStreamId()))
         {
            if (getBaseType() != BaseType.DOCUMENT)
            {
               return null;
            }
            Node contentNode = getNode().getNode(JcrCMIS.JCR_CONTENT);
            long contentLength = contentNode.getProperty(JcrCMIS.JCR_DATA).getLength();
            if (contentLength == 0)
            {
               return null;
            }
            MimeType mimeType = MimeType.fromString(contentNode.getProperty(JcrCMIS.JCR_MIMETYPE).getString());
            if (contentNode.hasProperty(JcrCMIS.JCR_ENCODING))
            {
               mimeType.getParameters().put(CmisConstants.CHARSET,
                  contentNode.getProperty(JcrCMIS.JCR_ENCODING).getString());
            }
            return new BaseContentStream(contentNode.getProperty(JcrCMIS.JCR_DATA).getStream(), contentLength,
               getContentStreamFileName(), mimeType);
         }
         try
         {
           Node rendition = getNode().getNode(streamId);
           javax.jcr.Property renditionContent = rendition.getProperty(JcrCMIS.CMIS_RENDITION_STREAM);
           MimeType mimeType =
                   MimeType.fromString(rendition.getProperty(JcrCMIS.CMIS_RENDITION_MIME_TYPE).getString());
           if (rendition.hasProperty(JcrCMIS.CMIS_RENDITION_ENCODING))
           {
             mimeType.getParameters().put(CmisConstants.CHARSET,
                     rendition.getProperty(JcrCMIS.CMIS_RENDITION_ENCODING).getString());
           }
           return new BaseContentStream(renditionContent.getStream(), renditionContent.getLength(), null, mimeType);
         }
         catch (PathNotFoundException pnfe)
         {
           if (LOG.isWarnEnabled())
             LOG.warn(pnfe.getMessage(), pnfe);
           return null;
         }
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unbale get content stream. " + re.getMessage(), re);
      }
   }

   /**
    * Set new or remove (if <code>content == null</code>) content stream.
    *
    * @param content content
    * @throws IOException if any i/o error occurs
    */
   void setContentStream(ContentStream content) throws IOException
   {
      if (getBaseType() != BaseType.DOCUMENT)
      {
         // method must not be call for object other then cmis:document
         throw new UnsupportedOperationException();
      }
      try
      {
         // jcr:content
         Node contentNode = getNode().hasNode(JcrCMIS.JCR_CONTENT) ? getNode().getNode(JcrCMIS.JCR_CONTENT) : 
           getNode().addNode(JcrCMIS.JCR_CONTENT,
               JcrCMIS.NT_RESOURCE);
         if (content != null)
         {
            MimeType mediaType = content.getMediaType();
            // Re-count content length
            long contentLength = contentNode.setProperty(JcrCMIS.JCR_DATA, content.getStream()).getLength();
            contentNode.setProperty(JcrCMIS.JCR_LAST_MODIFIED, Calendar.getInstance());
            if (getNode().isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
            {
               // Update CMIS properties
               if (!getNode().hasProperty(CmisConstants.CONTENT_STREAM_ID))
               {
                  // If new node
                  getNode().setProperty(CmisConstants.CONTENT_STREAM_ID, ((ExtendedNode)contentNode).getIdentifier());
               }
               getNode().setProperty(CmisConstants.CONTENT_STREAM_LENGTH, contentLength);
               getNode().setProperty(CmisConstants.CONTENT_STREAM_MIME_TYPE, mediaType.getBaseType());
            }
            // Add/update mimeType property after content updated. Need for fixing AddMetadataAction (JCR).
            contentNode.setProperty(JcrCMIS.JCR_MIMETYPE, mediaType.getBaseType());
            if (mediaType.getParameter(CmisConstants.CHARSET) != null)
            {
               contentNode.setProperty(JcrCMIS.JCR_ENCODING, mediaType.getParameter(CmisConstants.CHARSET));
            }
         }
         else
         {
            contentNode.setProperty(JcrCMIS.JCR_MIMETYPE, "");
            contentNode.setProperty(JcrCMIS.JCR_ENCODING, (Value)null);
            contentNode.setProperty(JcrCMIS.JCR_DATA, new ByteArrayInputStream(new byte[0]));
            contentNode.setProperty(JcrCMIS.JCR_LAST_MODIFIED, Calendar.getInstance());
            if (getNode().isNodeType(JcrCMIS.CMIS_MIX_DOCUMENT))
            {
               // Update CMIS properties
               getNode().setProperty(CmisConstants.CONTENT_STREAM_ID, (Value)null);
               getNode().setProperty(CmisConstants.CONTENT_STREAM_LENGTH, 0);
               getNode().setProperty(CmisConstants.CONTENT_STREAM_MIME_TYPE, (Value)null);
            }
         }
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set content stream. " + re.getMessage(), re);
      }

   }

   boolean hasContent()
   {
      if (getBaseType() != BaseType.DOCUMENT)
      {
         return false;
      }
      try
      {
         Node contentNode = getNode().getNode(JcrCMIS.JCR_CONTENT);
         long contentLength = contentNode.getProperty(JcrCMIS.JCR_DATA).getLength();
         return contentLength > 0;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException(re.getMessage(), re);
      }
   }

   Property<?> getProperty(PropertyDefinition<?> definition)
   {
      try
      {
         javax.jcr.Property jcrProperty = getNode().getProperty(definition.getId());
         return createProperty(definition, definition.isMultivalued() ? jcrProperty.getValues()
            : new Value[]{jcrProperty.getValue()});
      }
      catch (PathNotFoundException pnf)
      {
         if (LOG.isDebugEnabled())
            LOG.debug("Property " + definition.getId() + " is not set.");
         // Property is valid but not set in back-end. Return property
         // in 'value not set' state.
         return createProperty(definition, new Value[0]);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + definition.getId() + ". " + re.getMessage(), re);
      }
   }

   private Property<?> createProperty(PropertyDefinition<?> def, Value[] values)
   {
      try
      {
         if (def.getPropertyType() == PropertyType.BOOLEAN)
         {
            List<Boolean> v = new ArrayList<Boolean>(values.length);
            for (int i = 0; i < values.length; i++)
            {
               v.add(values[i].getBoolean());
            }
            return new BooleanProperty(def.getId(), def.getQueryName(), def.getLocalName(), def.getDisplayName(), v);
         }
         else if (def.getPropertyType() == PropertyType.DATETIME)
         {
            List<Calendar> v = new ArrayList<Calendar>(values.length);
            for (int i = 0; i < values.length; i++)
            {
               v.add(values[i].getDate());
            }
            return new DateTimeProperty(def.getId(), def.getQueryName(), def.getLocalName(), def.getDisplayName(), v);
         }
         else if (def.getPropertyType() == PropertyType.DECIMAL)
         {
            List<BigDecimal> v = new ArrayList<BigDecimal>(values.length);
            for (int i = 0; i < values.length; i++)
            {
               v.add(BigDecimal.valueOf(values[i].getDouble()));
            }
            return new DecimalProperty(def.getId(), def.getQueryName(), def.getLocalName(), def.getDisplayName(), v);
         }
         else if (def.getPropertyType() == PropertyType.HTML)
         {
            List<String> v = new ArrayList<String>(values.length);
            for (int i = 0; i < values.length; i++)
            {
               v.add(values[i].getString());
            }
            return new HtmlProperty(def.getId(), def.getQueryName(), def.getLocalName(), def.getDisplayName(), v);
         }
         else if (def.getPropertyType() == PropertyType.ID)
         {
            List<String> v = new ArrayList<String>(values.length);
            for (int i = 0; i < values.length; i++)
            {
               v.add(values[i].getString());
            }
            return new IdProperty(def.getId(), def.getQueryName(), def.getLocalName(), def.getDisplayName(), v);
         }
         else if (def.getPropertyType() == PropertyType.INTEGER)
         {
            List<BigInteger> v = new ArrayList<BigInteger>(values.length);
            for (int i = 0; i < values.length; i++)
            {
               v.add(BigInteger.valueOf(values[i].getLong()));
            }
            return new IntegerProperty(def.getId(), def.getQueryName(), def.getLocalName(), def.getDisplayName(), v);
         }
         else if (def.getPropertyType() == PropertyType.STRING)
         {
            List<String> v = new ArrayList<String>(values.length);
            for (int i = 0; i < values.length; i++)
            {
               v.add(values[i].getString());
            }
            return new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def.getDisplayName(), v);
         }
         else if (def.getPropertyType() == PropertyType.URI)
         {
            List<URI> v = new ArrayList<URI>(values.length);
            for (int i = 0; i < values.length; i++)
            {
               try
               {
                  v.add(new URI(values[i].getString()));
               }
               catch (URISyntaxException ue)
               {
                 if (LOG.isErrorEnabled()) {
                   LOG.error(ue.getMessage(), ue);
                 }
               }
            }
            return new UriProperty(def.getId(), def.getQueryName(), def.getLocalName(), def.getDisplayName(), v);
         }
         else
         {
            throw new CmisRuntimeException("Unknown property type.");
         }
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException(re.getMessage(), re);
      }
   }

   @SuppressWarnings("unchecked")
   void setProperty(Property<?> property) throws NameConstraintViolationException
   {
      // Type and value should be already checked.
      // 1. Allowed property for this type.
      // 2. Type matched to type definition.
      // 3. Required property has value(s).
      // 4. Single-valued property does not contains more then one value.
      try
      {
         if (!getNode().isNew() && property.getId().equals(CmisConstants.NAME) && property.getValues().size() > 0)
         {
            // Special property for JCR back-end.
            String name = (String)property.getValues().get(0);
            setName(name);
         }
         else
         {
            boolean multivalued = type.getPropertyDefinition(property.getId()).isMultivalued();
            if (property.getType() == PropertyType.BOOLEAN)
            {
               List<Boolean> booleans = (List<Boolean>)property.getValues();
               if (booleans.size() == 0)
               {
                  getNode().setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  getNode().setProperty(property.getId(), booleans.get(0));
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new BooleanValue(booleans.get(i));
                  }
                  getNode().setProperty(property.getId(), jcrValue);
               }
            }
            else if (property.getType() == PropertyType.DATETIME)
            {
               List<Calendar> datetime = (List<Calendar>)property.getValues();
               if (datetime.size() == 0)
               {
                  getNode().setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  getNode().setProperty(property.getId(), datetime.get(0));
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new DateValue(datetime.get(i));
                  }
                  getNode().setProperty(property.getId(), jcrValue);
               }
            }
            else if (property.getType() == PropertyType.DECIMAL)
            {
               List<BigDecimal> doubles = (List<BigDecimal>)property.getValues();
               if (doubles.size() == 0)
               {
                  getNode().setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  getNode().setProperty(property.getId(), doubles.get(0).doubleValue());
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new DoubleValue(doubles.get(i).doubleValue());
                  }
                  getNode().setProperty(property.getId(), jcrValue);
               }
            }
            else if (property.getType() == PropertyType.INTEGER)
            {
               List<BigInteger> integers = (List<BigInteger>)property.getValues();
               if (integers.size() == 0)
               {
                  getNode().setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  getNode().setProperty(property.getId(), integers.get(0).longValue());
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new LongValue(integers.get(i).longValue());
                  }
                  getNode().setProperty(property.getId(), jcrValue);
               }
            }
            else if (property.getType() == PropertyType.HTML || property.getType() == PropertyType.ID
               || property.getType() == PropertyType.STRING)
            {
               List<String> text = (List<String>)property.getValues();
               if (text.size() == 0)
               {
                  getNode().setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  getNode().setProperty(property.getId(), text.get(0));
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new StringValue(text.get(i));
                  }
                  getNode().setProperty(property.getId(), jcrValue);
               }
            }
            else if (property.getType() == PropertyType.URI)
            {
               List<URI> uris = (List<URI>)property.getValues();
               if (uris.size() == 0)
               {
                  getNode().setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  getNode().setProperty(property.getId(), uris.get(0).toString());
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new StringValue(uris.get(i).toString());
                  }
                  getNode().setProperty(property.getId(), jcrValue);
               }
            }
         }
      }
      catch (IOException io)
      {
         throw new CmisRuntimeException("Unable set property " + property.getId() + ". " + io.getMessage(), io);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + property.getId() + ". " + re.getMessage(), re);
      }
   }

   ItemsIterator<JcrNodeEntry> getChildren()
   {
      if (getBaseType() != BaseType.FOLDER)
      {
         // method must not be call for object other then cmis:folder
         throw new UnsupportedOperationException();
      }
      try
      {
         return new ChildrenIterator(getNode().getNodes());
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get children for folder " + getId() + ". " + re.getMessage(), re);
      }
   }

   boolean hasChildren()
   {
      return getBaseType() == BaseType.FOLDER && getChildren().hasNext();
   }

   void addObject(JcrNodeEntry entry)
   {
      try
      {
         Session session = getNode().getSession();
         Node entryNode = entry.getNode();
         Node link = getNode().addNode(entry.getName(), JcrCMIS.JCR_XCMIS_LINKEDFILE);
         link.setProperty(JcrCMIS.JCR_MULTIFILING_PROPERTY_PREFIX + entryNode.getUUID(), entryNode);
         session.save();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable add object to current folder. " + re.getMessage(), re);
      }

   }

   void removeObject(JcrNodeEntry entry)
   {
      try
      {
         Session session = getNode().getSession();
         Node entryNode = entry.getNode();
         if (((ExtendedNode)entryNode.getParent()).getIdentifier().equals(((ExtendedNode)getNode()).getIdentifier()))
         {
            // Node 'entryNode' is filed in current folder directly.
            // Check links from other folders.
            Node link = null;
            for (PropertyIterator references = entryNode.getReferences(); references.hasNext();)
            {
               Node next = references.nextProperty().getParent();
               if (next.isNodeType(JcrCMIS.JCR_XCMIS_LINKEDFILE))
               {
                  link = next;
                  break; // Get a first one which met.
               }
            }

            // Determine where we should place object.
            String destPath = null;
            if (link != null)
            {
               // At least one link (object filed in more then one folder) exists.
               // Replace founded link by original object.
               destPath = link.getPath();
               link.remove();
            }
            else
            {
               // Be sure we have have place to put real object.
               // If not found any xcmis:linkedFile then it minds
               // object is filed in one folder only. In this
               // case this method should not be called.
               // org.xcmis.spi.Connection.removeObjectFromFolder(String, String)
               // must be care about this. Since we don't support
               // 'unfiling' throws exception.
               throw new CmisRuntimeException("Unable remove object from last folder in which it is filed.");
            }
            // Move object node from current folder.
            session.move(entryNode.getPath(), destPath);
         }
         else
         {
            // Need find linkedFile in current folder.
            for (PropertyIterator references = entryNode.getReferences(); references.hasNext();)
            {
               Node next = references.nextProperty().getParent();
               if (next.isNodeType(JcrCMIS.JCR_XCMIS_LINKEDFILE)
                  && ((ExtendedNode)next.getParent()).getIdentifier().equals(((ExtendedNode)getNode()).getIdentifier()))
               {
                  next.remove();
                  break;
               }
            }
         }
         session.save();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable remove object from current folder. " + re.getMessage(), re);
      }
   }

  void moveTo(JcrNodeEntry target) throws NameConstraintViolationException, StorageException {
    try {
      Session session = getNode().getSession();
      String objectPath = path();
      StringBuffer destinationPath = new StringBuffer(target.getNode().getPath());
      if ("/".equals(destinationPath.toString())) {
        destinationPath.append(getName());
      } else {
        destinationPath.append("/").append(getName());
      }
      session.getWorkspace().move(objectPath, destinationPath.toString());
    } catch (ItemExistsException ie) {
      throw new NameConstraintViolationException("Object with the same name already exists in target folder.");
    } catch (javax.jcr.RepositoryException re) {
      throw new StorageException("Unable to move object. " + re.getMessage(), re);
    }
  }

   Collection<JcrNodeEntry> getParents()
   {
      try
      {
         Set<JcrNodeEntry> parents = new HashSet<JcrNodeEntry>();
         if (getBaseType() == BaseType.DOCUMENT)
         {
            for (PropertyIterator iterator = getNode().getReferences(); iterator.hasNext();)
            {
               Node refer = iterator.nextProperty().getParent();
               if (refer.isNodeType(JcrCMIS.JCR_XCMIS_LINKEDFILE))
               {
                  Node parent = refer.getParent();
                  try
                  {
                     parents.add(storage.fromNode(parent));
                  }
                  catch (ObjectNotFoundException onfe)
                  {
                     // Ignore nodes with object not found.
                  }
               }
            }
         }
         if (getBaseType() == BaseType.DOCUMENT || !isRoot())
         {
            try
            {
               JcrNodeEntry parent = storage.fromNode(getNode().getParent());
               parents.add(parent);
            }
            catch (ObjectNotFoundException onfe)
            {
              // Ignore nodes with object not found.
            }
         }
         return parents;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get object parent. " + re.getMessage(), re);
      }
   }

   Boolean getBoolean(String strID)
   {
      try
      {
         return getNode().getProperty(strID).getBoolean();
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + strID + ". " + re.getMessage(), re);
      }
   }

   Boolean[] getBooleans(String strID)
   {
      try
      {
         Value[] values = getNode().getProperty(strID).getValues();
         Boolean[] res = new Boolean[values.length];
         for (int i = 0; i < values.length; i++)
         {
            res[i] = values[i].getBoolean();
         }
         return res;
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + strID + ". " + re.getMessage(), re);
      }
   }

   Calendar getDate(String strID)
   {
      try
      {
         return getNode().getProperty(strID).getDate();
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + strID + ". " + re.getMessage(), re);
      }
   }

   Calendar[] getDates(String strID)
   {
      try
      {
         Value[] values = getNode().getProperty(strID).getValues();
         Calendar[] res = new Calendar[values.length];
         for (int i = 0; i < values.length; i++)
         {
            res[i] = values[i].getDate();
         }
         return res;
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + strID + ". " + re.getMessage(), re);
      }
   }

   Double getDouble(String strID)
   {
      try
      {
         return getNode().getProperty(strID).getDouble();
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + strID + ". " + re.getMessage(), re);
      }
   }

   Double[] getDoubles(String strID)
   {
      try
      {
         Value[] values = getNode().getProperty(strID).getValues();
         Double[] res = new Double[values.length];
         for (int i = 0; i < values.length; i++)
         {
            res[i] = values[i].getDouble();
         }
         return res;
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + strID + ". " + re.getMessage(), re);
      }
   }

   Long getLong(String strID)
   {
      try
      {
         return getNode().getProperty(strID).getLong();
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + strID + ". " + re.getMessage(), re);
      }
   }

   Long[] getLongs(String strID)
   {
      try
      {
         Value[] values = getNode().getProperty(strID).getValues();
         Long[] res = new Long[values.length];
         for (int i = 0; i < values.length; i++)
         {
            res[i] = values[i].getLong();
         }
         return res;
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + strID + ". " + re.getMessage(), re);
      }
   }

   String getString(String strID)
   {
      try
      {
         return getNode().getProperty(strID).getString();
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + strID + ". " + re.getMessage(), re);
      }
   }

   String[] getStrings(String strID)
   {
      try
      {
         Value[] values = getNode().getProperty(strID).getValues();
         String[] res = new String[values.length];
         for (int i = 0; i < values.length; i++)
         {
            res[i] = values[i].getString();
         }
         return res;
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + strID + ". " + re.getMessage(), re);
      }
   }

   void setValue(String id, Value value)
   {
      try
      {
         getNode().setProperty(id, value);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValues(String id, Value[] values)
   {
      try
      {
         getNode().setProperty(id, values);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValue(String id, boolean value)
   {
      try
      {
         getNode().setProperty(id, value);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValues(String id, boolean[] values)
   {
      try
      {
         Value[] jcrValue = new Value[values.length];
         for (int i = 0; i < jcrValue.length; i++)
         {
            jcrValue[i] = new BooleanValue(values[i]);
         }
         getNode().setProperty(id, jcrValue);
      }
      catch (IOException ioe)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + ioe.getMessage(), ioe);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValue(String id, Calendar value)
   {
      try
      {
         getNode().setProperty(id, value);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValues(String id, Calendar[] values)
   {
      try
      {
         Value[] jcrValue = new Value[values.length];
         for (int i = 0; i < jcrValue.length; i++)
         {
            jcrValue[i] = new DateValue(values[i]);
         }
         getNode().setProperty(id, jcrValue);
      }
      catch (IOException io)
      {
         // Looks as never happen with current JCR impl
         throw new CmisRuntimeException("Unable set property " + id + ". " + io.getMessage(), io);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValue(String id, double value)
   {
      try
      {
         getNode().setProperty(id, value);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValues(String id, double[] values)
   {
      try
      {
         Value[] jcrValue = new Value[values.length];
         for (int i = 0; i < jcrValue.length; i++)
         {
            jcrValue[i] = new DoubleValue(values[i]);
         }
         getNode().setProperty(id, jcrValue);
      }
      catch (IOException io)
      {
         // Looks as never happen with current JCR impl
         throw new CmisRuntimeException("Unable set property " + id + ". " + io.getMessage(), io);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValue(String id, long value)
   {
      try
      {
         getNode().setProperty(id, value);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValues(String id, long[] values)
   {
      try
      {
         Value[] jcrValue = new Value[values.length];
         for (int i = 0; i < jcrValue.length; i++)
         {
            jcrValue[i] = new LongValue(values[i]);
         }
         getNode().setProperty(id, jcrValue);
      }
      catch (IOException io)
      {
         // Looks as never happen with current JCR impl
         throw new CmisRuntimeException("Unable set property " + id + ". " + io.getMessage(), io);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValue(String id, String value)
   {
      try
      {
         getNode().setProperty(id, value);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValues(String id, String[] strings)
   {
      try
      {
         Value[] jcrValue = new Value[strings.length];
         for (int i = 0; i < jcrValue.length; i++)
         {
            jcrValue[i] = new StringValue(strings[i]);
         }
         getNode().setProperty(id, jcrValue);
      }
      catch (IOException io)
      {
         // Looks as never happen with current JCR impl
         throw new CmisRuntimeException("Failed set or update property " + id + ". " + io.getMessage(), io);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set property " + id + ". " + re.getMessage(), re);
      }
   }

   void save() throws StorageException
   {
      save(true);
   }

   void save(boolean updateLastModifiedAttributes) throws StorageException
   {
      try
      {
         Session session = getNode().getSession();
         if (updateLastModifiedAttributes)
         {
            getNode().setProperty(CmisConstants.LAST_MODIFICATION_DATE, Calendar.getInstance());
            getNode().setProperty(CmisConstants.LAST_MODIFIED_BY, getNode().getSession().getUserID());
            getNode().setProperty(CmisConstants.CHANGE_TOKEN, IdGenerator.generate());
         }
         session.save();
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable save object. " + re.getMessage(), re);
      }
   }

   void delete() throws StorageException
   {
      Collection<JcrNodeEntry> relationships = null;
      // cmis:relationship may not be source or target of other relationship
      if (getType().getBaseId() != BaseType.RELATIONSHIP)
      {
         try
         {
            relationships =
               getRelationships(RelationshipDirection.EITHER,
                  storage.getTypeDefinition(CmisConstants.RELATIONSHIP, true), true);
         }
         catch (TypeNotFoundException ignore)
         {
            // Should never happen since we support relationships.
         }
         if (relationships != null && relationships.size() > 0)
         {
            throw new StorageException("Object can't be deleted cause to storage referential integrity. "
               + "Probably this object is source or target at least one Relationship. "
               + "Those Relationship should be deleted before.");
         }
      }

      try
      {
         Session session = getNode().getSession();
         switch (getBaseType())
         {
            case DOCUMENT :
               if (LOG.isDebugEnabled())
                  LOG.debug("remove document " + path);

               // Check is Document node has any references.
               // It minds Document is multifiled, need remove all links first.
               PropertyIterator references = getNode().getReferences();
               while(references.hasNext())
               {
                  javax.jcr.Property nextProperty = references.nextProperty();
                  Node next = null;
                  try {
                     next = nextProperty.getParent();
                  } catch (InvalidItemStateException e) {
                     // there is no item, the node was removed.
                  }
                  if (next != null && next.isNodeType(JcrCMIS.JCR_XCMIS_LINKEDFILE))
                  {
                     next.remove();
                  }
               }
               String pwcId = getString(CmisConstants.VERSION_SERIES_CHECKED_OUT_ID);
               if (pwcId != null)
               {
                  // remove PWC
                  Node pwcNode = ((ExtendedSession)session).getNodeByIdentifier(pwcId);
                  pwcNode.getParent().remove();
               }
               getNode().remove();
               break;
            case FOLDER :
               if (LOG.isDebugEnabled())
                  LOG.debug("remove folder " + path);

               getNode().remove();
               break;
            case POLICY :
               if (LOG.isDebugEnabled())
                  LOG.debug("remove policy " + path);

               // Check is policy applied to at least one object.
               for (PropertyIterator iter = getNode().getReferences(); iter.hasNext();)
               {
                  Node controllable = iter.nextProperty().getParent();
                  if (controllable.isNodeType(JcrCMIS.NT_FILE) //
                     || controllable.isNodeType(JcrCMIS.NT_FOLDER) //
                     || controllable.isNodeType(JcrCMIS.CMIS_NT_POLICY))
                  {
                     throw new StorageException("Unable to delete applied policy.");
                  }
               }
               getNode().remove();
               break;
            case RELATIONSHIP :
               if (LOG.isDebugEnabled())
                  LOG.debug("remove relationship " + path);

               getNode().remove();
               break;
         }
         session.save();

         if (LOG.isDebugEnabled())
            LOG.debug("removed " + path);
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable delete object. " + re.getMessage(), re);
      }
   }

   Node getNode() throws LoginException, NoSuchWorkspaceException, RepositoryException
   {
      Session session = WCMCoreUtils.getUserSessionProvider().getSession(workspace, WCMCoreUtils.getRepository());
      return (Node)session.getItem(path);
   }  

   /**
    * {@inheritDoc}
    */
   public boolean equals(Object obj)
   {
      if (this == obj)
      {
         return true;
      }
      if (obj == null || obj.getClass() != getClass())
      {
         return false;
      }
      return ((JcrNodeEntry)obj).getId().equals(getId());
   }

   /**
    * {@inheritDoc}
    */
   public int hashCode()
   {
      int hash = 8;
      hash = hash * 31 + getId().hashCode();
      return hash;
   }
   
}
