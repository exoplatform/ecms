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

package org.exoplatform.services.portletcache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.portlet.MimeResponse;
import javax.portlet.RenderResponse;
import javax.portlet.filter.RenderResponseWrapper;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
class BufferedRenderResponse extends RenderResponseWrapper
{

   /** . */
   private ByteArrayOutputStream buffer;

   /** . */
   private String expirationCache;

   BufferedRenderResponse(RenderResponse response)
   {
      super(response);
   }

   public byte[] getBytes()
   {
      return buffer != null ? buffer.toByteArray() : new byte[0];
   }

   public String getExpirationCache()
   {
      return expirationCache;
   }

   @Override
   public PrintWriter getWriter() throws IOException
   {
      throw new UnsupportedOperationException("Not yet implemented");
   }

   @Override
   public OutputStream getPortletOutputStream() throws IOException
   {
      if (buffer == null)
      {
         buffer = new ByteArrayOutputStream();
      }
      return buffer;
   }

   @Override
   public void addProperty(String key, String value)
   {
      if (MimeResponse.EXPIRATION_CACHE.equals(key))
      {
         if (expirationCache != null)
         {
            expirationCache = value;
         }
      }
      else
      {
         super.addProperty(key, value);
      }
   }

   @Override
   public void setProperty(String key, String value)
   {
      if (MimeResponse.EXPIRATION_CACHE.equals(key))
      {
         expirationCache = value;
      }
      else
      {
         super.addProperty(key, value);
      }
   }
}
