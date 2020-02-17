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
    
    var pageBaseUrl = function(theLocation) {
      if (!theLocation) {
        theLocation = window.location;
      }

      var theHostName = theLocation.hostname;
      var theQueryString = theLocation.search;

      if (theLocation.port) {
        theHostName += ":" + theLocation.port;
      }

      return theLocation.protocol + "//" + theHostName;
    };
    
    var prefixUrl = pageBaseUrl(location);
    var currentWorkspace;
    
    var savePreferedProvider = function(fileId, provider){
      $.ajax({
        async : true,
        type : "POST",
        contentType: "application/json",
        url : prefixUrl + "/portal/rest/documents/editors/preffered/" + fileId,
        data : JSON.stringify({
          userId : eXo.env.portal.userName,
          provider : provider,
          workspace : currentWorkspace
        }),
        success: function(result) {
          console.log("Provider " + provider + " saved. " + result);
        },
        error : function(xhr,status,error) {
          console.log("Provider " + provider + " not saved. " + status + " " + error);
        }
      });
    };

    
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

    this.initActivityButtons = function(activityId, fileId, workspace, prefferedProvider) {
      console.log("PREFFERED PROVIDER FOR " + fileId + " is " + prefferedProvider);
      if(buttonsFns.length == 0) {
        return;
      }
      currentWorkspace = workspace;
      console.log("Activity buttons: " + JSON.stringify(buttonsFns));
      var $target = $("#activityContainer" + activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left");
      // Add buttons container
      var $container = $target.find(".editorButtonContainer");
      if ($container.length == 0) {
        $container = $("<div class='editorButtonContainer hidden-tabletL'></div>");
        $target.append($container);
      }
      
      // Sort buttons in user prefference order
      if(prefferedProvider != null) {
        buttonsFns.forEach(function(item,i){
          if(item.provider === prefferedProvider){
            buttonsFns.splice(i, 1);
            buttonsFns.unshift(item);
          }
        });
      }
      
      // Create editor button
      var $btn = buttonsFns[0].createButtonFn();
      $container.append($btn);
      let provider = buttonsFns[0].provider;
      $btn.click(function() {
        console.log("prefered provider: " + provider);
      });
      
      // Create pulldown with editor buttons
      if(buttonsFns.length > 1) {
        var $dropdownContainer = $("<div class='dropdown-container'></div>");
        var $toggle = $("<button class='btn dropdown-toggle' data-toggle='dropdown'>" +
        "<i class='uiIconArrowDown uiIconLightGray'></i></span></button>");
        
        var $dropdown = $("<ul class='dropdown-menu'></ul>");
        
        for(var i = 1; i < buttonsFns.length; i++) {
          var $btn = buttonsFns[i].createButtonFn();
          let provider = buttonsFns[i].provider;
          // Save user choice
          $btn.click(function() {
            console.log("prefered provider: " + provider);
            savePreferedProvider(fileId, provider);
          });
          $dropdown.append($btn);
        }
        $dropdownContainer.append($toggle);
        $dropdownContainer.append($dropdown);
        $container.append($dropdownContainer);
      }
    };
    
    this.initPreviewButtons = function(activityId, index, prefferedProvider) {
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