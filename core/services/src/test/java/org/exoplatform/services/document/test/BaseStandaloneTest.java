/**
 * Copyright (C) 2023 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
*/
package org.exoplatform.services.document.test;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.PortalContainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: BaseStandaloneTest.java 111 2008-11-11 11:11:11Z serg $
 */
public class BaseStandaloneTest extends TestCase
{

   public PortalContainer pcontainer;

   protected MimeTypeResolver mimetypeResolver = new MimeTypeResolver();

   protected File file;

   public void setUp() throws Exception
   {
      pcontainer = PortalContainer.getInstance();
   }

   @Override
   protected void tearDown() throws Exception
   {
      if (file != null)
         file.delete();
      super.tearDown();
   }

   public <T> T getComponentInstanceOfType(Class<T> componentType)
   {
      return pcontainer.getComponentInstanceOfType(componentType);
   }

   /**
    * Its a wrapper to cheat security.
    */
   public File createTempFile(String prefix, String suffix) throws IOException
   {
      return File.createTempFile(prefix, suffix);
   }

   /**
    * Its a wrapper to cheat security.
    */
   public boolean createNewFile(File f) throws IOException
   {
      return f.createNewFile();
   }

   /**
    * Its a wrapper to cheat security.
    */
   public InputStream getInputStream(File f) throws IOException
   {
      return new FileInputStream(f);
   }

   /**
    * Its a wrapper to cheat security.
    */
   public boolean deleteFile(File f) throws IOException
   {
      return f.delete();
   }

   public String normalizeWhitespaces(String str)
   {
      str = str.trim();
      str = str.replaceAll("\\s+", " ");
      return str;
   }

   /**
    * Replaces the first part of the content of the input stream that matches with the regular expression
    * with the provided replacement.
    */
   public void replaceFirstInFile(InputStream is, File targetFile, String regExpr, String replacement) throws IOException
   {
      replaceFirstInFile(is, targetFile, new String[]{regExpr}, new String[]{replacement});
   }

   /**
    * Replaces the first part of the content of the input stream that matches with each regular expressions
    * with the provided replacements.
    */
   public void replaceFirstInFile(InputStream is, File targetFile, String[] regExprs, String[] replacements) throws IOException
   {
      FileOutputStream fos = null;
      try
      {
         String fileContent = IOUtils.toString(is);
         for (int i = 0; i < regExprs.length; i++)
         {
            fileContent = fileContent.replaceFirst(regExprs[i], replacements[i]);
         }
         fos = new FileOutputStream(targetFile);
         IOUtils.write(fileContent, fos);
      }
      finally
      {
         IOUtils.closeQuietly(fos);
         IOUtils.closeQuietly(is);
      }
   }

   /**
    * Replaces the first part of the content of the given entry that matches with the regular expression
    * with the provided replacement directly into the target zip file.
    */
   public void replaceFirstInZip(InputStream is, File targetZipFile, String entryName, String regExpr, String replacement)
      throws IOException
   {
      replaceFirstInZip(is, targetZipFile, entryName, new String[]{regExpr}, new String[]{replacement});
   }

   /**
    * Replaces the first part of the content of the given entry that matches with each regular expressions
    * with the provided replacements directly into the target zip file.
    */
   public void replaceFirstInZip(InputStream is, File targetZipFile, String entryName, String[] regExprs, String[] replacements)
      throws IOException
   {
      ZipInputStream inZip = null;
      ZipOutputStream outZip = null;
      try
      {
         inZip = new ZipInputStream(is);
         outZip = new ZipOutputStream(new FileOutputStream(targetZipFile));

         for (ZipEntry in; (in = inZip.getNextEntry()) != null;)
         {
            ZipEntry out = new ZipEntry(in.getName());
            outZip.putNextEntry(out);
            if (in.getName().equals(entryName))
            {
               String fileContent = IOUtils.toString(inZip);
               for (int i = 0; i < regExprs.length; i++)
               {
                  fileContent = fileContent.replaceFirst(regExprs[i], replacements[i]);
               }
               out.setSize(fileContent.length());
               IOUtils.write(fileContent, outZip);
            }
            else
            {
               IOUtils.copy(inZip, outZip);
            }
            outZip.closeEntry();
         }
      }
      finally
      {
         IOUtils.closeQuietly(inZip);
         IOUtils.closeQuietly(outZip);
      }
   }
}
