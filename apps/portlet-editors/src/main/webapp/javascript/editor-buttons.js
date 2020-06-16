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
    var explorerFileId;

    const DOCUMENT_OPENED = "DOCUMENT_OPENED";
    const DOCUMENT_CLOSED = "DOCUMENT_CLOSED";
    const LAST_EDITOR_CLOSED = "LAST_EDITOR_CLOSED";
    const DOCUMENT_PREVIEW_OPENED = "DOCUMENT_PREVIEW_OPENED";
    const CURRENT_PROVIDER_INFO = "CURRENT_PROVIDER_INFO";
    // Current module name
    const EDITOR_BUTTONS = "editorbuttons";

    const lang = (eXo && eXo.env && eXo.env.portal && eXo.env.portal.language) || "en";
    const localePortlet = "locale.portlet";
    const resourceBundleName = "EditorsAdmin";
    const i18nUrl = eXo.env.portal.context + "/" + eXo.env.portal.rest + "/i18n/bundle/" + localePortlet + "." + resourceBundleName + "-" + lang + ".json";

    /**
     * Gets i18n
     */
    var geti18n = function() {
      return $.get(i18nUrl);
    };

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
     * Saves preferred provider.
     * 
     */
    var savePreferredProvider = function(fileId, provider) {
      $.post({
        async: true,
        type: "POST",
        url: prefixUrl + "/portal/rest/documents/editors/preferred/" + fileId,
        data: {
          userId: eXo.env.portal.userName,
          provider: provider,
          workspace: currentWorkspace
        }
      }).catch(function(xhr, status, error) {
        log("Cannot save preferred provider " + provider + ": " + status + " " + error);
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
     * Creates the dropdown with editor buttons
     */
    var getButtonsContainer = function(fileId, buttons, preferredProvider, currentProvider, dropclass) {
      var $containerLoader = $.Deferred();

      if (buttons.length) {
        // Sort buttons in user prefference order
        if (preferredProvider != null) {
          buttons.forEach(function(item, i) {
            if (item.provider === preferredProvider) {
              buttons.splice(i, 1);
              buttons.unshift(item);
            }
          });
        }
        geti18n().done(function(i18n) {
          // Add buttons container
          var $container = $("<div class='editorButtonContainer hidden-tabletL'></div>");
          if (buttons.length == 1) {
            // Create editor button
            var $btn = buttons[0].createButtonFn();
            $btn.addClass("editorButton");
            $btn.attr('data-provider', buttons[0].provider);
            $btn.attr('data-fileId', fileId);
            // If there is current open editor and it's not this one
            if (currentProvider && currentProvider != buttons[0].provider) {
              $btn.addClass("disabledProvider");
            }
            $btn.find(".editorLabel").html(i18n["editors.buttons.EditorButton"]);
            
            $container.append($btn);
            let provider = buttons[0].provider;
            $btn.click(function() {
              savePreferredProvider(fileId, provider);
            });
          } else {

            // Create pulldown with editor buttons
            var $dropdownContainer = $("<div class='dropdown-container'></div>");


            var $dropdown = $("<ul class='dropdown-menu'></ul>");

            for (var i = 0; i < buttons.length; i++) {
              var $btn = buttons[i].createButtonFn();
              let provider = buttons[i].provider;
              // Save user choice
              $btn.click(function() {
                savePreferredProvider(fileId, provider);
              });
              $btn.addClass("editorButton");
              $btn.attr('data-provider', buttons[i].provider);
              $btn.attr('data-fileId', fileId);
              // If there is current open editor and it's not this one
              if (currentProvider && currentProvider != buttons[i].provider) {
                $btn.addClass("disabledProvider");
              }
              $dropdown.append($btn);
            }
            var $toggle = $("<button class='btn dropdown-toggle' data-toggle='dropdown'><i class='uiIconEcmsOfficeOnlineOpen uiIconEcmsLightGray uiIconEdit'></i><span>" + i18n["editors.buttons.EditorButton"] + "</span>" +
              "<i class='uiIconArrowDown uiIconLightGray'></i></button>");
            $dropdownContainer.append($toggle);
            $dropdownContainer.append($dropdown);

            if (dropclass) {
              $container.addClass(dropclass);
            }
            $container.append($dropdownContainer);
          }
          $containerLoader.resolve($container);
        });
      } else {
        $containerLoader.reject();
      }
      return $containerLoader.promise();
    };

    /**
     * Loads providers JS module
     */
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

    /**
     * Handles events from editorsupport.js and enables/disables editor buttons.
     */
    var eventsHandler = function(result) {
      switch (result.type) {
        case DOCUMENT_OPENED: {
          $('.editorButton[data-provider!="' + result.provider + '"][data-fileId="' + result.fileId + '"]').each(function() {
            $(this).addClass("disabledProvider");
          });
        }
        break;
      case LAST_EDITOR_CLOSED: {
        $('.editorButton[data-provider!="' + result.provider + '"][data-fileId="' + result.fileId + '"]').each(function() {
          $(this).removeClass("disabledProvider");
        });
      }
      break;
      }
    }

    /**
     * Inits the ECMS Explorer
     */
    this.initExplorer = function(fileId, workspace, providersInfo) {
      var $placeholder = $(".uiIconEcmsEditorsOpen").parents(':eq(1)');
      $placeholder.css("display", "none");
      currentWorkspace = workspace;
      // reset buttons
      buttonsFns = [];
      var providersLoader = $.Deferred();
      var preferredProvider;
      var currentProvider;
      providersInfo.forEach(function(providerInfo, i, arr) {
        loadProviderModule(providerInfo.provider).done(function(module) {
          // The provider's module will call addCreateButtonFn() and 
          // add the button-function to buttonsFns array
          module.initExplorer(providerInfo.settings);
          if (providerInfo.preferred) {
            preferredProvider = providerInfo.provider;
          }
          if (providerInfo.current) {
            currentProvider = providerInfo.provider;
          }
          // Last provider loaded
          if (i == (arr.length - 1)) {
            providersLoader.resolve();
          }
        });
      });
      providersLoader.done(function() {
        if (buttonsFns.length) {
          getButtonsContainer(fileId, buttonsFns, preferredProvider, currentProvider, 'dropdown').done(function($pulldown) {
            $placeholder.replaceWith($pulldown);
            if (fileId != explorerFileId) {
              // We need unsubscribe from previous doc
              if (explorerFileId) {
                editorsupport.removeListener(EDITOR_BUTTONS, explorerFileId).done(function() {
                  editorsupport.addListener(EDITOR_BUTTONS, fileId, eventsHandler);
                });
              } else {
                editorsupport.addListener(EDITOR_BUTTONS, fileId, eventsHandler);
              }
              explorerFileId = fileId;
            }
          });
        }
      });
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
      var $target = $("#activityContainer" + config.activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left");
      getButtonsContainer(config.fileId, buttons, config.preferredProvider, config.currentProvider, 'dropdown').done(function($pulldown) {
        $target.append($pulldown);
      });
      editorsupport.addListener(EDITOR_BUTTONS, config.fileId, eventsHandler);
    };

    /**
     * Inits buttons on document preview.
     * 
     */
    this.initPreviewButtons = function(fileId, workspace, dropclass) {
      currentWorkspace = workspace;
      // reset buttons
      buttonsFns = [];
      var buttonsLoader = $.Deferred();
      initProvidersPreview(fileId, workspace).then(function(data) {
        var providersLoader = $.Deferred();
        var preferredProvider;
        var currentProvider;
        data.providersInfo.forEach(function(providerInfo, i, arr) {
          loadProviderModule(providerInfo.provider).done(function(module) {
            // The provider's module will call addCreateButtonFn() and 
            // add the button-function to buttonsFns array
            module.initPreview(providerInfo.settings);
            if (providerInfo.preferred) {
              preferredProvider = providerInfo.provider;
            }
            if (providerInfo.current) {
              currentProvider = providerInfo.provider;
            }
            // Last provider loaded
            if (i == (arr.length - 1)) {
              providersLoader.resolve();
            }
          });
        });
        providersLoader.done(function() {
          if (buttonsFns.length) {
            getButtonsContainer(fileId, buttonsFns, preferredProvider, currentProvider, dropclass).done(function($pulldown) {
              buttonsLoader.resolve($pulldown);
              editorsupport.addListener(EDITOR_BUTTONS, data.fileId, eventsHandler);
            });
          }
        });
      }).catch(function(xhr, status, error) {
        log("Cannot init providers preview for file" + fileId + ": " + status + " " + error);
      });
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