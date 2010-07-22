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

import org.xcmis.search.config.IndexConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * CMIS repository configuration.
 * 
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: StorageConfiguration.java 1262 2010-06-09 10:07:01Z andrew00x $
 */
public class StorageConfiguration
{

   /** Repository name. */
   private String repository;

   /** Workspace name. */
   private String workspace;

   /** Repository description. */
   private String description;

   /** Configuration for the index. */
   private IndexConfiguration indexConfiguration;

   /** ID of CMIS repository. */
   private String id;

   /** Additional properties. */
   private Map<String, Object> properties;

   /**
    * Get repository id.
    * 
    * @return the repository id
    */
   public String getId()
   {
      return id;
   }

   /**
    * Get index configuration.
    * 
    * @return the index configuration
    */
   public IndexConfiguration getIndexConfiguration()
   {
      return indexConfiguration;
   }

   /**
    * Get additional repository's properties.
    * 
    * @return properties
    */
   public Map<String, Object> getProperties()
   {
      if (properties == null)
      {
         properties = new HashMap<String, Object>();
      }
      return properties;
   }

   /**
    * Get repository name.
    * 
    * @return repository name.
    */
   public String getRepository()
   {
      return repository;
   }

   /**
    * Get description.
    * 
    * @return repository name.
    */
   public String getDescription()
   {
      return description;
   }

   /**
    * Get workspace name.
    * 
    * @return the workspace name
    */
   public String getWorkspace()
   {
      return workspace;
   }

   /**
    * Set CMIS repository id.
    * 
    * @param id repository id
    */
   public void setId(String id)
   {
      this.id = id;
   }

   /**
    * Set index configuration.
    * 
    * @param indexConfiguration the index configuration
    */
   public void setIndexConfiguration(IndexConfiguration indexConfiguration)
   {
      this.indexConfiguration = indexConfiguration;
   }

   /**
    * Set additional repository's properties.
    * 
    * @param properties properties map
    */
   public void setProperties(Map<String, Object> properties)
   {
      this.properties = properties;
   }

   /**
    * Set repository name.
    * 
    * @param repository the repository name
    */
   public void setRepository(String repository)
   {
      this.repository = repository;
   }

   /**
    * Sets description.
    * 
    * @param description string description
    */
   public void setDescription(String description)
   {
      this.description = description;
   }

   /**
    * Set workspace name.
    * 
    * @param workspace the workspace name
    */
   public void setWorkspace(String workspace)
   {
      this.workspace = workspace;
   }
}
