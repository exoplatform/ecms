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
import org.xcmis.spi.CmisRegistry;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.model.Property;

import java.io.File;
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

   // protected StorageImpl storage;

   protected JcrCmisRegistry registry;

   private volatile static boolean shoutDown;

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
         //         for (NodeIterator unfiled =
         //            rootNode.getNode(StorageImpl.XCMIS_SYSTEM_PATH.substring(1) + "/" + StorageImpl.XCMIS_UNFILED).getNodes(); unfiled
         //            .hasNext();)
         //         {
         //            unfiled.nextNode().remove();
         //         }

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
         LOG.error("Exception in tearDown() ", e);
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

}
