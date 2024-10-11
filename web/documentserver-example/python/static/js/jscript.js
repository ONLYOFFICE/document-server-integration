﻿/**
 *
 * (c) Copyright Ascensio System SIA 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

var directUrl;
var formatManager;

window.onload = function () {
    fetch('formats')
        .then((response) => response.json())
        .then((data) => {
            if (data.formats) {
                let formats = [];
                data.formats.forEach(format => {
                    formats.push(new Format(
                        format.name,
                        format.type,
                        format.actions,
                        format.convert,
                        format.mime
                    ));
                });
                formatManager = new FormatManager(formats);
            }
        })
}

if (typeof jQuery !== "undefined") {
    jq = jQuery.noConflict();

    directUrl = getUrlVars()["directUrl"] == "true";

    mustReload = false;

    if (directUrl)
        jq("#directUrl").prop("checked", directUrl);
    else
        directUrl = jq("#directUrl").prop("checked");

    jq("#directUrl").change(function() {
        window.location = "?directUrl=" + jq(this).prop("checked");
    });

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
    var checkConvert = function (filePass, fileType) {
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
        var posExt = fileName.lastIndexOf(".") + 1;
        posExt = 0 <= posExt ? fileName.substring(posExt).trim().toLowerCase() : "";

        if (!formatManager.isAutoConvertible(posExt)) {
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
                data: JSON.stringify({ filename: fileName, filePass: filePass, fileExt: fileType }),
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
                            if (response.error.includes("Error conversion output format")){
                                jq("#select-file-type").removeClass("invisible");
                                jq("#step2").removeClass("current");
                                jq("#hiddenFileName").attr("placeholder",filePass);
                                return;
                            }
                            jq(".current").removeClass("current");
                            jq(".step:not(.done)").addClass("error");
                            jq("#mainProgress .error-message").show().find("span").text(response.error);
                            jq('#hiddenFileName').val("");
                            return;
                        }
                    }

                    jq("#hiddenFileName").val(response.filename);

                    if (response.step && response.step < 100) {
                        checkConvert(filePass, fileType);
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
        var posExt = fileName.lastIndexOf(".") + 1;
        posExt = 0 <= posExt ? fileName.substring(posExt).trim().toLowerCase() : "";

        if (formatManager.isEditable(posExt) || formatManager.isFillable(posExt)) {
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

    jq(document).on("click", ".file-type:not(.disable)", function () {
        const currentElement = jq(this);
        var fileType = currentElement.attr("data");
        var filePass = jq("#hiddenFileName").attr("placeholder");
        jq('.file-type').addClass(["disable", "pale"]);
        currentElement.removeClass("pale");
        checkConvert(filePass, fileType);
    });

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
        var url = UrlEditor + "?mode=edit&filename=" + fileId+ "&directUrl=" + directUrl;
        window.open(url, "_blank");
        jq("#hiddenFileName").val("");
        jq.unblockUI();
        document.location.reload();
    });

    jq(document).on("click", "#beginView:not(.disable)", function () {
        var fileId = encodeURIComponent(jq("#hiddenFileName").val());
        var url = UrlEditor + "?mode=view&filename=" + fileId+ "&directUrl=" + directUrl;
        window.open(url, "_blank");
        jq("#hiddenFileName").val("");
        jq.unblockUI();
        document.location.reload();
    });

    jq(document).on("click", "#beginEmbedded:not(.disable)", function () {
        var fileId = encodeURIComponent(jq("#hiddenFileName").val());
        var url = UrlEditor + "?type=embedded&mode=embedded&filename=" + fileId + "&directUrl=" + directUrl;

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

    jq(document).on("click", ".clear-all", function () {
        if (confirm("Delete all the files?")) {
            jq.ajax({
                async: true,
                contentType: "text/xml",
                type: "delete",
                url: "remove",
                complete: function (data) {
                    if (JSON.parse(data.responseText).success) {
                        window.location.reload(true);
                    }
                }
            });
        }
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
                jq("#info").on("touchend", function () {
                    showUserTooltip(true);
                });
            }
    } else {
        jq("#info").mouseover(function (event) {
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

    jq(".info-tooltip").mouseover(function (event) {
        var target = event.target;
        var id = target.dataset.id ? target.dataset.id : target.id;
        var tooltip = target.dataset.tooltip;

        jq("<div class='tooltip'>" + tooltip + "<div class='arrow'></div></div>").appendTo("body");

        var top = jq("#" + id).offset().top + jq("#" + id).outerHeight() / 2 - jq("div.tooltip").outerHeight() / 2;
        var left = jq("#" + id).offset().left + jq("#" + id).outerWidth() + 20;
        jq("div.tooltip").css({"top": top, "left": left});
    }).mouseout(function () {
        jq("div.tooltip").remove();
    });
}

function toggleSidePanel(event) {
    event.preventDefault();
    let sidePanel = document.querySelector(".left-panel");
    let body = document.querySelector("body");
    if (sidePanel.classList.contains("active")) {
        sidePanel.classList.remove("active");
        body.classList.remove("menu-open");
    } else {
        sidePanel.classList.add("active")
        body.classList.add("menu-open");
    }
}

function toggleUserDescr(event) {
    let list = event.currentTarget.querySelector("ul");
    let cursor = window.getComputedStyle(event.currentTarget).getPropertyValue("cursor");

    if (cursor === "pointer") {
        if (list.classList.contains("active")) list.classList.remove("active");
        else list.classList.add("active");
    }
}