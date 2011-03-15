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
