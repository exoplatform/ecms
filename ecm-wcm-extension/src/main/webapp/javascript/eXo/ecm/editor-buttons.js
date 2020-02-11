/**
 * Onlyoffice Editor client.
 */
(function($) {
  "use strict";

  /**
   * Editor core class.
   */
  function EditorButtons() {
    
    /**
     * Returns the html markup of the 'Edit Online' button.
     */
    var addEditorButtons = function(buttons, $target) {
      var $container = $target.find(".editorButtonContainer");
      if ($container.length == 0) {
        $container = $("<div class='editorButtonContainer'></div>");
        $target.append($container);
      }
      var $editorButton = $("<a title=\"\" data-placement=\bottom\" data-toggle=\"tooltip\" data-original-title=\"Click to start editing\" class=\"btn\"><i class=\"uiIconEdit uiIconLightGray\"></i><span class=\"editorLabel\">" + buttons[0].label + "</span></a>");
      $container.append($editorButton);
    };

    this.initActivityButtons = function(buttons, activityId) {
      console.log("Activity buttons: " + JSON.stringify(buttons));
      var $target = $("#activityContainer" + activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left");
      addEditorButtons(JSON.parse(buttons), $target);
    };
    
    this.initPreviewButtons = function(buttons, activityId, index) {
      console.log("Preview buttons: " + JSON.stringify(buttons));
    };
    
  }
  return new EditorButtons();

})($);