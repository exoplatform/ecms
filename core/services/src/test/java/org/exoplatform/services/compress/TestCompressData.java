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

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by The eXo Platform SAS Author : Chung Nguyen
 * nguyenchung136@yahoo.com Feb 13, 2006
 */
public class TestCompressData extends TestCase
{

   public TestCompressData(String name)
   {
      super(name);
   }

   public void testCompressData() throws Exception
   {
      CompressData compress = new CompressData();
      CompressData compressIS = new CompressData();
      InputStream in = new FileInputStream("src/test/resources/ZipService.java");
      InputStream in2 = new FileInputStream("src/test/resources/helper.txt");
      // ---------- TEST InputStream --------------//
      compressIS.addInputStream("ZipService.java", in);
      compressIS.addInputStream("helper.txt", in2);
      compressIS.createJarFile("target/ZipServiceJar");
      compressIS.createZipFile("target/ZipServiceZip");
      in.close();
      in2.close();

      // ----------- Test with Add File ------------------//

      File file = new File("src/test/resources/ZipService.java");
      File file2 = new File("src/test/resources/helper.txt");
      compress.addFile("ZipService.java", file);
      compress.addFile("helper.txt", file2);
      compress.createZipFile("target/testZipFile");
      compress.createJarFile("target/testJarFile");
      // compress.cleanDataInstance();
      // --------------- Test thu muc --------------------------------//
      // TODO what is t?
      //    File folder = new File("/home/exo/setup/tailieu/chung/hcm/images");
      //    File folder1 = new File("/home/exo/setup/tailieu/chung/hcm/xuly");
      //    CompressData compressF = new CompressData();
      //    compressF.addDir(folder);
      //    compressF.addDir(folder1);
      //    compressF.createZipFile("/home/exo/setup/tailieu/chung/hcm/TestZip");
      // compress.createJarFile("/home/exo/setup/tailieu/chung/hcm/TestJar");
   }

   /**
    * Testcase based on http://jira.exoplatform.org/browse/KER-94
    * @throws Exception 
    */
   public void testCompressIS() throws Exception
   {

      CompressData compressData = new CompressData();

      File f1 = new File("src/test/resources/ZipService.java");
      File f2 = new File("src/test/resources/helper.txt");

      compressData.addFile("ZipService.java", f1);
      compressData.addFile("helper.txt", f2);

      OutputStream outStream = new FileOutputStream(new File("target/CompressedZipIS.zip"));
      compressData.createZip(outStream);
      outStream.close();

      outStream = new FileOutputStream(new File("target/CompressedJarIS.jar"));
      compressData.createJar(outStream);
      outStream.close();

   }
}
