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
      popup.offset({left:gj(this).offset().left, top:popup.offset().top});
    });

  }

  eXo.ecm.ShareContent = new ShareContent();
  return {
    ShareContent : eXo.ecm.ShareContent
  };

})(gj);