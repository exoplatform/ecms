/**
 * Onlyoffice Editor client.
 */
(function($) {
  "use strict";

  /**
   * Editor core class.
   */
  function EditorButtons() {

    this.initActivityButtons = function(buttons) {
      console.log("Activity buttons: " + JSON.stringify(buttons));
    };
    
    this.initPreviewButtons = function(buttons) {
      console.log("Preview buttons: " + JSON.stringify(buttons));
    };
    
  }
  return new EditorButtons();

})($);