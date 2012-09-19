/** 
 * Handle with toolbar of WAIPortal
 * @Author  Nguyen The Vinh From ECM
 */ 
function WAIPortalToolbar() {

}

WAIPortalToolbar.prototype.setCookie = function(c_name,value,exdays) {
  var exdate=new Date();
  exdate.setDate(exdate.getDate() + exdays);
  var c_value=escape(value) + ((exdays==null) ? "" : "; expires="+exdate.toUTCString());
  document.cookie=c_name + "=" + c_value;
}

WAIPortalToolbar.prototype.getCookie = function(c_name) {
  var i,x,y,ARRcookies=document.cookie.split(";");
  for (i=0;i<ARRcookies.length;i++)
  {
    x=ARRcookies[i].substr(0,ARRcookies[i].indexOf("="));
    y=ARRcookies[i].substr(ARRcookies[i].indexOf("=")+1);
    x=x.replace(/^\s+|\s+$/g,"");
    if (x==c_name) {
      return unescape(y);
    }
  }
}

WAIPortalToolbar.prototype.changeOveralFontSize = function(newFontSize) {
  document.body.style.fontSize = newFontSize;
  var fElementId = "WAIPortalFontSizeSmall";
  var dElement;
  var fClass     = "IconDefineStyle FontSizeSmallSelected";
  if (newFontSize == "90%") {
    fClass     = "IconDefineStyle FontSizeLargeSelected";
    fElementId = "WAIPortalFontSizeLarge";
  }else if  (newFontSize=="75%"){
    fClass     = "IconDefineStyle FontSizeMediumSelected";
    fElementId = "WAIPortalFontSizeMedium";
  }
  dElement = document.getElementById("WAIPortalFontSizeSmall");
  if (dElement) dElement.className = "IconDefineStyle FontSizeSmall";

  dElement = document.getElementById("WAIPortalFontSizeMedium");
  if (dElement) dElement.className = "IconDefineStyle FontSizeMedium";

  dElement = document.getElementById("WAIPortalFontSizeLarge");
  if (dElement) dElement.className = "IconDefineStyle FontSizeLarge";

  var fElement = document.getElementById(fElementId);
  if (fElement) fElement.className = fClass;
  eXo.ecm.WAIPortalToolbar.setCookie("ntTextSize", newFontSize, 20);
}

WAIPortalToolbar.prototype.changeOveralTheme = function(newTheme) {
  var pBody = document.getElementsByTagName("body")[0];
  var cName = pBody.className;
  cName = cName.replace(/SkinMono/g, "");
  cName = cName.replace(/undefine/g, "");
  cName = cName.trim();
  var dElement;
  if (newTheme=="SkinMono") {
    cName = cName + " " + newTheme;
    dElement = document.getElementById("WAIPortalThemeMono");
    if (dElement) dElement.className = "IconDefineStyle ChangeThemeMonoSelected";
    dElement = document.getElementById("WAIPortalThemeColor");
    if (dElement) dElement.className = "IconDefineStyle ChangeThemeColor";

    document.getElementById("WAIPortalFontSizeSmallImg").setAttribute("src", "/ecm-template-waiportal/skin/resources/WhiteSmallIcon.png");
    document.getElementById("WAIPortalFontSizeMediumImg").setAttribute("src", "/ecm-template-waiportal/skin/resources/WhiteMediumIcon.png");
    document.getElementById("WAIPortalFontSizeLargeImg").setAttribute("src", "/ecm-template-waiportal/skin/resources/WhiteLargeIcon.png");

  } else {
    dElement = document.getElementById("WAIPortalThemeMono");
    if (dElement) dElement.className = "IconDefineStyle ChangeThemeMono";
    dElement = document.getElementById("WAIPortalThemeColor");
    if (dElement) dElement.className = "IconDefineStyle ChangeThemeColorSelected";

    document.getElementById("WAIPortalFontSizeSmallImg").setAttribute("src", "/ecm-template-waiportal/skin/resources/BlackSmallIcon.png");
    document.getElementById("WAIPortalFontSizeMediumImg").setAttribute("src", "/ecm-template-waiportal/skin/resources/BlackMediumIcon.png");
    document.getElementById("WAIPortalFontSizeLargeImg").setAttribute("src", "/ecm-template-waiportal/skin/resources/BlackLargeIcon.png");
    
  }
  pBody.className = cName;
  eXo.ecm.WAIPortalToolbar.setCookie("ntCurrentTheme", newTheme, 20);
}

/** function initToolbar
 ** Purpose  init toolbar link, title attribute. alt attribute
 **          get the current selected theme and fontsize
 
**/
WAIPortalToolbar.prototype.initToolbar = function() {
  if (!document.getElementById("WAIPortalSkipToContentLink")) return;
  var savedTheme  = eXo.ecm.WAIPortalToolbar.getCookie("ntCurrentTheme");
  var savedTextSize = eXo.ecm.WAIPortalToolbar.getCookie("ntTextSize");
  eXo.ecm.WAIPortalToolbar.changeOveralTheme(savedTheme, "IconOnly");
  eXo.ecm.WAIPortalToolbar.changeOveralFontSize(savedTextSize, "IconOnly");
}
WAIPortalToolbar.prototype.addLoadEvent = function(func) { 
  var oldonload = window.onload; 
  if (typeof window.onload != 'function') { 
    window.onload = func; 
  } else { 
    window.onload = function() { 
      if (oldonload) { 
        oldonload(); 
      } 
      func(); 
    } 
  } 
} ;

eXo.ecm.WAIPortalToolbar = new WAIPortalToolbar();
_module.WAIPortalToolbar = eXo.ecm.WAIPortalToolbar;
eXo.ecm.WAIPortalToolbar.addLoadEvent(eXo.ecm.WAIPortalToolbar.initToolbar);