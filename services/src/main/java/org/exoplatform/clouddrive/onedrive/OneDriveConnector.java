package org.exoplatform.clouddrive.onedrive;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.clouddrive.*;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class OneDriveConnector extends CloudDriveConnector {
  protected static final Log LOG = ExoLogger.getLogger(OneDriveConnector.class);

  public OneDriveConnector(RepositoryService jcrService,
                           SessionProviderService sessionProviders,
                           NodeFinder finder,
                           ExtendedMimeTypeResolver mimeTypes,
                           InitParams params)
      throws ConfigurationException {
    super(jcrService, sessionProviders, finder, mimeTypes, params);
  }

  @Override
  protected CloudProvider createProvider() throws ConfigurationException {
    String authUrl = "";
    try {
      authUrl = new String(Files.readAllBytes(new File(System.getProperty("user.home") + "/authurl.txt").toPath()), Charset.forName("UTF-8"));
    } catch (IOException e) {
      authUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?\n" +
              "client_id=9920cb10-7801-49d8-9a75-2d8252eae87c\n" +
              "&response_type=code\n" +
              "&redirect_uri=http://localhost:8080/portal/rest/clouddrive/connect/onedrive\n" +
              "&response_mode=query\n" +
              "&scope=https://graph.microsoft.com/Files.Read.All https://graph.microsoft.com/Files.Read https://graph.microsoft.com/Files.Read.Selected https://graph.microsoft.com/Files.ReadWrite https://graph.microsoft.com/Files.ReadWrite.All https://graph.microsoft.com/Files.ReadWrite.AppFolder https://graph.microsoft.com/Files.ReadWrite.Selected https://graph.microsoft.com/User.Read https://graph.microsoft.com/User.ReadWrite https://graph.microsoft.com/User.ReadWrite offline_access\n" +
              "&state=1233333333";
//      e.printStackTrace();
    }
    return new OneDriveProvider(getProviderId(), getProviderName(), authUrl);
  }

  @Override
  protected CloudUser authenticate(Map<String, String> params) throws CloudDriveException {
    LOG.info("authenticate: " + params.toString());
    return new OneDriveUser("id-user1", "username", "some1@email.com", provider);
  }

  @Override
  protected JCRLocalOneDrive createDrive(CloudUser user, Node driveNode) throws CloudDriveException, RepositoryException {
    LOG.info("createDrive");
    return new JCRLocalOneDrive(user, driveNode, sessionProviders, jcrFinder, mimeTypes);
  }

  @Override
  protected CloudDrive loadDrive(Node driveNode) throws CloudDriveException, RepositoryException {
    LOG.info("loadDrive");
    JCRLocalCloudDrive.checkNotTrashed(driveNode);
    JCRLocalCloudDrive.migrateName(driveNode);
    return new JCRLocalOneDrive(new OneDriveUser("id-user1", "username", "some1@email.com", createProvider()),
                                driveNode,
                                sessionProviders,
                                jcrFinder,
                                mimeTypes);
  }
}
