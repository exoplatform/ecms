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
  
  
  
  /**
   * Editor core class.
   */
  function EditorButtons() {

    // Functions to create editor buttons
    var buttonsFns = []; 
    var prefixUrl = pageBaseUrl(location);
    var currentWorkspace;
    
    /**
     * Saves prefered provider.
     * 
     */
    var savePreferedProvider = function(fileId, provider){
      $.post({
        async : true,
        type : "POST",
        url : prefixUrl + "/portal/rest/documents/editors/prefered/" + fileId,
        data : {
          userId : eXo.env.portal.userName,
          provider : provider,
          workspace : currentWorkspace
        }
      }).then(function(result) {
        log("Prefered provider " + provider + " saved. " + result);
      }).catch(function(xhr,status,error) {
        log("Cannot save prefered provider " + provider + ": " + status + " " + error);
      });
    };
    
    /**
     * Saves prefered provider.
     * 
     */
    var initProviders = function(fileId, workspace){
      return $.post({
        async : true,
        type : "POST",
        url : prefixUrl + "/portal/rest/documents/editors/preview",
        data : {
          fileId : fileId,
          workspace : workspace
        }
      }).catch(function(xhr,status,error) {
        log("Cannot init providers for file" + fileId + ": " + status + " " + error);
      });
    };
    
    /**
     * Adds editor buttons container (button and pulldown)
     */
    var getButtonsContaner = function(fileId, buttons, dropclass) {
      if (!buttons) {
        return;
      }
      // Add buttons container
      // var $container = $target.find(".editorButtonContainer");
      var $container = $("<div class='editorButtonContainer hidden-tabletL'></div>");
      
      // Create editor button
      var $btn = buttons[0].createButtonFn();
      $container.append($btn);
      let provider = buttons[0].provider;
      $btn.click(function() {
        log("prefered provider: " + provider);
        savePreferedProvider(fileId, provider);
      });
      
      // Create pulldown with editor buttons
      if (buttons.length > 1) {
        var $dropdownContainer = $("<div class='dropdown-container'></div>");
        var $toggle = $("<button class='btn dropdown-toggle' data-toggle='dropdown'>" +
        "<i class='uiIconArrowDown uiIconLightGray'></i></span></button>");
        
        var $dropdown = $("<ul class='dropdown-menu'></ul>");
        
        for(var i = 1; i < buttons.length; i++) {
          var $btn = buttons[i].createButtonFn();
          let provider = buttons[i].provider;
          // Save user choice
          $btn.click(function() {
            savePreferedProvider(fileId, provider);
          });
          $dropdown.append($btn);
        }
        $dropdownContainer.append($toggle);
        $dropdownContainer.append($dropdown);
        $container.append($dropdownContainer);
      }
      if (dropclass) {
        $container.addClass(dropclass);
      }
      return $container;
    };

    var loadProvidersModule = function(provider) {
      var loader = $.Deferred();
      // try load provider client
      var moduleId = "SHARED/" + provider;
      if (window.require.s.contexts._.config.paths[moduleId]) {
        try {
          // load client module and work with it asynchronously
          window.require([moduleId], function(client) {
            // FYI client module's initialization (if provided) will be invoked
            // in initContext()
            loader.resolve(client);
          }, function(err) {
            log("ERROR: Cannot load provider module " + provider + " Error:" + err.message + ": " + JSON.stringify(err), err);
            loader.reject();
          });
        } catch(e) {
          // cannot load the module - default behaviour
          utils.log("ERROR: " + e, e);
          loader.reject();
        }
      } else {
        loader.reject();
      }
      return loader.promise();
    };
    
    
    /**
     * Inits editor buttons on DocumentUIActivity.
     * 
     */
    this.initActivityButtons = function(activityId, fileId, workspace, preferedProvider) {
      var buttons = buttonsFns.slice();
      if (buttons.length == 0) {
        return;
      }
      currentWorkspace = workspace;
      log("Init Activity buttons: " + JSON.stringify(buttons));
      // Sort buttons in user prefference order
      if (preferedProvider != null) {
        buttons.forEach(function(item,i){
          if (item.provider === preferedProvider){
            buttons.splice(i, 1);
            buttons.unshift(item);
          }
        });
      }
      var $target = $("#activityContainer" + activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left");
      $target.append(getButtonsContaner(fileId, buttons, 'dropdown'));
    };
    
    /**
     * Inits buttons on document preview.
     * 
     */
    this.initPreviewButtons = function(fileId, workspace) {
      buttonsFns = [];
      var buttonsLoader = $.Deferred();
      initProviders(fileId, workspace).done(function(data) {
        console.log("PROVIDERS INITED: " + JSON.stringify(data));
        var providersLoader = $.Deferred();
        data.forEach(function(elem, i, arr) {
          console.log("init provider:" + elem.provider);
          loadProvidersModule(elem.provider).done(function(module){
            module.initPreview(elem.settings);
            // Last provider loaded
            if (i == (arr.length - 1)) {
              providersLoader.resolve();
            }
          });
        });
        providersLoader.done(function(){
          var $pulldown =  getButtonsContaner(fileId, buttonsFns, 'dropup');
          buttonsLoader.resolve($pulldown);
        });
      });
      return buttonsLoader;
    };
    
    /**
     * API for providers to add their editor buttons.
     * 
     */
    this.addCreateButtonFn = function(provider, createButtonFn) {
      var buttonFn = { "provider" : provider, "createButtonFn" : createButtonFn };
      var index = buttonsFns.findIndex(elem => elem.provider === provider);
      if (index === -1) {
        buttonsFns.push(buttonFn);
      } else {
        buttonsFns[index] = buttonFn;
      }
    };
    
    this.editorOpened = function(provider, fileId) {
      log("Editor opened. Provider: " + provider + ", fileId: " + fileId);
    };
    
    this.editorClosed = function(provider, fileId) {
      log("Editor closed. Provider: " + provider + ", fileId: " + fileId);
    };
    
    /**
     * Clears buttonsFns
     * 
     */
    this.resetButtons = function() {
      buttonsFns = [];
    };
  }
  return new EditorButtons();

})($);