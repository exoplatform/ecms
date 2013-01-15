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

import javax.jcr.RepositoryException;

import org.xcmis.spi.CmisRuntimeException;
import org.xcmis.spi.Connection;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.Storage;
import org.xcmis.spi.UpdateConflictException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class JcrConnection extends Connection
{

   private boolean closed;

   public JcrConnection(Storage storage)
   {
      super(storage);
   }

   /**
    * {@inheritDoc}
    */
   public void close()
   {
      try {
        ((StorageImpl)storage).getSession().logout();
      } catch (RepositoryException re) {
        throw new CmisRuntimeException(re.getMessage(), re);
      }
      closed = true;
   }

   /**
    * {@inheritDoc}
    */
   protected void validateChangeToken(ObjectData object, String changeToken) throws UpdateConflictException
   {
      // Do not provide validation at the moment.
      // Some client may not work with this feature ON.
   }

   /**
    * {@inheritDoc}
    */
   protected void checkConnection() throws IllegalStateException
   {
      if (closed)
      {
         throw new IllegalStateException("Connection closed.");
      }
   }

}
