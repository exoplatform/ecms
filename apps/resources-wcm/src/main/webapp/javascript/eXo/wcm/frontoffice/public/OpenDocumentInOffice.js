/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 12/16/14
 * Open document js
 *
 * handle openDocument by ITHIT library
 * handle event click, rightclick on ECMS, AS
 *
 */

(function(gj) {

  var portal = eXo.env.portal.context;
  var rest   = eXo.env.portal.rest;

  var restPrefix = portal+"/"+rest;

  var OpenDocumentInOffice = function() {}
  var uiWorkingAreaWidth=0;
  var uiRightContainerWidth=0;
  var uiRightContainerStyle="";

  var uisideBarWidth=0;

  var resizeBarHeight=0;
  var resizeBarContentWidth="";

  var uiActionBarContainer="";

  OpenDocumentInOffice.prototype.errorCallback = function (message) {
    var installerFilePath = "/open-document/js/Plugins/" + ITHit.WebDAV.Client.DocManager.GetInstallFileName();
    window.open(installerFilePath);
    }

  /**
   * Open document by Office application or desktop apps
   * absolutePath is webdav path of document. webdav server have to support level 2
   * mountPath
   */
  OpenDocumentInOffice.prototype.openDocument = function(filePath, mountPath){
    fitLayout();
    if(eXo.ecm.ECMWebDav !== undefined) { // use ITHIT to an open document
      var webDav = eXo.ecm.ECMWebDav;
      var documentManager = eXo.ecm.ECMWebDav.WebDAV.Client.DocManager;
      //Workaround ECMS-7477 : Open in Office on Chrome for Mac
      if(webDav.DetectOS.OS == "MacOS" && webDav.DetectBrowser.Chrome){
        documentManager.DavProtocolEditDocument(filePath, mountPath, this.errorCallback);
      }else {
        documentManager.EditDocument(filePath, mountPath, this.errorCallback);
      }
    }
    if(uisideBarWidth === 0){ //hide side bar
      gj("#UISideBar").show();
    }
  }

  /**
   * Update OpenDocument button's label
   * objId is workspace_name ':' node_path of file
   * activityId is id of activity in home page
   * rightClick update button when right click (context-menu)
   */
  OpenDocumentInOffice.prototype.updateLabel = function(objId, activityId, rightClick){
    var currentDocumentObj = {};
    gj.ajax({
      url: restPrefix+"/office/updateDocumentTitle?objId=" + encodeURI(objId)+"&lang="+eXo.env.portal.language,
      dataType: "text",
      type: "GET",
      async:false,
      cache:false
    })
        .success(function (data) {
          data = gj.parseJSON(data);
          var elClass = "uiIconEcmsOpenDocument";
          var isRightClick="";

          if(activityId != null && activityId != "undefined" && activityId != "") elClass +="_"+activityId;
          if(rightClick) {
            isRightClick="#ECMContextMenu";
            openDocument = gj(isRightClick+" ."+elClass).parent();
          }else{
            openDocument = gj("."+elClass).closest("a");
          }
          var html = "<i class=\"uiIcon16x16FileDefault uiIcon16x16nt_file ";
		  if("uiIcon16x16FileDefault" === data.ico){
			html+="uiIconOpenOnDesktop ";
		  }
		  html+= data.ico+" "+elClass+"\"></i>\n"+data.title;
          openDocument.html(html);

          if (!data.isFile) {
            openDocument.addClass("hidden");
            return;
          }
          if(eXo.ecm.ECMWebDav !== undefined) {
            //showButton
            //console.log("ITHIT detected!");
            gj(openDocument).removeClass("hidden");
            gj(openDocument).closest("li").show();
            if (data.isLocked) return;//can not edit, just show popup(do not change href)
          }else{
            if(!data.isMsoffice){
              openDocument.addClass("hidden");
            }else{
              openDocument.removeClass("hidden");
            }
            //console.log("ITHIT not detected!");
            defaultEnviromentFilter(openDocument);//only show with support enviroment.
          }
        });
        uiWorkingAreaWidth    = gj("#UIWorkingArea").width();
        uiRightContainerWidth = gj(".rightContainer").width();
        uiRightContainerStyle = gj(".rightContainer").attr("style");

        uisideBarWidth        = gj("#UISideBar").width();
        resizeBarHeight       = gj(".resizeBar").attr("style");
        resizeBarContentWidth = gj(".resizeBar").width();
        uiActionBarContainer  = gj("#uiActionsBarContainer").html();
  }

  OpenDocumentInOffice.prototype.showConfirmBox = function() {
    gj("body").ajaxComplete(function () {
      var asPopup = gj("#UIDocViewerPopup .UIPopupWindow");
      asPopup.css("z-index", "120");
      asPopup.addClass("opendocMask");
      var cancelBtn = asPopup.find(".Cancel");
      asPopup.find(".uiIconClose").removeAttr("onclick");
      asPopup.find(".uiIconClose").one("click", function () {
        cancelBtn.click();
      });
    });
  }
  /**
   * Open Document with ActiveX. This required enviroments:
   * - MSOffice 2010, 2013 or least version Already installed
   * - Enable ActiveX on IE browser or least version (only from IE11)
   * - Return open status
   * - Have to enable "Inittialize and script ActiveX controls not marked as save for scripting"
   *  path: Document's dav url.
   */
  OpenDocumentInOffice.prototype.EditDocument = function(path){
    var obj = new ActiveXObject('SharePoint.OpenDocuments.3');
    if(checkMSOfficeVersion()){
      obj.EditDocument(path);
      //console.log("Open Document status: "+openStatus);
    }else{
      //console.log("Open document not support!");
      return false;
    }
  }

  function fitLayout(){
    if (navigator.appVersion.indexOf("Mac") === -1) return;
    
    uiRightContainerStyle = gj(".rightContainer").attr("style");

    if(uisideBarWidth === 0){ //hide side bar
      gj("#UISideBar").hide();
      gj(".rightContainer").width(uiWorkingAreaWidth);
      gj("#uiActionsBarContainer").html(uiActionBarContainer);
    }else{
      gj(".rightContainer").width(uiRightContainerWidth);
      gj(".resizeBar").width(resizeBarContentWidth);
      gj(".resizeBar").attr("style", resizeBarHeight);
      gj(".resizeBarContent").attr("style", resizeBarHeight);
      gj("#uiActionsBarContainer").html(uiActionBarContainer);
    }
  }
	
  /**
   *To filter OpenXXX button only working with support enviroments
   * -IE 11 or least version,
   * -Window 7, 8
   * -MSOffice 2010, 2013
   * return true if support
   */
  function defaultEnviromentFilter(element){
    var ua = window.navigator.userAgent;

    var OSName="Unknown OS";
    if (navigator.appVersion.indexOf("Win")!=-1) OSName="Windows";
    if (navigator.appVersion.indexOf("Mac")!=-1) OSName="MacOS";
    if (navigator.appVersion.indexOf("X11")!=-1) OSName="UNIX";
    if (navigator.appVersion.indexOf("Linux")!=-1) OSName="Linux";

    //check IE 11, Window, Office 2010
    if (OSName === "Windows") {
      //check IE11, Office
      var isAtLeastIE11 = !!(ua.match(/Trident/) && !ua.match(/MSIE/));
      if(checkMSOfficeVersion() && isAtLeastIE11){
        gj(element).parent().show();
        return true;
      }

      // Hide if not enought enviroments support
      if(gj(element).parent().hasClass("detailContainer"))
        gj(element).hide();
      else
        gj(element).parent().hide();
      return false;
    }else{
      //other browser, hide this functional
      gj(element).closest("li").hide();
      gj(element).hide();
    }

    // other browser
    return false;
  }

  /**
   * Check ActiveX to get MS Office version
   * Return MS Office version is support
   */
  function checkMSOfficeVersion(){
    try{
      var word = new ActiveXObject("Word.Application");
      var version = word.Version;
      word.Quit();
      return version >= "14.0";
    }catch(err){
      //console.log("ActiveX is not support \n"+err);
      return false;
    }
    return false;
  }

  eXo.ecm.OpenDocumentInOffice = new OpenDocumentInOffice();
  return {
    OpenDocumentInOffice : eXo.ecm.OpenDocumentInOffice
  };

})(gj);
