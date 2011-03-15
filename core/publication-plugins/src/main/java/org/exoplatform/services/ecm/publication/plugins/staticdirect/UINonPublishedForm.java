package org.exoplatform.services.ecm.publication.plugins.staticdirect;

import javax.jcr.Node;

import org.exoplatform.services.ecm.publication.plugins.webui.UIPublicationComponent;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.form.UIForm;


@ComponentConfig (
    lifecycle = UIApplicationLifecycle.class,
    template = "classpath:resources/templates/staticdirect/nonPublished.gtmpl"
)
public class UINonPublishedForm extends UIForm {

  public UINonPublishedForm() throws Exception {
    addChild(UIPublicationComponent.class, null, null);
    addChild(UIPublicationContainer.class, null, null) ;
  }

  public void setNode(Node node) {
    getChild(UIPublicationComponent.class).setNode(node);
  }

}
