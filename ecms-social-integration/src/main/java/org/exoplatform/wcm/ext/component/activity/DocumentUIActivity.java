/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
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
package org.exoplatform.wcm.ext.component.activity;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.webui.component.explorer.documents.IsEditorPluginPresentFilter;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cms.documents.DocumentEditorPlugin;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.model.EditorProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;

/**
 * The Class CustomizedFileUIActivity.
 */
@ComponentConfigs({
    @ComponentConfig(lifecycle = UIFormLifecycle.class, template = "war:/groovy/ecm/social-integration/plugin/space/FileUIActivity.gtmpl", events = {
        @EventConfig(listeners = FileUIActivity.ViewDocumentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
        @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
        @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
        @EventConfig(listeners = FileUIActivity.OpenFileActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.EditActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.EditCommentActionListener.class) }), })
public class DocumentUIActivity extends FileUIActivity {

  /** The Constant LOG. */
  private static final Log                       LOG            = ExoLogger.getLogger(DocumentUIActivity.class);

  /** The customize service. */
  protected final DocumentService                documentService;

  /** The Constant STREAM_CONTEXT. */
  protected static final String                  STREAM_CONTEXT = "stream";

  /** The Constant FILTERS. */
  protected static final List<UIExtensionFilter> FILTERS        = Arrays.asList(new UIExtensionFilter[] {
      new IsEditorPluginPresentFilter(), });

  /**
   * Instantiates a new customized file UI activity.
   *
   * @throws Exception the exception
   */
  public DocumentUIActivity() throws Exception {
    super();
    this.documentService = this.getApplicationComponent(DocumentService.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void end() throws Exception {
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    JavascriptManager js = requestContext.getJavascriptManager();
    String activityId = getActivity().getId();
    RequireJS require = js.require("SHARED/editorbuttons", "editorbuttons");
    if (getFilesCount() == 1) {
      Node node = getContentNode(0);
      require.addScripts("editorbuttons.resetButtons();");
      // call plugins init handlers
      documentService.getRegisteredEditorPlugins().forEach(plugin -> {
        try {
          EditorProvider editorProvider = documentService.getEditorProvider(plugin.getProviderName());
          if (hasPermissions(editorProvider)) {
            plugin.initActivity(node.getUUID(), node.getSession().getWorkspace().getName(), activityId, STREAM_CONTEXT);
          }
        } catch (Exception e) {
          LOG.error("Cannot init activity from plugin {}, {}", plugin.getProviderName(), e.getMessage());
        }
      });
      String prefferedEditor = getPrefferedEditor(node);
      require.addScripts("editorbuttons.initActivityButtons('" + activityId + "', '" + node.getUUID() + "', '"
          + node.getSession().getWorkspace().getName() + "'," + prefferedEditor + ");");

    }

    // Init preview links for each of file
    for (int index = 0; index < getFilesCount(); index++) {
      Node node = getContentNode(index);
      require.addScripts("editorbuttons.resetButtons();");
      // call plugins init handlers
      for (DocumentEditorPlugin plugin : documentService.getRegisteredEditorPlugins()) {
        try {
          EditorProvider editorProvider = documentService.getEditorProvider(plugin.getProviderName());
          if (hasPermissions(editorProvider)) {
            plugin.initPreview(node.getUUID(), node.getSession().getWorkspace().getName(), activityId, STREAM_CONTEXT, index);
          }
        } catch (Exception e) {
          LOG.error("Cannot init preview from plugin {}, {}", plugin.getProviderName(), e.getMessage());
        }
      }
      String prefferedEditor = getPrefferedEditor(node);
      require.addScripts("editorbuttons.initPreviewButtons('" + activityId + "', '" + index + "', '" + node.getUUID() + "', "
          + prefferedEditor + ");");
    }
    super.end();
  }

  /**
   * Gets the filters.
   *
   * @return the filters
   */
  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  /**
   * Gets the preffered editor.
   *
   * @param node the node
   * @return the preffered editor
   * @throws Exception the exception
   */
  protected String getPrefferedEditor(Node node) throws Exception {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    String prefferedEditor =
                           documentService.getPreferedEditor(userId, node.getUUID(), node.getSession().getWorkspace().getName());
    if (prefferedEditor != null) {
      prefferedEditor = "'" + prefferedEditor + "'";
    } else {
      prefferedEditor = "null".intern();
    }
    return prefferedEditor;
  }

  /**
   * Checks for permissions.
   *
   * @param editorProvider the editor provider
   * @return true, if successful
   */
  protected boolean hasPermissions(EditorProvider editorProvider) {
    UserACL userACL = (UserACL) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(UserACL.class);
    List<String> permissions = editorProvider.getPermissions().stream().map(permission -> {
      if(permission.startsWith("/")) {
        permission = "*:" + permission;
      }
      return permission;
    }).collect(Collectors.toList());
    String currentUser = ConversationState.getCurrent().getIdentity().getUserId();
    return editorProvider.getActive() && (permissions.contains(currentUser) || userACL.hasPermission(permissions.toArray(new String[0])));
  }
}
