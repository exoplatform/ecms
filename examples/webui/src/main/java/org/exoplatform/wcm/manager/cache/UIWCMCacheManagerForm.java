package org.exoplatform.wcm.manager.cache;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SARL Author : Phan Trong Lam lamptdev@gmail.com
 * July 29, 2010 9:00:17 AM
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "system:/groovy/webui/form/UIForm.gtmpl", events = {
		@EventConfig(listeners = UIWCMCacheManagerForm.SaveActionListener.class),
		@EventConfig(listeners = UIWCMCacheManagerForm.ClearActionListener.class),
		@EventConfig(listeners = UIWCMCacheManagerForm.CancelActionListener.class) })
public class UIWCMCacheManagerForm extends UIForm {

	final static public String FIELD_LABEL_CURRENTSIZE = "currentSize";
	final static public String FIELD_LABEL_HIT = "hit";
	final static public String FIELD_LABEL_MISS = "miss";
	final static public String FIELD_LIVE_TIME = "livetime";
	final static public String FIELD_MAX_SIZE = "maxsize";
	final static public String CHECKBOX_ENABLE_CACHE = "isCacheEnable";
	final static public String FIELD_MODIFY = "cacheModify" ;
		
	public ExoCache cache;		
	
	public UIWCMCacheManagerForm() throws Exception {		
						
		cache = LockUtil.getLockCache();
		int hit = cache.getCacheHit();
		long currentSize = cache.getCacheSize();
		int missHit = cache.getCacheHit();
		long livetime = cache.getLiveTime();
		int maxsize = cache.getMaxSize();
		List<SelectItemOption<String>> modifyOptions = new ArrayList<SelectItemOption<String>>();
			
    UIFormSelectBox typeModify = 
      new UIFormSelectBox(FIELD_MODIFY, FIELD_MODIFY, modifyOptions) ;    
    addUIFormInput(typeModify) ;
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
				CHECKBOX_ENABLE_CACHE, null).setChecked(true));
		setActions(new String[] { "Clear", "Save", "Cancel" });
		update();
	}
	
	private void update() throws Exception {
		getUIStringInput(FIELD_LIVE_TIME).setValue(String.valueOf(cache.getLiveTime()));
		getUIStringInput(FIELD_MAX_SIZE).setValue(String.valueOf(cache.getMaxSize()));
		getUIStringInput(FIELD_LABEL_MISS).setValue(String.valueOf(cache.getCacheHit()));
		getUIStringInput(FIELD_LABEL_HIT).setValue(String.valueOf(cache.getCacheMiss()));
		getUIStringInput(FIELD_LABEL_CURRENTSIZE).setValue(String.valueOf(cache.getCacheSize()));				
	}
	
	public static class SaveActionListener extends EventListener<UIWCMCacheManagerForm> {
		public void execute(Event<UIWCMCacheManagerForm> event) throws Exception {
			UIWCMCacheManagerForm uiCacheForm = event.getSource();
			UIWCMCacheManagerPanel uiCachePanel = event.getSource().getAncestorOfType(
					UIWCMCacheManagerPanel.class);
			String livetime = uiCacheForm.getUIStringInput(FIELD_LIVE_TIME)
					.getValue();
			String maxsize = uiCacheForm.getUIStringInput(FIELD_MAX_SIZE).getValue();			
			uiCacheForm.cache.setLiveTime(Long.parseLong(livetime));
			uiCacheForm.cache.setMaxSize(Integer.parseInt(maxsize));			
			event.getRequestContext().addUIComponentToUpdateByAjax(uiCachePanel);
		}
	}
	
	public static class ClearActionListener extends EventListener<UIWCMCacheManagerForm> {
		public void execute(Event<UIWCMCacheManagerForm> event) throws Exception {
			UIWCMCacheManagerPanel uiCachePanel = event.getSource().getParent();
			UIWCMCacheManagerForm uiCacheForm = event.getSource();			
			uiCacheForm.cache.clearCache();			
			event.getRequestContext().addUIComponentToUpdateByAjax(uiCachePanel);
		}
	}

	public static class CancelActionListener extends
			EventListener<UIWCMCacheManagerForm> {
		public void execute(Event<UIWCMCacheManagerForm> event) throws Exception {
			UIWCMCacheManagerForm uiForm = event.getSource();
			UIWCMCacheManagerPanel wcmCache = uiForm
					.getAncestorOfType(UIWCMCacheManagerPanel.class);
			event.getRequestContext().addUIComponentToUpdateByAjax(wcmCache);
		}
	}
}
