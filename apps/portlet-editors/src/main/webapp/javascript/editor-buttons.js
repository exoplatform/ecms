/**
 * Editor buttons module.
 */
(function($, cCometD) {
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
    var currentUserId;
    var subscribedDocuments = {};
    
    // CometD transport bus
    var cometd, cometdContext;
    
    const DOCUMENT_OPENED = "DOCUMENT_OPENED";
    const DOCUMENT_CLOSED = "DOCUMENT_CLOSED";
    
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
        },
        success: function(result) {
          log("Prefered provider " + provider + " saved. " + result);
        },
        error : function(xhr,status,error) {
          log("Cannot save prefered provider " + provider + ": " + status + " " + error);
        }
      });
    };
    
    /**
     * Adds editor buttons container (button and pulldown)
     */
    var addEditorButtonsContainer = function($target, fileId, buttons) {
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
            tryAddEditorButtonToPreview(attempts - 1, delay, fileId, buttons);
          }, delay);
        } else {
          log("Cannot find element " + $elem);
        }
      } else {
        addEditorButtonsContainer($elem, fileId, buttons);
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
            tryAddEditorButtonNoPreview(attempts - 1, delay, fileId, buttons);
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
        addEditorButtonsContainer($target, fileId, buttons);
      }
    };
    
    var subscribeDocument = function(docId) {
      // Use only one channel for one document
      if (subscribedDocuments.docId) {
        return;
      }
      var subscription = cometd.subscribe("/eXo/Application/documents/" + docId, function(message) {
        // Channel message handler
        var result = tryParseJson(message);
        log("EVENT: " + message);
      }, cometdContext, function(subscribeReply) {
        // Subscription status callback
        if (subscribeReply.successful) {
          // The server successfully subscribed this client to the channel.
          log("Document updates subscribed successfully: " + JSON.stringify(subscribeReply));
          subscribedDocuments.docId = subscription;
        } else {
          var err = subscribeReply.error ? subscribeReply.error : (subscribeReply.failure ? subscribeReply.failure.reason
              : "Undefined");
          log("Document updates subscription failed for " + docId, err);
        }
      });
    };

    var unsubscribeDocument = function(docId) {
      var subscription = subscribedDocuments.docId;
      if (subscription) {
        cometd.unsubscribe(subscription, {}, function(unsubscribeReply) {
          if (unsubscribeReply.successful) {
            // The server successfully unsubscribed this client to the channel.
            log("Document updates unsubscribed successfully for: " + docId);
            delete subscribedDocuments.docId;
          } else {
            var err = unsubscribeReply.error ? unsubscribeReply.error
                : (unsubscribeReply.failure ? unsubscribeReply.failure.reason : "Undefined");
            log("Document updates unsubscription failed for " + docId, err);
          }
        });
      }
    };
    
    var publishDocument = function(docId, data) {
      var deferred = $.Deferred();
      cometd.publish("/eXo/Application/documents/" + docId, data, cometdContext, function(publishReply) {
        // Publication status callback
        if (publishReply.successful) {
          deferred.resolve();
          // The server successfully subscribed this client to the channel.
          log("Document update published successfully: " + JSON.stringify(publishReply));
        } else {
          deferred.reject();
          var err = publishReply.error ? publishReply.error : (publishReply.failure ? publishReply.failure.reason : "Undefined");
          log("Document updates publication failed for " + docId, err);
        }
      });
      return deferred;
    };
    
    this.init = function(userId, workspace, cometdConf) {
      currentWorkspace = workspace;
      if (userId == currentUserId) {
        log("Already initialized user: " + userId);
      } else if (userId) {
        currentUserId = userId;
        log("Initialize user: " + userId);
        if (cometdConf) {
          cCometD.configure({
            "url" : prefixUrl + cometdConf.path,
            "exoId" : userId,
            "exoToken" : cometdConf.token,
            "maxNetworkDelay" : 30000,
            "connectTimeout" : 60000
          });
          cometdContext = {
            "exoContainerName" : cometdConf.containerName
          };
          cometd = cCometD;
        }
      } else {
        log("Cannot initialize user: " + userId);
      }
    }
    
    /**
     * Inits editor buttons on DocumentUIActivity.
     * 
     */
    this.initActivityButtons = function(activityId, fileId, preferedProvider) {
      var buttons = buttonsFns.slice();
      if(buttons.length == 0) {
        return;
      }
      log("Init Activity buttons: " + JSON.stringify(buttons));
      // Sort buttons in user prefference order
      if(preferedProvider != null) {
        buttons.forEach(function(item,i){
          if(item.provider === preferedProvider){
            buttons.splice(i, 1);
            buttons.unshift(item);
          }
        });
      }
      var $target = $("#activityContainer" + activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left");
      addEditorButtonsContainer($target, fileId, buttons);
      subscribeDocument(fileId);
    };
    
    /**
     * Inits buttons on document preview.
     * 
     */
    this.initPreviewButtons = function(activityId, index, fileId, preferedProvider) {
      var buttons = buttonsFns.slice();
      if(buttons.length == 0) {
        return;
      }
      log("Init preview buttons: " + JSON.stringify(buttons));
      var clickSelector = "#Preview" + activityId + "-" + index;
      if(preferedProvider != null) {
        buttons.forEach(function(item,i){
          if(item.provider === preferedProvider){
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
        subscribeDocument(fileId);
      });
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
      publishDocument(fileId, {
        "type" : DOCUMENT_OPENED,
        "provider" : provider
      });
    }

    this.editorClosed = function(provider, fileId) {
      log("Editor closed. Provider: " + provider + ", fileId: " + fileId);
      publishDocument(fileId, {
        "type" : DOCUMENT_CLOSED,
        "provider" : provider
      });
    }
    
    /**
     * Clears buttonsFns
     * 
     */
    this.resetButtons = function() {
      buttonsFns = [];
    }
    
  }
  return new EditorButtons();

})($, cCometD);