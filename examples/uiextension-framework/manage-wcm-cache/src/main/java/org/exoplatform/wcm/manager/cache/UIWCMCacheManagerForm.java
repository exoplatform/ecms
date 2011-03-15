package org.exoplatform.wcm.manager.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;

import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;


@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIForm.gtmpl", events = {
    @EventConfig(listeners = UIWCMCacheManagerForm.SaveActionListener.class),
    @EventConfig(listeners = UIWCMCacheManagerForm.ClearActionListener.class),
    @EventConfig(listeners = UIWCMCacheManagerForm.RefreshActionListener.class),
    @EventConfig(listeners = UIWCMCacheManagerForm.OnChangeActionListener.class)
    })
public class UIWCMCacheManagerForm extends UIForm {

  final static public String FIELD_LABEL_CURRENTSIZE = "currentSize";
  final static public String FIELD_LABEL_HIT = "hit";
  final static public String FIELD_LABEL_MISS = "miss";
  final static public String FIELD_LIVE_TIME = "livetime";
  final static public String FIELD_MAX_SIZE = "maxsize";
  final static public String CHECKBOX_ENABLE_CACHE = "isCacheEnable";
  final static public String FIELD_MODIFY = "cacheModify" ;

  public static UIFormInputBase<String> UI1;
  public static UIFormInputBase<String> UI2;
  public static UIFormInputBase<String> UI3;
  public static UIFormInputBase<String> UI4;
  public static UIFormInputBase<String> UI5;


  public Collection<ExoCache<? extends Serializable, ?>> caches;
  public static ExoCache cache = null;

  public UIWCMCacheManagerForm() throws Exception {
    //get collection of cache instances
    caches = WCMCoreUtils.getService(CacheService.class).getAllCacheInstances();
    List<SelectItemOption<String>> modifyOptions = new ArrayList<SelectItemOption<String>>();
    Iterator<ExoCache<? extends Serializable, ?>> it = caches.iterator();
    while (it.hasNext()){
      SelectItemOption itm = new SelectItemOption(it.next().getName());
      modifyOptions.add(itm);
    }
    UIFormSelectBox typeModify = new UIFormSelectBox(FIELD_MODIFY, FIELD_MODIFY, modifyOptions) ;
    typeModify.setOnChange("OnChange");
    // add dropbox list
    addUIFormInput(typeModify) ;
    int hit = 0;
    long currentSize = 0 ;
    int missHit = 0;
    long livetime = 0;
    int maxsize = 0;
    UIFormInputBase<String> UI1 = new UIFormStringInput(FIELD_LABEL_CURRENTSIZE,
                                         FIELD_LABEL_CURRENTSIZE, String.valueOf(currentSize))
                                         .setEditable(false);
    addUIFormInput (UI1);
    UIFormInputBase<String> UI2 = new UIFormStringInput(FIELD_LABEL_HIT, FIELD_LABEL_HIT,
                                         String.valueOf(hit)).setEditable(false);
    addUIFormInput (UI2);
    UIFormInputBase<String> UI3 = new UIFormStringInput(FIELD_LABEL_MISS, FIELD_LABEL_MISS,
                                         String.valueOf(missHit)).setEditable(false);
    addUIFormInput (UI3);
    UIFormInputBase<String> UI4 = new UIFormStringInput(FIELD_MAX_SIZE, FIELD_MAX_SIZE, String
                                         .valueOf(maxsize));
    addUIFormInput (UI4);
    UIFormInputBase<String> UI5 = new UIFormStringInput(FIELD_LIVE_TIME, FIELD_LIVE_TIME,
                                         String.valueOf(livetime));
    addUIFormInput (UI5);
    addUIFormInput( new UIFormCheckBoxInput<Boolean>(CHECKBOX_ENABLE_CACHE,
                                         CHECKBOX_ENABLE_CACHE, null).setChecked(true));
    setActions(new String[] { "Clear", "Save", "Refresh" });
  }

  private void update(ExoCache cache) throws Exception {
    getUIStringInput(FIELD_LIVE_TIME).setValue(String.valueOf(cache.getLiveTime()));
    getUIStringInput(FIELD_MAX_SIZE).setValue(String.valueOf(cache.getMaxSize()));
    getUIStringInput(FIELD_LABEL_MISS).setValue(String.valueOf(cache.getCacheMiss()));
    getUIStringInput(FIELD_LABEL_HIT).setValue(String.valueOf(cache.getCacheHit()));
    getUIStringInput(FIELD_LABEL_CURRENTSIZE).setValue(String.valueOf(cache.getCacheSize()));
  }

  public static class SaveActionListener extends EventListener<UIWCMCacheManagerForm> {
    public void execute(Event<UIWCMCacheManagerForm> event) throws Exception {
      UIWCMCacheManagerForm uiCacheForm = event.getSource();
      UIWCMCacheManagerPanel uiCachePanel = uiCacheForm.getAncestorOfType(UIWCMCacheManagerPanel.class);
      //droplist option choice
      //String cacheOpt = uiCacheForm.getUIFormSelectBox(FIELD_MODIFY).getValue();

      //get current cache
      //cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance(cacheOpt);

      // check whether its allowed to change
      Boolean isChecked = uiCacheForm.getUIFormCheckBoxInput(CHECKBOX_ENABLE_CACHE).isChecked();
      if (isChecked){
        String livetime = uiCacheForm.getUIStringInput(FIELD_LIVE_TIME).getValue();
        String maxsize = uiCacheForm.getUIStringInput(FIELD_MAX_SIZE).getValue();
        //update changes
        uiCacheForm.cache.setLiveTime(Long.parseLong(livetime));
        uiCacheForm.cache.setMaxSize(Integer.parseInt(maxsize));
        //update cache info
        uiCacheForm.update(cache);
      }else {
        throw new Exception("cache enable is not set");
      }

      event.getRequestContext().addUIComponentToUpdateByAjax(uiCachePanel);
    }
  }
  public static class ClearActionListener extends EventListener<UIWCMCacheManagerForm> {
    public void execute(Event<UIWCMCacheManagerForm> event) throws Exception {
      UIWCMCacheManagerPanel uiCachePanel = event.getSource().getParent();
      UIWCMCacheManagerForm uiCacheForm = event.getSource();
      //String cacheOpt = uiCacheForm.getUIFormSelectBox(FIELD_MODIFY).getValue();
      //get current cache
      //cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance(cacheOpt);
      uiCacheForm.cache.clearCache();
      //update cache info
      uiCacheForm.update(cache);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiCachePanel);
    }
  }
  public static class RefreshActionListener extends EventListener<UIWCMCacheManagerForm> {
    public void execute(Event<UIWCMCacheManagerForm> event) throws Exception {
      UIWCMCacheManagerForm uiCacheForm = event.getSource();
      UIWCMCacheManagerPanel wcmCache = uiCacheForm.getAncestorOfType(UIWCMCacheManagerPanel.class);
      uiCacheForm.update(cache);
      event.getRequestContext().addUIComponentToUpdateByAjax(wcmCache);
    }
  }
  public static class OnChangeActionListener extends EventListener<UIWCMCacheManagerForm> {
    public void execute(Event<UIWCMCacheManagerForm> event) throws Exception {
      UIWCMCacheManagerForm uiCacheForm = event.getSource();
      String cacheOpt = uiCacheForm.getUIFormSelectBox(FIELD_MODIFY).getValue();
      // get current cache
      cache = WCMCoreUtils.getService(CacheService.class).getCacheInstance(cacheOpt);
      //update cache info
      uiCacheForm.update(cache);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiCacheForm);
    }
  }
}
