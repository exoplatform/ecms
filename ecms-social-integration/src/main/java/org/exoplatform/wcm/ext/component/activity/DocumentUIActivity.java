package org.exoplatform.wcm.ext.component.activity;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.services.cms.documents.DocumentEditorPlugin;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.model.EditorButton;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.ws.frameworks.json.JsonGenerator;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * The Class CustomizedFileUIActivity.
 */
public class DocumentUIActivity extends FileUIActivity {

  /** The Constant LOG. */
  private static final Log        LOG            = ExoLogger.getLogger(DocumentUIActivity.class);

  /** The customize service. */
  protected final DocumentService documentService;

  /** The Constant STREAM_CONTEXT. */
  protected static final String   STREAM_CONTEXT = "stream";

  /**
   * Instantiates a new customized file UI activity.
   *
   * @param documentService the document service
   * @throws Exception the exception
   */
  public DocumentUIActivity(DocumentService documentService) throws Exception {
    super();
    this.documentService = documentService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void end() throws Exception {
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
          plugin.initActivity(button.getFileId());
        } catch (Exception e) {
          LOG.error("Cannot get editor button or init activity from customize plugin {}, {}",
                    plugin.getProviderName(),
                    e.getMessage());
        }
      });
      // Call JS to init pulldown with links

    }

    // Init preview links for each of file
    for (int index = 0; index < getFilesCount(); index++) {
      Node node = getContentNode(index);
      List<EditorButton> buttons = new ArrayList<>();
      documentService.getRegisteredEditorPlugins().forEach(plugin -> {
        try {
          EditorButton button =
                              plugin.getEditorButton(node.getUUID(), node.getSession().getWorkspace().getName(), STREAM_CONTEXT);
          buttons.add(button);
          plugin.initPreview(button.getFileId());
        } catch (Exception e) {
          LOG.error("Cannot get editor button or init preview from customize plugin {}, {}",
                    plugin.getProviderName(),
                    e.getMessage());
        }
      });
      // Call JS to add pulldown to the preview
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

}
