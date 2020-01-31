package org.exoplatform.social.addons.rdbms.listener;

import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.WebpagePublicationPlugin;

public class MockWCMPublicationService implements WCMPublicationService {

  @Override
  public void addPublicationPlugin(WebpagePublicationPlugin p) {
  }

  @Override
  public Map<String, WebpagePublicationPlugin> getWebpagePublicationPlugins() {
    return null;
  }

  @Override
  public boolean isEnrolledInWCMLifecycle(Node node) throws NotInPublicationLifecycleException, Exception {
    return false;
  }

  @Override
  public void enrollNodeInLifecycle(Node node, String lifecycleName) throws Exception {
  }

  @Override
  public void enrollNodeInLifecycle(Node node, String siteName, String remoteUser) throws Exception {
  }

  @Override
  public void unsubcribeLifecycle(Node node) throws NotInPublicationLifecycleException, Exception {
  }

  @Override
  public void updateLifecyleOnChangeContent(Node node, String siteName, String remoteUser) throws Exception {
  }

  @Override
  public void updateLifecyleOnChangeContent(Node node, String siteName, String remoteUser, String newState) throws Exception {
  }

  @Override
  public String getContentState(Node node) throws Exception {
    return null;
  }
}
