/**
 * Editor support module.
 */
(function($, cCometD) {
  "use strict";

  /** For debug logging. */
  var log = function(msg, err) {
    var logPrefix = "[EDITORSUPPORT] ";
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
  function EditorSupport() {

    var prefixUrl = pageBaseUrl(location);
    var subscribedDocuments = {};
    var listeners = {};
    // CometD transport bus
    var cometd, cometdContext;
    var cometdConf;
    var userId;
    var configLoader = $.Deferred();

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
     * Subscribes the document and reacts to the events. Providers param is
     * optional (used for Documents app)
     */
    var subscribeDocument = function(fileId, callback) {
      var initloader;
      if (!cometd) {
        initloader = init();
      }
      if (initloader) {
        initloader.done(function() {
          subscribe(fileId, callback);
        });
      } else {
        subscribe(fileId, callback);
      }
    };

    var subscribe = function(fileId, callback) {
      log("Subscribinng on " + fileId);
      var subscription = cometd.subscribe("/eXo/Application/documents/" + fileId, function(message) {
        // Channel message handler
        var result = tryParseJson(message);
        callback(result);
      }, cometdContext, function(subscribeReply) {
        // Subscription status callback
        if (subscribeReply.successful) {
          // The server successfully subscribed this client to the channel.
          log("Document updates subscribed successfully: " + JSON.stringify(subscribeReply));
          subscribedDocuments.fileId = subscribeReply.subscription;
        } else {
          var err = subscribeReply.error ? subscribeReply.error : (subscribeReply.failure ? subscribeReply.failure.reason
              : "Undefined");
          log("Document updates subscription failed for " + fileId, err);
        }
      });
    }

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

    var init = function() {
      log("Initializing editor support module");
      var initLoader = $.Deferred();
      configLoader.done(function() {
        cCometD.configure({
          "url" : prefixUrl + cometdConf.path,
          "exoId" : userId,
          "exoToken" : cometdConf.token,
          "maxNetworkDelay" : 30000,
          "connectTimeout" : 60000
        });
        cometdContext = {
          "exoContainerName" : cometdConf.containerName,
          "provider" : cometdConf.provider,
          "workspace" : cometdConf.workspace
        };
        cometd = cCometD;
        initLoader.resolve();
      });
      return initLoader;
    };

    this.init = init;

    this.initConfig = function(user, conf) {
      log("Config inited");
      userId = user;
      cometdConf = conf;
      configLoader.resolve();
    };

    this.addListener = function(caller, fileId, callback) {
      if (listeners[caller] && listeners[caller][fileId]) {
        log("Listener already registered for " + caller + " and fileId: " + fileId);
        return;
      }

      subscribeDocument(fileId, callback);
      if (listeners[caller]) {
        listeners[caller].push(fileId);
      } else {
        listeners[caller] = [ fileId ];
      }
    };

    this.onEditorOpen = function(fileId, workspace, provider) {
      log("Editor opened. Provider: " + provider + ", fileId: " + fileId);
      // subsribe to track opened editors on server-side
      if (!cometd) {
        init();
      }
      var subscription = cometd.subscribe("/eXo/Application/documents/" + fileId, function(message) {
      }, cometdContext, function(subscribeReply) {
      });
      publishEvent(fileId, {
        "type" : DOCUMENT_OPENED,
        "provider" : provider,
        "fileId" : fileId,
        "workspace" : workspace
      });
    };

    this.refreshStatus = function(fileId, workspace) {
      publishEvent(fileId, {
        "type" : REFRESH_STATUS,
        "fileId" : fileId,
        "workspace" : workspace
      });
    }

  }

  return new EditorSupport();

})($, cCometD);