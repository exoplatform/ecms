/**
 * Editor buttons module.
 */
(function($) {
  "use strict";

  // Functions to create editor buttons
  var buttonsFns = [];
  /**
   * Editor core class.
   */
  function EditorButtons() {
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
      var $target = $("#activityContainer" + activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left");
      var $container = $target.find(".editorButtonContainer");
      if ($container.length == 0) {
        $container = $("<div class='editorButtonContainer'></div>");
        $target.append($container);
      }
      
      
      if(buttonsFns.length == 1) {
        var $btn = buttonsFns[0].createButtonFn();
        console.log("BTN: " + JSON.stringify($btn));
        $container.append($btn);
      } else {
        var $btn = buttonsFns[0].createButtonFn();
        console.log("BTN: " + JSON.stringify($btn));
        $container.append($btn);
        
        var $dropdownContainer = $("<div class='dropdown-container'></div>");
        var $toggle = $("<button class='btn dropdown-toggle' data-toggle='dropdown'>" +
        "<i class='uiIconArrowDown uiIconLightGray'></i></span></button>");
        
        var $dropdown = $("<ul class='dropdown-menu'></ul>");
        
        for(var i = 1; i < buttonsFns.length; i++) {
          var $btn = buttonsFns[i].createButtonFn();
          console.log("BTN to dropdown: " + JSON.stringify($btn));
          $dropdown.append($btn);
        }
        $dropdownContainer.append($toggle);
        $dropdownContainer.append($dropdown);
        $container.append($dropdownContainer);

      }
      
      /* 
      addEditorButtons(JSON.parse(buttons), $target);*/
    };
    
    this.initPreviewButtons = function(activityId, index) {
      console.log("Preview buttons: " + JSON.stringify(buttonsFns));
    };
    
    this.addCreateButtonFn = function(provider, createButtonFn) {
      console.log("Add create button fn: " + provider + " " + createButtonFn);
      var buttonFn = { "provider" : provider, "createButtonFn" : createButtonFn };
      var index = buttonsFns.findIndex(elem => elem.provider === provider);
      if (index === -1) {
        buttonsFns.push(buttonFn);
      } else {
        buttonsFns[index] = buttonFn;
      }
    };
    
    this.resetButtons = function() {
      console.log("reset buttons");
      buttonsFns = [];
    }
    
  }
  return new EditorButtons();

})($);