package org.exoplatform.services.cms.documents.mock;

import java.util.List;

import org.gatein.api.Portal;
import org.gatein.api.navigation.Navigation;
import org.gatein.api.oauth.OAuthProvider;
import org.gatein.api.page.Page;
import org.gatein.api.page.PageId;
import org.gatein.api.page.PageQuery;
import org.gatein.api.security.Permission;
import org.gatein.api.security.User;
import org.gatein.api.site.Site;
import org.gatein.api.site.SiteId;
import org.gatein.api.site.SiteQuery;

public class PortalMock implements Portal {

  @Override
  public Site getSite(SiteId siteId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Site createSite(SiteId siteId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Site createSite(SiteId siteId, String templateName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Site> findSites(SiteQuery query) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void saveSite(Site site) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean removeSite(SiteId siteId) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Navigation getNavigation(SiteId siteId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Page getPage(PageId pageId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Page createPage(PageId pageId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Page> findPages(PageQuery query) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void savePage(Page page) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean removePage(PageId pageId) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasPermission(User user, Permission permission) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public OAuthProvider getOAuthProvider(String oauthProviderKey) {
    // TODO Auto-generated method stub
    return null;
  }

}
