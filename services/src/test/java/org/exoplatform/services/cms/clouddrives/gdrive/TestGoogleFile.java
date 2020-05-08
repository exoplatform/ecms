/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.cms.clouddrives.gdrive;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.File;

import junit.framework.Assert;

/**
 * TODO Test should be adopted to API rework.
 *
 */
public class TestGoogleFile {

  protected static final Log LOG        = ExoLogger.getLogger(TestGoogleFile.class);

  public List<File>          listOfGoogleFiles;

  public List<CloudFile>     listOfCloudFiles;

  public static final String DATE1      = "2012-07-24T15:19:34.491Z";

  public static final String DATE2      = "2007-05-01T15:43:26.3452Z";

  public static File         file1;

  public static File         file2;

  public static final String ID1        = "firstId";

  public static final String ID2        = "secondId";

  public static final String TITLE1     = "title1";

  public static final String TITLE2     = "title2";

  public static final String TYPE1      = "application/vnd.google-apps.folder";

  public static final String TYPE2      = "application/vnd.google-apps.file";

  public static final String LINK1      = "link1";

  public static final String LINK2      = "link2";

  public static final String LAST_USER1 = "last_user1";

  public static final String LAST_USER2 = "last_user2";

  public static final String AUTHOR1    = "author1";

  public static final String AUTHOR2    = "author2";

  @BeforeClass
  public void initClass() {
    file1 = new File();
    file2 = new File();
    List<String> owners1 = new ArrayList<String>();
    List<String> owners2 = new ArrayList<String>();
    owners1.add(AUTHOR1);
    owners2.add(AUTHOR2);
    TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
    DateTime dt = new DateTime(new Date(), tz);

    file1.setId(ID1);
    file2.setId(ID2);

    file1.setTitle(TITLE1);
    file2.setTitle(TITLE2);

    file1.setMimeType(TYPE1);
    file2.setMimeType(TYPE2);

    file1.setAlternateLink(LINK1);
    file2.setAlternateLink(LINK2);

    file1.setLastModifyingUserName(LAST_USER1);
    file2.setLastModifyingUserName(LAST_USER2);

    file1.setOwnerNames(owners1);
    file2.setOwnerNames(owners2);

    file1.setCreatedDate(dt);
    file2.setCreatedDate(dt);

    file1.setModifiedDate(dt);
    file2.setModifiedDate(dt);
  }

  @BeforeMethod
  public void initMethod() {
    listOfGoogleFiles = new ArrayList<File>();
    listOfCloudFiles = new ArrayList<CloudFile>();
  }

  // TODO need another tests?
//  /**
//   * Test if information about the cloud file is incorrect.
//   */
//  @Test
//  public void testParseGoogleFile() {
//    listOfGoogleFiles.add(file1);
//    listOfCloudFiles = GoogleDrive.parseGoogleFile(listOfGoogleFiles);
//    CloudFile cf = listOfCloudFiles.get(0);
//    Assert.assertEquals(cf.getId(), ID1);
//    Assert.assertEquals(cf.getTitle(), TITLE1);
//    Assert.assertEquals(cf.getType(), TYPE1);
//    Assert.assertEquals(cf.getLink(), LINK1);
//    Assert.assertEquals(cf.getLastUser(), LAST_USER1);
//    Assert.assertEquals(cf.getAuthor(), AUTHOR1);
//    Assert.assertTrue(cf.isFolder());
//  }
//
//  /**
//   * Test if list isn't full(not all files).
//   */
//  @Test
//  public void testNumberOfFiles() {
//    listOfGoogleFiles.add(file1);
//    listOfGoogleFiles.add(file2);
//    listOfCloudFiles = GoogleDrive.parseGoogleFile(listOfGoogleFiles);
//    Assert.assertEquals(listOfCloudFiles.size(), 2);
//  }

  /**
   * 
   */
  @Test
  public void testParseRFC3339Date() {
    //Calendar cDate1 = GoogleDriveUtils.parseRFC3339Date(DATE1);
    Calendar cDate1 = Calendar.getInstance(); 
    //Calendar cDate2 = GoogleDriveUtils.parseRFC3339Date(DATE2);
    Calendar cDate2 = Calendar.getInstance();
    
    Assert.assertNotNull(cDate1);
    Assert.assertNotNull(cDate2);

    Assert.assertEquals("Tue Jul 24 15:19:34 FET 2012", cDate1.getTime().toString());
    Assert.assertEquals("Tue May 01 15:43:29 FEST 2007", cDate2.getTime().toString());
  }
}
