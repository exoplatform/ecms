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

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.value.BooleanValue;
import org.exoplatform.services.jcr.impl.core.value.DateValue;
import org.exoplatform.services.jcr.impl.core.value.DoubleValue;
import org.exoplatform.services.jcr.impl.core.value.LongValue;
import org.exoplatform.services.jcr.impl.core.value.StringValue;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.spi.BaseContentStream;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.NameConstraintViolationException;
import org.xcmis.spi.PolicyData;
import org.xcmis.spi.StorageException;
import org.xcmis.spi.model.AccessControlEntry;
import org.xcmis.spi.model.BaseType;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.PropertyType;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.Permission.BasicPermissions;
import org.xcmis.spi.model.impl.BooleanProperty;
import org.xcmis.spi.model.impl.DateTimeProperty;
import org.xcmis.spi.model.impl.DecimalProperty;
import org.xcmis.spi.model.impl.HtmlProperty;
import org.xcmis.spi.model.impl.IdProperty;
import org.xcmis.spi.model.impl.IntegerProperty;
import org.xcmis.spi.model.impl.StringProperty;
import org.xcmis.spi.model.impl.UriProperty;
import org.xcmis.spi.utils.MimeType;

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

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: JcrNodeEntry.java 1262 2010-06-09 10:07:01Z andrew00x $
 */
final class JcrNodeEntry
{

   private static final Log LOG = ExoLogger.getLogger(JcrNodeEntry.class);

   private Node node;

   private final TypeDefinition type;

   JcrNodeEntry(Node node) throws RepositoryException
   {
      this.node = node;
      this.type = JcrTypeHelper.getTypeDefinition(node.getPrimaryNodeType(), true);
   }

   JcrNodeEntry(Node node, TypeDefinition type)
   {
      this.node = node;
      this.type = type;
   }

