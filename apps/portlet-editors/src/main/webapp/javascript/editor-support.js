/**
 * Editor support module.
 */
(function($, cCometD) {
  'use strict';

  /** For debug logging. */
  const log = function(msg, err) {
    const logPrefix = '[editorsupport] ';
    if (typeof console !== 'undefined' && typeof console.log !== 'undefined') {
      const isoTime = ` -- ${  new Date().toISOString()}`;
      let msgLine = msg;
      if (err) {
        msgLine += '. Error: ';
        if (err.name || err.message) {
          if (err.name) {
            msgLine += `[${  err.name  }] `;
          }
          if (err.message) {
            msgLine += err.message;
          }
        } else {
          msgLine += (typeof err === 'string' ? err : JSON.stringify(err) +
            (err.toString && typeof err.toString === 'function' ? `; ${  err.toString()}` : ''));
        }

        console.log(logPrefix + msgLine + isoTime);
        if (typeof err.stack !== 'undefined') {
          console.log(err.stack);
        }
      } else {
        if (err !== null && typeof err !== 'undefined') {
          msgLine += `. Error: '${  err  }'`;
        }
        console.log(logPrefix + msgLine + isoTime);
      }
    }
  };

  const pageBaseUrl = function(theLocation) {
    if (!theLocation) {
      theLocation = window.location;
    }

    let theHostName = theLocation.hostname;
    const theQueryString = theLocation.search;

    if (theLocation.port) {
      theHostName += `:${  theLocation.port}`;
    }

    return `${theLocation.protocol  }//${  theHostName}`;
  };

  /**
   * Editor core class.
   */
  function EditorSupport() {

    const prefixUrl = pageBaseUrl(location);
    const listeners = {};
    // CometD transport bus
    let cometd, cometdContext;
    const configLoader = $.Deferred();
    let initLoader;
    let idleTimer;
    let closeInterval;
    let $idleModal;
    let idleEnabled = false;
    let idlePopupTimeout;
    
    const DOCUMENT_OPENED = 'DOCUMENT_OPENED';
    const DOCUMENT_CLOSED = 'DOCUMENT_CLOSED';
    const LAST_EDITOR_CLOSED = 'LAST_EDITOR_CLOSED';
    const REFRESH_STATUS = 'REFRESH_STATUS';
    const CURRENT_PROVIDER_INFO = 'CURRENT_PROVIDER_INFO';
    
    
    let messages = {}; // should be initialized by initConfig

    const message = function(key) {
      const m = messages[key];
      return m ? m : key;
    };

    /**
     * Parses comet message from JSON
     */
    const tryParseJson = function(message) {
      const src = message.data ? message.data : (message.error ? message.error : message.failure);
      if (src) {
        try {
          if (typeof src === 'string' && (src.startsWith('{') || src.startsWith('['))) {
            return JSON.parse(src);
          }
        } catch (e) {
          log(`Error parsing '${  src  }' as JSON: ${  e}`, e);
        }
      }
      return src;
    };

    /**
     * Subscribes the document and passes events to the callback.
     */
    const subscribeDocument = function(fileId, callback) {
      if (!initLoader) {
        init();
      }
      const subscriptionPromise = $.Deferred();
      initLoader.done(function() {
        var subscription = cometd.subscribe(`/eXo/Application/documents/editor/${  fileId}`, function(message) {
          // Channel message handler
          const result = tryParseJson(message);
          if (callback) {
            callback(result);
          }
        }, cometdContext, function(subscribeReply) {
          // Subscription status callback
          if (subscribeReply.successful) {
            // The server successfully subscribed this client to the channel.
            log(`Document updates subscribed successfully: ${  JSON.stringify(subscribeReply)}`);
            subscriptionPromise.resolve(subscription);
          } else {
            const err = subscribeReply.error ? subscribeReply.error : (subscribeReply.failure ? subscribeReply.failure.reason :
              'Undefined');
            log(`Document updates subscription failed for ${  fileId}`, err);
          }
        });
      });
      return subscriptionPromise;
    };

    /**
     * Publish event to the cometd channel.
     */
    const publishEvent = function(fileId, data) {
      const deferred = $.Deferred();
      cometd.publish(`/eXo/Application/documents/editor/${  fileId}`, data, cometdContext, function(publishReply) {
        // Publication status callback
        if (publishReply.successful) {
          deferred.resolve();
          // The server successfully subscribed this client to the channel.
          log(`Event published successfully: ${  JSON.stringify(publishReply)}`);
        } else {
          deferred.reject();
          const err = publishReply.error ? publishReply.error : (publishReply.failure ? publishReply.failure.reason : 'Undefined');
          log(`Event publication failed for ${  fileId}`, err);
        }
      });
      return deferred;
    };

    /**
     * Gets listener by caller and fileId.
     */
    const getListener = function(caller, fileId) {
      if (!(caller in listeners)) {
        return null;
      }
      for (let i = 0; i < listeners[caller].length; i++) {
        if (listeners[caller][i].fileId === fileId) {
          return listeners[caller][i];
        }
      }
      return null;
    };

    /**
     * Inits cometd
     */
    var init = function(provider, workspace) {
      if (initLoader) {
        log('Init is in progress or already done');
        return;
      }
      log('Initializing editor support module');
      initLoader = $.Deferred();
      configLoader.done(function(user, cometdConf, i18n, idleTimeout) {
        messages = i18n;
        idlePopupTimeout = idleTimeout;
        cCometD.configure({
          'url': prefixUrl + cometdConf.path,
          'exoId': user,
          'exoToken': cometdConf.token,
          'maxNetworkDelay': 30000,
          'connectTimeout': 60000
        });
        cometdContext = {
          'exoContainerName': cometdConf.containerName,
          'provider': provider,
          'workspace': workspace
        };
        cometd = cCometD;
        initLoader.resolve();
      });
    };

    /**
     * Shows close popup (idle) 
     */
    const showClosePopup = function() {
      $idleModal = $('#editorIdleModal');
      const title = message('idlePopup.title');
      let text = message('idlePopup.message');
      text = text.replace(/%s/g, '<span id=\'closeCountdown\'></span>');
      if ($idleModal.length == 0) {
        $idleModal = $(`<div id='editorIdleModal'><div id='editorIdleModalContent'> <span id='editorIdleModalClose'>&times;</span> <h3>${  title  }</h3> <p>${  text  }</p> </div> </div>`);
        $('body').prepend($idleModal);
      }
      $('body').blur();
      $idleModal.css('display', 'block');
      let countdown = 30;
      const $closeCountdown = $('#closeCountdown');
      $closeCountdown.html(countdown);
      closeInterval = setInterval(function() {
        countdown--;
        $closeCountdown.html(countdown);
        if (countdown == 0) {
          window.close();
        }
      }, 1000);
      $('#editorIdleModalClose').on('click', function() {
        $idleModal.css('display', 'none');
        clearInterval(closeInterval);
        notifyActive();
      });
      window.onclick = function(event) {
        if (event.target == $idleModal.get(0)) {
          $idleModal.css('display', 'none');
          clearInterval(closeInterval);
          notifyActive();
        }
      };
    };

    /**
     * Notifies that the editor is active.
     */
    var notifyActive = function() {
      if (idleEnabled) {
        clearTimeout(idleTimer);
        idleTimer = setTimeout(showClosePopup, idlePopupTimeout);
        clearInterval(closeInterval);
        if ($idleModal) {
          $idleModal.css('display', 'none');
        }
      }
    };

    this.notifyActive = notifyActive;

    this.init = init;

    /**
     * Inits configuration
     */
    this.initConfig = function(user, conf, i18n, idleTimeout) {
      configLoader.resolve(user, conf, i18n, idleTimeout);
    };

    /**
     * Removes listener
     */
    this.removeListener = function(caller, fileId) {
      const removeLoader = $.Deferred();
      const listener = getListener(caller, fileId);
      if (!listener) {
        log(`Listener isn't registered for ${  caller  } and fileId: ${  fileId}`);
        return;
      }
      for (let i = 0; i < listeners[caller].length; i++) {
        if (listeners[caller][i].fileId === fileId) {
          listeners[caller].splice(i, 1);
        }
      }
      if (listener.subscription) {
        cometd.unsubscribe(listener.subscription, {}, function(unsubscribeReply) {
          if (unsubscribeReply.successful) {
            // The server successfully unsubscribed this client to the channel.
            log(`Document updates unsubscribed successfully for: ${  fileId}`);
            removeLoader.resolve();
          } else {
            const err = unsubscribeReply.error ? unsubscribeReply.error :
              (unsubscribeReply.failure ? unsubscribeReply.failure.reason : 'Undefined');
            log(`Document updates unsubscription failed for ${  fileId}`, err);
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
        return;
      }
      const subscriptionLoader = subscribeDocument(fileId, callback);
      // Save listener before subscription inited
      const listener = {
        fileId: fileId
      };
      if (listeners[caller]) {
        listeners[caller].push(listener);
      } else {
        listeners[caller] = [listener];
      }

      // Set subscription for listener
      subscriptionLoader.done(function(subscription) {
        listener.subscription = subscription;
      });
    };

    /**
     * Opens cometd connection and sends DOCUMENT_OPENED event to track the editor.
     * Used as an API for providers.
     */
    this.onEditorOpen = function(fileId, workspace, provider) {
      log(`Editor opened. Provider: ${  provider  }, fileId: ${  fileId}`);
      // subsribe to track opened editors on server-side
      if (!cometd) {
        init(provider, workspace);
      }
      const $loader = $.Deferred();

      initLoader.done(function() {
        subscribeDocument(fileId, function(result) {
          if (result.type === CURRENT_PROVIDER_INFO) {
            if (result.allProviders.length > 1) {
              idleEnabled = true;
            }
            if (result.available == 'true') {
              $loader.resolve();
            } else {
              $loader.reject();
            }
          }
        }).done(function() {
          publishEvent(fileId, {
            'type': DOCUMENT_OPENED,
            'provider': provider,
            'fileId': fileId,
            'workspace': workspace,
            'userId': eXo.env.portal.userName
          });
        });

      });
      notifyActive();
      return $loader.promise();
    };
  }
  return new EditorSupport();

})($, cCometD);