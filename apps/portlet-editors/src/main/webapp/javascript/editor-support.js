/**
 * Editor support module.
 */
(function($, cCometD) {
  "use strict";

  /** For debug logging. */
  var log = function(msg, err) {
    var logPrefix = "[editorsupport] ";
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
  function EditorSupport() {

    var prefixUrl = pageBaseUrl(location);
    var listeners = {};
    // CometD transport bus
    var cometd, cometdContext;
    var cometdConf;
    var userId;
    var configLoader = $.Deferred();
    var initLoader;

    const DOCUMENT_OPENED = "DOCUMENT_OPENED";
    const DOCUMENT_CLOSED = "DOCUMENT_CLOSED";
    const LAST_EDITOR_CLOSED = "LAST_EDITOR_CLOSED";
    const REFRESH_STATUS = "REFRESH_STATUS";
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
     * Subscribes the document and passes events to the callback.
     */
    var subscribeDocument = function(fileId, callback) {
      if (!initLoader) {
        init();
      }
      var subscriptionPromise = $.Deferred();
      initLoader.done(function() {
        var subscription = cometd.subscribe("/eXo/Application/documents/" + fileId, function(message) {
          // Channel message handler
          var result = tryParseJson(message);
          callback(result);
        }, cometdContext, function(subscribeReply) {
          // Subscription status callback
          if (subscribeReply.successful) {
            // The server successfully subscribed this client to the channel.
            log("Document updates subscribed successfully: " + JSON.stringify(subscribeReply));
            subscriptionPromise.resolve(subscription);
          } else {
            var err = subscribeReply.error ? subscribeReply.error : (subscribeReply.failure ? subscribeReply.failure.reason :
              "Undefined");
            log("Document updates subscription failed for " + fileId, err);
          }
        });
      });
      return subscriptionPromise;
    };

    /**
     * Publish event to the cometd channel.
     */
    var publishEvent = function(fileId, data) {
      var deferred = $.Deferred();
      cometd.publish("/eXo/Application/documents/" + fileId, data, cometdContext, function(publishReply) {
        // Publication status callback
        if (publishReply.successful) {
          deferred.resolve();
          // The server successfully subscribed this client to the channel.
          log("Event published successfully: " + JSON.stringify(publishReply));
        } else {
          deferred.reject();
          var err = publishReply.error ? publishReply.error : (publishReply.failure ? publishReply.failure.reason : "Undefined");
          log("Event publication failed for " + fileId, err);
        }
      });
      return deferred;
    };

    /**
     * Gets listener by caller and fileId.
     */
    var getListener = function(caller, fileId) {
      if (!(caller in listeners)) {
        return null;
      }
      for (var i = 0; i < listeners[caller].length; i++) {
        if (listeners[caller][i].fileId === fileId) {
          return listeners[caller][i];
        }
      }
      return null;
    };

    /**
     * Inits cometd
     */
    var init = function() {
      if (initLoader) {
        log("Init is in progress or already done");
        return;
      }
      log("Initializing editor support module");
      initLoader = $.Deferred();
      configLoader.done(function() {
        cCometD.configure({
          "url": prefixUrl + cometdConf.path,
          "exoId": userId,
          "exoToken": cometdConf.token,
          "maxNetworkDelay": 30000,
          "connectTimeout": 60000
        });
        cometdContext = {
          "exoContainerName": cometdConf.containerName,
          "provider": cometdConf.provider,
          "workspace": cometdConf.workspace
        };
        cometd = cCometD;
        initLoader.resolve();
      });
    };

    this.init = init;

    /**
     * Inits configuration
     */
    this.initConfig = function(user, conf) {
      userId = user;
      cometdConf = conf;
      configLoader.resolve();
    };

    /**
     * Removes listener
     */
    this.removeListener = function(caller, fileId) {
      var removeLoader = $.Deferred();
      log("Remove listener handled");
      var listener = getListener(caller, fileId)
      if (!listener) {
        log("Listener isn't registered for " + caller + " and fileId: " + fileId);
        return;
      }
      for (var i = 0; i < listeners[caller].length; i++) {
        if (listeners[caller][i].fileId === fileId) {
          listeners[caller].splice(i, 1);
        }
      }
      if (listener.subscription) {
        cometd.unsubscribe(listener.subscription, {}, function(unsubscribeReply) {
          if (unsubscribeReply.successful) {
            // The server successfully unsubscribed this client to the channel.
            log("Document updates unsubscribed successfully for: " + fileId);
            removeLoader.resolve();
          } else {
            var err = unsubscribeReply.error ? unsubscribeReply.error :
              (unsubscribeReply.failure ? unsubscribeReply.failure.reason : "Undefined");
            log("Document updates unsubscription failed for " + fileId, err);
          }
        });
      } else {
        removeLoader.resolve();
      }
      return removeLoader;
    };

    /**
     * Adds listener
     */
    this.addListener = function(caller, fileId, callback) {
      if (getListener(caller, fileId)) {
        log("Listener already registered for " + caller + " and fileId: " + fileId);
        return;
      }
      var subscriptionLoader = subscribeDocument(fileId, callback);
      // Save listener before subscription inited
      var listener = {
        fileId: fileId
      };
      if (listeners[caller]) {
        listeners[caller].push(listener);
      } else {
        listeners[caller] = [listener];
      }

      // Set subscription for listener
      subscriptionLoader.done(function(subscription) {
        listener.subscription = subscription
      });
    };

    /**
     * Opens cometd connection and sends DOCUMENT_OPENED event to track the editor.
     * Used as an API for providers.
     */
    this.onEditorOpen = function(fileId, workspace, provider) {
      log("Editor opened. Provider: " + provider + ", fileId: " + fileId);
      // subsribe to track opened editors on server-side
      if (!cometd) {
        init();
      }
      var subscription = cometd.subscribe("/eXo/Application/documents/" + fileId, function(message) {}, cometdContext, function(subscribeReply) {});
      publishEvent(fileId, {
        "type": DOCUMENT_OPENED,
        "provider": provider,
        "fileId": fileId,
        "workspace": workspace
      });
    };
  }
  return new EditorSupport();

})($, cCometD);