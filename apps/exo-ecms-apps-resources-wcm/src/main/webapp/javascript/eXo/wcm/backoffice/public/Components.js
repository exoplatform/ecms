function SELocalization(){
}

SELocalization.prototype.cleanName = function(title, targetId) {
  nameField = document.getElementById(targetId);
  if (!nameField.readOnly) {
    var portalContext = eXo.env.portal.context;
    var portalRest = eXo.env.portal.rest;
    var retText = ajaxAsyncGetRequest(portalContext+"/"+portalRest+"/l11n/cleanName?name="+title, false);
    nameField.value = retText;
  }
};

eXo.ecm.SELocalization = new SELocalization();