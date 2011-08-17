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
  }
};

InlineEditor.loadLanguage =function (msg) {
  if (InlineEditor.languageLoaded) return;
  with(InlineEditor) {
    if (msg[0]) InternalServerErrorMsg =decodeURI(msg[0]);
    if (msg[1]) EmptyTitleErrorMsg = decodeURI(msg[1]);
  }  
}
InlineEditor.onEnterRequest =function () {
  if (window.event && window.event.keyCode == 13){
    return false;
  }
}
InlineEditor.presentationSwitchBlock = function (block2hidden, block2show) {
  var titleBlock = document.getElementById(block2hidden);
  var editBlock = document.getElementById(block2show);
  if (titleBlock!=null && editBlock!=null) {
    titleBlock.style.display="none";
    editBlock.style.display="block";
  }  
}

InlineEditor.presentationRequestChangeTitle = function (oldTitleID, newTitleID, repo, workspace, uuid, block2hidden, block2show, siteName, language, isCKEDITOR){
  var functionName ="/title?"
  var strTitle ="";
  var params ="";
  if (isCKEDITOR) {
    strTitle = CKEDITOR.instances[newTitleID].getData();
  }else {
    strTitle= document.getElementById(newTitleID).value;
  }
  var strOldTitle = document.getElementById(oldTitleID).innerHTML;
  if (strTitle==null) { 
    alert(InlineEditor.EmptyTitleErrorMsg);
    return false;
  }
  if (strTitle.length ==0) {
    alert(InlineEditor.EmptyTitleErrorMsg);
    return false;
  }
  if (strTitle==strOldTitle) {
    InlineEditor.presentationSwitchBlock(block2hidden, block2show);
    return false;
  }
  params = "newValue=" + encodeURIComponent(strTitle);
  InlineEditor.presentationRequestChangePropertyPOST(functionName, null, repo, workspace, uuid, siteName, language, params);
  return false;
}

InlineEditor.presentationRequestChangeSummary = function (oldSummary, newSummaryID, repo, workspace, uuid, block2hidden, block2show, siteName, language, isCKEDITOR){
  var functionName ="/summary?"
  var params =""; 
  var strSummary ="";
  if (isCKEDITOR) {
    strSummary = CKEDITOR.instances[newSummaryID].getData();
  }else {
    strSummary= document.getElementById(newSummaryID).value;
  }  
  
  params = "newValue=" + encodeURIComponent(strSummary);
  InlineEditor.presentationRequestChangePropertyPOST(functionName, null, repo, workspace, uuid, siteName, language, params);  
  return false;
}

InlineEditor.presentationRequestChangeText = function (oldText, newTextID, repo, workspace, uuid, block2hidden, block2show, siteName, language, isCKEDITOR){
  var functionName ="/text?"
  var params =""; 
  var strText ="";
  if (isCKEDITOR) {
    strText = CKEDITOR.instances[newTextID].getData();
  }else {
    strText= document.getElementById(newTextID).value;
  }  
  
  params = "newValue=" + encodeURIComponent(strText);
  InlineEditor.presentationRequestChangePropertyPOST(functionName, null, repo, workspace, uuid, siteName, language, params);  
  return false;
}

InlineEditor.presentationRequestChangeProperty = function (functionName, propertyName, oldText, newTextID, repo, workspace, uuid, block2hidden, block2show, siteName, language, isCKEDITOR){  
  var params =""; 
  var strText ="";
  if (isCKEDITOR) {
    strText = CKEDITOR.instances[newTextID].getData();
  }else {
    strText= document.getElementById(newTextID).value;
  }  
  params = "newValue=" + encodeURIComponent(strText);
  InlineEditor.presentationRequestChangePropertyPOST(functionName, propertyName, repo, workspace, uuid, siteName, language, params);  
  return false;
}

InlineEditor.presentationRequestChangePropertyPOST = function (functionName, propertyName, repo, workspace, uuid,  siteName, language, params){
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
        if(locale_message == "OK") location.reload(true);
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