   /**
    * Create permission map which can be passed to JCR node.
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
               // In CMIS child may be add without write permission for
               // parent.
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
                  LOG.error(ue.getMessage(), ue);
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

   void applyPolicy(PolicyData policy)
   {
      try
      {
         String policyId = policy.getObjectId();
         if (!node.hasProperty(policyId))
         {
            node.setProperty(policyId, ((PolicyDataImpl)policy).getNode());
         }
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable apply policy. " + re.getMessage(), re);
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

   BaseType getBaseType()
   {
      return type.getBaseId();
   }

   ContentStream getContentStream()
   {
      try
      {
         if (getBaseType() != BaseType.DOCUMENT)
         {
            return null;
         }
         Node contentNode = node.getNode(JcrCMIS.JCR_CONTENT);
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
         return new BaseContentStream(contentNode.getProperty(JcrCMIS.JCR_DATA).getStream(), contentLength, getName(),
            mimeType);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unbale get content stream. " + re.getMessage(), re);
      }
   }

   String getId()
   {
      try
      {
         return ((ExtendedNode)node).getIdentifier();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get objects's id ." + re.getMessage(), re);
      }
   }

   String getName()
   {
      try
      {
         return node.getName();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable object's name. " + re.getMessage(), re);
      }

   }

   Node getNode()
   {
      return node;
   }

   String getPath()
   {
      try
      {
         return node.getPath();
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get object's path. " + re.getMessage(), re);
      }
   }

   Collection<JcrNodeEntry> getPolicies()
   {
      Set<JcrNodeEntry> policies = new HashSet<JcrNodeEntry>();
      try
      {
         for (PropertyIterator iter = getNode().getProperties(); iter.hasNext();)
         {
            javax.jcr.Property prop = iter.nextProperty();
            if (prop.getType() == javax.jcr.PropertyType.REFERENCE)
            {
               try
               {
                  Node pol = prop.getNode();
                  if (pol.getPrimaryNodeType().isNodeType(JcrCMIS.CMIS_NT_POLICY))
                  {
                     policies.add(new JcrNodeEntry(pol));
                  }
               }
               catch (ValueFormatException ignored)
               {
                  // Can be thrown id met multi-valued property.
                  // Not care about it cause policy reference may not be
                  // multi-valued.
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

   Property<?> getProperty(PropertyDefinition<?> definition)
   {
      try
      {
         javax.jcr.Property jcrProperty = node.getProperty(definition.getId());
         // javax.jcr.Property jcrProperty = node.getProperty(definition.getLocalName());
         return createProperty(definition, definition.isMultivalued() ? jcrProperty.getValues()
            : new Value[]{jcrProperty.getValue()});
      }
      catch (PathNotFoundException pnf)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Property " + definition.getId() + " is not set.");
         }
         // TODO : need more virtual properties ??
         // Property is valid but not set in back-end.
         // Return property in 'value not set' state.
         return createProperty(definition, new Value[0]);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + definition.getId() + ". " + re.getMessage(), re);
      }
   }

   TypeDefinition getType()
   {
      return type;
   }

   boolean hasContent()
   {
      try
      {
         if (getBaseType() != BaseType.DOCUMENT)
         {
            return false;
         }
         Node contentNode = node.getNode(JcrCMIS.JCR_CONTENT);
         long contentLength = contentNode.getProperty(JcrCMIS.JCR_DATA).getLength();
         return contentLength > 0;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException(re.getMessage(), re);
      }
   }

   void removePolicy(PolicyData policy)
   {
      try
      {
         String policyId = policy.getObjectId();
         node.setProperty(policyId, (Node)null);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable remove policy. " + re.getMessage(), re);
      }

   }

   void save() throws StorageException
   {
      try
      {
         Session session = node.getSession();
         session.save();
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable save object. " + re.getMessage(), re);
      }
   }

   void updateAndSave() throws StorageException
   {
      try
      {
         Session session = node.getSession();
         node.setProperty(CmisConstants.LAST_MODIFICATION_DATE, Calendar.getInstance());
         node.setProperty(CmisConstants.LAST_MODIFIED_BY, node.getSession().getUserID());
         node.setProperty(CmisConstants.CHANGE_TOKEN, IdGenerator.generate());
         session.save();
      }
      catch (RepositoryException re)
      {
         throw new StorageException("Unable save object. " + re.getMessage(), re);
      }
   }

   void setACL(List<AccessControlEntry> aces)
   {
      try
      {
         if (!node.isNodeType(JcrCMIS.EXO_PRIVILEGABLE))
         {
            node.addMixin(JcrCMIS.EXO_PRIVILEGABLE);
         }
         ExtendedNode extNode = (ExtendedNode)node;
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

   Boolean getBoolean(String id)
   {
      try
      {
         return node.getProperty(id).getBoolean();
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + id + ". " + re.getMessage(), re);
      }
   }

   Boolean[] getBooleans(String id)
   {
      try
      {
         Value[] values = node.getProperty(id).getValues();
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
         throw new CmisRuntimeException("Unable get property " + id + ". " + re.getMessage(), re);
      }
   }

   Calendar getDate(String id)
   {
      try
      {
         return node.getProperty(id).getDate();
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + id + ". " + re.getMessage(), re);
      }
   }

   Calendar[] getDates(String id)
   {
      try
      {
         Value[] values = node.getProperty(id).getValues();
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
         throw new CmisRuntimeException("Unable get property " + id + ". " + re.getMessage(), re);
      }
   }

   Double getDouble(String id)
   {
      try
      {
         return node.getProperty(id).getDouble();
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + id + ". " + re.getMessage(), re);
      }
   }

   Double[] getDoubles(String id)
   {
      try
      {
         Value[] values = node.getProperty(id).getValues();
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
         throw new CmisRuntimeException("Unable get property " + id + ". " + re.getMessage(), re);
      }
   }

   Long getLong(String id)
   {
      try
      {
         return node.getProperty(id).getLong();
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + id + ". " + re.getMessage(), re);
      }
   }

   Long[] getLongs(String id)
   {
      try
      {
         Value[] values = node.getProperty(id).getValues();
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
         throw new CmisRuntimeException("Unable get property " + id + ". " + re.getMessage(), re);
      }
   }

   String getString(String id)
   {
      try
      {
         return node.getProperty(id).getString();
      }
      catch (PathNotFoundException pe)
      {
         // does not exist
         return null;
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable get property " + id + ". " + re.getMessage(), re);
      }
   }

   String[] getStrings(String id)
   {
      try
      {
         Value[] values = node.getProperty(id).getValues();
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
         throw new CmisRuntimeException("Unable get property " + id + ". " + re.getMessage(), re);
      }
   }

   void setValue(String id, Value value)
   {
      try
      {
         node.setProperty(id, value);
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
         node.setProperty(id, values);
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
         node.setProperty(id, value);
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
         node.setProperty(id, jcrValue);
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

   /**
    * Set new or remove (if <code>content == null</code>) content stream.
    * 
    * @param content content
    * @throws IOException if any i/o error occurs
    */
   void setContentStream(ContentStream content) throws IOException
   {
      if (type.getBaseId() != BaseType.DOCUMENT)
      {
         // method must not be call for object other then cmis:document
         throw new UnsupportedOperationException();
      }
      try
      {
         // jcr:content
         Node contentNode =
            node.hasNode(JcrCMIS.JCR_CONTENT) ? node.getNode(JcrCMIS.JCR_CONTENT) : node.addNode(JcrCMIS.JCR_CONTENT,
               JcrCMIS.NT_RESOURCE);
         if (content != null)
         {
            MimeType mediaType = content.getMediaType();
            contentNode.setProperty(JcrCMIS.JCR_MIMETYPE, mediaType.getBaseType());
            if (mediaType.getParameter(CmisConstants.CHARSET) != null)
            {
               contentNode.setProperty(JcrCMIS.JCR_ENCODING, mediaType.getParameter(CmisConstants.CHARSET));
            }
            // Re-count content length
            long contentLength = contentNode.setProperty(JcrCMIS.JCR_DATA, content.getStream()).getLength();
            contentNode.setProperty(JcrCMIS.JCR_LAST_MODIFIED, Calendar.getInstance());
            // Update CMIS properties
            if (!node.hasProperty(CmisConstants.CONTENT_STREAM_ID))
            {
               // If new node
               node.setProperty(CmisConstants.CONTENT_STREAM_ID, ((ExtendedNode)contentNode).getIdentifier());
            }
            node.setProperty(CmisConstants.CONTENT_STREAM_LENGTH, contentLength);
            node.setProperty(CmisConstants.CONTENT_STREAM_MIME_TYPE, mediaType.getBaseType());
         }
         else
         {
            contentNode.setProperty(JcrCMIS.JCR_MIMETYPE, "");
            contentNode.setProperty(JcrCMIS.JCR_ENCODING, (Value)null);
            contentNode.setProperty(JcrCMIS.JCR_DATA, new ByteArrayInputStream(new byte[0]));
            contentNode.setProperty(JcrCMIS.JCR_LAST_MODIFIED, Calendar.getInstance());
            // Update CMIS properties
            node.setProperty(CmisConstants.CONTENT_STREAM_ID, (Value)null);
            node.setProperty(CmisConstants.CONTENT_STREAM_LENGTH, 0);
            node.setProperty(CmisConstants.CONTENT_STREAM_MIME_TYPE, (Value)null);
         }
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unable set content stream. " + re.getMessage(), re);
      }

   }

