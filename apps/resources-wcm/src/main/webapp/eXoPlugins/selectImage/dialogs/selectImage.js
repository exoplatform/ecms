/**
 * @license Copyright (c) 2003-2017, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.md or http://ckeditor.com/license
 */

/**
 * @fileOverview Image plugin based on Widgets API
 */

'use strict';

CKEDITOR.dialog.add( 'selectImage', function( editor ) {

    var lang = editor.lang.selectImage,
    commonLang = editor.lang.common,

    helpers = CKEDITOR.plugins.selectImage,

    // Editor instance configuration.
    config = editor.config,

    hasFileBrowser = !!( config.filebrowserImageBrowseUrl || config.filebrowserBrowseUrl ),

    // Content restrictions defined by the widget which
    // impact on dialog structure and presence of fields.
    features = editor.widgets.registered.selectImage.features,

    // Functions inherited from image2 plugin.
    getNatural = helpers.getNatural,

    // Global variables referring to the dialog's context.
    doc, widget, image,

    // Global variable referring to this dialog's image pre-loader.
    preLoader,

    // Global variables holding the original size of the image.
    domWidth, domHeight,

    // Global variables related to image pre-loading.
    preLoadedWidth, preLoadedHeight,

    // Global variables referring to dialog fields and elements.
    lockButton, resetButton, widthField, heightField,

    natural;

  // Creates a function that pre-loads images. The callback function passes
  // [image, width, height] or null if loading failed.
  //
  // @returns {Function}
  function createPreLoader() {
    var image = doc.createElement( 'img' ),
      listeners = [];

    function addListener( event, callback ) {
      listeners.push( image.once( event, function( evt ) {
        removeListeners();
        callback( evt );
      } ) );
    }

    function removeListeners() {
      var l;

      while ( ( l = listeners.pop() ) )
        l.removeListener();
    }

    // @param {String} src.
    // @param {Function} callback.
    return function( src, callback, scope ) {
      addListener( 'load', function() {
        // Don't use image.$.(width|height) since it's buggy in IE9-10 (http://dev.ckeditor.com/ticket/11159)
        var dimensions = getNatural( image );

        callback.call( scope, image, dimensions.width, dimensions.height );
      } );

      addListener( 'error', function() {
        callback( null );
      } );

      addListener( 'abort', function() {
        callback( null );
      } );

      image.setAttribute( 'src',
        ( config.baseHref || '' ) + src + '?' + Math.random().toString( 16 ).substring( 2 ) );
    };
  }

  return {
    title: lang.title,
    minWidth: 600,
    minHeight: 270,
    resizable: CKEDITOR.DIALOG_RESIZE_NONE,
    onLoad: function() {
      var parentElement = this.getElement();

      parentElement.addClass("dropFileWindow").addClass("uiPopup").addClass("cke_dialog");
      parentElement.removeClass('cke_reset_all');
      parentElement.findOne('.cke_dialog_title').$.className += ' popupHeader';
      parentElement.findOne('.cke_dialog_close_button').$.className = 'uiIconClose cke_dialog_close_button';

      // Create a "global" reference to the document for this dialog instance.
      doc = this._.element.getDocument();

      // Create a pre-loader used for determining dimensions of new images.
      preLoader = createPreLoader();
    },
    onShow: function() {
      // Create a "global" reference to edited widget.
      widget = this.widget;

      // Create a "global" reference to widget's image.
      image = widget.parts.image;

      // Natural dimensions of the image.
      natural = getNatural( image );

      // Get the natural width of the image.
      preLoadedWidth = domWidth = natural.width;

      // Get the natural height of the image.
      preLoadedHeight = domHeight = natural.height;

      var parentElement = this.getElement();
      var element = this.element;

      if ( element )
          element = element.getAscendant( 'img', true );

      if (!element) {
        element = editor.document.createElement('img');
        this.insertMode = true;
      } else {
        this.insertMode = false;
      }

      this.element = element;
      this.setupContent(this.element);

      var dialog = this;
      require(["SHARED/uiSelectImage"], function(UISelectImage){
        UISelectImage.init(parentElement.$, widget.data, function() {
          dialog.enableButton('ok');
        }, function() {
          dialog.disableButton('ok');
        });
      })
    },
    contents: [
        {
            id: 'tab1',
            label: 'Label',
            title: 'Title',
            width: 380,
            elements: [
                {
                    type: 'html',
                    html: '<div class="selectImageBox" />',
                    commit: function( widget ) {
                      if(!widget || widget.name !== 'selectImage') {
                        return;
                      }
                      var dialog = this.getDialog();
                      require(["SHARED/jquery"], function($) {
                        var $imageElement = $(dialog.getElement().$).find(".selectedImagePreview img");
                        const imageLink = $(dialog.getElement().$).find(".imageLinkArea input").val();
                        const imageLinkTarget = $(dialog.getElement().$).find(".imageLinkTargetArea select").find(":selected").val();
                        widget.setData( 'src', $imageElement.attr("src") );
                        widget.setData( 'alt', $imageElement.attr("alt") );
                        if (imageLink) {
                          const url = imageLink.split('://')[1];
                          const linkData = {
                            type: 'url',
                            url: {url: url, protocol: imageLink.split(url)[0],},
                            target: {type: imageLinkTarget},
                          }
                          widget.setData('link', linkData);
                          widget.setData('href', imageLink);
                          widget.setData('target', imageLinkTarget);
                        } else {
                          widget.setData('link', null);
                        }
                        if ($imageElement.hasClass("left")) {
                          widget.setData( 'align', 'left' );
                        } else if ($imageElement.hasClass("right")) {
                          widget.setData( 'align', 'right' );
                        } else if ($imageElement.hasClass("center")) {
                          widget.setData( 'align', 'center' );
                        }
                      });
                    }
             }
            ]
        }
    ],
    buttons: [CKEDITOR.dialog.okButton, CKEDITOR.dialog.cancelButton],
    onCancel: function() {
      require(["SHARED/uiSelectImage"], function(UISelectImage){
        UISelectImage.cancel();
      })
    },
  };
} );
