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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;

import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * TODO Test should be adopted to API rework.
 */
public class TestGoogleDrive {
  protected static final Log                  LOG           = ExoLogger.getLogger(TestGoogleDrive.class);

  private final String                        CODE1         = "4/8GJ7lnHSD5xbUPvFgHfWSjgZE01r.cjMtzpMWxWoYOl05ti8ZT3YJt8xgdAI";

  private final String                        CODE2         = "4/kcnLNaH7ay_cFtLq6v4PEIbon6_O.EoxkoUh1UXMZOl05ti8ZT3ZrzcnddAI";

  private final String                        GRANT_TYPE    = "authorization_code";

  private static final String                 SCOPES        = "https://www.googleapis.com/auth/userinfo.email "
      + "https://www.googleapis.com/auth/userinfo.profile " + "https://www.googleapis.com/auth/drive.readonly";

  private GoogleUser                          googleUser;

  private GoogleAuthorizationCodeFlow         flow;

  private GoogleAuthorizationCodeTokenRequest tokenRequest;

  private CredentialUtils                     credentialHelper;

  private Credential                          credential;

  private GoogleProvider                      provider;

  private final String                        PROVIDER_ID   = "gdrive";

  private final String                        PROVIDER_NAME = "Google Drive";

  @BeforeClass
  public void init() throws IOException, CloudDriveException {
    credentialHelper = new CredentialUtils();
    flow = credentialHelper.getFlow();
    tokenRequest = flow.newTokenRequest(CODE1);

    credential = credentialHelper.getCredential(CODE2);
    provider = new GoogleProvider(PROVIDER_ID, PROVIDER_NAME, "", "");
    // googleUser = new GoogleUser("name", "email", CODE2, provider, credential, "");
    // gDrive = new GoogleDrive(googleUser, "");

  }

  @Test
  public void testListOfFiles() throws CloudDriveException {
    // List<CloudFile> listOfFiles = gDrive.listFiles();
    List<CloudFile> listOfFiles = new ArrayList<CloudFile>();

    CloudFile first = listOfFiles.get(0);
    LOG.info("Number files in list: " + listOfFiles.size());
    Assert.assertNotNull(listOfFiles);
    Assert.assertTrue(listOfFiles.size() >= 0);
    Assert.assertNotNull(first.getAuthor());
    Assert.assertNotNull(first.getCreatedDate());
    Assert.assertNotNull(first.getId());
    Assert.assertNotNull(first.getTitle());
    Assert.assertNotNull(first.getType());
    Assert.assertNotNull(first.getLink());
    for (CloudFile cf : listOfFiles) {
      LOG.info("Title----" + cf.getTitle());
    }
  }

  @Test
  public void testListOfFilesInFolder() throws CloudDriveException {
    // List<CloudFile> listOfFiles = gDrive.listFiles();
    List<CloudFile> listOfFiles = new ArrayList<CloudFile>();

    CloudFile folder = null;
    CloudFile file = null;
    int i = 0;
    while ((folder == null) && listOfFiles.size() > i) {
      file = listOfFiles.get(i);
      if (file.isFolder()) {
        folder = file;
        LOG.info("Folder ----" + folder.getTitle());
        // List<CloudFile> filesInFolder = gDrive.listFiles(folder);
        List<CloudFile> filesInFolder = new ArrayList<CloudFile>();

        Assert.assertNotNull(folder.getAuthor());
        Assert.assertNotNull(folder.getCreatedDate());
        Assert.assertNotNull(folder.getId());
        Assert.assertNotNull(folder.getTitle());
        Assert.assertNotNull(folder.getType());
        Assert.assertNotNull(folder.getLink());
        Assert.assertTrue(folder.isFolder());
        for (CloudFile cf : filesInFolder) {
          LOG.info("Title----" + cf.getTitle());
        }
      }
      i++;
    }
  }

  @Test
  public void testToken() {
    Assert.assertNotNull(flow.getClientId());
    Assert.assertNotNull(flow.getScopes());
    Assert.assertEquals(GRANT_TYPE,
                        tokenRequest.getGrantType());
    Assert.assertEquals(CODE1,
                        tokenRequest.getCode());
    Assert.assertEquals(SCOPES,
                        tokenRequest.getScopes());
  }
}
