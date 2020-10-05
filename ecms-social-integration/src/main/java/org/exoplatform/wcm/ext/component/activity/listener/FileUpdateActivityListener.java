/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.ext.component.activity.listener;

import java.util.Arrays;
import java.util.List;

import javax.jcr.*;

import org.apache.commons.chain.Context;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.core.AuditPropertyImpl;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;


/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 15, 2011
 */
public class FileUpdateActivityListener extends Listener<Context, String> {

  private static final Log LOG = ExoLogger.getLogger(FileUpdateActivityListener.class);
  private static final String UPDATE_COMMENT = "files:spaces.UPDATE_COMMENT";

  private String[]  editedField     = {"exo:title", "exo:summary", "exo:language", "dc:title", "dc:description", "dc:creator", "dc:source", "jcr:data"};
  private String[]  bundleMessage   = {"SocialIntegration.messages.rename",
                                       "SocialIntegration.messages.editSummary",
                                       "SocialIntegration.messages.editLanguage",
                                       "SocialIntegration.messages.editTitle",
                                       "SocialIntegration.messages.editDescription",
                                       "SocialIntegration.messages.singleCreator",
                                       "SocialIntegration.messages.addSource",
                                       "SocialIntegration.messages.editFile"};
  private String[]  bundleRemoveMessage = {"SocialIntegration.messages.removeName",
      																 	   "SocialIntegration.messages.removeSummary",
      																 	  "SocialIntegration.messages.removeLanguage",
                                           "SocialIntegration.messages.removeTitle",
                                           "SocialIntegration.messages.removeDescription",
                                           "SocialIntegration.messages.removeCreator",
                                           "SocialIntegration.messages.addSource",
                                           "SocialIntegration.messages.editFile"};
  
  private boolean[] needUpdate      = {true, true, false, true, true, false, false, true};
  private int consideredFieldCount = editedField.length;
  /**
   * Instantiates a new post edit content event listener.
   */
  public FileUpdateActivityListener() {
	  
  }

