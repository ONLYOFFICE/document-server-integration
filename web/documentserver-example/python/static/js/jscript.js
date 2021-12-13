/*
 *
 * (c) Copyright Ascensio System SIA 2021
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

if (typeof jQuery !== "undefined") {
    jq = jQuery.noConflict();

    mustReload = false;

    jq(function () {
        jq("#fileupload").fileupload({
            dataType: "json",
            add: function (e, data) {
                jq(".error").removeClass("error");
                jq(".done").removeClass("done");
                jq(".current").removeClass("current");
                jq("#step1").addClass("current");
                jq("#mainProgress .error-message").hide().find("span").text("");
                jq("#blockPassword").hide();
                jq("#mainProgress").removeClass("embedded");
                jq("#uploadFileName").text("");

                jq.blockUI({
                    theme: true,
                    title: "File upload" + "<div class=\"dialog-close\"></div>",
                    message: jq("#mainProgress"),
                    overlayCSS: { "background-color": "#aaa" },
                    themedCSS: { width: "539px", top: "20%", left: "50%", marginLeft: "-269px" }
                });
                jq("#beginEdit, #beginView, #beginEmbedded").addClass("disable");

                data.submit();
            },
            always: function (e, data) {
                if (!jq("#mainProgress").is(":visible")) {
                    return;
                }
                var response = data.result;
                if (response.error) {
                    jq(".current").removeClass("current");
                    jq(".step:not(.done)").addClass("error");
                    jq("#mainProgress .error-message").show().find("span").text(response.error);
                    jq('#hiddenFileName').val("");
                    return;
                }

                jq("#hiddenFileName").val(response.filename);
                jq("#uploadFileName").text(response.filename);
                jq("#uploadFileName").addClass(response.documentType);

                mustReload = true;

                jq("#step1").addClass("done").removeClass("current");

                checkConvert();
            }
        });

        initSelectors();
    });

    var timer = null;
    var checkConvert = function (filePass) {
        filePass = filePass ? filePass : null;
        if (timer !== null) {
            clearTimeout(timer);
        }

        if (!jq("#mainProgress").is(":visible")) {
            return;
        }
        jq("#step2").addClass("current");
        jq("#filePass").val("");

        var fileName = jq("#hiddenFileName").val();
        var posExt = fileName.lastIndexOf(".");
        posExt = 0 <= posExt ? fileName.substring(posExt).trim().toLowerCase() : "";

        if (ConverExtList.indexOf(posExt) === -1) {
            jq("#step2").addClass("done").removeClass("current");
            loadScripts();
            return;
        }

        timer = setTimeout(function () {
            jq.ajax({
                async: true,
                contentType: "text/xml",
                type: "post",
                dataType: "json",
                data: JSON.stringify({ filename: fileName, filePass: filePass }),
                url: UrlConverter,
                complete: function (data) {
                    var responseText = data.responseText;
                    var response = jq.parseJSON(responseText);
                    if (response.error) {
                        if (response.error.includes("Incorrect password")) {
                            jq(".current").removeClass("current");
                            jq("#step2").addClass("error");
                            jq("#blockPassword").show();
                            if (filePass) {
                                jq("#filePass").addClass("errorInput");
                                jq(".errorPass").text("The password is incorrect, please try again.");
                            }
                            return;
                        } else {
                            jq(".current").removeClass("current");
                            jq(".step:not(.done)").addClass("error");
                            jq("#mainProgress .error-message").show().find("span").text(response.error);
                            jq('#hiddenFileName').val("");
                            return;
                        }
                    }

                    jq("#hiddenFileName").val(response.filename);

                    if (response.step && response.step < 100) {
                        checkConvert(filePass);
                    } else {
                        jq("#step2").addClass("done").removeClass("current");
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
        jq("#step3").addClass("current");

        if (jq("#loadScripts").is(":empty")) {
            var urlScripts = jq("#loadScripts").attr("data-docs");
            var frame = "<iframe id=\"iframeScripts\" width=1 height=1 style=\"position: absolute; visibility: hidden;\" ></iframe>";
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
        var posExt = fileName.lastIndexOf(".");
        posExt = 0 <= posExt ? fileName.substring(posExt).trim().toLowerCase() : "";

        if (EditedExtList.indexOf(posExt) !== -1 || FillExtList.indexOf(posExt) !== -1) {
            jq("#beginEdit").removeClass("disable");
        }
    };

    var initSelectors = function () {
        var userSel = jq("#user");
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

        var userId = getCookie("uid");
        if (userId) userSel.val(userId);
        var langId = getCookie("ulang");
        if (langId) langSel.val(langId);

        userSel.on("change", function () {
            setCookie("uid", userSel.val());
        });
        langSel.on("change", function () {
            setCookie("ulang", langSel.val());
        });
    };

    jq(document).on("click", "#enterPass", function () {
        var filePass = jq("#filePass").val();
        if (filePass) {
            jq("#step2").removeClass("error");
            jq("#blockPassword").hide();
            checkConvert(filePass);
        } else {
            jq("#filePass").addClass("errorInput");
            jq(".errorPass").text("Password can't be blank.");
        }
    });

    jq(document).on("click", "#skipPass", function () {
        jq("#blockPassword").hide();
        loadScripts();
    });

    jq(document).on("click", "#beginEdit:not(.disable)", function () {
        var fileId = encodeURIComponent(jq("#hiddenFileName").val());
        var url = UrlEditor + "?mode=edit&filename=" + fileId;
        window.open(url, "_blank");
        jq("#hiddenFileName").val("");
        jq.unblockUI();
    });

    jq(document).on("click", "#beginView:not(.disable)", function () {
        var fileId = encodeURIComponent(jq("#hiddenFileName").val());
        var url = UrlEditor + "?mode=view&filename=" + fileId;
        window.open(url, "_blank");
        jq("#hiddenFileName").val("");
        jq.unblockUI();
    });

    jq(document).on("click", "#beginEmbedded:not(.disable)", function () {
        var fileId = encodeURIComponent(jq("#hiddenFileName").val());
        var url = UrlEditor + "?type=embedded&mode=embedded&filename=" + fileId;

        jq("#mainProgress").addClass("embedded");
        jq("#beginEmbedded").addClass("disable");

        jq("#uploadSteps").after('<iframe id="embeddedView" src="' + url + '" height="345px" width="432px" frameborder="0" scrolling="no" allowtransparency></iframe>');
    });

    jq(document).on("click", "#cancelEdit, .dialog-close", function () {
        jq('#hiddenFileName').val("");
        jq("#embeddedView").attr("src", "");
        jq.unblockUI();
        if (mustReload) {
            document.location.reload();
        }
    });

    jq(document).on("click", ".try-editor", function (e) {
        var url = "create?fileType=" + e.target.attributes["data-type"].value;
        if (jq("#createSample").is(":checked")) {
            url += "&sample=true";
        }
        var w = window.open(url, "_blank");
        w.onload = function () {
            window.location.reload();
        }
    });

    jq(document).on("click", ".delete-file", function () {
        var requestAddress = "remove"
            + "?filename=" + encodeURIComponent(jq(this).attr("data-filename"));

        jq.ajax({
            async: true,
            contentType: "text/xml",
            url: requestAddress,
            complete: function (data) {
                document.location.reload();
            }
        });
    });

        function showUserTooltip (isMobile) {
        if ( jq("div#portal-info").is(":hidden") ) {
            jq("div#portal-info").show();
            jq("div.stored-list").hide();
        } else if (isMobile && jq("div#portal-info").is(":visible")) {
            jq("div#portal-info").hide();
            jq("div.stored-list").show();
        }
    };

    var fileList = jq("tr.tableRow");

    var mouseIsOverTooltip = false;
    var hideTooltipTimeout = null;
    if (/android|avantgo|playbook|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od|ad)|iris|kindle|lge |maemo|midp|mmp|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\|plucker|pocket|psp|symbian|treo|up\\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i
        .test(navigator.userAgent)) {
            if (fileList.length > 0) {
                if (hideTooltipTimeout != null) {
                    clearTimeout(hideTooltipTimeout);
                }
                jq(".info").on("touchend", function () {
                    showUserTooltip(true);
                });
            }
    } else {
        jq(".info").mouseover(function (event) {
            if (fileList.length > 0) {
                if (hideTooltipTimeout != null) {
                    clearTimeout(hideTooltipTimeout);
                }
                showUserTooltip(false);

                jq("div#portal-info").mouseenter(function () {
                    mouseIsOverTooltip = true;
                }).mouseleave(function () {
                    mouseIsOverTooltip = false;
                    jq("div.stored-list").show();
                    jq("div#portal-info").hide();
                })
            }
        }).mouseleave(function () {
            hideTooltipTimeout = setTimeout(function () {
                if (mouseIsOverTooltip == false && fileList.length > 0) {
                    jq("div.stored-list").show();
                    jq("div#portal-info").hide();
                }
            }, 500);
        });
    }
}