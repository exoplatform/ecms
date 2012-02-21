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

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.xcmis.spi.CmisConstants;
import org.xcmis.spi.CmisRegistry;
import org.xcmis.spi.ContentStream;
import org.xcmis.spi.DocumentData;
import org.xcmis.spi.FolderData;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.model.Property;
import org.xcmis.spi.model.PropertyDefinition;
import org.xcmis.spi.model.TypeDefinition;
import org.xcmis.spi.model.VersioningState;
import org.xcmis.spi.model.impl.StringProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public abstract class BaseTest extends TestCase
{

   private static final Log LOG = ExoLogger.getLogger(BaseTest.class);

   protected StandaloneContainer container;

   private CredentialsImpl credentials;

   private RepositoryService repositoryService;

   protected ThreadLocalSessionProviderService sessionProviderService;

   protected JcrCmisRegistry registry;

   private volatile static boolean shoutDown;
   
   // For the StorageTest and QueryUsecasesTest
   protected StorageImpl storageA;

   // For the StorageTest and QueryUsecasesTest
   protected TypeDefinition folderTypeDefinition;

   @Override
   public void setUp() throws Exception
   {
      if (!shoutDown)
      {
         FileUtils.deleteQuietly(new File("target/temp"));
      }
      String containerConf = getClass().getResource("/conf/standalone/test-configuration.xml").toString();
      String loginConf = Thread.currentThread().getContextClassLoader().getResource("login.conf").toString();
      StandaloneContainer.setConfigurationURL(containerConf);
      container = StandaloneContainer.getInstance();

      if (System.getProperty("java.security.auth.login.config") == null)
      {
         System.setProperty("java.security.auth.login.config", loginConf);
      }

      credentials = new CredentialsImpl("root", "exo".toCharArray());

      repositoryService = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      registry = (JcrCmisRegistry)container.getComponentInstanceOfType(CmisRegistry.class);
      ConversationState cs = new ConversationState(new Identity("root"));
      ConversationState.setCurrent(cs);

      // storage = (StorageImpl)registry.getConnection("driveA").getStorage();
      sessionProviderService =
         (ThreadLocalSessionProviderService)container
            .getComponentInstanceOfType(ThreadLocalSessionProviderService.class);
      assertNotNull(sessionProviderService);

      sessionProviderService.setSessionProvider(null, new SessionProvider(cs));

      if (!shoutDown)
      {
         Runtime.getRuntime().addShutdownHook(new Thread()
         {
            @Override
            public void run()
            {
               // database.close();
               container.stop();
               System.out.println("The container are stopped");
            }
         });
         shoutDown = true;
      }
   }

   @Override
   protected void tearDown() throws Exception
   {

      driveTearDown("db1", "ws", "exo:drives/driveA");
      driveTearDown("db1", "ws", "exo:drives/driveB");
      driveTearDown("db1", "ws1", "exo:drives/driveC");
      super.tearDown();
   }

   protected void driveTearDown(String repositoryName, String wsName, String cmisRootPath) throws Exception
   {
      Session session = getJcrSession(repositoryName, wsName);

      try
      {
         session.refresh(false);
         Node rootNode = session.getRootNode();
         for (NodeIterator relationships =
            rootNode.getNode(StorageImpl.XCMIS_SYSTEM_PATH.substring(1) + "/" + StorageImpl.XCMIS_RELATIONSHIPS)
               .getNodes(); relationships.hasNext();)
         {
            relationships.nextNode().remove();
         }
         session.save();
         for (NodeIterator wc =
            rootNode.getNode(StorageImpl.XCMIS_SYSTEM_PATH.substring(1) + "/" + StorageImpl.XCMIS_WORKING_COPIES)
               .getNodes(); wc.hasNext();)
         {
            wc.nextNode().remove();
         }
         session.save();
         if (session.getRootNode().hasNode(cmisRootPath))
         {
            Node driveRoot = session.getRootNode().getNode(cmisRootPath);
            if (driveRoot.hasNodes())
            {
               // clean test root
               for (NodeIterator children = driveRoot.getNodes(); children.hasNext();)
               {
                  Node node = children.nextNode();
                  if (!node.getPath().startsWith("/jcr:system") //
                     && !node.getPath().startsWith("/exo:audit") //
                     && !node.getPath().startsWith("/exo:organization") //
                     && !node.getPath().equals(StorageImpl.XCMIS_SYSTEM_PATH))
                  {
                     node.remove();
                  }
               }
               session.save();
            }
         }
      }
      catch (Exception e)
      {
        if (LOG.isErrorEnabled()) {
          LOG.error("Exception in tearDown() ", e);
        }
      }
      finally
      {
         session.logout();
      }

   }

   protected Session getJcrSession(String repositoryName, String wsName) throws RepositoryException,
      RepositoryConfigurationException
   {
      RepositoryImpl repo = (RepositoryImpl)repositoryService.getRepository(repositoryName);
      return repo.login(credentials, wsName);

   }

   protected void setProperty(ObjectData object, Property<?> property) throws Exception
   {
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(property.getId(), property);
      object.setProperties(properties);
   }
   
   protected DocumentData createDocument(FolderData folder, String name, String typeId, ContentStream content,
         VersioningState versioningState) throws Exception
   {
      TypeDefinition documentTypeDefinition = storageA.getTypeDefinition("cmis:document", true);
      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:document", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), name));

      DocumentData document =
         storageA.createDocument(folder, documentTypeDefinition, properties, content, null, null,
            versioningState == null ? VersioningState.MAJOR : versioningState);
      return (DocumentData)document;
   }
   
   protected FolderData createFolder(FolderData folder, String name, String typeId) throws Exception
   {
      PropertyDefinition<?> def = PropertyDefinitions.getPropertyDefinition("cmis:folder", CmisConstants.NAME);
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(CmisConstants.NAME, new StringProperty(def.getId(), def.getQueryName(), def.getLocalName(), def
         .getDisplayName(), name));

      FolderData newFolder = storageA.createFolder(folder, folderTypeDefinition, properties, null, null);
      //      newFolder.setName(name);
      return (FolderData)newFolder;
   }
   
   protected String convertStreamToString(InputStream is) throws IOException {
      if (is != null) {
         Writer writer = new StringWriter();
      
         char[] buffer = new char[1024];
         try {
             Reader reader = new BufferedReader(
                     new InputStreamReader(is, "UTF-8"));
             int n;
             while ((n = reader.read(buffer)) != -1) {
                 writer.write(buffer, 0, n);
             }
         } finally {
             is.close();
         }
         return writer.toString();
      } else {        
         return "";
      }
   }

}
