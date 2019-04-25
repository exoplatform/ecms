package org.exoplatform.clouddrive.onedrive;

import org.exoplatform.clouddrive.*;
import org.exoplatform.clouddrive.jcr.JCRLocalCloudDrive;
import org.exoplatform.clouddrive.jcr.NodeFinder;
import org.exoplatform.clouddrive.utils.ExtendedMimeTypeResolver;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

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
      authUrl = new String(Files.readAllBytes(new File("E://authurl.txt").toPath()), Charset.forName("UTF-8"));
    } catch (IOException e) {
      authUrl = "https://login.live.com/oauth20_authorize.srf?client_id=9920cb10-7801-49d8-9a75-2d8252eae87c&scope=FILES.READ.ALL\n" +
              "  &response_type=code&redirect_uri=http://localhost:8080/portal/rest/clouddrive/connect/onedrive";
      e.printStackTrace();
    }
    return new OneDriveProvider(getProviderId(), getProviderName(), authUrl);
  }

  @Override
  protected CloudUser authenticate(Map<String, String> params) throws CloudDriveException {
    LOG.info("authenticate: " + params.toString());
//    LOG.info("authenticate: ");
    return new OneDriveUser("id", "username", "email", provider);
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
    return new JCRLocalOneDrive(new OneDriveUser("a", "a", "a", createProvider()),
                                driveNode,
                                sessionProviders,
                                jcrFinder,
                                mimeTypes);
    // return new JCRLocalOneDrive(authenticate(Collections.emptyMap()),
    // getProvider(), driveNode, sessionProviders, jcrFinder, mimeTypes);
  }
}
