CKEDITOR.plugins.add('acceptInline',
	{
    lang : ['en','fr','vi'],
		init : function(editor) {
			var pluginName = 'acceptInline';
			var mypath = this.path;	
			editor.ui.addButton(
				'acceptInline.btn',
				{
					label : editor.lang.AcceptUpdateInline,
					command : 'acceptInline.cmd',
					icon : mypath + '/images/accept.png'
				}
			);
			var cmd = editor.addCommand('acceptInline.cmd', {exec:acceptUpdate});
			cmd.modes = {wysiwyg: 1, source: 1};
			cmd.canUndo = false;	
		}
	}
);


function acceptUpdate(e){

  var InlineEditor = {
    init : function() {
      var parentLocation = window.location;
      this.hostName = parentLocation.href.substring(0, parentLocation.href.indexOf(parentLocation.pathname));
      this.xmlHttpRequest = false;
      this.command = "/contents/editing"
      this.defaultMethod = "POST";
      this.languageLoaded = false;
      this.InternalServerErrorMsg="";
      this.EmptyTitleErrorMsg = "";
      this.editorName = "";
    }
  };

  InlineEditor.presentationRequestChangePropertyPOST = function (functionName, propertyName, repo, workspace, uuid,  siteName, language,    params){
    var url = InlineEditor.hostName + eXo.env.portal.context + "/" + eXo.env.portal.rest + InlineEditor.command + functionName;
    url = url + "repositoryName="+repo + "&workspaceName=" + workspace + "&nodeUIID=" + uuid + "&siteName=" + siteName + "&language=" + language;
    if (propertyName!=null) {
      url = url + "&propertyName=" +encodeURIComponent( propertyName);
    }

    InlineEditor.presentationAjaxRequest(url, params, "POST");
  }

InlineEditor.presentationAjaxRequest = function (url, params, method) {
	  if(window.XMLHttpRequest && !(window.ActiveXObject)) {
	    try {
	      InlineEditor.xmlHttpRequest = new XMLHttpRequest();
	    } catch(e) {
	      InlineEditor.xmlHttpRequest = false;
	    }
	  } else if(window.ActiveXObject) {
	      try {
	        InlineEditor.xmlHttpRequest = new ActiveXObject("Msxml2.XMLHTTP");
	      } catch(e) {
	        try {
	          InlineEditor.xmlHttpRequest = new ActiveXObject("Microsoft.XMLHTTP");
	        } catch(e) {
	          InlineEditor.xmlHttpRequest = false;
	        }
	    }
	  }
	  if(InlineEditor.xmlHttpRequest) {
	    try {
	      InlineEditor.xmlHttpRequest.status = 200;
	    }catch (e) {
	    }
	    InlineEditor.xmlHttpRequest.onreadystatechange = InlineEditor.presentationAjaxResponse;
	    if (method) {
	      InlineEditor.xmlHttpRequest.open(method, url, true);
	      InlineEditor.xmlHttpRequest.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	      if (params) {
	        InlineEditor.xmlHttpRequest.setRequestHeader("Content-length", params.length);
	      }else {
	        InlineEditor.xmlHttpRequest.setRequestHeader("Content-length", 0);
	      }
	      InlineEditor.xmlHttpRequest.setRequestHeader("Connection", "close");      
	    }else {
	      InlineEditor.xmlHttpRequest.open(InlineEditor.defaultMethod, url, true);
	    }
	    if (params) {
	      InlineEditor.xmlHttpRequest.send(params);
	    }else {
	      InlineEditor.xmlHttpRequest.send();
	    }
	  }
	};
	
	InlineEditor.presentationAjaxResponse = function (){
		var xmlTreeNodes = InlineEditor.xmlHttpRequest.responseXML;		
	    var nodeList = xmlTreeNodes.getElementsByTagName("bundle");   
	    var locale_message = nodeList[0].getAttribute("message"); 
	    if (InlineEditor.xmlHttpRequest.readyState == 4) {
	      if (InlineEditor.xmlHttpRequest.status == 200) {
	        if(locale_message == "OK") {
			CKEDITOR.instances[InlineEditor.editorName].updateElement();
		}
	        else alert(locale_message);
	      }
	    }else {
	      try{
	        if (InlineEditor.xmlHttpRequest.status!=200) {
	          alert(InlineEditor.InternalServerErrorMsg + "\n" + InlineEditor.xmlHttpRequest.statusText);
	        }      
	      }catch (e) {
	      }
	    }
	}



  InlineEditor.init();
  window.InlineEditor = InlineEditor;

  var instanceCK = document.getElementById("cke_"+e.name);
  var container = e.container;
  InlineEditor.editorName = e.name;
  var repo = container.getAttribute("repo");
  var workspace = container.getAttribute("workspace");
  var uuid = container.getAttribute("uuid");
  var sitename = container.getAttribute("sitename");
  var language = container.getAttribute("language");
  var propertyname = container.getAttribute("propertyname");
  var data = "";
  if(propertyname.indexOf("exo:title") >= 0)
    data = e.editable().getText();
  else
    data = e.getData();

  
  var params =""; 
  params = "newValue=" + encodeURIComponent(data);
  InlineEditor.presentationRequestChangePropertyPOST("/property?", propertyname, repo, workspace, uuid, sitename, language, params);         
  return false;

}
