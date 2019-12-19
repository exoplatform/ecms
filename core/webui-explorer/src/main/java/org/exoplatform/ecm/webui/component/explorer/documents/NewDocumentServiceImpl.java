/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.documents;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.resolver.ApplicationResourceResolver;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The NewDocumentServiceImpl implements NewDocumentService API and provides a
 * way for managing document template/editor plugins, creating new documents.
 */
public class NewDocumentServiceImpl implements NewDocumentService, Startable {

  /** The Constant LOG. */
  protected static final Log                             LOG               = ExoLogger.getLogger(NewDocumentServiceImpl.class);

  /**  The Constant MIX_VERSIONABLE. */
  public static final String                             MIX_VERSIONABLE   = "mix:versionable";

  /** The Constant JCR_LAST_MODIFIED. */
  protected static final String                          JCR_LAST_MODIFIED = "jcr:lastModified";

  /** The Constant JCR_MIME_TYPE. */
  protected static final String                          JCR_MIME_TYPE     = "jcr:mimeType";

  /** The Constant JCR_DATA. */
  protected static final String                          JCR_DATA          = "jcr:data";

  /** The template plugins. */
  protected final Map<String, NewDocumentTemplatePlugin> templatePlugins   = new HashMap<>();

  /** The editor plugins. */
  protected final Map<String, NewDocumentEditorPlugin>   editorPlugins     = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    LOG.info("NEW Document Service started");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // Nothing
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addDocumentTemplatePlugin(ComponentPlugin plugin) {
    Class<NewDocumentTemplatePlugin> pclass = NewDocumentTemplatePlugin.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      NewDocumentTemplatePlugin newPlugin = pclass.cast(plugin);

      LOG.info("Adding NewDocumentTemplatePlugin [{}]", newPlugin.toString());
      templatePlugins.put(newPlugin.getProvider(), newPlugin);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Registered NewDocumentTemplatePlugin instance of {}", plugin.getClass().getName());
      }
    } else {
      LOG.error("The NewDocumentTemplatePlugin plugin is not an instance of " + pclass.getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addDocumentEditorPlugin(ComponentPlugin plugin) {
    Class<NewDocumentEditorPlugin> pclass = NewDocumentEditorPlugin.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      NewDocumentEditorPlugin newPlugin = pclass.cast(plugin);

      LOG.info("Adding NewDocumentEditorPlugin [{}]", newPlugin.toString());
      editorPlugins.put(newPlugin.getProvider(), newPlugin);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Registered NewDocumentEditorPlugin instance of {}", plugin.getClass().getName());
      }
    } else {
      LOG.error("The NewDocumentEditorPlugin plugin is not an instance of " + pclass.getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node createDocument(Node currentNode, String title, DocumentTemplate template) throws Exception {
    InputStream data = new ByteArrayInputStream(new byte[0]);
    if (template.getPath() != null && !template.getPath().trim().isEmpty()) {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ApplicationResourceResolver appResolver = context.getApplication().getResourceResolver();
      ResourceResolver resolver = appResolver.getResourceResolver(template.getPath());
      data = resolver.getInputStream(template.getPath());
    }
    // Add node
    Node addedNode = currentNode.addNode(title, Utils.NT_FILE);

    // Set title
    if (!addedNode.hasProperty(Utils.EXO_TITLE)) {
      addedNode.addMixin(Utils.EXO_RSS_ENABLE);
    }
    // Enable versioning
    if (addedNode.canAddMixin(MIX_VERSIONABLE)) {
      addedNode.addMixin(MIX_VERSIONABLE);
    }

    addedNode.setProperty(Utils.EXO_TITLE, title);
    Node content = addedNode.addNode("jcr:content", "nt:resource");

    content.setProperty(JCR_DATA, data);
    content.setProperty(JCR_MIME_TYPE, template.getMimeType());
    content.setProperty(JCR_LAST_MODIFIED, new GregorianCalendar());
    ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
    listenerService.broadcast(ActivityCommonService.FILE_CREATED_ACTIVITY, null, addedNode);
    currentNode.save();
    data.close();
    return addedNode;
  }

  /**
   * NewDocumentTypesConfig contains all registered templates for specified provider.
   */
  public static class DocumentTemplatesConfig {

    /** The document templates. */
    protected List<DocumentTemplate> templates;

    /** The provider. */
    protected String                 provider;

    /**
     * Gets the document templates.
     *
     * @return the document types
     */
    public List<DocumentTemplate> getTemplates() {
      return templates;
    }

    /**
     * Sets the document templates.
     *
     * @param templates the new templates
     */
    public void setTemplates(List<DocumentTemplate> templates) {
      this.templates = templates;
    }

    /**
     * Gets the provider.
     *
     * @return the provider
     */
    public String getProvider() {
      return provider;
    }

    /**
     * {
     * @param provider
     */
    public void setProvider(String provider) {
      this.provider = provider;
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DocumentTemplate getDocumentTemplate(String provider, String label) {
    NewDocumentTemplatePlugin plugin = templatePlugins.get(provider);
    if (plugin != null) {
      return plugin.getTemplates().stream().filter(template -> template.getLabel().equals(label)).findFirst().get();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NewDocumentTemplatePlugin getDocumentTemplatePlugin(String provider) {
    return templatePlugins.get(provider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NewDocumentEditorPlugin getDocumentEditorPlugin(String provider) {
    return editorPlugins.get(provider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, NewDocumentTemplatePlugin> getRegisteredTemplatePlugins() {
    return templatePlugins;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDocumentTemplatePlugins() {
    return templatePlugins.size() > 0;
  }
}
