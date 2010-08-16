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
package org.exoplatform.services.migration.templates;

import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong.phan@exoplatform.com; phan.le.thanh.chuong@gmail.com
 * Aug 12, 2010  
 */
public class TemplateMigrationService implements Startable {
  
  private static final String TEMPLATE_QUERY = "SELECT * FROM exo:template";
  
  private static final String SCRIPT_QUERY = "SELECT * FROM nt:resource WHERE jcr:path LIKE ";

  private RepositoryService repositoryService;
  
  private TemplateService templateService;
  
  private ScriptService scriptService;
  
  private SessionProviderService sessionProviderService;
  
  private String scriptFolderPath ;
  
  private Log log = ExoLogger.getLogger("DRIVE MIGRATION") ;
  
  public TemplateMigrationService() {
    repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    templateService = WCMCoreUtils.getService(TemplateService.class);
    sessionProviderService = WCMCoreUtils.getService(SessionProviderService.class);
    scriptService = WCMCoreUtils.getService(ScriptService.class);
    
    NodeHierarchyCreator nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    scriptFolderPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_SCRIPTS_PATH);
  }
  
  public void start() {
    try {
      List<RepositoryEntry> repositoryEntries = repositoryService.getConfig().getRepositoryConfigurations();
      for (RepositoryEntry repositoryEntry : repositoryEntries) {
        ManageableRepository repository = repositoryService.getRepository(repositoryEntry.getName());
        List<WorkspaceEntry> workspaceEntries = repository.getConfiguration().getWorkspaceEntries();
        for (WorkspaceEntry workspaceEntry : workspaceEntries) {
          SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null); 
          Session session = sessionProvider.getSession(workspaceEntry.getName(), repository);
          QueryManager queryManager = session.getWorkspace().getQueryManager();
          NodeIterator templateNodes = queryManager.createQuery(TEMPLATE_QUERY, Query.SQL).execute().getNodes();
          while(templateNodes.hasNext()) {
            Node oldTemplate = templateNodes.nextNode();
            templateService.createTemplate(oldTemplate.getParent(), oldTemplate.getName(), oldTemplate.getProperty("exo:templateFile").getStream(), getTemplateRoles(oldTemplate));
            oldTemplate.remove();
          }
          
          NodeIterator scriptNodes = queryManager.createQuery(SCRIPT_QUERY + "'" + scriptFolderPath + "/%'", Query.SQL).execute().getNodes();
          while(scriptNodes.hasNext()) {
            Node oldScript = scriptNodes.nextNode();
            System.out.println("==================== " + oldScript.getPath().replaceAll(scriptFolderPath + "/", ""));
            scriptService.addScript(oldScript.getPath().replaceAll(scriptFolderPath + "/", ""), oldScript.getProperty("jcr:data").getString(), repository.getConfiguration().getName(), sessionProvider);
            oldScript.remove();
          }
          
          session.save();
        }
      }  
    } catch (Exception e) {
      log.error("Cannot migrate template data because of", e);
    }
  }
  
  private String[] getTemplateRoles(Node template) throws Exception {
    Value[] values = template.getProperty("exo:roles").getValues();
    List<Value> listValues = Arrays.asList(values);
    String[] roles = new String[values.length];
    for (int i = 0; i < listValues.size(); i++) {
      roles[i] = listValues.get(i).getString();
    }
    return roles;
  }
  
  public void stop() {}
  
}
