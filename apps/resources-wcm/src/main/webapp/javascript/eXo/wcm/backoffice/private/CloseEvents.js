(function(gj, uiForm) {
	/**
	* Variable to check if Content Selector is opened in IE
	* - true if Content Selector is opened
	**/
	window.popup_opened = false;

	/**
	 * Variable to check if some content has changed
	 * - true if some content has changed
	 **/
	var b_changed = false;
	var rememberedLocation = "rememberedLocation";
	var rememberedDriveName = "rememberedDriveName";
	
	/**
	 * Change the current state to inform some content has changed
	 **/
	window.changed = function () {
	  b_changed = true;
	}
	
	/**
	 * UPDATE AJAX GET METHOD
	 * - manage changes popup
	 **/
	
	window.ajaxGet = function (url, callback) {
	  var bypassActionbar= -1; //url.indexOf("uicomponent=UIActionBar_");
	  if (b_changed && bypassActionbar<=0) {
	    var answer = confirm(document.getElementById("NavigateConfirmationMsg").innerHTML);
	    if (answer) {
	      b_changed = false;
	    } else {
	      return;
	    }
	    
	  }
	
	  if (!callback) callback = null ;
	  doRequest("Get", url, null, callback) ;
	};
	
	/**
	 * UPDATE FORM SUBMIT METHOD
	 * - manage changes popup
	 * - manage CKeditor update in textareas
	 **/
	eXo.webui.UIForm.submitForm = function(formId, action, useAjax, callback) {
	  if(action.toLowerCase() == "changetype" || action.toLowerCase() == "close" || action.toLowerCase() == "back") {
	  	if (eXo.ecm.ECMUtils) {
	      eXo.ecm.ECMUtils.editFullScreen = false;
	    }
	    if (b_changed) {      
			  var answer = null;
			  if (action.toLowerCase() == "changetype") {
			  	answer = confirm(document.getElementById("ChangeTypeConfirmationMsg").innerHTML);
			  } else {
			  	answer = confirm(document.getElementById("CloseConfirmationMsg").innerHTML);
			  }  
	      if (answer) {
	      	b_changed = false;
	      }	else {
	      	return;
	      }
	    }
	  }    
	 
	 if (!callback) callback = null;
	 var form = this.getFormElemt(formId) ;

	 try {
	  if (CKEDITOR && typeof CKEDITOR == "object") {
	    for ( var name in CKEDITOR.instances ) {
	      var oEditor ;
	      try {
	        oEditor = CKEDITOR.instances[name] ;
	        if (oEditor && document.getElementById(name)) {
	            var rendered = gj(document.getElementById(name)).nextAll('div:first')[0].id.indexOf('cke');
	            if (rendered == 0) document.getElementById(name).value = oEditor.getData();
	        }
	      } catch(e) {
	        continue ;
	      }
	    }
	  }
	 } catch(e) {}
	  form.elements['formOp'].value = action ;
	  
	 var userAgent = window.navigator.userAgent;
	 if (userAgent.indexOf('MSIE ') > 0 || userAgent.indexOf('Trident/') > 0)
	 {// If IE11 or lower
	   if ((action.toLowerCase() == "save" || action.toLowerCase() == "saveandclose" || action.toLowerCase() == "close") && window.popup_opened == true) {
	     //with IE 11 and lower, we have to do a submit instead of a ajaxPost
	     //to keep memory of backTo parameter, we get it from query
	     //and recopy it as parameter of the form action
	     //then, on the next page, after the submit, the backto parameter will still exists
	     var backto = "";
	     var query = window.location.search.substring(1);
	     var vars = query.split("&");
	     for (var i=0;i<vars.length;i++) {
	         var pair = vars[i].split("=");
	         if(pair[0] == "backto") {
	             backto=pair[1];
	             break;
		}
	     }
	     if (!backto=="") {
		form.action=form.action+("&backto="+backto);
	     }
	     window.onbeforeunload = null;
	     useAjax=false;
	     window.popup_opened = false;
	   }
	  }

	  if(useAjax) {
	    b_changed = false;
	    this.ajaxPost(form, callback) ;
	  } else {
	    form.submit();
	  }
	  if (action.toLowerCase() == "saveandclose" || action.toLowerCase() == "close") {
	    localStorage.removeItem(rememberedLocation);
	    localStorage.removeItem(rememberedDriveName);
	  }
	} ;
	
	/**
	 * END UPDATE FORM
	 **/
	
	/**
	 * Submits a form by Ajax, with the given action and the given parameters
	 * Calls ajaxPost of PortalHttpRequest
	 * Note: ie bug  you cannot have more than one button tag
	 */
	eXo.webui.UIForm.submitEvent = function(formId, action, params) {
	  var form = this.getFormElemt(formId) ;
	  try {
		  if (CKEDITOR && typeof CKEDITOR == "object") {
		    for ( var name in CKEDITOR.instances ) {
		      var oEditor ;
		      try {
		        oEditor = CKEDITOR.instances[name] ;
		        if (oEditor && document.getElementById(name)) {
		            var rendered = gj(document.getElementById(name)).nextAll('div:first')[0].id.indexOf('cke');
		            if (rendered == 0) document.getElementById(name).value = oEditor.getData();
		        }
		      } catch(e) {
		        continue ;
		      }
		    }
		  }
		 } catch(e) {}


	  form.elements['formOp'].value = action ; 
	  if(!form.originalAction) form.originalAction = form.action ; 
		form.action =  form.originalAction +  encodeURI(params) ;
	  b_changed = false;
	  this.ajaxPost(form) ;
	  
	  var userAgent = window.navigator.userAgent;
	  if (userAgent.indexOf('MSIE ') > 0 || userAgent.indexOf('Trident/') > 0)
	  {//If IE 11 or lower
	    if (action.toLowerCase() == "changetab" && window.popup_opened == true) {
	      //with IE 11 and lower, we have to do a submit instead of a ajaxPost
	      //to keep memory of backTo parameter, we get it from query
	      //and recopy it as parameter of the form action
	      //then, on the next page, after the submit, the backto parameter will still exists
	      var backto = "";
	      var query = window.location.search.substring(1);
	      var vars = query.split("&");
	      for (var i=0;i<vars.length;i++) {
	         var pair = vars[i].split("=");
	         if(pair[0] == "backto") {
	             backto=pair[1];
	             break;
		}
	      }
	      if (!backto=="") {
	         form.action=form.action+("&backto="+backto);
	      }
	      window.popup_opened = false;
	      form.submit();
	    }
	  }
	  
	};
	
	/**
	 * Before we change the url, we check if the content has changed
	 * Inform the user with a popup
	 **/
	
	window.closeIt = function (e) {
	  if (b_changed) {
	    var e = e || window.event;
	    // For IE and Firefox
	    if (e) {
	      e.returnValue = 'The changes you made will be lost if you navigate away from this page.';
	    }
	  
	    // For Safari
	    return 'The changes you made will be lost if you navigate away from this page.';
	  }
	};
	 
	window.changeElements = function (divId) {
	  var UIDocForm = document.getElementById(divId);
	  if (!UIDocForm) return;
	  //add onchange event into <input> tags
	  var inputTags = UIDocForm.getElementsByTagName("input");
	  for (var i = 0; i < inputTags.length; i++) {
	    inputTags[i].attachEvent("onchange", new Function ("changed();"));
	  }
	
	  //add onchange event into <select> tags
	  var selectTags = UIDocForm.getElementsByTagName("select");
	  for (var i = 0; i < selectTags.length; i++) {
	    selectTags[i].attachEvent("onchange", new Function ("changed();"));
	  }
	
	  //add onchange event into <textarea> tags
	  var textareaTags = UIDocForm.getElementsByTagName("textarea");
	  for (var i = 0; i < textareaTags.length; i++) {
	    textareaTags[i].attachEvent("onchange", new Function ("changed();"));
	  }
	}
	
	
	window.changeWarning = function () {
	  b_changed = false;
	  /**
	   * Catch when some content has changed in the form
	   **/
	  if (navigator.userAgent.indexOf("MSIE") >= 0) {
	      changeElements("UIDocumentForm");
	      changeElements("UITask");
	  } else {
	    if (document.getElementById("UIDocumentForm")) {
	      document.getElementById("UIDocumentForm").setAttribute("onchange", "changed()");
	    }
	    if (document.getElementById("UITask")) {
	      document.getElementById("UITask").setAttribute("onchange", "changed()");
	    }
	  }
	    
	  /**
	  * Catch any url changes in the browser
	  **/
	  window.onbeforeunload = closeIt;  
	    
	  /**
	  * Update each textarea when you type inside CKEditor
	  * Inform the page that some content has changed
	  **/
	  try {
	    if (CKEDITOR && typeof CKEDITOR == "object") {
		  for ( var name in CKEDITOR.instances ) {
		    var oEditor ;
		    try {
			  oEditor = CKEDITOR.instances[name] ;
			  /**
			  * inform the content has changed
			  * update the textarea with last modifiedcontent
			  */
			  oEditor.on( 'key', function() {
			    b_changed = true;
			  });
		    } catch(e) {
		      continue ;
		    }
	      }
	    }
	  } catch(e) {}  
	};
})(gj, uiForm);

