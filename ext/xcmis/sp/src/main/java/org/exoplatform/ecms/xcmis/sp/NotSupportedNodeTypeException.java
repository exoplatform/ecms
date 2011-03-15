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

import org.xcmis.spi.CmisRuntimeException;

/**
 * Should be thrown if requested JCR node-type registered but that type is not
 * one of supported by xCMIS implementation.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: NotSupportedNodeTypeException.java 1260 2010-06-09 09:18:30Z
 *          andrew00x $
 */
public class NotSupportedNodeTypeException extends CmisRuntimeException
{

   private static final long serialVersionUID = -2990445717857972378L;

   public NotSupportedNodeTypeException(String message)
   {
      super(message);
   }

}
