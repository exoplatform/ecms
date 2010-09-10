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

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;
import org.picocontainer.Startable;
import org.xcmis.spi.CmisRegistry;
import org.xcmis.spi.CmisRegistryFactory;
import org.xcmis.spi.RenditionManager;

import java.util.Iterator;

/**
 * @version $Id$
 */
public class ExoContainerCmisRegistry extends CmisRegistry implements Startable, CmisRegistryFactory
{

   //   private static final Logger LOG = Logger.getLogger(ExoContainerCmisRegistry.class);

   protected final InitParams initParams;

   public ExoContainerCmisRegistry(InitParams initParams)
   {
      this.initParams = initParams;
   }

   /**
    * {@inheritDoc}
    */
   @SuppressWarnings("unchecked")
   public void start()
   {
      if (initParams != null)
      {
         Iterator<ValuesParam> vparams = initParams.getValuesParamIterator();
         while (vparams.hasNext())
         {
            ValuesParam next = vparams.next();
            if (next.getName().equalsIgnoreCase("renditionProviders"))
            {
               this.renditionProviders.addAll(next.getValues());
            }
         }
      }
      RenditionManager manager = RenditionManager.getInstance();
      manager.addRenditionProviders(renditionProviders);
      setFactory(this);
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
   }

   public CmisRegistry getRegistry()
   {
      return (CmisRegistry)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(CmisRegistry.class);
   }

}
