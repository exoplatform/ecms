/**
 * Created by toannh on 6/19/15.
 */
(function(gj, _) {
  var ShareContent = function() {}

  function correctSpacePos(){
    var dropdown = gj(".uiShareDocuments.resizable .spaceSwitcherContainer #DisplayModesDropDown");
    var popup = gj(".uiShareDocuments.resizable .spaceChooserPopup");
    gj(popup).addClass("hide");
    gj(dropdown).bind( "click", function() {
      popup.offset({left:gj(this).offset().left, top:gj(this).offset().top+gj(this).height()-1});
      gj(".spaceChooserPopup").css("width", gj("#SpaceSwitcher").width()-2);
      gj(popup).removeClass("hide");
    });
  }

  ShareContent.prototype.init = function(){
    gj(".uiShareDocuments.resizable .selectbox").addClass("input-medium");
    var sharePopupWindow = gj(".uiShareDocuments.resizable").closest(".UIPopupWindow");
    sharePopupWindow.css("min-width", sharePopupWindow.width());
    sharePopupWindow.find(".uiIconResize").bind("mousedown", function(){
      gj(".uiShareDocuments.resizable .spaceChooserPopup .uiIconClose").trigger("click");
    })
    correctSpacePos();
    //disable shortcuts for pdf.js when a document is opened
    gj("#UIShareDocument").on("keydown", function (evt) {
      evt.stopPropagation();
    });
    gj(".mention-input").on("change", function (event) {
      if (event.target.value !== "") {
        gj(".PopupContent .uiActionBorder .btn-primary").removeAttr("disabled");
      }
    });
    gj('.sharePermission .dropdown-toggle').on('click', function () {
        var $dropdown = gj(this).parent().find('.dropdown-menu');
        $dropdown.css('left', -1 * $dropdown.width() + gj(this).parent().width());
    });
    gj("#AccessEntry .btn-toolbar").on("click", function (et) {
      var $button = gj(et.target).closest(".uiActionWithLabel");
      var elId = $button.attr("id").split("-")[1];
      var a = gj("#who .dropdown-menu");
      for (index = 0; index < a.length; ++index) {
        var element = gj("#who .dropdown-menu")[index];
        if (element.parentElement.id.split("-")[1] == elId) continue;
        if (element.style.display == "block") {
          element.style.display = "none";
        }
      }
      var parentTop = $button[0].getClientRects()[0].top;
      if (gj(et.target).closest(".uiActionWithLabel").attr("id").indexOf("view") == 0) {
        var element = gj("#canModify-" + elId + " .dropdown-menu")[0];
      } else {
        var element = gj("#canView-" + elId + " .dropdown-menu")[0];
      }
      if (element.style.display == "block") {
        element.style.display = "none";
      } else {
        var $popup = gj(element).closest(".UIPopupWindow");
        var top = parentTop - $popup[0].getClientRects()[0].top + 25;
        element.style.top = top + "px";
        element.style.left = ($button[0].getClientRects()[0].left - $popup[0].getClientRects()[0].left) + 'px';
        element.style.display = "block";
      }
    });
    gj("#who").on("scroll", function (e) {
      var a = gj("#who .dropdown-menu");
      for (index = 0; index < a.length; ++index) {
        var element = gj("#who .dropdown-menu")[index];
        if (element.style.display == "block") {
          /*var parentTop = element.parentNode.getClientRects()[0].top;
           var top = parentTop - element.closest(".UIPopupWindow").getClientRects()[0].top + 23;
           element.style.top = top + "px";*/
          element.style.display = "none";
        }
      }
    });
    gj("#UIShareDocument").on("scroll", function () {
      var a = gj("#who .dropdown-menu");
      for (index = 0; index < a.length; ++index) {
        var element = gj("#who .dropdown-menu")[index];
        if (element.style.display == "block") {
          /*var parentTop = element.parentNode.getClientRects()[0].top;
           var top = parentTop - element.closest(".UIPopupWindow").getClientRects()[0].top + 23;
           element.style.top = top + "px";*/
          element.style.display = "none";
        }
      }
    });

    gj(".uiShareDocuments.resizable #textAreaInput").exoMentions({
      onDataRequest : function(mode, query, callback) {
        var url = window.location.protocol + '//' + window.location.host + '/' + eXo.env.portal.rest + '/social/people/getprofile/data.json?search=' + query;
        gj.getJSON(url, function(responseData) {
          responseData = _.filter(responseData, function(item) {
            return item.name.toLowerCase().indexOf(query.toLowerCase()) > -1;
          });
          callback.call(this, responseData);
        });
      },
      //idAction : 'ShareButton',
      actionLink : 'AttachButton',
      actionMention : 'mentionButton',
      elasticStyle : {
        maxHeight : '80px',
        minHeight : '80px',
        marginButton: '4px',
        enableMargin: false
      },
      messages : window.eXo.social.I18n.mentions
    });
    if (gj("#userSuggester").val() != "") {
      eXo.ecm.ShareContent.checkUpdatedEntry();
    }
  }

  ShareContent.prototype.doShare = function(){
    gj(".uiShareDocuments.resizable #textAreaInput").exoMentions('val', function(value) {
      value = value.replace(/<br\/?>/gi, '\n').replace(/&lt;/gi, '<').replace(/&gt;/gi, '>');
      gj(".uiShareDocuments.resizable #textAreaInput").val(value);
      gj(".PopupContent .uiActionBorder .btn-primary").attr("disabled","disabled");
      gj("#shareActionBtn").trigger("click");
    });
  }

  /**
   * Check entry is selected,
   * if selected then enable share button otherwise not enable
   * @param entry
   */
  ShareContent.prototype.checkSelectedEntry = function(entry){
    correctSpacePos();
    if (entry) {
      if ("[]" === entry) {
        gj(".PopupContent .uiActionBorder .btn-primary").attr("disabled", "disabled");
      } else {
        gj(".PopupContent .uiActionBorder .btn-primary").removeAttr("disabled")
      }
    }
  }

  /**
   * Check entry is updated,
   * if the permission of the entry has changed or the entry is removed
   */
  ShareContent.prototype.checkUpdatedEntry = function(){
    gj(".PopupContent .uiActionBorder .btn-primary").removeAttr("disabled");
  }

  eXo.ecm.ShareContent = new ShareContent();
  return {
    ShareContent : eXo.ecm.ShareContent
  };

})(gj, mentions._);
