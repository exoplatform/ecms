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
package org.xcmis.sp.jcr.exo.query;

import org.xcmis.search.InvalidQueryException;
import org.xcmis.search.content.Schema.Column;
import org.xcmis.search.content.Schema.Table;
import org.xcmis.search.model.constraint.Operator;
import org.xcmis.search.model.source.SelectorName;
import org.xcmis.search.query.QueryExecutionException;
import org.xcmis.sp.jcr.exo.index.CmisSchema;
import org.xcmis.spi.InvalidArgumentException;
import org.xcmis.spi.ItemsIterator;
import org.xcmis.spi.query.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class AvailableQueryOperatorsTest extends BaseQueryTest
{
   private CmisSchema cmisSchema;

   /**
    * @see org.xcmis.sp.jcr.exo.query.BaseQueryTest#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      cmisSchema = new CmisSchema(storage);

   }

   public void testCmisDocument() throws Exception
   {
      checkCmisType("cmis:document");
   }

   public void testCmisFolder() throws Exception
   {
      checkCmisType("cmis:folder");
   }

   public void testCmisRelationship() throws Exception
   {
      checkCmisType("cmis:relationship");
   }

   public void testCmisPolicy() throws Exception
   {
      checkCmisType("cmis:policy");
   }

   /**
    * @param documentType
    * @throws QueryExecutionException
    * @throws InvalidQueryException
    */
   private void checkCmisType(final String documentType) throws QueryExecutionException, InvalidQueryException
   {
      Table table = cmisSchema.getTable(new SelectorName(documentType));

      for (Column column : table.getColumns())
      {
         checkValid(documentType, column);
         checkInValid(documentType, column);
      }
   }

   /**
    * Check invalid operator's
    * @param column
    * @throws QueryExecutionException 
    */
   private void checkInValid(String tableName, Column column) throws QueryExecutionException
   {

      Operator[] unAvailableQueryOperators = getUnAvailableQueryOperators(column.getAvailableQueryOperators());

      for (Operator operator : unAvailableQueryOperators)
      {

         StringBuffer query = new StringBuffer();
         query.append("SELECT ").append(tableName).append(".*");
         query.append(" FROM ").append(tableName);
         query.append(" WHERE ");
         query.append(tableName).append(".").append(column.getName());
         switch (operator)
         {

            case EQUAL_TO :
               query.append("= 1");
               break;
            case GREATER_THAN :
               query.append("> 1");
               break;
            case GREATER_THAN_OR_EQUAL_TO :
               query.append(">= 1");
               break;
            case LESS_THAN :
               query.append("< 1");
               break;
            case LESS_THAN_OR_EQUAL_TO :
               query.append("<= 1");
               break;
            case LIKE :
               query.append(" LIKE 'mooo' ");
               break;
            case NOT_EQUAL_TO :
               query.append(" <> 1");
               break;
            default :
               fail("unknown operator " + operator);
               break;
         }
         try
         {
            org.xcmis.spi.query.Query cmisQuery = new org.xcmis.spi.query.Query(query.toString(), true);
            ItemsIterator<Result> result = storage.query(cmisQuery);
            fail("InvalidArgumentException should be thrown for invalid operator " + operator + " for columnt ='"
               + column.getName() + "'");
         }
         catch (InvalidArgumentException e)
         {
            //ok
         }
      }
   }

   /**
    * Check valid operator's
    * 
    * @param column
    * @throws InvalidQueryException 
    * @throws QueryExecutionException 
    */
   private void checkValid(String tableName, Column column) throws QueryExecutionException, InvalidQueryException
   {

      for (Operator operator : column.getAvailableQueryOperators())
      {

         StringBuffer query = new StringBuffer();
         query.append("SELECT ").append(tableName).append(".*");
         query.append(" FROM ").append(tableName);
         query.append(" WHERE ");
         query.append(tableName).append(".").append(column.getName());
         switch (operator)
         {

            case EQUAL_TO :
               query.append("= 1");
               break;
            case GREATER_THAN :
               query.append("> 1");
               break;
            case GREATER_THAN_OR_EQUAL_TO :
               query.append(">= 1");
               break;
            case LESS_THAN :
               query.append("< 1");
               break;
            case LESS_THAN_OR_EQUAL_TO :
               query.append("<= 1");
               break;
            case LIKE :
               query.append(" LIKE 'mooo' ");
               break;
            case NOT_EQUAL_TO :
               query.append(" <> 1");
               break;
            default :
               fail("unknown operator " + operator);
               break;
         }
         org.xcmis.spi.query.Query cmisQuery = new org.xcmis.spi.query.Query(query.toString(), true);
         ItemsIterator<Result> result = storage.query(cmisQuery);
      }
   }

   /**
    * Return the array of unAvailableQueryOperators
    * @param availableQueryOperators
    * @return
    */
   private Operator[] getUnAvailableQueryOperators(Operator[] availableQueryOperators)
   {
      List<Operator> result = new ArrayList<Operator>();
      for (Operator operator : Operator.ALL)
      {
         boolean isValid = false;
         for (int i = 0; i < availableQueryOperators.length; i++)
         {
            if (operator.equals(availableQueryOperators[i]))
            {
               isValid = true;
               break;
            }
         }
         if (!isValid)
         {
            result.add(operator);
         }
      }
      return result.toArray(new Operator[result.size()]);
   }
}
