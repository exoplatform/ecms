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

package org.exoplatform.ecms.xcmis.sp.jcr.exo;

import org.xcmis.spi.Connection;
import org.xcmis.spi.ObjectData;
import org.xcmis.spi.Storage;
import org.xcmis.spi.UpdateConflictException;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: JcrConnection.java 652 2010-04-09 16:19:08Z andrew00x $
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
      // TODO
      ((StorageImpl)storage).session.logout();
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
