/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.compress;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by The eXo Platform SAS Author : Chung Nguyen
 * nguyenchung136@yahoo.com Feb 10, 2006
 */
public class CompressData
{
   /**
    * The logger
    */
   private static final Log LOG = ExoLogger.getLogger("exo.kernel.component.common.CompressData");

   private String base_;

   private List<DataInstance> datas_ = new ArrayList<DataInstance>();

   protected static final int EOF = -1;

   protected static final int BUFFER = 2048;

   public CompressData()
   {

   }

   public CompressData(String base)
   {
      base_ = base;
   }

   public String getBase()
   {
      return base_;
   }

   public void addFile(String entryName, File file)
   {
      try
      {
         InputStream is = new FileInputStream(file);
         datas_.add(new InputStreamDataInstance(entryName, is));
      }
      catch (FileNotFoundException e)
      {
         LOG.error(e.getLocalizedMessage(), e);
      }
   }

   public void addDir(File srcDir)
   {
      if (srcDir.isFile())
      {
         addFile(srcDir.getName(), srcDir);
      }
      else
      {
         datas_.add(new FileDataInstance(srcDir.getName(), srcDir));
      }
      // create recursive loop to go through all files in the srcDir.....
   }

   public void addInputStream(String entryName, InputStream is) throws Exception
   {
      datas_.add(new InputStreamDataInstance(entryName, is));
   }

   public void createZipFile(String fileName) throws Exception
   {
      File fileZip = new File(fileName + ".zip");
      FileOutputStream out = new FileOutputStream(fileZip);
      ZipOutputStream zos = new ZipOutputStream(out);
      int size = datas_.size();
      byte InputData[] = new byte[BUFFER];
      if (size < 0)
      {
         throw new Exception("Data size is null");
      }
      for (int i = 0; i < size; i++)
      {
         DataInstance di = datas_.get(i);
         if (di instanceof InputStreamDataInstance)
         {
            InputStream is = di.getInputStream();
            zos.putNextEntry(new ZipEntry(di.getEntryName()));
            int len;
            while ((len = is.read(InputData)) != EOF)
            {
               zos.write(InputData, 0, len);
            }
            zos.closeEntry();
         }
         else if (di instanceof FileDataInstance)
         {
            di.getZipOut(true, zos);
         }
      }
      zos.close();
      out.close();
   }

   public void createZip(OutputStream os) throws Exception
   {
      int size = datas_.size();
      ZipOutputStream zos = new ZipOutputStream(os);
      if (size == 0)
         throw new Exception("Data is null");
      for (int i = 0; i < size; i++)
      {
         DataInstance di = datas_.get(i);
         if (di instanceof InputStreamDataInstance)
         {
            InputStream is = di.getInputStream();
            zos.putNextEntry(new ZipEntry(di.getEntryName()));
            int len;
            byte InputData[] = new byte[BUFFER];
            while ((len = is.read(InputData)) != EOF)
            {
               zos.write(InputData, 0, len);
            }
            zos.closeEntry();
         }
         else if (di instanceof FileDataInstance)
         {
            di.setType("Zip");
            InputStream is = di.getInputStream();
            int len;
            byte[] data = new byte[BUFFER];
            while ((len = is.read(data)) != EOF)
            {
               os.write(data, 0, len);
            }
            is.close();
         }
      }
      zos.close();
      os.close();
   }

   public void createJarFile(String fileName) throws Exception
   {
      File fileZip = new File(fileName + ".jar");
      FileOutputStream out = new FileOutputStream(fileZip);
      JarOutputStream jos = new JarOutputStream(out);
      int size = datas_.size();
      if (size < 0)
         throw new Exception("Data size is null");
      for (int i = 0; i < size; i++)
      {
         DataInstance di = datas_.get(i);

         if (di instanceof InputStreamDataInstance)
         {
            String entryName = di.getEntryName();
            InputStream is = di.getInputStream();
            jos.putNextEntry(new ZipEntry(entryName));
            int len;
            byte InputData[] = new byte[BUFFER];
            while ((len = is.read(InputData)) != EOF)
            {
               jos.write(InputData, 0, len);
            }
            jos.closeEntry();
         }
         else if (di instanceof FileDataInstance)
         {
            di.getJarOut(true, jos);
         }
      }
      jos.close();
      out.close();
   }

   public void createJar(OutputStream os) throws Exception
   {
      int size = datas_.size();
      JarOutputStream jos = new JarOutputStream(os);
      if (size == 0)
         throw new Exception("Data is null");
      for (int i = 0; i < size; i++)
      {
         DataInstance di = datas_.get(i);
         if (di instanceof InputStreamDataInstance)
         {
            InputStream is = di.getInputStream();

            jos.putNextEntry(new ZipEntry(di.getEntryName()));
            int len;
            byte InputData[] = new byte[BUFFER];
            while ((len = is.read(InputData)) != EOF)
            {
               jos.write(InputData, 0, len);
            }
            jos.closeEntry();
            is.close();
         }
         else if (di instanceof FileDataInstance)
         {
            di.setType("Jar");
            InputStream is = di.getInputStream();
            int len;
            byte[] data = new byte[BUFFER];
            while ((len = is.read(data)) != EOF)
            {
               os.write(data, 0, len);
            }
            is.close();
         }
      }
      jos.close();
      os.close();
   }

   public void cleanDataInstance()
   {
      int count = datas_.size();
      for (int i = 0; i < count; i++)
      {
         datas_.remove(i);
      }
   }

   abstract public static class DataInstance
   {
      protected String entryName_;

      protected String typeZip_;

      abstract public InputStream getInputStream();

