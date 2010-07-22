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
package org.xcmis.sp.jcr.exo.index;

import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xcmis.search.value.PathSplitter;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
@Deprecated
public class JcrPathSplitter implements PathSplitter<String>
{
   /**
    * Class loger.
    */
   protected static Log log = ExoLogger.getLogger(JcrPathSplitter.class);

   private final LocationFactory locationFactory;

   /**
    * Constructor.
    * @param locationFactory LocationFactory
    */
   public JcrPathSplitter(LocationFactory locationFactory)
   {
      this.locationFactory = locationFactory;
   }

   /**
    * @see org.xcmis.search.value.PathSplitter#splitPath(java.lang.String)
    */
   public String[] splitPath(String path)
   {
      String[] result = null;
      try
      {
         QPath qpath = locationFactory.parseJCRPath(path).getInternalPath();
         result = new String[qpath.getEntries().length];
         for (int i = 0; i < qpath.getEntries().length; i++)
         {
            result[i] = locationFactory.createJCRName(qpath.getEntries()[i]).getAsString();

         }
      }
      catch (RepositoryException e)
      {
         log.error(e.getLocalizedMessage(), e);
      }
      return result;
   }
}
