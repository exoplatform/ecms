/**
 * Created by toannh on 6/19/15.
 */
(function(gj) {
  var ShareContent = function() {}

  ShareContent.prototype.init = function(){
    gj(".uiShareDocuments.resizable .selectbox").addClass("input-medium");
    var dropdown = gj(".uiShareDocuments.resizable .spaceSwitcherContainer #DisplayModesDropDown");
    gj(dropdown).bind( "click", function() {
      var popup = spaceChooserPopup = gj(".uiShareDocuments.resizable .spaceChooserPopup");
      popup.offset({left:gj(this).offset().left, top:gj(this).offset().top+gj(this).height()-1});
    });

  }

  /**
   * Check space is selected,
   * if selected then enable share button otherwise not enable
   * @param space
   */
  ShareContent.prototype.checkSelectedSpace = function(space){
    if("[]" === space) {
      gj(".PopupContent .uiActionBorder .btn-primary").attr("disabled","disabled");
    }else{
      gj(".PopupContent .uiActionBorder .btn-primary").removeAttr("disabled")
    }
  }

  eXo.ecm.ShareContent = new ShareContent();
  return {
    ShareContent : eXo.ecm.ShareContent
  };

})(gj);