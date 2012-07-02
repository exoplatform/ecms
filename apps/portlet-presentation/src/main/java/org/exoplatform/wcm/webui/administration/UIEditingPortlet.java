package org.exoplatform.wcm.webui.administration;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform
 * ngoc.tran@exoplatform.com Jan 28, 2010
 */
@ComponentConfig(
                 lifecycle = UIApplicationLifecycle.class,
                 template = "app:/groovy/Editing/UIEditingPortlet.gtmpl"
               )
public class UIEditingPortlet extends UIPortletApplication {

  public UIEditingPortlet() throws Exception {
    addChild(UIEditingForm.class, null, null);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.webui.core.UIPortletApplication#processRender(org.exoplatform
   * .webui.application.WebuiApplication,
   * org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
//    RenderResponse response = context.getResponse();
//    Element elementS = response.createElement("script");
//    elementS.setAttribute("type", "text/javascript");
//    elementS.setAttribute("src", "/eXoWCMResources/javascript/eXo/wcm/frontoffice/private/QuickEdit.js");
//    response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT,elementS);
    
//    elementS = response.createElement("script");
//    elementS.setAttribute("type", "text/javascript");
//    elementS.setAttribute("src", "/eXoWCMResources/javascript/eXo/wcm/frontoffice/private/InlineEditing.js");
//    response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT,elementS);
    
    UIEditingForm editingForm = getChild(UIEditingForm.class);
    UIFormSelectBox orderBySelectBox = editingForm.getChild(UIFormSelectBox.class);
    orderBySelectBox.setValue((Utils.isShowQuickEdit())?UIEditingForm.DRAFT:UIEditingForm.PUBLISHED);

    super.processRender(app, context) ;
  }
}
