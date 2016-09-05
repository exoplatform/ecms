(function($,userPopupPlugin,socialUtil) {

function UIVersionInfo(){
};

  UIVersionInfo.prototype.init = function(formId) {
  var me = eXo.ecm.UIVersionInfo;
  me.selectedCheckbox = new Array();
  var versionListForm = document.getElementById(formId);
  var inputs = $(versionListForm).find(':checkbox');
  me.compareButton = $("#CompareVersion");
  $(this.compareButton).click(function(evt) {
    if (me.selectedCheckbox.length == 2) {
      //eXo.ecm.AjaxRequest.makeNewHash("#CompareRevision");
    }
  });

  var countCheckBox = inputs.length;
  for ( var i = 0; i < countCheckBox; i++) {
    var input = inputs[i];
    input.checked = false;
    $(input).click(me.onCheck);
  }
  $(me.compareButton).attr('class', 'btn disableButton');
  $(me.compareButton).attr('disabled', true);
};

  UIVersionInfo.prototype.onCheck = function(evt) {
  var me = eXo.ecm.UIVersionInfo;
  var evt = evt || window.event;
  var target = evt.target || evt.srcElement;
  
  if (target.checked == true) {
    me.selectedCheckbox.push(target);
    if (me.selectedCheckbox.length > 2) {
      var popCheckbox = me.selectedCheckbox.shift();
      popCheckbox.checked = false;
    }
  } else {
    me.selectedCheckbox.splice(me.selectedCheckbox.indexOf(target),1);
  }
  
  if (me.selectedCheckbox.length == 2) {
    $(me.compareButton).attr('class','btn btn-primary');
	$(me.compareButton).attr('disabled', false);
  } else {
    $(me.compareButton).attr('class','btn disableButton');
	$(me.compareButton).attr('disabled', true);
  }
};

  UIVersionInfo.prototype.initUserProfilePopup = function(globalLabels) {
    var labels = {};
    var profileLabels = $.extend(true, {}, labels, globalLabels);
    $.each(profileLabels, function(key) {
      profileLabels[key] =  window.decodeURIComponent(profileLabels[key]);
    });
    $(".userAvatarLink").userPopup({
      restURL: '//' + window.location.host + eXo.social.portal.context + '/' + eXo.social.portal.rest + '/social/people' + '/getPeopleInfo/{0}.json',
      labels: profileLabels,
      content: false,
      defaultPosition: "left",
      keepAlive: true,
      maxWidth: "240px"
    });
  };

eXo.ecm.UIVersionInfo = new UIVersionInfo();
return eXo.ecm.UIVersionInfo;

})($,userPopupPlugin,socialUtil);
