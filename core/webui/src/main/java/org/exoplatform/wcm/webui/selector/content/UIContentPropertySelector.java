package org.exoplatform.wcm.webui.selector.content;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.core.UIPopupWindow;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Author : TAN DUNG DANG
 *          dzungdev@gmail.com
 * Jan 21, 2009
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(phase=Phase.DECODE, listeners = UIContentPropertySelector.CancelActionListener.class),
      @EventConfig(listeners = UIContentPropertySelector.AddActionListener.class),
      @EventConfig(listeners = UIContentPropertySelector.ChangeMetadataTypeActionListener.class)
    }
)
public class UIContentPropertySelector extends UIForm{

  public final static String WEB_CONTENT_METADATA_POPUP = "WebContentMetadataPopup";

  final static public String METADATA_TYPE = "metadataType" ;
  final static public String PROPERTY_SELECT = "property_select" ;

  private String fieldName = null ;

  private List<SelectItemOption<String>> properties = new ArrayList<SelectItemOption<String>>() ;

  public UIContentPropertySelector() throws Exception {
    setActions(new String[] {"Add", "Cancel"}) ;
  }

  public void init() throws Exception {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    NodeHierarchyCreator nodeHierarchyCreator = getApplicationComponent(NodeHierarchyCreator.class);

    UIFormSelectBox uiSelect = new UIFormSelectBox(METADATA_TYPE, METADATA_TYPE, options);
    uiSelect.setOnChange("ChangeMetadataType");
    addUIFormInput(uiSelect);
    SessionProvider sessionProvider = Utils.getSystemSessionProvider();
    RepositoryService repoService = getApplicationComponent(RepositoryService.class);
    ManageableRepository manRepository = repoService.getCurrentRepository();
    //String workspaceName = manRepository.getConfiguration().getSystemWorkspaceName();
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspaceName = dmsConfiguration.getConfig().getSystemWorkspace();
    Session session = sessionProvider.getSession(workspaceName, manRepository);
    String metadataPath = nodeHierarchyCreator.getJcrPath(BasePath.METADATA_PATH);
    Node homeNode = (Node) session.getItem(metadataPath);
    NodeIterator nodeIter = homeNode.getNodes();
    Node meta = nodeIter.nextNode();
    renderProperties(meta.getName());
    options.add(new SelectItemOption<String>(meta.getName(), meta.getName()));
    while(nodeIter.hasNext()) {
      meta = nodeIter.nextNode();
      options.add(new SelectItemOption<String>(meta.getName(), meta.getName()));
    }
    addUIFormInput(new UIFormRadioBoxInput(PROPERTY_SELECT, null, properties).
        setAlign(UIFormRadioBoxInput.VERTICAL_ALIGN));
  }

  public void setFieldName(String fieldName) { this.fieldName = fieldName ; }

  public void renderProperties(String metadata) throws Exception {
    properties.clear() ;
    SessionProvider sessionProvider = Utils.getSystemSessionProvider();
    RepositoryService repoService = getApplicationComponent(RepositoryService.class);
    ManageableRepository manRepository = repoService.getCurrentRepository();
    String workspaceName = manRepository.getConfiguration().getSystemWorkspaceName();
    Session session = sessionProvider.getSession(workspaceName, manRepository);
    NodeTypeManager ntManager = session.getWorkspace().getNodeTypeManager();
    NodeType nt = ntManager.getNodeType(metadata);
    PropertyDefinition[] propertieDefs = nt.getPropertyDefinitions();
    for(PropertyDefinition property : propertieDefs) {
      String name = property.getName();
      if(!name.equals("exo:internalUse")) {
        this.properties.add(new SelectItemOption<String>(name,name));
      }
    }
  }

  static  public class CancelActionListener extends EventListener<UIContentPropertySelector> {
    public void execute(Event<UIContentPropertySelector> event) throws Exception {
      UIContentPropertySelector contentPropertySelector = event.getSource();
      Utils.closePopupWindow(contentPropertySelector, WEB_CONTENT_METADATA_POPUP);
    }
  }

  static  public class AddActionListener extends EventListener<UIContentPropertySelector> {
    public void execute(Event<UIContentPropertySelector> event) throws Exception {
      UIContentPropertySelector contentPropertySelector = event.getSource();
      String property = contentPropertySelector.<UIFormRadioBoxInput>getUIInput(PROPERTY_SELECT).getValue();
      UIPopupWindow popupWindow = Utils.getPopupContainer(contentPropertySelector)
                                       .getChildById(UIContentSelector.CORRECT_CONTENT_SELECTOR_POPUP_WINDOW);
      UIContentSelector contentSelector = (UIContentSelector) popupWindow.getUIComponent();
      UIContentSearchForm contentSearchForm =contentSelector.findFirstComponentOfType(UIContentSearchForm.class);
      contentSearchForm.getUIStringInput(contentPropertySelector.getFieldName()).setValue(property);
      Utils.closePopupWindow(contentPropertySelector, WEB_CONTENT_METADATA_POPUP);
      contentSelector.setSelectedTab(contentSearchForm.getId());
    }
  }

  static  public class ChangeMetadataTypeActionListener extends EventListener<UIContentPropertySelector> {
    public void execute(Event<UIContentPropertySelector> event) throws Exception {
      UIContentPropertySelector contentPropertySelector = event.getSource();
      contentPropertySelector.renderProperties(contentPropertySelector.getUIFormSelectBox(METADATA_TYPE).getValue());
      event.getRequestContext().addUIComponentToUpdateByAjax(contentPropertySelector);
    }
  }

  public String getFieldName() {
    return fieldName;
  }
}
