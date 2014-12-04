(function(gj, ecmWebdav) {
  var OpenDocumentInOffice = function() {}

  OpenDocumentInOffice.prototype.openDocument = function(filePath){
    var documentManager = eXo.ecm.ECMWebDav.WebDAV.Client.DocManager;
    if (documentManager.IsMicrosoftOfficeAvailable() && documentManager.IsMicrosoftOfficeDocument(filePath)) {
      documentManager.MicrosoftOfficeEditDocument(filePath);
    } else {
      documentManager.JavaEditDocument(filePath, null, "/ecmexplorer/applet/ITHitMountOpenDocument.jar");
    }
  }

  /**
   * Update OpenDocument button's label
   * objId is workspace_name ':' node_path of file
   * activityId is id of activity in home page
   * rightClick update button when right click (context-menu)
   */
  OpenDocumentInOffice.prototype.updateLabel = function(objId, activityId, rightClick){
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
          openDocument.attr("href", "javascript:void(0);");
          openDocument.attr("onclick", "eXo.ecm.OpenDocumentInOffice.openDocument('"+data.filePath+"')");
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