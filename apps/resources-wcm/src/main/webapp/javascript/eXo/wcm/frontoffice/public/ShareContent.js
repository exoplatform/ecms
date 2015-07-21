/**
 * Created by toannh on 6/19/15.
 */
(function(gj, _) {
  var ShareContent = function() {}

  function correctSpacePos(){
    var dropdown = gj(".uiShareDocuments.resizable .spaceSwitcherContainer #DisplayModesDropDown");
    gj(dropdown).bind( "click", function() {
      var popup = gj(".uiShareDocuments.resizable .spaceChooserPopup");
      gj(popup).addClass("hide");
      popup.offset({left:gj(this).offset().left, top:gj(this).offset().top+gj(this).height()-1});
      gj(popup).removeClass("hide");
    });
  }

  ShareContent.prototype.init = function(){
    gj(".uiShareDocuments.resizable .selectbox").addClass("input-medium");
    correctSpacePos();
    var placeholderMsg = gj(".uiShareDocuments.resizable #textAreaInput").attr("placeholder");
    if(placeholderMsg === gj(".uiShareDocuments.resizable #textAreaInput").val()) gj(".uiShareDocuments.resizable #textAreaInput").val("");

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
      idAction : 'ShareButton',
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
    var placeHolderDiv = "<div class=\"placeholder\" style=\"top: 5px;\">"+placeholderMsg+"</div>"
    gj(".uiShareDocuments.resizable .exo-mentions").append(placeHolderDiv);
    gj('#DisplaytextAreaInput').bind('DOMNodeInserted DOMSubtreeModified DOMNodeRemoved', function(event) {
      gj(".uiShareDocuments.resizable #textAreaInput").exoMentions('val', function(value) {
        gj(".uiShareDocuments.resizable #textAreaInput").val(value);
      });
    });
    gj('#DisplaytextAreaInput').trigger('focus');
  }

  /**
   * Check space is selected,
   * if selected then enable share button otherwise not enable
   * @param space
   */
  ShareContent.prototype.checkSelectedSpace = function(space){
    correctSpacePos();
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

})(gj, mentions._);