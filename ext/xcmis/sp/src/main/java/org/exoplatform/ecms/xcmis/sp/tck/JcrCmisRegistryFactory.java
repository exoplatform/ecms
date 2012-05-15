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

package org.exoplatform.ecms.xcmis.sp.tck;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.spi.CmisRegistry;
import org.xcmis.spi.CmisRegistryFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class JcrCmisRegistryFactory implements CmisRegistryFactory
{

   private CmisRegistry reg;

   private static final Log LOG = ExoLogger.getLogger(JcrCmisRegistryFactory.class.getName());

   public JcrCmisRegistryFactory()
   {
      URL resource = getClass().getResource("tck-configuration.xml");
      try
      {
         StandaloneContainer.setConfigurationURL(resource.toString());
         StandaloneContainer container = StandaloneContainer.getInstance();
         reg = (CmisRegistry)container.getComponentInstanceOfType(CmisRegistry.class);
      }
      catch (MalformedURLException e)
      {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage(), e);
        }
      }
      catch (Exception e)
      {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage(), e);
        }
      }
   }

   public CmisRegistry getRegistry()
   {
      return reg;
   }

}
