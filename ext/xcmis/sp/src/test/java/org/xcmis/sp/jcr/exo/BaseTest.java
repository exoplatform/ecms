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

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.StorageProvider;
import org.xcmis.spi.model.Property;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

public abstract class BaseTest extends TestCase
{

   private static final Log LOG = ExoLogger.getLogger(BaseTest.class);

   protected final String wsName = "ws1";

   protected final String jcrRepositoryName = "db1";

   protected String cmisRepositoryId = "cmis1";

   protected String testRootFolderId;

   protected StandaloneContainer container;

   protected SessionImpl session;

   protected RepositoryImpl repository;

   protected CredentialsImpl credentials;

   protected RepositoryService repositoryService;

   protected Node root;

   protected Node relationshipsNode;

   protected StorageProvider storageProvider;

   protected ThreadLocalSessionProviderService sessionProviderService;

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
      StandaloneContainer.addConfigurationURL(containerConf);
      container = StandaloneContainer.getInstance();

      if (System.getProperty("java.security.auth.login.config") == null)
      {
         System.setProperty("java.security.auth.login.config", loginConf);
      }

      credentials = new CredentialsImpl("root", "exo".toCharArray());

      repositoryService = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      repository = (RepositoryImpl)repositoryService.getRepository(jcrRepositoryName);

      storageProvider = (StorageProvider)container.getComponentInstanceOfType(StorageProvider.class);

      session = (SessionImpl)repository.login(credentials, wsName);

      root = session.getRootNode();

      sessionProviderService =
         (ThreadLocalSessionProviderService)container
            .getComponentInstanceOfType(ThreadLocalSessionProviderService.class);
      assertNotNull(sessionProviderService);

      ConversationState cs = new ConversationState(new Identity("root"));
      ConversationState.setCurrent(cs);

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
         for (NodeIterator unfiled =
            rootNode.getNode(StorageImpl.XCMIS_SYSTEM_PATH.substring(1) + "/" + StorageImpl.XCMIS_UNFILED).getNodes(); unfiled
            .hasNext();)
         {
            unfiled.nextNode().remove();
         }

         session.save();

         if (rootNode.hasNodes())
         {
            // clean test root
            for (NodeIterator children = rootNode.getNodes(); children.hasNext();)
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
      catch (Exception e)
      {
         LOG.error("Exception in tearDown() ", e);
      }
      finally
      {
         session.logout();
      }

      super.tearDown();
   }

   protected void setProperty(ObjectData object, Property<?> property) throws Exception
   {
      Map<String, Property<?>> properties = new HashMap<String, Property<?>>();
      properties.put(property.getId(), property);
      object.setProperties(properties);
   }

}
