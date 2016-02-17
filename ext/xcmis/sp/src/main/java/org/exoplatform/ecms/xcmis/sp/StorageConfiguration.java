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

import java.util.HashMap;
import java.util.Map;

/**
 * CMIS repository configuration.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class StorageConfiguration
{

   /** ID of CMIS repository. */
   private String id;

   /** JCR repository name. */
   private String repository;

   /** JCR workspace name. */
   private String workspace;

   /** Path of JCR node which must be considered as root folder. */
   private String rootNodePath = "/";

   //
   //   /** Configuration for the index. */
   //   private IndexConfiguration indexConfiguration;

   /** Additional properties. */
   private Map<String, Object> properties;

   /** Repository description. */
   private String description;

   public StorageConfiguration()
   {
   }

   public StorageConfiguration(String id, String repository, String workspace, String rootNodePath,
      Map<String, Object> properties, String description)
   {
      this.id = id;
      this.repository = repository;
      this.workspace = workspace;
      this.rootNodePath = rootNodePath;
      if (properties != null)
      {
         this.properties = new HashMap<String, Object>(properties);
      }
      this.description = description;
   }

   /**
    * Get repository id.
    *
    * @return the repository id
    */
   public String getId()
   {
      return id;
   }

   //

   /**
    * Set CMIS repository id.
    *
    * @param id
    *           repository id
    */
   public void setId(String id)
   {
      this.id = id;
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
    * Set repository name.
    *
    * @param repository
    *           the repository name
    */
   public void setRepository(String repository)
   {
      this.repository = repository;
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
    * Set workspace name.
    *
    * @param workspace
    *           the workspace name
    */
   public void setWorkspace(String workspace)
   {
      this.workspace = workspace;
   }

   /**
    * Get path of JCR node which must be considered as root folder.
    *
    * @return root folder path
    */
   public String getRootNodePath()
   {
      return rootNodePath;
   }

   /**
    * Set path of JCR node which must be considered as root folder.
    *
    * @param rootNodePath root folder path
    */
   public void setRootNodePath(String rootNodePath)
   {
      this.rootNodePath = rootNodePath;
   }

   /**
    * Get description.
    *
    * @return repository description.
    */
   public String getDescription()
   {
      return description;
   }

   /**
    * Set repository description.
    *
    * @param description
    *           string description
    */
   public void setDescription(String description)
   {
      this.description = description;
   }

   //   /**
   //    * Get index configuration.
   //    *
   //    * @return the index configuration
   //    */
   //   public IndexConfiguration getIndexConfiguration()
   //   {
   //      return indexConfiguration;
   //   }
   //
   //   /**
   //    * Set index configuration.
   //    *
   //    * @param indexConfiguration
   //    *           the index configuration
   //    */
   //   public void setIndexConfiguration(IndexConfiguration indexConfiguration)
   //   {
   //      this.indexConfiguration = indexConfiguration;
   //   }

   /**
    * Get additional repository's properties.
    *
    * @return properties. If there is no any properties specified for repository
    *         then empty map will be returned
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
    * Set additional repository's properties.
    *
    * @param properties
    *           properties map
    */
   public void setProperties(Map<String, Object> properties)
   {
      if (properties != null)
      {
         this.properties = new HashMap<String, Object>(properties);
      }
      else
      {
         this.properties = null;
      }
   }

}