   void setValue(String id, Calendar value)
   {
      try
      {
         node.setProperty(id, value);
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
         node.setProperty(id, jcrValue);
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
         node.setProperty(id, value);
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
         node.setProperty(id, jcrValue);
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
         node.setProperty(id, value);
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
         node.setProperty(id, jcrValue);
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
         if (!node.isNew() && property.getId().equals(CmisConstants.NAME) && property.getValues().size() > 0)
         {
            // Special property for JCR back-end.
            String name = (String)property.getValues().get(0);
            if (name == null || name.length() == 0)
            {
               throw new NameConstraintViolationException("Name can't be null or empty string.");
            }
            if (name.equals(getName()))
            {
               return;
            }

            if (node.getParent().hasNode(name))
            {
               throw new NameConstraintViolationException("Object with name " + name + " already exists.");
            }
            if (name != null)
            {
               Session session = node.getSession();
               String srcPath = node.getPath();
               String destPath = srcPath.substring(0, srcPath.lastIndexOf('/') + 1) + name;
               session.move(srcPath, destPath);
               node = (Node)session.getItem(destPath);
            }
            // 'cmis:name' is not stored as property. This is virtual property in xcmis-jcr.
         }
         else
         {
            boolean multivalued = type.getPropertyDefinition(property.getId()).isMultivalued();
            if (property.getType() == PropertyType.BOOLEAN)
            {
               List<Boolean> booleans = (List<Boolean>)property.getValues();
               if (booleans.size() == 0)
               {
                  node.setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  node.setProperty(property.getId(), booleans.get(0));
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new BooleanValue(booleans.get(i));
                  }
                  node.setProperty(property.getId(), jcrValue);
               }
            }
            else if (property.getType() == PropertyType.DATETIME)
            {
               List<Calendar> datetime = (List<Calendar>)property.getValues();
               if (datetime.size() == 0)
               {
                  node.setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  node.setProperty(property.getId(), datetime.get(0));
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new DateValue(datetime.get(i));
                  }
                  node.setProperty(property.getId(), jcrValue);
               }
            }
            else if (property.getType() == PropertyType.DECIMAL)
            {
               List<BigDecimal> doubles = (List<BigDecimal>)property.getValues();
               if (doubles.size() == 0)
               {
                  node.setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  node.setProperty(property.getId(), doubles.get(0).doubleValue());
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new DoubleValue(doubles.get(i).doubleValue());
                  }
                  node.setProperty(property.getId(), jcrValue);
               }
            }
            else if (property.getType() == PropertyType.INTEGER)
            {
               List<BigInteger> integers = (List<BigInteger>)property.getValues();
               if (integers.size() == 0)
               {
                  node.setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  node.setProperty(property.getId(), integers.get(0).longValue());
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new LongValue(integers.get(i).longValue());
                  }
                  node.setProperty(property.getId(), jcrValue);
               }
            }
            else if (property.getType() == PropertyType.HTML || property.getType() == PropertyType.ID
               || property.getType() == PropertyType.STRING)
            {
               List<String> text = (List<String>)property.getValues();
               if (text.size() == 0)
               {
                  node.setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  node.setProperty(property.getId(), text.get(0));
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new StringValue(text.get(i));
                  }
                  node.setProperty(property.getId(), jcrValue);
               }
            }
            else if (property.getType() == PropertyType.URI)
            {
               List<URI> uris = (List<URI>)property.getValues();
               if (uris.size() == 0)
               {
                  node.setProperty(property.getId(), (Value)null);
               }
               else if (!multivalued)
               {
                  node.setProperty(property.getId(), uris.get(0).toString());
               }
               else
               {
                  Value[] jcrValue = new Value[property.getValues().size()];
                  for (int i = 0; i < jcrValue.length; i++)
                  {
                     jcrValue[i] = new StringValue(uris.get(i).toString());
                  }
                  node.setProperty(property.getId(), jcrValue);
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

   void setValue(String id, String value)
   {
      try
      {
         node.setProperty(id, value);
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
         node.setProperty(id, jcrValue);
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

   void unfile()
   {
      try
      {
         if (node.getParent().isNodeType("xcmis:unfiledObject"))
         {
            // Object is already in unfiled store.
            return;
         }
         // Remove all links.
         for (PropertyIterator iterator = node.getReferences(); iterator.hasNext();)
         {
            Node link = iterator.nextProperty().getParent();
            if (link.isNodeType("nt:linkedFile"))
            {
               link.remove();
            }
         }
         // Move node in unfiled storage
         Session session = node.getSession();
         Node unfiledStore = (Node)session.getItem(StorageImpl.XCMIS_SYSTEM_PATH + "/" + StorageImpl.XCMIS_UNFILED);
         Node unfiled = unfiledStore.addNode(getId(), "xcmis:unfiledObject");
         String destPath = unfiled.getPath() + "/" + node.getName();
         session.move(node.getPath(), destPath);
         session.save();
         node = (Node)session.getItem(destPath);
      }
      catch (RepositoryException re)
      {
         throw new CmisRuntimeException("Unbale unfile object. " + re.getMessage(), re);
      }
   }
}
