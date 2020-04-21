/**
 * Editor buttons module.
 */
(function($, editorsupport) {
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
          msgLine += (typeof err === "string" ? err : JSON.stringify(err) +
            (err.toString && typeof err.toString === "function" ? "; " + err.toString() : ""));
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
    var providers;
    var explorerFileId;

    const DOCUMENT_OPENED = "DOCUMENT_OPENED";
    const DOCUMENT_CLOSED = "DOCUMENT_CLOSED";
    const LAST_EDITOR_CLOSED = "LAST_EDITOR_CLOSED";
    const DOCUMENT_PREVIEW_OPENED = "DOCUMENT_PREVIEW_OPENED";
    const CURRENT_PROVIDER_INFO = "CURRENT_PROVIDER_INFO";

    /**
     * Parses comet message from JSON
     */
    var tryParseJson = function(message) {
      var src = message.data ? message.data : (message.error ? message.error : message.failure);
      if (src) {
        try {
          if (typeof src === "string" && (src.startsWith("{") || src.startsWith("["))) {
            return JSON.parse(src);
          }
        } catch (e) {
          log("Error parsing '" + src + "' as JSON: " + e, e);
        }
      }
      return src;
    };

    /**
     * Saves prefered provider.
     * 
     */
    var savePrefferedProvider = function(fileId, provider) {
      $.post({
        async: true,
        type: "POST",
        url: prefixUrl + "/portal/rest/documents/editors/prefered/" + fileId,
        data: {
          userId: eXo.env.portal.userName,
          provider: provider,
          workspace: currentWorkspace
        }
      }).then(function(result) {
        log("Prefered provider " + provider + " saved. " + result);
      }).catch(function(xhr, status, error) {
        log("Cannot save prefered provider " + provider + ": " + status + " " + error);
      });
    };

    /**
     * Inits providers preview
     * 
     */
    var initProvidersPreview = function(fileId, workspace) {
      return $.post({
        async: true,
        type: "POST",
        url: prefixUrl + "/portal/rest/documents/editors/preview",
        data: {
          fileId: fileId,
          workspace: workspace
        }
      });
    };

    /**
     * Adds editor buttons container (button and pulldown)
     */
    var getButtonsContainer = function(fileId, buttons, preferedProvider, dropclass) {
      if (!buttons) {
        return;
      }
      // Sort buttons in user prefference order
      if (preferedProvider != null) {
        buttons.forEach(function(item, i) {
          if (item.provider === preferedProvider) {
            buttons.splice(i, 1);
            buttons.unshift(item);
          }
        });
      }
      // Add buttons container
      // var $container = $target.find(".editorButtonContainer");
      var $container = $("<div class='editorButtonContainer hidden-tabletL'></div>");

      // Create editor button
      var $btn = buttons[0].createButtonFn();
      $btn.addClass("editorButton");
      $btn.attr('data-provider', buttons[0].provider);
      $btn.attr('data-fileId', fileId);
      $container.append($btn);
      let provider = buttons[0].provider;
      $btn.click(function() {
        log("prefered provider: " + provider);
        savePrefferedProvider(fileId, provider);
      });

      // Create pulldown with editor buttons
      if (buttons.length > 1) {
        var $dropdownContainer = $("<div class='dropdown-container'></div>");
        var $toggle = $("<button class='btn dropdown-toggle' data-toggle='dropdown'>" +
          "<i class='uiIconArrowDown uiIconLightGray'></i></span></button>");

        var $dropdown = $("<ul class='dropdown-menu'></ul>");

        for (var i = 1; i < buttons.length; i++) {
          var $btn = buttons[i].createButtonFn();
          let provider = buttons[i].provider;
          // Save user choice
          $btn.click(function() {
            savePrefferedProvider(fileId, provider);
          });
          $btn.addClass("editorButton");
          $btn.attr('data-provider', buttons[i].provider);
          $btn.attr('data-fileId', fileId);
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

    var loadProviderModule = function(provider) {
      var loader = $.Deferred();
      var moduleId = "SHARED/" + provider;
      if (window.require.s.contexts._.config.paths[moduleId]) {
        try {
          window.require([moduleId], function(client) {
            loader.resolve(client);
          }, function(err) {
            log("Cannot require provider module " + provider, err);
            loader.reject();
          });
        } catch (e) {
          log("Cannot load provider module " + provider, e);
          loader.reject();
        }
      } else {
        loader.reject();
      }
      return loader;
    };

    var disableECMSButtons = function(currentProvider) {
      if (providers) {
        var allProviders = providers.slice();
        var index = allProviders.indexOf(currentProvider);
        if (index !== -1) allProviders.splice(index, 1);
        allProviders.forEach(provider => {
          $("#UIActionBar i[class*='uiIconEcms" + provider + "Open' i]").each(function() {
            $(this).parents(':eq(1)').addClass("disabledProvider");
          });
        });
      }
    };

    var enableECMSButtons = function(currentProvider) {
      if (providers) {
        var allProviders = providers.slice();
        var index = allProviders.indexOf(currentProvider);
        if (index !== -1) allProviders.splice(index, 1);
        allProviders.forEach(provider => {
          $("#UIActionBar i[class*='uiIconEcms" + provider + "Open' i]").each(function() {
            $(this).parents(':eq(1)').removeClass("disabledProvider");
          });
        });
      }
    };


    var eventsHandler = function(result) {
      log("EVENT HANDLED: " + JSON.stringify(result));
      switch (result.type) {
        case DOCUMENT_OPENED: {
          $('.editorButton[data-provider!="' + result.provider + '"][data-fileId="' + result.fileId + '"]').each(function() {
            $(this).addClass("disabledProvider");
          });
          if (explorerFileId) {
            disableECMSButtons(result.provider);
          }
        }
        break;
      case LAST_EDITOR_CLOSED: {
        $('.editorButton[data-provider!="' + result.provider + '"][data-fileId="' + result.fileId + '"]').each(function() {
          $(this).removeClass("disabledProvider");
        });
        if (explorerFileId) {
          enableECMSButtons(result.provider);
        }
      }
      break;
      case CURRENT_PROVIDER_INFO: {
        log("Current provider info: " + result.provider + " fileId: " + result.fileId);
        setTimeout(function() {
          if (result.provider && result.provider != "null") {
            $('.editorButton[data-provider!="' + result.provider + '"][data-fileId="' + result.fileId + '"]').each(function() {
              $(this).addClass("disabledProvider");
            });
          }
        }, 100);
        break;
      }
      }
    }

    this.initExplorer = function(fileId, workspace, allProviders, currentProvider) {
      currentWorkspace = workspace;
      providers = allProviders;
      // Web UI buttons
      if (currentProvider) {
        disableECMSButtons(currentProvider);
      }
      if (fileId != explorerFileId) {
        // We need unsubscribe from previous doc
        if (explorerFileId) {
          editorsupport.removeListener("editorbuttons", explorerFileId).done(function() {
            editorsupport.addListener("editorbuttons", fileId, eventsHandler);
          });
        } else {
          editorsupport.addListener("editorbuttons", fileId, eventsHandler);
        }
        explorerFileId = fileId;
      }
    };

    /**
     * Inits editor buttons on DocumentUIActivity.
     * 
     */
    this.initActivityButtons = function(config) {
      currentWorkspace = config.workspace;
      var buttons = buttonsFns.slice();
      if (buttons.length == 0) {
        return;
      }
      log("Init Activity buttons: " + JSON.stringify(buttons));
      var $target = $("#activityContainer" + config.activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left");
      console.log(JSON.stringify(config));
      $target.append(getButtonsContainer(config.fileId, buttons, config.prefferedProvider, 'dropdown'));

      // Disable editor buttons if the document is currently editing in one of
      // editors.
      if (config.currentProvider != null) {
        $('.editorButton[data-provider!="' + config.currentProvider + '"][data-fileId="' + config.fileId + '"]').each(function() {
          $(this).addClass("disabledProvider");
        });
      }
      editorsupport.addListener("editorbuttons", config.fileId, eventsHandler);
    };

    /**
     * Inits buttons on document preview.
     * 
     */
    this.initPreviewButtons = function(fileId, workspace, dropclass) {
      currentWorkspace = workspace;
      buttonsFns = [];
      var buttonsLoader = $.Deferred();
      initProvidersPreview(fileId, workspace).then(function(data) {
        var providersLoader = $.Deferred();
        var preferedProvider;
        data.forEach(function(providerInfo, i, arr) {
          loadProviderModule(providerInfo.provider).done(function(module) {
            module.initPreview(providerInfo.settings);
            if (providerInfo.prefered) {
              preferedProvider = providerInfo.provider;
            }
            // Last provider loaded
            if (i == (arr.length - 1)) {
              providersLoader.resolve();
            }
          });
        });
        providersLoader.done(function() {
          var $pulldown = getButtonsContainer(fileId, buttonsFns, preferedProvider, dropclass);
          buttonsLoader.resolve($pulldown);
        });
      }).catch(function(xhr, status, error) {
        log("Cannot init providers preview for file" + fileId + ": " + status + " " + error);
      });

      editorsupport.addListener("editorbuttons", fileId, eventsHandler);
      editorsupport.refreshStatus(fileId, workspace);
      return buttonsLoader;
    };

    /**
     * API for providers to add their editor buttons.
     * 
     */
    this.addCreateButtonFn = function(provider, createButtonFn) {
      var buttonFn = {
        "provider": provider,
        "createButtonFn": createButtonFn
      };
      var index = buttonsFns.findIndex(elem => elem.provider === provider);
      if (index === -1) {
        buttonsFns.push(buttonFn);
      } else {
        buttonsFns[index] = buttonFn;
      }
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

})($, editorsupport);