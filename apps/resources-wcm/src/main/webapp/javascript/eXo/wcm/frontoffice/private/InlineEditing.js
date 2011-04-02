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
    if (msg[0]) InternalServerErrorMsg = msg[0];
    if (msg[1]) EmptyTitleErrorMsg = msg[1];
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

InlineEditor.presentationRequestChangeTitle = function (oldTitleID, newTitleID, repo, workspace, uuid, block2hidden, block2show, siteName, isCKEDITOR){
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
  InlineEditor.presentationRequestChangePropertyPOST(functionName, null, repo, workspace, uuid, siteName, params);
  return false;
}

InlineEditor.presentationRequestChangeSummary = function (oldSummary, newSummaryID, repo, workspace, uuid, block2hidden, block2show, siteName){
  var functionName ="/summary?" 
  var strSummary = document.getElementById(newSummaryID).value;
  var params ="";
  
  if (strSummary==oldSummary) {
    InlineEditor.presentationSwitchBlock(block2hidden, block2show);
    return false;
  }
  params = "newValue=" + encodeURIComponent(strSummary);
  InlineEditor.presentationRequestChangePropertyPOST(functionName, null, repo, workspace, uuid, siteName, params);  
  return false;
}

InlineEditor.presentationRequestChangeProperty = function (functionName, propertyName, newValue, repo, workspace, uuid,  siteName, params, method){
  var url = InlineEditor.hostName + eXo.env.portal.context + "/" + eXo.env.portal.rest + InlineEditor.command + functionName
  url = url + "&repositoryName="+repo + "&workspaceName=" + workspace + "&nodeUIID=" + uuid + "&siteName=" + siteName;
  if (propertyName!=null) {
    url = url + "&" +encodeURIComponent( propertyName);
  }
  InlineEditor.presentationAjaxRequest(url, params, method);
}

InlineEditor.presentationRequestChangePropertyPOST = function (functionName, propertyName, repo, workspace, uuid,  siteName, params){
  var url = InlineEditor.hostName + eXo.env.portal.context + "/" + eXo.env.portal.rest + InlineEditor.command + functionName
  url = url + "repositoryName="+repo + "&workspaceName=" + workspace + "&nodeUIID=" + uuid + "&siteName=" + siteName
  if (propertyName!=null) {
    url = url + "&" +encodeURIComponent( propertyName);
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
    if (InlineEditor.xmlHttpRequest.readyState == 4) {
      if (InlineEditor.xmlHttpRequest.status == 200) {
        location.reload(true);
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