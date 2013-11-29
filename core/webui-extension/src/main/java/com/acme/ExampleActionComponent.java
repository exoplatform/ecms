 package com.acme;


  import javax.jcr.Node;


  import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;//ecms-core-webui-explorer

  import org.exoplatform.ecm.webui.component.explorer.control.listener.UIActionBarActionListener;

  import org.exoplatform.web.application.ApplicationMessage;//exo.portal.component.webui.controller

  import org.exoplatform.webui.config.annotation.ComponentConfig;

  import org.exoplatform.webui.config.annotation.EventConfig;

  import org.exoplatform.webui.core.UIComponent;

  import org.exoplatform.webui.event.Event;//exo.portal.webui.framework
  
//commons-webui-ext

  @ComponentConfig(

   events = { @EventConfig(listeners = ExampleActionComponent.ExampleActionListener.class)

  })

  public class ExampleActionComponent extends UIComponent {


public static class ExampleActionListener extends UIActionBarActionListener<ExampleActionComponent> {

  @Override

  protected void processEvent(Event<ExampleActionComponent> event) throws Exception {

UIJCRExplorer uiJCRExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);

Node node = uiJCRExplorer.getCurrentNode();

event.getRequestContext()

     .getUIApplication()

     .addMessage(new ApplicationMessage("Node path:" + node.getPath(), null, ApplicationMessage.INFO));

  }

}


  }