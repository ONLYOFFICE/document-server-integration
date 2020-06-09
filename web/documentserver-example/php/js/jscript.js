/*
 *
 * (c) Copyright Ascensio System SIA 2020
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
*/

if (typeof jQuery != "undefined") {
    jq = jQuery.noConflict();

    user = getUrlVars()["user"];
    if ("" != user && undefined != user)
        jq("#user").val(user);
    else
        user = jq("#user").val();

    jq(document).on("change", "#user", function() {
        window.location = "?user=" + jq(this).val();
    });

    jq(function () {
        jq('#fileupload').fileupload({
            dataType: 'json',
            add: function (e, data) {
                jq(".error").removeClass("error");
                jq(".done").removeClass("done");
                jq(".current").removeClass("current");
                jq("#step1").addClass("current");
                jq("#mainProgress .error-message").hide().find("span").text("");
                jq("#mainProgress").removeClass("embedded");

                jq.blockUI({
                    theme: true,
                    title: "Getting ready to load the file" + "<div class=\"dialog-close\"></div>",
                    message: jq("#mainProgress"),
                    overlayCSS: { "background-color": "#aaa" },
                    themedCSS: { width: "656px", top: "20%", left: "50%", marginLeft: "-328px" }
                });
                jq("#beginEdit, #beginView, #beginEmbedded").addClass("disable");

                data.submit();
            },
            always: function (e, data) {
                if (!jq("#mainProgress").is(":visible")) {
                    return;
                }
                var response = data.result;
                if (response.hasOwnProperty("error")) {
                    jq(".current").removeClass("current");
                    jq(".step:not(.done)").addClass("error");
                    jq("#mainProgress .error-message").show().find("span").text(response.error);
                    jq('#hiddenFileName').val("");
                    return;
                }

                jq("#hiddenFileName").val(response.filename);

                jq("#step1").addClass("done").removeClass("current");
                jq("#step2").addClass("current");

                checkConvert();
            }
        });

        initSelectors();
    });

    var timer = null;
    var checkConvert = function (fileUri) {
        if (timer != null) {
            clearTimeout(timer);
        }
        
        if (!jq("#mainProgress").is(":visible")) {
            return;
        }

        var fileName = jq("#hiddenFileName").val();
        var posExt = fileName.lastIndexOf('.');
        posExt = 0 <= posExt ? fileName.substring(posExt).trim().toLowerCase() : '';

        if (ConverExtList.indexOf(posExt) == -1) {
            loadScripts();
            return;
        }

        if (jq("#checkOriginalFormat").is(":checked")) {
            loadScripts();
            return;
        }

        timer = setTimeout(function () {
            var requestAddress = "webeditor-ajax.php"
                + "?type=convert"
                + "&filename=" + encodeURIComponent(jq("#hiddenFileName").val())
                + "&fileUri=" + encodeURIComponent(fileUri || "");

            jq.ajax({
                async: true,
                contentType: "text/xml",
                type: "get",
                url: requestAddress,
                complete: function (data) {
                    var responseText = data.responseText;
                    try {
                        var response = jq.parseJSON(responseText);
                    } catch (e)	{
                        response = { error: e };
                    }

                    if (response.error) {
                        jq(".current").removeClass("current");
                        jq(".step:not(.done)").addClass("error");
                        jq("#mainProgress .error-message").show().find("span").text(response.error);
                        jq('#hiddenFileName').val("");
                        return;
                    }

                    jq("#hiddenFileName").val(response.filename);

                    if (response.step < 100) {
                        checkConvert(response.fileUri);
                    } else {
                        loadScripts();
                    }
                }
            });
        }, 1000);
    };

    var loadScripts = function () {
        if (!jq("#mainProgress").is(":visible")) {
            return;
        }
        jq("#step2").addClass("done").removeClass("current");
        jq("#step3").addClass("current");

        if (jq("#loadScripts").is(":empty")) {
            var urlScripts = jq("#loadScripts").attr("data-docs");
            var frame = '<iframe id="iframeScripts" width=1 height=1 style="position: absolute; visibility: hidden;" ></iframe>';
            jq("#loadScripts").html(frame);
            document.getElementById("iframeScripts").onload = onloadScripts;
            jq("#loadScripts iframe").attr("src", urlScripts);
        } else {
            onloadScripts();
        }
    };

    var onloadScripts = function () {
        if (!jq("#mainProgress").is(":visible")) {
            return;
        }
        jq("#step3").addClass("done").removeClass("current");
        jq("#beginView, #beginEmbedded").removeClass("disable");

        var fileName = jq("#hiddenFileName").val();
        var posExt = fileName.lastIndexOf('.');
        posExt = 0 <= posExt ? fileName.substring(posExt).trim().toLowerCase() : '';

        if (EditedExtList.indexOf(posExt) != -1) {
            jq("#beginEdit").removeClass("disable");
        }
    };

    var initSelectors = function () {
        var langSel = jq("#language");

        function getCookie(name) {
            let matches = document.cookie.match(new RegExp(
                "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
            ));
            return matches ? decodeURIComponent(matches[1]) : null;
        }
        function setCookie(name, value) {
            document.cookie = name + "=" + value + "; expires=" + new Date(Date.now() + 1000 * 60 * 60 * 24 * 7).toUTCString(); //week
        }

        var langId = getCookie("ulang");
        if (langId) langSel.val(langId);

        langSel.on("change", function () {
            setCookie("ulang", langSel.val());
        });
    };

    jq(document).on("click", "#beginEdit:not(.disable)", function () {
        var fileId = encodeURIComponent(jq('#hiddenFileName').val());
        var url = "doceditor.php?fileID=" + fileId + "&user=" + user;
        window.open(url, "_blank");
        jq('#hiddenFileName').val("");
        jq.unblockUI();
        document.location.reload();
    });

    jq(document).on("click", "#beginView:not(.disable)", function () {
        var fileId = encodeURIComponent(jq('#hiddenFileName').val());
        var url = "doceditor.php?action=view&fileID=" + fileId + "&user=" + user;
        window.open(url, "_blank");
        jq('#hiddenFileName').val("");
        jq.unblockUI();
        document.location.reload();
    });

    jq(document).on("click", "#beginEmbedded:not(.disable)", function () {
        var fileId = encodeURIComponent(jq('#hiddenFileName').val());
        var url = "doceditor.php?type=embedded&fileID=" + fileId + "&user=" + user;

        jq("#mainProgress").addClass("embedded");
        jq("#beginEmbedded").addClass("disable");

        jq("#embeddedView").attr("src", url);
    });

    jq(document).on("click", ".reload-page", function () {
        setTimeout(function () { document.location.reload(); }, 1000);
        return true;
    });

    jq(document).on("mouseup", ".reload-page", function (event) {
        if (event.which == 2) {
            setTimeout(function () { document.location.reload(); }, 1000);
        }
        return true;
    });

    jq(document).on("click", "#cancelEdit, .dialog-close", function () {
        jq('#hiddenFileName').val("");
        jq("#embeddedView").attr("src", "");
        jq.unblockUI();
    });

    jq(document).on("click", ".delete-file", function () {
        var fileName = jq(this).attr("data");

        var requestAddress = "webeditor-ajax.php?type=delete&fileName=" + fileName;

        jq.ajax({
            async: true,
            contentType: "text/xml",
            type: "get",
            url: requestAddress,
            complete: function (data) {
                document.location.reload();
            }
        });
    });

    jq(document).on("click", "#createSample", function () {
        jq(".try-editor").each(function () {
            var href = jq(this).attr("href");
            if (jq("#createSample").is(":checked")) {
                href += "&sample=true";
            } else {
                href = href.replace("&sample=true", "");
            }
            jq(this).attr("href", href);
        });
    });
}

function getUrlVars() {
    var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for (var i = 0; i < hashes.length; i++) {
        hash = hashes[i].split('=');
        vars.push(hash[0]);
        vars[hash[0]] = hash[1];
    }
    return vars;
};
