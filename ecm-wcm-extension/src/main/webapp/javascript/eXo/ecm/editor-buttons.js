/**
 * Editor buttons module.
 */
(function($) {
  "use strict";

  /** For debug logging. */
  var log = function(msg, err) {
    var logPrefix = "[editorbuttons] ";
    if (typeof console != "undefined" && typeof console.log != "undefined") {
      var isoTime = " -- " + new Date().toISOString();
      var msgLine = msg;
      if (err) {
        msgLine += ". Error: ";
        if (err.name || err.message) {
          if (err.name) {
            msgLine += "[" + err.name + "] ";
          }
          if (err.message) {
            msgLine += err.message;
          }
        } else {
          msgLine += (typeof err === "string" ? err : JSON.stringify(err)
              + (err.toString && typeof err.toString === "function" ? "; " + err.toString() : ""));
        }

        console.log(logPrefix + msgLine + isoTime);
        if (typeof err.stack != "undefined") {
          console.log(err.stack);
        }
      } else {
        if (err !== null && typeof err !== "undefined") {
          msgLine += ". Error: '" + err + "'";
        }
        console.log(logPrefix + msgLine + isoTime);
      }
    }
  };
  
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
          log("Provider " + provider + " saved. " + result);
        },
        error : function(xhr,status,error) {
          log("Provider " + provider + " not saved. " + status + " " + error);
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
      var buttons = buttonsFns.slice();
      if(buttons.length == 0) {
        return;
      }
      currentWorkspace = workspace;
      log("Init Activity buttons: " + JSON.stringify(buttons));
      // Sort buttons in user prefference order
      if(prefferedProvider != null) {
        buttons.forEach(function(item,i){
          if(item.provider === prefferedProvider){
            buttons.splice(i, 1);
            buttons.unshift(item);
          }
        });
      }
      var $target = $("#activityContainer" + activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left");
      addEditorButtons($target, fileId, buttons);
    };
    
    var addEditorButtons = function($target, fileId, buttons) {
      if(!buttons) {
        return;
      }
      // Add buttons container
      var $container = $target.find(".editorButtonContainer");
      if ($container.length == 0) {
        $container = $("<div class='editorButtonContainer hidden-tabletL'></div>");
        $target.append($container);
      }
      
      // Create editor button
      var $btn = buttons[0].createButtonFn();
      $container.append($btn);
      let provider = buttons[0].provider;
      $btn.click(function() {
        log("prefered provider: " + provider);
        savePreferedProvider(fileId, provider);
      });
      
      // Create pulldown with editor buttons
      if(buttons.length > 1) {
        var $dropdownContainer = $("<div class='dropdown-container'></div>");
        var $toggle = $("<button class='btn dropdown-toggle' data-toggle='dropdown'>" +
        "<i class='uiIconArrowDown uiIconLightGray'></i></span></button>");
        
        var $dropdown = $("<ul class='dropdown-menu'></ul>");
        
        for(var i = 1; i < buttons.length; i++) {
          var $btn = buttons[i].createButtonFn();
          let provider = buttons[i].provider;
          // Save user choice
          $btn.click(function() {
            log("prefered provider: " + provider);
            savePreferedProvider(fileId, provider);
          });
          $dropdown.append($btn);
        }
        $dropdownContainer.append($toggle);
        $dropdownContainer.append($dropdown);
        $container.append($dropdownContainer);
      }
    };
    
    /**
     * Adds the 'Edit Online' button to a preview (from the activity stream) when it's loaded.
     */
    var tryAddEditorButtonToPreview = function(attempts, delay, fileId, buttons) {
      var $elem = $("#uiDocumentPreview .previewBtn");
      if ($elem.length == 0 || !$elem.is(":visible")) {
        if (attempts > 0) {
          setTimeout(function() {
            tryAddEditorButtonToPreview(attempts - 1, delay, fileId);
          }, delay);
        } else {
          log("Cannot find element " + $elem);
        }
      } else {
        addEditorButtons($elem, fileId, buttons);
        $(".previewBtn .editorButtonContainer").addClass("dropup");
      }
    };
    
    /**
     * Adds the 'Edit Online' button to No-preview screen (from the activity stream) when it's loaded.
     */
    var tryAddEditorButtonNoPreview = function(attempts, delay, fileId, buttons) {
      var $elem = $("#documentPreviewContainer .navigationContainer.noPreview");
      if ($elem.length == 0 || !$elem.is(":visible")) {
        if (attempts > 0) {
          setTimeout(function() {
            tryAddEditorButtonNoPreview(attempts - 1, delay, fileId);
          }, delay);
        } else {
          log("Cannot find .noPreview element");
        }
      } else if ($elem.find("div.editorButtonContainer").length == 0) {
        var $detailContainer = $elem.find(".detailContainer");
        var $downloadBtn = $detailContainer.find(".uiIconDownload").closest("a.btn");
        var $target = $("<div style='display: inline;'></div>");
        if ($downloadBtn.length != 0) {
          $downloadBtn.after($target);
        } else {
          $detailContainer.append($target);
        }
        addEditorButtons($target, fileId, buttons);
      }
    };
    
    this.initPreviewButtons = function(activityId, index, fileId, prefferedProvider) {
      var buttons = buttonsFns.slice();
      log("Preview buttons: " + JSON.stringify(buttons));
      var clickSelector = "#Preview" + activityId + "-" + index;
      if(prefferedProvider != null) {
        buttons.forEach(function(item,i){
          if(item.provider === prefferedProvider){
            buttons.splice(i, 1);
            buttons.unshift(item);
          }
        });
      }
      $(clickSelector).click(function() {
        // We set timeout here to avoid the case when the element is rendered but is going to be updated soon
        setTimeout(function() {
          tryAddEditorButtonToPreview(100, 100, fileId, buttons);
          // We need wait for about 2min when doc cannot generate its preview
          tryAddEditorButtonNoPreview(600, 250, fileId, buttons);
        }, 100);
      });
    };
    
    
    
    this.addCreateButtonFn = function(provider, createButtonFn) {
      log("Add create button fn: " + provider);
      var buttonFn = { "provider" : provider, "createButtonFn" : createButtonFn };
      var index = buttonsFns.findIndex(elem => elem.provider === provider);
      if (index === -1) {
        buttonsFns.push(buttonFn);
      } else {
        buttonsFns[index] = buttonFn;
      }
    };
    
    this.resetButtons = function() {
      log("Reset buttons");
      buttonsFns = [];
    }
    
  }
  return new EditorButtons();

})($);