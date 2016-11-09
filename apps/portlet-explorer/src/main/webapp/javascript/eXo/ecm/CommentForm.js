(function ($, _) {
    var CommentForm = function() {}
    //
    CommentForm.prototype.init = function(placeholder) {

        var MAX_LENGTH = 2000;
        // TODO this line is mandatory when a custom skin is defined, it should not be mandatory
        CKEDITOR.basePath = '/commons-extension/ckeditor/';
        $('textarea#comment').ckeditor({
            //TODO we should ensure adding external plugins for link and image
            customConfig: '/commons-extension/ckeditorCustom/config.js',
            placeholder: placeholder,
            on: {
                instanceReady: function (evt) {
                    // Hide the editor top bar.
                    $('#' + evt.editor.id + '_bottom').removeClass('cke_bottom_visible');
                },
                focus : function ( evt ) {
                    evt.editor.execCommand('autogrow');
                    var $content = $('#' + evt.editor.id + '_contents');
                    var contentHeight = $content.height();
                    var $ckeBottom = $('#' + evt.editor.id + '_bottom');
                    $ckeBottom.animate({
                        height: "39"
                    }, {
                        step: function(number, tween) {
                            $content.height(contentHeight - number);
                            if (number >= 9) {
                                $ckeBottom.addClass('cke_bottom_visible');
                            }
                        }
                    });
                },
                blur: function (evt) {
                    // Hide the editor toolbar
                    $('#' + evt.editor.id + '_contents').css('height', $('#' + evt.editor.id + '_contents').height() + 39);
                    $('#' + evt.editor.id + '_bottom').css('height', '0px');
                    $('#' + evt.editor.id + '_bottom').removeClass('cke_bottom_visible');

                },
                change: function (evt) {
                    var newData = evt.editor.getData();
                    if (newData && newData.length > 0) {
                        var elId = this.element.$.id.replace('CommentTextarea', '');
                        $('#CommentButton' + elId).removeAttr("disabled");
                    } else {
                        $('#CommentButton' + elId).prop("disabled", true);
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
    }
    
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
    eXo.ecm.CommentForm = new CommentForm();
    return {
        CommentForm : eXo.ecm.CommentForm
    };

})($, mentions._);