      abstract public void getJarOut(boolean containParent, JarOutputStream jos) throws Exception;

      abstract public void getZipOut(boolean containParent, ZipOutputStream zos) throws Exception;

      public String getEntryName()
      {
         return entryName_;
      }

      public void setType(String typeZip)
      {
         typeZip_ = typeZip;
      }

      public String getType()
      {
         return typeZip_;
      }

   }

   public static class FileDataInstance extends DataInstance
   {
      private File file_;

      public FileDataInstance(String entryName, File file)
      {
         entryName_ = entryName;
         file_ = file;
      }

      @Override
      public InputStream getInputStream()
      {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();

         if (getType().equals("Zip"))
         {
            ZipOutputStream zos = new ZipOutputStream(baos);
            try
            {
               getZipOut(true, zos);
            }
            catch (Exception e)
            {
               LOG.error(e.getLocalizedMessage(), e);
            }
         }
         else
         {
            JarOutputStream jos;
            try
            {
               jos = new JarOutputStream(baos);
               getJarOut(true, jos);
            }
            catch (Exception e)
            {
               LOG.error(e.getLocalizedMessage(), e);
            }
         }
         InputStream is = new ByteArrayInputStream(baos.toByteArray());

         try
         {
            baos.close();
         }
         catch (IOException e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }

         return is;
      }

      @Override
      public void getJarOut(boolean containParent, JarOutputStream jos) throws Exception
      {
         String path = file_.getAbsolutePath();
         InputStream bufInput = null;
         List<File> list = listFile(file_);
         if (file_.isDirectory())
            list.remove(file_);
         if (list == null || list.size() < 1)
            throw new Exception("nothing in the list");
         for (File f : list)
         {
            StringBuilder filePath = new StringBuilder(f.getAbsolutePath());

            if (f.getAbsolutePath().startsWith(path))
            {
               if (containParent && file_.isDirectory())
               {
                  filePath = new StringBuilder(file_.getName());
                  filePath.append(File.separator);
                  filePath.append(f.getAbsolutePath().substring(path.length() + 1));
               }
               else if (file_.isDirectory())
               {
                  filePath = new StringBuilder(f.getAbsolutePath().substring(path.length() + 1));
               }
               else
               {
                  filePath = new StringBuilder(file_.getName());
               }
            }

            if (f.isFile())
            {
               bufInput = new FileInputStream(f);
            }
            else
            {
               filePath.append("/");
            }

            addToArchive(jos, bufInput, filePath.toString());
         }
      }

      private List<File> listFile(File dir)
      {
         final List<File> list = new ArrayList<File>();
         if (dir.isFile())
         {
            list.add(dir);
            return list;
         }
         dir.listFiles(new FileFilter()
         {
            public boolean accept(File f)
            {
               if (f.isDirectory())
                  list.addAll(listFile(f));
               list.add(f);
               return true;
            }
         });
         return list;
      }

      public ZipOutputStream addToArchive(ZipOutputStream zipOutput, InputStream input, String entryName1)
         throws Exception
      {
         byte data[] = new byte[BUFFER];
         ZipEntry entry = new ZipEntry(entryName1);
         zipOutput.putNextEntry(entry);
         if (input != null)
         {
            int count;
            while ((count = input.read(data, 0, BUFFER)) != EOF)
               zipOutput.write(data, 0, count);
         }
         zipOutput.closeEntry();
         return zipOutput;
      }

      public JarOutputStream addToArchive(JarOutputStream jarOutput, InputStream input, String entryName1)
         throws Exception
      {
         byte data[] = new byte[BUFFER];
         JarEntry entry = new JarEntry(entryName1);
         jarOutput.putNextEntry(entry);
         if (input != null)
         {
            int count;
            while ((count = input.read(data, 0, BUFFER)) != EOF)
               jarOutput.write(data, 0, count);
         }
         jarOutput.closeEntry();
         return jarOutput;
      }

      @Override
      public void getZipOut(boolean containParent, ZipOutputStream zos) throws Exception
      {
         String path = file_.getAbsolutePath();
         InputStream bufInput = null;
         List<File> list = listFile(file_);
         if (file_.isDirectory())
            list.remove(file_);
         if (list == null || list.size() < 1)
            throw new Exception("nothing in the list");
         for (File f : list)
         {
            StringBuilder filePath = new StringBuilder(f.getAbsolutePath());

            if (f.getAbsolutePath().startsWith(path))
            {
               if (containParent && file_.isDirectory())
               {
                  filePath = new StringBuilder(file_.getName());
                  filePath.append(File.separator);
                  filePath.append(f.getAbsolutePath().substring(path.length() + 1));
               }
               else if (file_.isDirectory())
               {
                  filePath = new StringBuilder(f.getAbsolutePath().substring(path.length() + 1));
               }
               else
               {
                  filePath = new StringBuilder(file_.getName());
               }
            }

            if (f.isFile())
            {
               bufInput = new FileInputStream(f);
            }
            else
            {
               filePath.append("/");
            }

            addToArchive(zos, bufInput, filePath.toString());
         }
      }
   }

   public static class InputStreamDataInstance extends DataInstance
   {
      private InputStream is_;

      public InputStreamDataInstance(String entryName, InputStream is)
      {
         entryName_ = entryName;
         is_ = is;
      }

      @Override
      public InputStream getInputStream()
      {
         return is_;
      }

      @Override
      public void getJarOut(boolean containParent, JarOutputStream jos) throws Exception
      {

      }

      @Override
      public void getZipOut(boolean containParent, ZipOutputStream zos) throws Exception
      {

      }

   }

}
