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

import org.xcmis.spi.query.Result;
import org.xcmis.spi.query.Score;

/**
 * Single row from query result.
 */
public class ResultImpl implements Result
{

   private final String id;

   private final String[] properties;

   private final Score score;

   public ResultImpl(String id, String[] properties, Score score)
   {
      this.id = id;
      this.properties = properties;
      this.score = score;
   }

   public String[] getPropertyNames()
   {
      return properties;
   }

   public String getObjectId()
   {
      return id;
   }

   public Score getScore()
   {
      return score;
   }

}