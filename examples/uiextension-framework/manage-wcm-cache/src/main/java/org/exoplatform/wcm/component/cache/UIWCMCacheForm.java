package org.exoplatform.wcm.component.cache;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL Author : Phan Trong Lam lamptdev@gmail.com
 * July 29, 2010 9:00:17 AM
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIForm.gtmpl", events = {
    @EventConfig(listeners = UIWCMCacheForm.SaveActionListener.class),
    @EventConfig(listeners = UIWCMCacheForm.ClearActionListener.class),
    @EventConfig(listeners = UIWCMCacheForm.RefreshActionListener.class) })
public class UIWCMCacheForm extends UIForm {

  final static public String FIELD_LABEL_CURRENTSIZE = "currentSize";
  final static public String FIELD_LABEL_HIT = "hit";
  final static public String FIELD_LABEL_MISS = "miss";
  final static public String FIELD_LIVE_TIME = "livetime";
  final static public String FIELD_MAX_SIZE = "maxsize";
  final static public String CHECKBOX_ENABLE_CACHE = "isCacheEnable";

  /** WCMComposer variable */
  private WCMComposer composer;

  /** Declare cache. */
  private ExoCache<String, Object> cache;

  public UIWCMCacheForm() throws Exception {
    cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance("wcm.composer");
    composer = WCMCoreUtils.getService(WCMComposer.class);

    int hit = cache.getCacheHit();
    long currentSize = cache.getCacheSize();
    int missHit = cache.getCacheHit();
    long livetime = cache.getLiveTime();
    int maxsize = cache.getMaxSize();

    addUIFormInput(new UIFormStringInput(FIELD_LABEL_CURRENTSIZE,
        FIELD_LABEL_CURRENTSIZE, String.valueOf(currentSize))
        .setEditable(false));
    addUIFormInput(new UIFormStringInput(FIELD_LABEL_HIT, FIELD_LABEL_HIT,
        String.valueOf(hit)).setEditable(false));
    addUIFormInput(new UIFormStringInput(FIELD_LABEL_MISS, FIELD_LABEL_MISS,
        String.valueOf(missHit)).setEditable(false));
    addUIFormInput(new UIFormStringInput(FIELD_MAX_SIZE, FIELD_MAX_SIZE, String
        .valueOf(maxsize)));
    addUIFormInput(new UIFormStringInput(FIELD_LIVE_TIME, FIELD_LIVE_TIME,
        String.valueOf(livetime)));
    addUIFormInput(new UIFormCheckBoxInput<Boolean>(CHECKBOX_ENABLE_CACHE,
        CHECKBOX_ENABLE_CACHE, composer.isCached()));
    setActions(new String[] { "Clear", "Save", "Refresh" });
    update();
  }

  public void update() throws Exception {
    getUIStringInput(FIELD_LIVE_TIME).setValue(String.valueOf(cache.getLiveTime()));
    getUIStringInput(FIELD_MAX_SIZE).setValue(String.valueOf(cache.getMaxSize()));
    getUIStringInput(FIELD_LABEL_MISS).setValue(String.valueOf(cache.getCacheMiss()));
    getUIStringInput(FIELD_LABEL_HIT).setValue(String.valueOf(cache.getCacheHit()));
    getUIStringInput(FIELD_LABEL_CURRENTSIZE).setValue(String.valueOf(cache.getCacheSize()));
    getUIFormCheckBoxInput(CHECKBOX_ENABLE_CACHE).setChecked(composer.isCached());

  }

  public static class SaveActionListener extends EventListener<UIWCMCacheForm> {
    public void execute(Event<UIWCMCacheForm> event) throws Exception {
      UIWCMCacheForm uiCacheForm = event.getSource();
      UIWCMCachePanel cachePanel = event.getSource().getAncestorOfType(
          UIWCMCachePanel.class);
      String livetime = uiCacheForm.getUIStringInput(FIELD_LIVE_TIME)
          .getValue();
      Boolean isChecked = uiCacheForm.getUIFormCheckBoxInput(CHECKBOX_ENABLE_CACHE).isChecked();
      if (isChecked){
        String maxsize = uiCacheForm.getUIStringInput(FIELD_MAX_SIZE).getValue();
        uiCacheForm.cache.setLiveTime(Long.parseLong(livetime));
        uiCacheForm.cache.setMaxSize(Integer.parseInt(maxsize));
        uiCacheForm.update();
      }else {
        throw new Exception("cache enable is not set");
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(cachePanel);
    }
  }

  public static class ClearActionListener extends EventListener<UIWCMCacheForm> {
    public void execute(Event<UIWCMCacheForm> event) throws Exception {
      UIWCMCacheForm uiCacheForm = event.getSource();
      UIWCMCachePanel uiCachePanel = event.getSource().getParent();
      uiCacheForm.cache.clearCache();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiCachePanel);
    }
  }

  public static class RefreshActionListener extends EventListener<UIWCMCacheForm> {
    public void execute(Event<UIWCMCacheForm> event) throws Exception {
      UIWCMCacheForm uiForm = event.getSource();
      UIWCMCachePanel wcmCachePanel = uiForm
          .getAncestorOfType(UIWCMCachePanel.class);
      uiForm.update();
      event.getRequestContext().addUIComponentToUpdateByAjax(wcmCachePanel);
    }
  }
}
