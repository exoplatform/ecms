/**
 * Editor buttons module.
 */
(function($) {
  "use strict";

  /**
   * Editor core class.
   */
  function EditorButtons() {
    
    // Functions to create editor buttons
    var buttonsFns = [];
    
    /*
    var addEditorButtons = function(buttons, $target) {
      
      var $container = $target.find(".editorButtonContainer");
      if ($container.length == 0) {
        $container = $("<div class='editorButtonContainer'></div>");
        $target.append($container);
      }
      var $editorButton = $("<a title=\"\" data-placement=\bottom\" data-toggle=\"tooltip\" data-original-title=\"Click to start editing\" class=\"btn\"><i class=\"uiIconEdit uiIconLightGray\"></i><span class=\"editorLabel\">" + buttons[0].label + "</span></a>");
      $container.append($editorButton);
    };*/

    this.initActivityButtons = function(activityId) {
      console.log("Activity buttons: " + JSON.stringify(buttonsFns));
     /* var $target = $("#activityContainer" + activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left");
      addEditorButtons(JSON.parse(buttons), $target);*/
    };
    
    this.initPreviewButtons = function(activityId, index) {
      console.log("Preview buttons: " + JSON.stringify(buttonsFns));
    };
    
    this.addCreateButtonFn = function(provider, createButtonFn) {
      var buttonFn = { [provider]: createButtonFn };
      var index = buttonsFns.findIndex(it => Object.keys(it)[0] === provider);
      if (index === -1) {
        buttonsFns.push(buttonFn);
      } else {
        buttonsFns[index] = buttonFn;
      }
    };
    
    this.resetButtons = function() {
      buttonsFns = [];
    }
    
  }
  return new EditorButtons();

})($);