  @Override
  public void onEvent(Event<Context, String> event) throws Exception {
	ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
	if(!activityManager.isActivityTypeEnabled(UPDATE_COMMENT)) {
	  return;
	}
    Context context = event.getSource();
    Property currentProperty = (Property) context.get(InvocationContext.CURRENT_ITEM);
    Property previousProperty = (Property) context.get(InvocationContext.PREVIOUS_ITEM);
    String propertyName = event.getData();
    StringBuilder oldValueBuilder = new StringBuilder();
    StringBuilder newValueBuilder = new StringBuilder();
    StringBuilder commentValueBuilder = new StringBuilder();
    Node currentNode = currentProperty.getParent();
    try {
    	if(!propertyName.equals(NodetypeConstant.JCR_DATA)) {
	    	if(currentProperty.getDefinition().isMultiple()){
	    		Value[] values = currentProperty.getValues();
	    		if(values != null && values.length > 0) {
	    			for (Value value : values) {
              newValueBuilder.append(value.getString()).append(ActivityCommonService.METADATA_VALUE_SEPERATOR);
              commentValueBuilder.append(value.getString()).append(", ");
						}
	    			if(newValueBuilder.length() >= ActivityCommonService.METADATA_VALUE_SEPERATOR.length()) 
	    				newValueBuilder.delete(newValueBuilder.length() - ActivityCommonService.METADATA_VALUE_SEPERATOR.length(),
	    				                       newValueBuilder.length());
	    			if(commentValueBuilder.length() >=2) 
	    			  commentValueBuilder.delete(commentValueBuilder.length() - 2, commentValueBuilder.length());
	    		}
			List<ValueData> valueList = ((PersistedPropertyData) ((AuditPropertyImpl) previousProperty).getData()).getValues();
	    		if(valueList != null) {
	    			for (ValueData value : valueList) {
	    				oldValueBuilder.append(value.toString()).append(ActivityCommonService.METADATA_VALUE_SEPERATOR);
						}
	    			if(oldValueBuilder.length() >= ActivityCommonService.METADATA_VALUE_SEPERATOR.length()) 
	    				oldValueBuilder.delete(oldValueBuilder.length() - ActivityCommonService.METADATA_VALUE_SEPERATOR.length(),
	    				                       oldValueBuilder.length());
	    		}
	    	} else {
	    		newValueBuilder = new StringBuilder(currentProperty.getString());
	    		commentValueBuilder = newValueBuilder;
	    		if(previousProperty != null && previousProperty.getValue() != null)
	    			oldValueBuilder = new StringBuilder(previousProperty.getValue().getString());
	    	}
    	}
    }catch (Exception e) {
        LOG.info("Cannot get old value");
    }
    String newValue = newValueBuilder.toString().trim();
    String oldValue = oldValueBuilder.toString().trim();
    String commentValue = commentValueBuilder.toString().trim();
    
    if(currentNode.isNodeType(NodetypeConstant.NT_RESOURCE)) currentNode = currentNode.getParent();
    String resourceBundle = "";
    boolean hit = false;
    for (int i=0; i< consideredFieldCount; i++) {
      if (propertyName.equals(editedField[i])) {
      	hit = true;
      	if(newValue.length() > 0) {
      		
      		resourceBundle = bundleMessage[i];
      		//Post activity when update dc:creator property
      		if(propertyName.equals(NodetypeConstant.DC_CREATOR))
      		{
      			List<String> lstOld = Arrays.asList(oldValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR));
    				List<String> lstNew = Arrays.asList(newValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR));
    				String itemsRemoved = "";
    				int removedCount = 0;
    				int addedCount = 0;
    				StringBuffer sb = new StringBuffer();
    				for (String item : lstOld) {
							if(!lstNew.contains(item)) {
								sb.append(item).append(", ");
								removedCount++;
							}
						}
    				if(sb.length() > 0) {
    				  itemsRemoved = sb.toString();
    				  itemsRemoved = itemsRemoved.substring(0, itemsRemoved.length()-2);
    				}
    				sb.delete(0, sb.length());
    				String itemsAdded = "";
    				for (String item : lstNew) {
							if(!lstOld.contains(item)) {
								sb.append(item).append(", ");
								addedCount++;
							}
						}
    				if(sb.length() > 0) {
    					itemsAdded = sb.toString();
    					itemsAdded = itemsAdded.substring(0, itemsAdded.length()-2);
    				}
    				
    				if(itemsRemoved.length() > 0 && itemsAdded.length() > 0){ 
    					resourceBundle = (removedCount > 1) ?
    							"SocialIntegration.messages.removeMultiCreator" : "SocialIntegration.messages.removeCreator";
    					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, itemsRemoved, "");
    					
    					resourceBundle = (lstNew.size() > 1) ?
    							"SocialIntegration.messages.multiCreator" : "SocialIntegration.messages.singleCreator";
    					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, commentValue, "");
    	        break;
    				}      				  
    				else if(itemsRemoved.length() > 0) {
    					resourceBundle = (removedCount > 1) ?
    							"SocialIntegration.messages.removeMultiCreator" : "SocialIntegration.messages.removeCreator";
    					newValue = itemsRemoved;
    					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, newValue, "");
    	        break;
    				}
    				else if(itemsAdded.length() > 0) {
    					resourceBundle = (commentValue.split(",").length > 1) ?
    							"SocialIntegration.messages.multiCreator" : "SocialIntegration.messages.singleCreator";
    					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, commentValue, "");
    	        break;
    				}     			
      		}
      	  //Post activity when update dc:source property
      		if(propertyName.equals(NodetypeConstant.DC_SOURCE)) {      			
      				List<String> lstOld = Arrays.asList(oldValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR));
      				List<String> lstNew = Arrays.asList(newValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR));
      				String itemsRemoved = "";
      				int removedCount = 0;
      				int addedCount = 0;
      				StringBuffer sb = new StringBuffer();
      				for (String item : lstOld) {
								if(!lstNew.contains(item)) {
									sb.append(item).append(", ");
									removedCount++;
								}
							}
      				if(sb.length() > 0) {
      				  itemsRemoved = sb.toString();
      				  itemsRemoved = itemsRemoved.substring(0, itemsRemoved.length()-2);
      				}
      				sb.delete(0, sb.length());
      				String itemsAdded = "";
      				for (String item : lstNew) {
								if(!lstOld.contains(item)) {
									sb.append(item).append(", ");
									addedCount++;
								}
							}
      				if(sb.length() > 0) {
      					itemsAdded = sb.toString();
      					itemsAdded = itemsAdded.substring(0, itemsAdded.length()-2);
      				}
      				if(itemsRemoved.length() > 0 && itemsAdded.length() > 0){  					
      					resourceBundle = (removedCount > 1) ?
      							"SocialIntegration.messages.removeMultiSource" : "SocialIntegration.messages.removeSource";
      					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, itemsRemoved, "");
      					
      					resourceBundle = (addedCount > 1) ?
      							"SocialIntegration.messages.addMultiSource" : "SocialIntegration.messages.addSource";
      					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, itemsAdded, "");
      	        break;
      				}      				  
      				else if(itemsRemoved.length() > 0) {
      					resourceBundle = (removedCount > 1) ?
      							"SocialIntegration.messages.removeMultiSource" : "SocialIntegration.messages.removeSource";
      					newValue = itemsRemoved;
      					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, newValue, "");
      	        break;
      				}
      				else if(itemsAdded.length() > 0) {
      					resourceBundle = (addedCount > 1) ?
      							"SocialIntegration.messages.addMultiSource" : "SocialIntegration.messages.addSource";
      					newValue = itemsAdded;
      					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, newValue, "");
      	        break;
      				}      			
      		}
      		Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, commentValue, "");
	        break;
      	} else if(!propertyName.equals(NodetypeConstant.EXO_LANGUAGE)){ //Remove the property
      		resourceBundle = bundleRemoveMessage[i];      		
      		if(propertyName.equals(NodetypeConstant.DC_CREATOR)) {
      			resourceBundle = (oldValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR).length > 1) ?
  							"SocialIntegration.messages.removeMultiCreator" : "SocialIntegration.messages.removeCreator";
      		} else if(propertyName.equals(NodetypeConstant.DC_SOURCE)) {
      			resourceBundle = (oldValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR).length > 1) ?
  							"SocialIntegration.messages.removeMultiSource" : "SocialIntegration.messages.removeSource";
      		}
      		
      		if(propertyName.equals(NodetypeConstant.DC_SOURCE) || propertyName.equals(NodetypeConstant.DC_CREATOR)) {
      			commentValue = oldValue.replaceAll(ActivityCommonService.METADATA_VALUE_SEPERATOR, ", ");
      		}      		
      		Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, commentValue, "");
          break;
      	} else break;
      	        
      }
    }
    if(!hit && propertyName.startsWith("dc:") && !propertyName.equals("dc:date")) {
    	PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    	String dcProperty = propertyName;
    	try {
    		dcProperty = portletRequestContext.getApplicationResourceBundle().getString("ElementSet.dialog.label." + 
    	  		propertyName.substring(propertyName.lastIndexOf(":") + 1, propertyName.length()));
    	} catch(Exception ex) {
            LOG.info("Cannot get propertyName");
    	}
      if (newValue.length() > 0) {
        resourceBundle = "SocialIntegration.messages.updateMetadata";
      } else {
        resourceBundle = "SocialIntegration.messages.removeMetadata";
      }
      commentValue = dcProperty + ActivityCommonService.METADATA_VALUE_SEPERATOR + commentValue;
      Utils.postFileActivity(currentNode, resourceBundle, false, true, commentValue, "");
    }
  }
}
