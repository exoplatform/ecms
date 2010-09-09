/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.migration.pages;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Container;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.impl.UnmarshallingContext;
import org.picocontainer.Startable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com; phan.le.thanh.chuong@gmail.com
 * Sep 8, 2010  
 */
public class PageMigrationService implements Startable {

  private ConfigurationManager configurationManager;
  
  private DataStorage dataStorage;
  
  private ChromatticManager chromatticManager;
  
  private Log log = ExoLogger.getLogger("PAGE MIGRATION") ;
  
  public PageMigrationService() {
    configurationManager = WCMCoreUtils.getService(ConfigurationManager.class);
    dataStorage = WCMCoreUtils.getService(DataStorage.class);
    chromatticManager = WCMCoreUtils.getService(ChromatticManager.class);
  }
  
  private void addNavigations() throws Exception {
    String path = "jar:/conf/portal/navigation.xml";
    String out = IOUtil.getStreamContentAsString(configurationManager.getInputStream(path));
    ByteArrayInputStream is = new ByteArrayInputStream(out.getBytes("UTF-8"));
    IBindingFactory bfact = BindingDirectory.getFactory(Container.class);
    UnmarshallingContext uctx = (UnmarshallingContext)bfact.createUnmarshallingContext();
    uctx.setDocument(is, null, "UTF-8", false);
    PageNavigation newPageNavigation = (PageNavigation)uctx.unmarshalElement();
    PageNode newPageNode = newPageNavigation.getNodes().get(0);
    
    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(configurationManager.getInputStream(path));
    Element root = (Element)document.getElementsByTagName("node-navigation").item(0);
    String ownerType = root.getElementsByTagName("owner-type").item(0).getChildNodes().item(0).getNodeValue();
    String ownerId = root.getElementsByTagName("owner-id").item(0).getChildNodes().item(0).getNodeValue();
    PageNavigation currentPageNavigation = dataStorage.getPageNavigation(ownerType, ownerId);
    currentPageNavigation.addNode(newPageNode);
    
    dataStorage.save(currentPageNavigation);
  }
  
  private void addPages() throws Exception {
    String path = "jar:/conf/portal/pages.xml";
    String out = IOUtil.getStreamContentAsString(configurationManager.getInputStream(path));
    ByteArrayInputStream is = new ByteArrayInputStream(out.getBytes("UTF-8"));
    IBindingFactory bfact = BindingDirectory.getFactory(Container.class);
    UnmarshallingContext uctx = (UnmarshallingContext)bfact.createUnmarshallingContext();
    uctx.setDocument(is, null, "UTF-8", false);
    Page page = (Page)uctx.unmarshalElement();
    
    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(configurationManager.getInputStream(path));
    Element root = (Element)document.getElementsByTagName("page").item(0);
    String ownerType = root.getElementsByTagName("owner-type").item(0).getChildNodes().item(0).getNodeValue();
    String ownerId = root.getElementsByTagName("owner-id").item(0).getChildNodes().item(0).getNodeValue();
    page.setOwnerType(ownerType);
    page.setOwnerId(ownerId);
    
    dataStorage.create(page);
  }
  
  public void start() {
    try {
      RequestLifeCycle.begin(chromatticManager);
      addPages();
      addNavigations();
    } catch (Exception e) {
      log.error("Cannot migrate page data because of", e);
    } finally {
      RequestLifeCycle.end();
    }
  }
    
  public void stop() {}
}
