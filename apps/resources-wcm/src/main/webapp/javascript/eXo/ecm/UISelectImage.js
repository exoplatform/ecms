  (function(UISelectFromDrives, $, base){
  var UISelectImage = {
    urlPattern: new RegExp("(http|ftp|https)://(.*)/(.*)"),
    imageLinkUrlPattern: new RegExp("((?:www\\.|(?!www))[a-zA-Z0-9-]+\\.[^\\s]{2,})"),
    init : function(dialogElement, widgetData, enableOkButtonCallback, disableOkButtonCallback) {
      this.status = {};
      this.$parentDialog = $(dialogElement);
      var $dialogFooter = this.$parentDialog.find(".cke_dialog_footer");
      if ($dialogFooter.length) {
        $dialogFooter[0].className = "uiActionBorder";
        this.okButton = this.$parentDialog.find(".cke_dialog_ui_button_ok");
        this.okButton[0].className = "btn btn-primary"
        this.cancelButton = this.$parentDialog.find(".cke_dialog_ui_button_cancel");
        // To enforce style of cancel button not to be primary
        $("<a class='btn hidden' />").insertBefore(this.cancelButton);
        this.cancelButton[0].className = "btn";
        $(".cke_dialog_background_cover").addClass("uiPopupWrapper");
        $(".cke_dialog_background_cover").css({"background-color": "", "opacity": ""});
      } else {
        this.okButton = this.$parentDialog.find(".btn-primary");
        this.cancelButton = this.$parentDialog.find(".btn").last();
      }

      this.enableOkButtonCallback = enableOkButtonCallback;
      this.disableOkButtonCallback = disableOkButtonCallback;
      this.enableOKButton(false);
      const hideUploadImageLink = CKEDITOR.currentInstance.config.hideUploadImageLink || false;
      const isImageDragBlocked = CKEDITOR.currentInstance.config.isImageDragBlocked || false;
      this.$parentDialog.find(".selectImageBox").html(
        '<div class="alert alert-error hidden">' +
          '<i class="uiIconError"></i><span class="message"></span>' +
        '</div>' +
        '<div class="dropFileArea">' +
          '<div class="backButton hidden">' +
            '<a href="javascript:void(0)">${CKEditor.image.Back}</a>' +
          '</div>' +
          '<div class="imageURLContainer hidden">' +
            '<label for="composerImageURL">${CKEditor.Image.URL}:</label>' +
            '<div class="clearfix"></div>' +
            '<input type="text" name="composerImageURL" placeholder="https://" class="imageURL" />' +
          '</a>' +
          '</div>' +
          '<div class="uploadContainer hidden">' +
            '<div class="fileNameAndSize" data-toggle="tooltip" rel="tooltip" data-placement="top">' +
            '</div>' +
            '<div class="progressBar">' +
              '<div class="progress progress-striped pull-left">' +
                '<div class="bar" style="width:0%">0%</div>' +
              '</div>' +
              '<div class="abortFile pull-right">' +
                '<a href="#" class="actionIcon" rel="tooltip" data-placement="top" title="${CKEditor.image.CancelUpload}">' +
                  '<i class="uiIconAbort uiIconLightGray"></i>' +
                '</a>' +
              '</div>' +
            '</div>' +
            '<div class="fileHidden">' +
              '<input class="file" name="file" type="file" accept=".gif,.jpg,.jpeg,.png">' +
            '</div>' +
          '</div>' +
          '<div class="selectImageLinks">' +
          ( isImageDragBlocked ? '' : '<span class="dropFileDescription visible-desktop hidden-tablet hidden-phone">${CKEditor.image.DropYouImageHere}<br /></span>' ) +
            ( hideUploadImageLink ? '' :  '<span class="hidden-tablet hidden-phone">${CKEditor.image.or}<br /></span>' +
                '<span class="uploadImageLink">' +
                '<a href="javascript:void(0)"><span class="visible-desktop hidden-tablet hidden-phone">${CKEditor.image.uploadFromYourDesktop}</span><span class="hidden-desktop visible-tablet visible-phone">${CKEditor.image.UploadFromYourMobile}</span>\</a>' +
                '<br />' +
                '</span>' ) +
          ( !isImageDragBlocked || !hideUploadImageLink ? '<span>${CKEditor.image.or}</span>' : '') +
            '<span class="selectFromExistingUpload">' +
              '<br />' +
              '<a href="javascript:void(0)"><span>${CKEditor.image.selectFromExistingUploads}</span></a>' +
              '<br />' +
            '</span>' +
            '<span>${CKEditor.image.or}</span>' +
            '<span class="selectImageURL">' +
              '<br />' +
              '<a href="javascript:void(0)"><span>${CKEditor.image.pointAnImageURL}</span></a>' +
            '</span>' +
          '</div>' +
          '<div class="selectedImagePreview hidden">' +
            '<img referrerpolicy="no-referrer" />' +
          '</div>' +
        '</div>' +
        '<div class="removeFile hidden">' +
          '<a href="javascript:void(0)">' +
            '${CKEditor.image.RemoveImage}' +
          '</a>' +
        '</div>' +
        '<div class="altArea hidden">' +
         '<div class="altLabelInput ">' +
        '<label for="composerImageAlt">${CKEditor.image.imageAltTextTitle}</label>' +
        '<div class="inputAlt ">' +
          '<input type="text" name="composerImageAlt" class="imageAlt" />' +
                  '<p class="alt-description">'+
                  '<span class="accessibility-icon">'+
                  '<i class="mdi mdi-human"></i>'+
                  '</span>'+
                  '<span class="caption text-light-color">${CKEditor.image.imageAltTextDescription}</span>'+
                  '</p>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '<div class="selectImageAlign clearfix">' +
          '<span class="selectImageAlignLabel">' +
            '${CKEditor.image.Alignment}:' +
          '</span>' +
          '<div class="btn-group">' +
            '<a class="btn" data-placement="top" rel="tooltip" data-align="Left" data-original-title="${CKEditor.image.alignment.Left}">' +
              '<i class="uiIconAlignLeft"/>' +
            '</a>' +
            '<a class="btn " data-placement="top" rel="tooltip" data-align="Middle" data-original-title="${CKEditor.image.alignment.Middle}">' +
              '<i class="uiIconAlignCenter"/>' +
            '</a>' +
            '<a class="btn " data-placement="top" rel="tooltip" data-align="Right" data-original-title="${CKEditor.image.alignment.Right}">' +
              '<i class="uiIconAlignRight"/>' +
            '</a>' +
          '</div>' +
        '</div>' +
        '<div class="imageLinkOptions hidden">' +
          '<div class="imageLinkArea">' +
          '<label for="imageLink">${CKEditor.image.link}:</label>' +
          '<p class="caption text-light-color">${CKEditor.image.link.description}</p>' +
          '<input type="text" name="imageLink" class="imageLink" />' +
          '<p class="caption text-error hidden">${CKEditor.image.link.format.error}</p>' +
          '</div>' +
          '<div class="imageLinkTargetArea">' +
          '<label for="imageLinkTarget">${CKEditor.image.linkTarget}:</label>' +
          '<p class="caption text-light-color">${CKEditor.image.linkTarget.description}</p>' +
          '<select name="imageLinkTarget" class="imageLinkTarget">' +
          '<option value="_self">${CKEditor.image.linkTarget.self}</option>' +
          '<option value="_blank">${CKEditor.image.linkTarget.blank}</option>' +
          '</select>' +
          '</div>' +
        '</div>'
      );

      this.uploadLinksCnt = this.$parentDialog.find(".selectImageLinks");
      this.uploadCnt = this.$parentDialog.find(".uploadContainer");
      this.imagePreviewCnt = this.$parentDialog.find(".selectedImagePreview");
      this.warningMessageCnt = this.$parentDialog.find(".alert");
      this.imageURLCnt = this.$parentDialog.find(".imageURLContainer");
      this.imageElement = this.imagePreviewCnt.find("img");
      this.imageLink = this.imagePreviewCnt.find("a");
      this.imageLinkOptionsContainer = this.$parentDialog.find(".imageLinkOptions");
      this.linkFormatError = this.$parentDialog.find(".text-error");
      this.imageElement.on("load", function (data){
        self.showBlock(self.backBtn, false);
        self.showBlock(self.imageURLCnt, false);
        self.showBlock(self.uploadCnt, false);
        self.showBlock(self.uploadLinksCnt, false);
        self.showBlock(self.imagePreviewCnt, true);
        self.showBlock(self.deleteFile, true);
        self.showBlock(self.altImageContainer, true);
        self.showBlock(self.imageLinkOptionsContainer, true);
        self.enableOKButton(true);
        self.triggerResizeEvent();
      }).on("error", function (data) {
        if(self.aborted) {
          self.imageElement.removeAttr("src");
          self.aborted = false;
          return;
        } else {
          self.abortUpload(self.status.uploadId);
        }
        self.enableOKButton(false);
        self.showBlock(self.uploadLinksCnt, false);
        self.showBlock(self.warningMessageCnt, false);
        self.showBlock(self.imagePreviewCnt, false);
        self.showBlock(self.deleteFile, false);
        self.showBlock(self.altImageContainer, false);
        self.showBlock(self.imageLinkOptionsContainer, false);
        self.displayWarning("${CKEditor.image.error.badURL}");
      });

      this.backBtn = this.$parentDialog.find(".backButton");

      this.fileNameAndSizeCnt = this.uploadCnt.find(".fileNameAndSize");
      this.progress = this.uploadCnt.find(".progress");
      this.progressBar = this.uploadCnt.find(".bar");
      this.abortFile = this.uploadCnt.find(".abortFile");

      var self = this;
      var $input = this.$parentDialog.find("input[type=file]");
      var $uploadBtn = this.$parentDialog.find(".uploadImageLink");
      var $selectURLBtn = this.$parentDialog.find(".selectImageURL");
      var $selectExistingUpload = this.$parentDialog.find(".selectFromExistingUpload");
      this.deleteFile = this.$parentDialog.find(".removeFile");
      this.altImageContainer = this.$parentDialog.find(".altArea");
      var altValue = this.altImageContainer.find("input[type='text']").val();
      this.abortFile.find("a").click(function() {
        self.aborted = true;
        if(self.status && self.status.jqXHR) {
          self.status.jqXHR.abort();
        }
        self.abortUpload(self.status.uploadId);
      });
      this.deleteFile.find("a").off("click").click(function() {
        self.deleteUpload((self.status && self.status.uploadId)? self.status.uploadId : null);
        self.displayImage();
        self.imageURLCnt.find("input").val("");
        self.altImageContainer.find("input[type='text']").val("");
        self.imageElement.attr("alt", "");
        self.showBlock(self.imageLinkOptionsContainer, false);
      });
      if (base.Browser.isIE()) {
        $uploadBtn.find("label").attr("for", $input.attr("id"));
        $input.css({
          "position" : "absolute",
          "left" : "-5000px"
        }).show();
      } else {
        $uploadBtn.off("click").click(function() {
          $input.click();
        })
      }
      $selectURLBtn.find("a").off("click").click(function() {
        if(self.status && self.status.uploadId) {
          self.deleteUpload(self.status.uploadId);
        }
        var $textField = self.imageURLCnt.find("input[type='text']");
        $textField.val("");
        self.altImageContainer.find("input[type='text']").val("");

        self.showBlock(self.uploadLinksCnt, false);
        self.showBlock(self.warningMessageCnt, false);
        self.showBlock(self.imagePreviewCnt, false);
        self.showBlock(self.deleteFile, false);
        self.showBlock(self.altImageContainer, false);
        self.showBlock(self.imageURLCnt, true);
        self.showBlock(self.backBtn, true);

        // focus on Text input
        $textField.focus();
      });
      $selectExistingUpload.find("a").off("click").click(function() {
        var imageDialogTitleCnts = self.$parentDialog.find(".cke_dialog_title, .cke_dialog_close_button");
        var imageDialogTitleCnt = self.$parentDialog.find(".cke_dialog_title");

        var oldTitle = imageDialogTitleCnt.html();

        var imageDialogWindowCnt = self.$parentDialog.find(".cke_dialog_body");
        imageDialogWindowCnt.find(">*").addClass("hidden");
        imageDialogTitleCnts.removeClass("hidden");
        imageDialogTitleCnt.html("${CKEditor.image.SelectFiles}");

        UISelectFromDrives.init(imageDialogWindowCnt, CKEDITOR.currentInstance.config.spaceGroupId, function (selectImageURL) {
          imageDialogTitleCnt.html(oldTitle);
          self.displayImage(selectImageURL);
        });
        self.triggerResizeEvent();
      });
      this.backBtn.find("a").off("click").click(function() {
        self.showBlock(self.uploadLinksCnt, true);
        self.showBlock(self.warningMessageCnt, false);
        self.showBlock(self.imagePreviewCnt, false);
        self.showBlock(self.deleteFile, false);
        self.showBlock(self.altImageContainer, false);
        self.showBlock(self.imageURLCnt, false);
        self.showBlock(self.backBtn, false);
      });
      self.imageURLCnt.find("input[type='text']").on("blur", function() {
        var value = self.imageURLCnt.find("input[type='text']").val();
        if(value && value.trim()) {
          if(self.urlPattern.test(value.trim()) ) {
            value = value.trim();
            self.displayImage(value);
          } else {
            self.displayWarning("${CKEditor.image.error.badURLFormat}");
          }
        }
      });

      self.altImageContainer.find("input[type='text']").on("blur", function() {
        self.altValue = self.altImageContainer.find("input[type='text']").val();
        self.imageElement.attr("alt", self.altValue);
      });
      self.imageLinkOptionsContainer.find("input[type='text']").on("blur, keyup", function (e) {
        const linkValue = e.target.value.trim();
        if (linkValue && !self.imageLinkUrlPattern.test(linkValue)) {
          self.showBlock(self.linkFormatError, true);
          self.enableOKButton(false);
        } else {
          self.showBlock(self.linkFormatError, false);
          self.enableOKButton(true);
        }
      });
      $input.on("change", function() {
        self.handleFileUpload(this.files, self.$parentDialog);
      });
      this.$parentDialog.find(".selectImageAlign .btn-group .btn").on("click", function(e) {
        self.$parentDialog.find(".selectImageAlign .btn-group .btn").removeClass("active");
        $(this).addClass("active");
        var alignment = $(this).data("align");
        self.imageElement.removeClass("left");
        self.imageElement.removeClass("right");
        self.imageElement.removeClass("center");
        if(alignment == "Left") {
          self.imageElement.addClass("left");
        } else if(alignment == "Right") {
          self.imageElement.addClass("right");
        } else if(alignment == "Middle") {
          self.imageElement.addClass("center");
        }
      });
      if (!isImageDragBlocked) {
        var $dropFileArea = this.$parentDialog.find(".dropFileArea");
        $dropFileArea.on('dragover', function(e) {
          e.stopPropagation();
          e.preventDefault();
          $(this).addClass('dragEntered');
        });
        $dropFileArea.on('dragleave', function(e) {
          e.stopPropagation();
          e.preventDefault();
          $(this).removeClass('dragEntered');
        });
        $dropFileArea.on('drop', function(e) {
          $(this).removeClass('dragEntered');
          e.preventDefault();
          var files = e.originalEvent.dataTransfer.files;
          // We need to send dropped files to Server
          self.handleFileUpload(files, self.$parentDialog);
        });
      }
      this.$parentDialog.find(".selectImageAlign .btn-group .btn[data-align=Left]").addClass("active");
      if(widgetData  && widgetData.src ) {
        this.displayImage(widgetData.src);
        if( widgetData.alt ) {
            self.altImageContainer.find("input[type='text']").val(widgetData.alt);
            self.imageElement.attr("alt", widgetData.alt);
        } else {
            self.altImageContainer.find("input[type='text']").val("");
            self.imageElement.attr("alt", "");
        }
        if (widgetData.link && !widgetData.link.url) {
          self.imageLinkOptionsContainer.find("input[type='text']").val(widgetData.link);
        } else if (widgetData?.link?.url) {
          const linkValue = widgetData.link.url.protocol + widgetData.link.url.url;
          self.imageLinkOptionsContainer.find("input[type='text']").val(linkValue);
        }
        if (widgetData?.link?.target?.type) {
          const targetValue = widgetData?.link?.target?.type;
          self.imageLinkOptionsContainer.find(".imageLinkTarget").val(targetValue).change();
        }
        if (widgetData.align == "center") {
          this.$parentDialog.find(".selectImageAlign .btn-group .btn[data-align=Middle]").trigger("click");
        } else if (widgetData.align == "right") {
          this.$parentDialog.find(".selectImageAlign .btn-group .btn[data-align=Right]").trigger("click");
        } else {
          this.$parentDialog.find(".selectImageAlign .btn-group .btn[data-align=Left]").trigger("click");
        }
      } else {
        this.$parentDialog.find(".selectImageAlign .btn-group .btn[data-align=Left]").trigger("click");
      }
      $('*[rel="tooltip"]').tooltip();
      this.triggerResizeEvent();
    },
    cancel : function() {
      this.deleteUpload(this.status.uploadId);
    },
    showBlock : function($blockToshow, show, delay) {
      if(show) {
        $blockToshow.removeClass('hidden');
        $blockToshow.show();
        if(delay) {
          this.showBlock($blockToshow, false, delay);
        }
      } else {
        if(delay) {
          $blockToshow.fadeOut(delay, function() {
            $(this).addClass('hidden');
          });
        } else {
          $blockToshow.addClass("hidden");
          $blockToshow.hide();
        }
      }
    },
    displayWarning : function(message) {
      if(message) {
        this.warningMessageCnt.find(".message").html(message);
        this.showBlock(this.warningMessageCnt, true, 5000);
      } else {
        this.showBlock(this.warningMessageCnt, false);
      }
    },
    displayImage: function (imageURL, delay) {
      this.displayWarning();
      if(imageURL) {
        if(delay) {
          var self = this;
          setTimeout(function() {
            if(imageURL && imageURL.indexOf(self.getUploadURL()) >= 0 && self.aborted) {
              self.aborted = false;
              return;
            }
            self.imageElement.attr("src", imageURL);
          }, delay);
        } else {
          this.imageElement.attr("src", imageURL);
        }
      } else {
        this.enableOKButton(false);
        this.imagePreviewCnt.find("img").removeAttr("src");
      }
    },
    enableOKButton: function(enable) {
      if(enable) {
        this.enableOkButtonCallback();
        this.okButton.removeAttr("disabled");
        this.okButton.removeClass("disabled");
      } else {
        this.disableOkButtonCallback();
        this.okButton.removeClass("cke_disabled");
        this.okButton.attr("disabled", "disabled");
        this.okButton.addClass("disabled");
      }
    },
    handleFileUpload : function(files, $parentDialog) {
      this.showBlock(this.warningMessageCnt, false);
      if(!files.length) {
        return;
      }
      if(files.length != 1) {
        this.displayWarning("${CKEditor.image.error.multipleImagesNotAllowed}");
        return;
      }
      var fileToUpload = files[0];
      if(!endsWithCaseInsensitive(fileToUpload.name, ".gif") && !endsWithCaseInsensitive(fileToUpload.name, ".png") && !endsWithCaseInsensitive(fileToUpload.name, ".jpg") && !endsWithCaseInsensitive(fileToUpload.name, ".jpeg")) {
        this.displayWarning("${CKEditor.image.error.unknownImageFileExtension}");
        return;
      }

      // Delete old uploaded file when replacing existing
      if(this.status && this.status.uploadId) {
        this.deleteUpload(this.status.uploadId);
      }

      this.status = new this.createStatusbar();
      var maxFileSize = ajaxAsyncGetRequest(this.getControlURL(this.status.uploadId), false);
      maxFileSize = parseInt(maxFileSize);
      var sizeInMB = this.getSizeInMB(fileToUpload.size);
      if(sizeInMB > maxFileSize) {
        this.displayWarning("${CKEditor.image.error.imageMaxSizeExceeded}".replace("{0}", maxFileSize));
        return;
      }

      this.status.formData = new FormData();
      this.status.formData.append('file', fileToUpload);

      this.setProgress(0);
      this.setFileNameSize(fileToUpload.name, sizeInMB);

      this.showBlock(this.uploadLinksCnt, false);
      this.showBlock(this.imagePreviewCnt, false);
      this.showBlock(this.deleteFile, false);
      this.showBlock(this.altImageContainer, false);
      this.showBlock(this.uploadCnt, true);

      this.sendFileToServer(this.status);

      this.triggerResizeEvent();

      $parentDialog.find("input[type=file]").val('');
    },
    getSizeInMB : function(size) {
      var sizeMB = size / 1024 / 1024;
      sizeMB = sizeMB.toFixed(2);
      return sizeMB;
    },
    getContext: function() {
      return eXo.env.server.context;
    },
    getRestContext: function() {
      return this.getContext() + "/" + eXo.env.portal.rest;
    },
    getControlURL: function(uploadId) {
      return this.getRestContext() + "/composer/image/new?uploadId=" + (uploadId ? uploadId : "");
    },
    getProgressURL: function(uploadId) {
      return this.getContext() + "/upload?action=progress&uploadId=" + (uploadId ? uploadId : "");
    },
    getImageURL: function(uuid) {
      return this.getRestContext() + "/images/repository/collaboration/" + (uuid ? uuid : "");
    },
    getDeleteURL: function(uploadId) {
      return this.getContext() + "/upload?action=delete&uploadId=" + (uploadId ? uploadId : "");
    },
    getAbortURL: function(uploadId) {
      return this.getContext() + "/upload?action=abort&uploadId=" + (uploadId ? uploadId : "");
    },
    getUploadURL: function(uploadId) {
      return this.getContext() + "/upload?action=upload&uploadId=" + (uploadId ? uploadId : "");
    },
    generateUploadId: function() {
      return (((1+Math.random())*0x10000000)|0).toString(16).substring(1);
    },
    createStatusbar : function() {
      this.uploadId =  UISelectImage.generateUploadId();
    },
    setFileNameSize : function(name, sizeInMB) {
      this.status.size = sizeInMB;
      this.status.name = name;

      var sizeStr = this.status.size + " ${CKEditor.image.MegaByte}";
      this.fileNameAndSizeCnt.html(name + " ( " + sizeStr + " )");
      this.fileNameAndSizeCnt.attr("data-original-title", name + " ( " + sizeStr + " )");
    },
    setProgress : function(progress) {
      this.progressBar.css("width", progress + "%");
      this.progressBar.html(progress + " %");
    },
    deleteUpload : function(uploadId) {
      if(uploadId) {
        var url = this.getDeleteURL(uploadId);
        ajaxAsyncGetRequest(url, true);
      }

      this.showBlock(this.imagePreviewCnt, false);
      this.showBlock(this.uploadCnt, false);
      this.showBlock(this.deleteFile, false);
      this.showBlock(this.altImageContainer, false);
      this.showBlock(this.uploadLinksCnt, true);
    },
    abortUpload : function(uploadId) {
      this.showBlock(this.imagePreviewCnt, false);
      this.showBlock(this.uploadCnt, false);
      this.showBlock(this.deleteFile, false);
      this.showBlock(this.altImageContainer, false);
      this.showBlock(this.uploadLinksCnt, true);

      if(this.abortedComplete) {
        return;
      }
      var url = this.getAbortURL(uploadId);
      ajaxAsyncGetRequest(url, true);
      this.abortedComplete = true;
    },
    sendFileToServer : function(status) {
      var self = this;
      this.aborted = false;
      this.abortedComplete = false;
      status.jqXHR = $.ajax({
        xhr : function() {
          var xhrobj = $.ajaxSettings.xhr();
          if (xhrobj.upload) {
            xhrobj.upload.addEventListener('progress', function(event) {
              var percent = position = event.loaded || event.position;
              var total = event.total;
              if (event.lengthComputable) {
                percent = Math.ceil(position / total * 100);
              }

              // Set progress
              self.setProgress(percent);
            }, false);
          }
          return xhrobj;
        },
        url : self.getUploadURL(status.uploadId),
        type : "POST",
        contentType : false,
        processData : false,
        cache : false,
        data : status.formData,
        success: function (data) {
          if (self.aborted) {
            return;
          }
          var uploadFinished = false;
          var uploadError = false;
          var driveName = CKEDITOR.currentInstance.config.spaceGroupId && CKEDITOR.currentInstance.config.spaceGroupId.replaceAll("/", ".") || "Personal Documents";

          var imagesDownloadFolder = CKEDITOR.currentInstance.config.imagesDownloadFolder;
          var restURL = self.getRestContext() + "/"
              + "managedocument/uploadFile/control?workspaceName=collaboration&driveName=" + driveName
              + "&currentPortal=" + eXo.env.portal.portalName + "&language="
              + eXo.env.portal.language + "&currentFolder=" + imagesDownloadFolder
              + "&uploadId=" + status.uploadId + "&fileName=" + status.name + "&action=save";
          fetch(restURL, {
            credentials: 'include',
            method: 'GET',
          }).then(response => {
            if (response.ok) {
              uploadFinished = true;
              return response.text();
            } else {
              return response.text().then(error => {
                log(`Error uploading image: ${error}`);
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
              self.displayImage(self.getImageURL(uuid));
            }
            if (uploadError) {
              self.displayWarning(self.getUploadingImageErrorMessage());
              self.abortUpload(status.uploadId);
            }
            self.triggerResizeEvent();
          });
        },
        error : function(data) {
          if(self.aborted) {
            return;
          }
          if(data.status == 200) {
            this.success(data);
          } else {
            self.displayWarning(self.getUploadingImageErrorMessage());
            self.abortUpload(status.uploadId);
          }
        }
      });
    },
    getUploadingImageErrorMessage: function() {
      return "${CKEditor.image.error.uploadingImageError}";
    },
    triggerResizeEvent : function(data) {
      var resizeEvent = document.createEvent('Event');
      resizeEvent.initEvent('resize', true, true);
      window.dispatchEvent(resizeEvent);
    }
  };

  function endsWithCaseInsensitive(stringValue, searchTerm) {
    if(!stringValue || !searchTerm) {
      return false;
    }
    return stringValue.toUpperCase().indexOf(searchTerm.toUpperCase(), stringValue.length - searchTerm.length) !== -1;
  };
  return UISelectImage;
})(UISelectFromDrives, jQuery, gtnbase)
