(function ($, _) {
    var CommentForm = function() {}
    //
    CommentForm.prototype.init = function(placeholder) {
        var peopleSearchCached = {};
        var lastNoResultQuery = false;

        $('body').suggester('addProvider', 'exo:people', function(query, callback) {
            if (lastNoResultQuery && query.length > lastNoResultQuery.length) {
                if (query.substr(0, lastNoResultQuery.length) === lastNoResultQuery) {
                    callback.call(this, []);
                    return;
                }
            }
            if (peopleSearchCached[query]) {
                callback.call(this, peopleSearchCached[query]);
            } else {
                var url = window.location.protocol + '//' + window.location.host + '/' + eXo.social.portal.rest + '/social/people/getprofile/data.json?search=' + query;
                $.getJSON(url, function(responseData) {
                    responseData = _.filter(responseData, function(item) {
                        return item.name.toLowerCase().indexOf(query.toLowerCase()) > -1;
                    });

                    var result = [];
                    for (var i = 0; i < responseData.length; i++) {
                        var d = responseData[i];
                        var item = {
                            uid: d.id.substr(1),
                            name: d.name,
                            avatar: d.avatar
                        };
                        result.push(item);
                    }

                    peopleSearchCached[query] = result;
                    if (peopleSearchCached[query].length == 0) {
                        lastNoResultQuery = query;
                    } else {
                        lastNoResultQuery = false;
                    }
                    callback.call(this, peopleSearchCached[query]);
                });
            }
        });
        var MAX_LENGTH = 2000;
        // TODO this line is mandatory when a custom skin is defined, it should not be mandatory
        var extraPlugins = 'simpleLink,simpleImage,suggester,hideBottomToolbar';
        if ($(window).width() > $(window).height() && $(window).width() < 768) {
            // Disable suggester on smart-phone landscape
            extraPlugins = 'simpleLink,simpleImage';
        }
        CKEDITOR.basePath = '/commons-extension/ckeditor/';
        $('textarea#comment').ckeditor({
            //TODO we should ensure adding external plugins for link and image
            customConfig: '/commons-extension/ckeditorCustom/config.js',
            extraPlugins: extraPlugins,
            placeholder: placeholder,
            typeOfRelation: 'mention_comment',
            on: {
                instanceReady: function (evt) {
                    // Hide the editor top bar.
                    var editor = CKEDITOR.instances["comment"];
                    if (editor.element.$.defaultValue != "") {
                        var comment = editor.element.$.defaultValue;
                        var i = 0, length = comment.length;
                        for (i; i < length; i++) {
                            comment = comment.replace("\\\"", "\"");
                        }
                        if (comment.indexOf("class=\"pull-left\"") != -1) {
                            comment = comment.replace("class=\"pull-left\"", "style=\"float:left\" class=\"pull-left\"")
                        } else if (comment.indexOf("class=\"pull-right\"") != -1) {
                            comment = comment.replace("class=\"pull-right\"", "style=\"float:right\" class=\"pull-right\"")
                        }
                        $(CKEDITOR.instances["comment"].document.getBody().$).html(comment);
                    }
                },
                change: function( evt) {
                    var newData = evt.editor.getData();
                    var pureText = newData? newData.replace(/<[^>]*>/g, "").replace(/&nbsp;/g,"").trim() : "";

                    if (pureText.length <= MAX_LENGTH) {
                        evt.editor.getCommand('simpleImage').enable();
                    } else {
                        evt.editor.getCommand('simpleImage').disable();
                    }
                },
                key: function( evt) {
                    var newData = evt.editor.getData();
                    var pureText = newData? newData.replace(/<[^>]*>/g, "").replace(/&nbsp;/g,"").trim() : "";
                    if (pureText.length > MAX_LENGTH) {
                        if ([8, 46, 33, 34, 35, 36, 37,38,39,40].indexOf(evt.data.keyCode) < 0) {
                            evt.cancel();
                        }
                    }
                }

            }
        });
    };

    CommentForm.prototype.editComment = function() {
        var editor = CKEDITOR.instances["comment"];
        //editor.setData(editor.element.$.defaultValue.replace("↵",""));
        editor.insertHtml(editor.element.$.defaultValue);
        //editor.setData("<p><img class=\"pull-left\" src=\"https://exo.mybalsamiq.com/mockups/4569881.png?key&#61;249fdfb33bdb917511880603ded31e8ce185539b&amp;lastUpdate&#61;1469636847000#\" title=\"a\" />sdfs</p>↵↵<p>sdf</p>↵");
        //$(CKEDITOR.instances["comment"].document.getBody().$).html('');
    };


    eXo.ecm.CommentForm = new CommentForm();
    return {
        CommentForm : eXo.ecm.CommentForm
    };

})($, mentions._);