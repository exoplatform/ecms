package org.exoplatform.wcm.ext.component.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import com.google.gson.Gson;

import org.exoplatform.ecm.webui.component.explorer.documents.IsEditorPluginPresentFilter;
import org.exoplatform.services.cms.documents.DocumentEditorPlugin;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.model.EditorButton;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.web.application.JavascriptManager;
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
    if (getFilesCount() == 1) {
      Node node = getContentNode(0);
      Map<DocumentEditorPlugin, EditorButton> pluginButtons = getEditorsButtons(node);
      String jsonButtons = new Gson().toJson(pluginButtons.values());
      js.require("SHARED/editorbuttons", "editorbuttons")
        .addScripts("editorbuttons.initActivityButtons('" + jsonButtons + "', '" + activityId + "');");
      // call plugins init handlers
      pluginButtons.forEach((plugin, btn) -> {
        try {
          plugin.initActivity(btn.getFileId(), activityId);
        } catch (Exception e) {
          LOG.error("Cannot init activity from plugin {}, {}", plugin.getProviderName(), e.getMessage());
        }
      });

    }

    // Init preview links for each of file
    for (int index = 0; index < getFilesCount(); index++) {
      Node node = getContentNode(index);
      Map<DocumentEditorPlugin, EditorButton> pluginButtons = getEditorsButtons(node);

      String jsonButtons = new Gson().toJson(pluginButtons.values());
      js.require("SHARED/editorbuttons", "editorbuttons")
        .addScripts("editorbuttons.initPreviewButtons('" + jsonButtons + "', '" + activityId + "', '" + index + "');");

      // call plugins init handlers
      for (Map.Entry<DocumentEditorPlugin, EditorButton> entry : pluginButtons.entrySet()) {
        DocumentEditorPlugin plugin = entry.getKey();
        EditorButton editorButton = entry.getValue();
        try {
          plugin.initPreview(editorButton.getFileId(), activityId, index);
        } catch (Exception e) {
          LOG.error("Cannot init activity from plugin {}, {}", plugin.getProviderName(), e.getMessage());
        }
      }
    }
    super.end();
  }

  /**
   * Gets the editor buttons.
   *
   * @param node the node
   * @return the editor buttons
   */
  protected List<EditorButton> getEditorButtons(Node node) {
    List<EditorButton> buttons = new ArrayList<>();
    documentService.getRegisteredEditorPlugins().forEach(plugin -> {
      try {
        buttons.add(plugin.getEditorButton(node.getUUID(), node.getSession().getWorkspace().getName(), STREAM_CONTEXT));
      } catch (Exception e) {
        LOG.error("Cannot get editor button from customize plugin {}, {}", plugin.getProviderName(), e.getMessage());
      }
    });
    return buttons;
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
   * Gets the editors buttons.
   *
   * @param node the node
   * @return the editors buttons
   */
  protected Map<DocumentEditorPlugin, EditorButton> getEditorsButtons(Node node) {
    Map<DocumentEditorPlugin, EditorButton> pluginButtons = new HashMap<>();
    documentService.getRegisteredEditorPlugins().forEach(plugin -> {
      try {
        pluginButtons.putIfAbsent(plugin,
                                  plugin.getEditorButton(node.getUUID(),
                                                         node.getSession().getWorkspace().getName(),
                                                         STREAM_CONTEXT));
      } catch (Exception e) {
        LOG.error("Cannot get editor button from customize plugin {}, {}", plugin.getProviderName(), e.getMessage());
      }
    });
    return pluginButtons;
  }

}
