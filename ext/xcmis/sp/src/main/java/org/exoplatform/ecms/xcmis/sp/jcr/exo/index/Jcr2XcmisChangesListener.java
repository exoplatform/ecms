/*
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
package org.exoplatform.ecms.xcmis.sp.jcr.exo.index;

import org.exoplatform.ecms.xcmis.sp.jcr.exo.StorageConfiguration;
import org.exoplatform.ecms.xcmis.sp.jcr.exo.StorageImpl;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.picocontainer.Startable;
import org.xcmis.spi.CmisRegistry;

import javax.jcr.Session;

/**
 * @author <a href="mailto:foo@bar.org">Foo Bar</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z
 *          aheritier $
 * 
 */
public class Jcr2XcmisChangesListener implements ItemsPersistenceListener, Startable
{

   //   private final DriveCmisRegistry cmisRegistry;

   private String ws;

   private Session session;

   private final StorageConfiguration conf =
      new StorageConfiguration("ppc", "repository", "collaboration", "/", null, null, "");

   private final StorageImpl storage = new StorageImpl(session, conf, null, null/*TODO*/);

   public Jcr2XcmisChangesListener(PersistentDataManager dataManager, CmisRegistry cmisRegistry)
   {
      super();
      //      this.cmisRegistry = cmisRegistry;
      //      dataManager.addItemPersistenceListener(this);
      //      Set<RepositoryShortInfo> storageInfos = cmisRegistry.getStorageInfos();
      //      for (RepositoryShortInfo i : storageInfos)
      //      {
      //         String repositoryId = i.getRepositoryId();
      //         StorageImpl storage = (StorageImpl)cmisRegistry.getConnection(repositoryId).getStorage();
      //         StorageConfiguration storageConfiguration =
      //            ((StorageImpl)cmisRegistry.getConnection(repositoryId).getStorage()).getStorageConfiguration();
      //         String workspace = storageConfiguration.getWorkspace();
      //         if (ws.equals(workspace))
      //         {
      //            
      //         }
      //      }
   }

   /**
    * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#isTXAware()
    */
   @Override
   public boolean isTXAware()
   {
      return false;
   }

   /**
    * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#onSaveItems(org.exoplatform.services.jcr.dataflow.ItemStateChangesLog)
    */
   @Override
   public void onSaveItems(ItemStateChangesLog itemStates)
   {
      System.out.println(itemStates.dump());
   }

   @Override
   public void start()
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void stop()
   {
      // TODO Auto-generated method stub

   }

}
