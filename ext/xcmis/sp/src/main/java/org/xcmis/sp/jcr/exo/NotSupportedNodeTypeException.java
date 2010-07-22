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

import org.xcmis.spi.CmisRuntimeException;

/**
 * Should be thrown if requested JCR node-type registered but that type is not
 * one of supported by xCMIS implementation.
 * 
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: NotSupportedNodeTypeException.java 1260 2010-06-09 09:18:30Z
 *          andrew00x $
 */
class NotSupportedNodeTypeException extends CmisRuntimeException
{

   private static final long serialVersionUID = -2990445717857972378L;

   public NotSupportedNodeTypeException(String message)
   {
      super(message);
   }

}
