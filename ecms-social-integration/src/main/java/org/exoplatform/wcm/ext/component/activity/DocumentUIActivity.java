package org.exoplatform.wcm.ext.component.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

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
import org.exoplatform.ws.frameworks.json.JsonGenerator;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

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
    JsonGenerator jsonGenerator = new JsonGeneratorImpl();
    String activityId = getActivity().getId();
    if (getFilesCount() == 1) {
      Node node = getContentNode(0);
      List<EditorButton> buttons = new ArrayList<>();
      documentService.getRegisteredEditorPlugins().forEach(plugin -> {
        try {
          EditorButton button =
                              plugin.getEditorButton(node.getUUID(), node.getSession().getWorkspace().getName(), STREAM_CONTEXT);
          buttons.add(button);
          plugin.initActivity(button.getFileId(), activityId);
        } catch (Exception e) {
          LOG.error("Cannot get editor button or init activity from customize plugin {}, {}",
                    plugin.getProviderName(),
                    e.getMessage());
        }
      });
      String jsonButtons = jsonGenerator.createJsonObject(buttons).toString();
      js.require("SHARED/editor-buttons", "editorbuttons")
        .addScripts("editorbuttons.initActivityButtons('" + jsonButtons + "');");

    }

    // Init preview links for each of file
    for (int index = 0; index < getFilesCount(); index++) {
      Node node = getContentNode(index);
      List<EditorButton> buttons = new ArrayList<>();
      for (DocumentEditorPlugin plugin : documentService.getRegisteredEditorPlugins()) {
        try {
          EditorButton button =
                              plugin.getEditorButton(node.getUUID(), node.getSession().getWorkspace().getName(), STREAM_CONTEXT);
          buttons.add(button);
          plugin.initPreview(button.getFileId(), activityId, index);
        } catch (Exception e) {
          LOG.error("Cannot get editor button or init preview from customize plugin {}, {}",
                    plugin.getProviderName(),
                    e.getMessage());
        }
      }
      String jsonButtons = jsonGenerator.createJsonObject(buttons).toString();
      js.require("SHARED/editor-buttons", "editorbuttons")
        .addScripts("editorbuttons.initPreviewButtons('" + jsonButtons + "');");
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

}
