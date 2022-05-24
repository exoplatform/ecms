/**
 * @license Copyright (c) 2003-2017, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.md or http://ckeditor.com/license
 */

'use strict';

(function() {
  CKEDITOR.plugins.add('uploadImage', {
    requires: 'uploadwidget,autogrow',

    onLoad: function() {
      CKEDITOR.addCss(
        '.cke_upload_uploading img{' +
        'opacity: 0.3' +
        '}'
      );
    },

    init: function(editor) {
      if (!editor.ui || !editor.ui.space("top")) {
        // Workaround for bug: https://dev.ckeditor.com/ticket/14610
        editor.on('uiSpace', function(event) {
          if (event.data.space != "top")
            return;
          event.data.html += "<div />";
        });
      }
      var config = editor.config

      require(["SHARED/uiSelectImage", "SHARED/jquery"], function(UISelectImage, $) {
        if (editor.editable()) {
          $(editor.editable().$).parent().on('dragover', function(e) {
            if (e && e.originalEvent && e.originalEvent.dataTransfer
              && e.originalEvent.dataTransfer.types
              && e.originalEvent.dataTransfer.types.length
              && e.originalEvent.dataTransfer.types[0] == 'Files') {
              $(this).addClass('dragEntered');
            }
          });
          $(editor.editable().$).parent().on('dragleave', function(e) {
            $(this).removeClass('dragEntered');
          });
          $(editor.editable().$).parent().on('drop', function(e) {
            $(this).removeClass('dragEntered');
          });
        }

        var uploadId = UISelectImage.generateUploadId();
        config.uploadUrl = UISelectImage.getUploadURL(uploadId);
        editor.on('fileUploadRequest', function(evt) {
          // Prevent the default request handler.
          evt.stop();

          var fileLoader = evt.data.fileLoader,
            formData = new FormData(),
            xhr = fileLoader.xhr;
          fileLoader.uploadId = uploadId;
          fileLoader.thumbnailURL = evt.data.fileLoader.data;

          fileLoader.uploadUrl = config.uploadUrl;
          xhr.open('POST', fileLoader.uploadUrl, true);
          formData.append('upload', fileLoader.file, fileLoader.fileName);
          fileLoader.xhr.send(formData);

          // Renew uploadId
          uploadId = UISelectImage.generateUploadId();
          config.uploadUrl = UISelectImage.getUploadURL(uploadId);
        }, null, null, 4);
        editor.on('fileUploadResponse', function(evt) {
          // Prevent the default response handler.
          evt.stop();
          // Get XHR and response.
          var data = evt.data,
            xhr = data.fileLoader.xhr,
            status = xhr.status;

          if (status == 200) {
            data.url = data.fileLoader.thumbnailURL;
          } else {
            // An error occurred during upload.
            data.message = UISelectImage.getUploadingImageErrorMessage();
            UISelectImage.abortUpload(data.fileLoader.uploadId);
            evt.cancel();
          }
          if (editor.resizeEditor) {
            editor.resizeEditor(editor);
          }
        });
      })

      // Do not execute this paste listener if it will not be possible to upload file.
      if (!CKEDITOR.plugins.clipboard.isFileApiSupported) {
        return;
      }

      var fileTools = CKEDITOR.fileTools,
        uploadUrl = fileTools.getUploadUrl(editor.config, 'selectImage');

      if (!uploadUrl) {
        CKEDITOR.error('uploadimage-config');
        return;
      }

      // Handle images which are available in the dataTransfer.
      fileTools.addUploadWidget(editor, 'uploadimage', {
        supportedTypes: /image\/(jpeg|png|gif|jpg)/,

        uploadUrl: uploadUrl,

        fileToElement: function() {
          var root = (editor.editable ? editor.editable() : (editor.mode == 'wysiwyg' ? editor.document && editor.document.getBody() : editor.textarea));

          // Remove placeholder class
          if (root.hasClass('placeholder')) {
            root.removeClass('placeholder');
            root.setHtml("");
          }

          var img = new CKEDITOR.dom.element('img');
          img.setAttribute('src', loadingImage);
          return img;
        },

        parts: {
          img: 'img'
        },

        onUploading: function(upload) {
          // Show the image during the upload.
          this.parts.img.setAttribute('src', upload.data);
          // Update CKEditor height
          setTimeout(function() {
            editor.execCommand('autogrow');
          }, 500);
        },

        onUploaded: function(upload) {
          var self = this;
          var uploadFinished = false;
          var uploadError = false;
          var driveName = CKEDITOR.currentInstance.config.spaceGroupId && CKEDITOR.currentInstance.config.spaceGroupId.replaceAll("/", ".") || "Personal Documents";

          var imagesDownloadFolder = CKEDITOR.currentInstance.config.imagesDownloadFolder;
          var restURL = eXo.env.server.context + "/" + eXo.env.portal.rest + "/"
            + "managedocument/uploadFile/control?workspaceName=collaboration&driveName=" + driveName
            + "&currentPortal=" + eXo.env.portal.portalName + "&language="
            + eXo.env.portal.language + "&currentFolder=" + imagesDownloadFolder
            + "&uploadId=" + upload.uploadId + "&fileName=" + upload.fileName + "&action=save";

          fetch(restURL, {
            credentials: 'include',
            method: 'GET',
          }).then(response => {
            if (response.ok) {
              uploadFinished = true;
              return response.text();
            } else {
              return response.text().then(error => {
                uploadError = true;
                throw new Error(error);
              });
            }
          })
            .then(xmlStr => (new window.DOMParser()).parseFromString(xmlStr, 'text/xml'))
            .then(xml => {
              if (xml) {
                return xml.childNodes[0].attributes[0].value;
              }
            }).then(uuid => {
              if (uploadFinished && !uploadError) {
                self.replaceWith('<img src="' + eXo.env.server.context + "/" + eXo.env.portal.rest + "/images/repository/collaboration/" + (uuid ? uuid : "") + '" />');
              }

              if (editor.resizeEditor) {
                editor.resizeEditor();
              } else if (editor.resize) {
                editor.resize();
              }
    
              setTimeout(function() {
                editor.execCommand('autogrow');
              }, 500);
            });
        }
      });

      // This means that we need to read them from the <img src="data:..."> elements.
      editor.on('paste', function(evt) {
        // For performance reason do not parse data if it does not contain img tag and data attribute.
        if (!evt.data.dataValue.match(/<img[\s\S]+data:/i)) {
          return;
        }

        var data = evt.data,
          // Prevent XSS attacks.
          tempDoc = document.implementation.createHTMLDocument(''),
          temp = new CKEDITOR.dom.element(tempDoc.body),
          imgs, img, i;

        // Without this isReadOnly will not works properly.
        temp.data('cke-editable', 1);

        temp.appendHtml(data.dataValue);

        imgs = temp.find('img');

        for (i = 0; i < imgs.count(); i++) {
          img = imgs.getItem(i);

          // Image have to contain src=data:...
          var isDataInSrc = img.getAttribute('src') && img.getAttribute('src').substring(0, 5) == 'data:',
            isRealObject = img.data('cke-realelement') === null;

          // We are not uploading images in non-editable blocs and fake objects (http://dev.ckeditor.com/ticket/13003).
          if (isDataInSrc && isRealObject && !img.data('cke-upload-id') && !img.isReadOnly(1)) {
            var loader = editor.uploadRepository.create(img.getAttribute('src'));
            loader.upload(uploadUrl);

            fileTools.markElement(img, 'uploadimage', loader.id);

            fileTools.bindNotifications(editor, loader);
          }
        }

        data.dataValue = temp.getHtml();
      });
    }
  });

  // jscs:disable maximumLineLength
  // Black rectangle which is shown before image is loaded.
  var loadingImage = 'data:image/gif;base64,R0lGODlhDgAOAIAAAAAAAP///yH5BAAAAAAALAAAAAAOAA4AAAIMhI+py+0Po5y02qsKADs=';
  // jscs:enable maximumLineLength

  /**
   * The URL where images should be uploaded.
   *
   * @since 4.5
   * @cfg {String} [imageUploadUrl='' (empty string = disabled)]
   * @member CKEDITOR.config
   */
})();
