(function(gj, ecmWebdav) {
  var OpenDocumentInOffice = function() {}

  OpenDocumentInOffice.prototype.openDocument = function(absolutePath, workspace, filePath){
    var documentManager = eXo.ecm.ECMWebDav.WebDAV.Client.DocManager;
    var openStatus = false;
    if (documentManager.IsMicrosoftOfficeAvailable() && documentManager.IsMicrosoftOfficeDocument(absolutePath)) {
      if (!('ActiveXObject' in window)) absolutePath += '\0';
      openStatus = documentManager.MicrosoftOfficeEditDocument(absolutePath);
    } else {
      openStatus = documentManager.JavaEditDocument(absolutePath, null, "/ecmexplorer/applet/ITHitMountOpenDocument.jar");
    }

    //create version when successfully open
    if(openStatus){
      eXo.ecm.OpenDocumentInOffice.checkout(workspace, filePath);
    }
  }

  /*
   * Checkout a versioned document when open successfully with desktop application to edit.
   */
  OpenDocumentInOffice.prototype.checkout = function(workspace, filePath){
    gj.ajax({
      url: "/portal/rest/office/checkout?filePath=" + filePath+"&workspace="+workspace,
      dataType: "text",
      type: "GET",
      async: true
    })
        .success(function (data) {
          console.log("checkout status "+!data);
        });
  };

  /*Lock item */
  OpenDocumentInOffice.prototype.lockItem = function(filePath){

  } //end lock function

  /**
   * Update OpenDocument button's label
   * objId is workspace_name ':' node_path of file
   * activityId is id of activity in home page
   * rightClick update button when right click (context-menu)
   */
  OpenDocumentInOffice.prototype.updateLabel = function(objId, activityId, rightClick){

    eXo.ecm.ECMWebDav.WebDAV.Client.DocManager.ShowMicrosoftOfficeWarning();
    gj.ajax({
      url: "/portal/rest/office/updateDocumentLabel?objId=" + objId+"&lang="+eXo.env.portal.language,
      dataType: "text",
      type: "GET",
      async: false
      //	timeout:1000 * 10
    })
        .success(function (data) {
          data = gj.parseJSON(data);
          var elClass = "uiIconEcmsOpenDocument";
          var isRightClick="";
          if(activityId != null && activityId != "undefined" && activityId != "") elClass +="_"+activityId;
          if(rightClick) isRightClick="#ECMContextMenu";
          var openDocument = gj(isRightClick+" ."+elClass).parent();
          var html = "<i class=\"uiIcon16x16FileDefault uiIcon16x16nt_file "+data.ico+" "+elClass+"\"></i>\n"+data.title;
          openDocument.html(html);
          gj(".detailContainer").find('.openDocument').html(data.title);
        });

    setCookie("_currentDocument", objId, 1);
  }

  gj(window).load(function(){
    var _currentDocument = getCookie("_currentDocument");
    if(_currentDocument!=null && _currentDocument!="undefined" && _currentDocument!="")
      eXo.ecm.OpenDocumentInOffice.updateLabel(_currentDocument);

  });

  /**
   * Close all popup
   */

  OpenDocumentInOffice.prototype.closePopup = function(){
    console.log("close all popup");
  }


  /**
   * Set value to browser's cookie
   */
  function setCookie(cname, cvalue, exdays) {
    var d = new Date();
    d.setTime(d.getTime() + (exdays*24*60*60*1000));
    var expires = "expires="+d.toUTCString();
    document.cookie = cname + "=" + cvalue + "; " + expires;
  }

  /**
   *get cookie by key
   */
  function getCookie(cname) {
    var name = cname + "=";
    var ca = document.cookie.split(';');
    for(var i=0; i<ca.length; i++) {
      var c = ca[i];
      while (c.charAt(0)==' ') c = c.substring(1);
      if (c.indexOf(name) != -1) return c.substring(name.length,c.length);
    }
    return "";
  }

  OpenDocumentInOffice.prototype.openDocument_doClick = function(){
    gj("#uiActionsBarContainer .uiIconEcmsOpenDocument").parent().click();
  }

  eXo.ecm.OpenDocumentInOffice = new OpenDocumentInOffice();
  return {
    OpenDocumentInOffice : eXo.ecm.OpenDocumentInOffice
  };

})(gj, ecmWebdav);