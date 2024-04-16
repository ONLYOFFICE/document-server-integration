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

var language;
var userid;
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

if (typeof jQuery != "undefined") {
    jq = jQuery.noConflict();

    userid = getUrlVars()["userid"];
    language = getUrlVars()["lang"];

    mustReload = false;

    if ("" != language && undefined != language)
        jq("#language").val(language);
    else
        language = jq("#language").val();

    if ("" != userid && undefined != userid)
        jq("#user").val(userid);
    else
        userid = jq("#user").val();

    jq(function () {
        jq('#fileupload').fileupload({
            dataType: 'json',
            add: function (e, data) {
                if (jq("#mainProgress").is(":visible")) {
                    return;
                }
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
                if (!response || response.error) {
                    jq(".current").removeClass("current");
                    jq(".step:not(.done)").addClass("error");
                    jq("#mainProgress .error-message").show().find("span").text(response ? response.error : "Undefined error");
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
    });

    var timer = null;
    var checkConvert = function (filePass, fileType) {
        filePass = filePass ? filePass : null;
        if (timer != null) {
            clearTimeout(timer);
        }

        if (!jq("#mainProgress").is(":visible")) {
            return;
        }
        jq("#step2").addClass("current");
        jq("#filePass").val("");

        var fileName = jq("#hiddenFileName").val();
        var posExt = fileName.lastIndexOf('.') + 1;
        posExt = 0 <= posExt ? fileName.substring(posExt).trim().toLowerCase() : '';

        if (!formatManager.isAutoConvertible(posExt)) {
            jq("#step2").addClass("done").removeClass("current");
            loadScripts();
            return;
        }

        timer = setTimeout(function () {
            jq.ajaxSetup({ cache: false });
            jq.ajax({
                async: true,
                type: "post",
                dataType: "json",
                data: {filename: fileName, filePass: filePass, lang: language, fileExt: fileType},
                url: UrlConverter,
                complete: function (data) {
                    var responseText = data.responseText;
                    try {
                        var response = jq.parseJSON(responseText);
                    } catch (e)	{
                        response = { error: e };
                    }
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
                            if (response.error.includes("-9")){
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

                    if (typeof response.step != "undefined" && response.step < 100) {
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
        var posExt = fileName.lastIndexOf('.') + 1;
        posExt = 0 <= posExt ? fileName.substring(posExt).trim().toLowerCase() : '';

        if (formatManager.isEditable(posExt) || formatManager.isFillable(posExt)) {
            jq("#beginEdit").removeClass("disable");
        }
    };

    jq(document).on("click", ".file-type:not(.disable)", function () {
        const currentElement = jq(this);
        var fileType = currentElement.attr("data");
        var filePass = jq("#hiddenFileName").attr("placeholder");
        jq(".file-type").addClass(["disable", "pale"]);
        currentElement.removeClass("pale");
        checkConvert(filePass, fileType);
    });

    jq(document).on("click", "#enterPass", function () {
        var pass = jq("#filePass").val();
        if (pass) {
            jq("#step2").removeClass("error");
            jq("#blockPassword").hide();
            checkConvert(pass);
        } else {
            jq("#filePass").addClass("errorInput");
            jq(".errorPass").text("Password can't be blank.");
        }
    });

    jq(document).on("click", ".action-link", function (e) {
        e.preventDefault();
        let url = this.href + collectParams(true);
        let target = null;

        if (e.target.hasAttribute("target")) {
            target = e.target.getAttribute("target");
        } else if (e.target.parentNode.hasAttribute("target")) {
            target = e.target.parentNode.getAttribute("target");
        }

        target !== null ? window.open(url, target) :  window.location = url;
    });

    jq(document).on("click", "#skipPass", function () {
        jq("#blockPassword").hide();
        loadScripts();
    });

    jq(document).on("click", "#beginEdit:not(.disable)", function () {
        var fileId = encodeURIComponent(jq('#hiddenFileName').val());
        if (UrlEditor == "wopi-action"){
            var url = UrlEditor + "/" + fileId + "?action=edit" + collectParams(true);
        }else{
            var url = UrlEditor + "?fileName=" + fileId + collectParams(true);
        }
        window.open(url, "_blank");
        jq('#hiddenFileName').val("");
        jq.unblockUI();
        window.location = collectParams();
    });

    jq(document).on("click", "#beginView:not(.disable)", function () {
        var fileId = encodeURIComponent(jq('#hiddenFileName').val());
        if (UrlEditor == "wopi-action"){
            var url = UrlEditor + "/" + fileId + "?action=view" + collectParams(true);
        }else{
            var url = UrlEditor + "?mode=view&fileName=" + fileId + collectParams(true);
        }
        window.open(url, "_blank");
        jq('#hiddenFileName').val("");
        jq.unblockUI();
        window.location = collectParams();
    });

    jq(document).on("click", "#beginEmbedded:not(.disable)", function () {
        var fileId = encodeURIComponent(jq('#hiddenFileName').val());
        var url = UrlEditor + "?type=embedded&fileName=" + fileId + collectParams(true);

        jq("#mainProgress").addClass("embedded");
        jq("#beginEmbedded").addClass("disable");

        jq("#uploadSteps").after('<iframe id="embeddedView" src="' + url + '" height="345px" width="432px" frameborder="0" scrolling="no" allowtransparency></iframe>');
    });

    jq(document).on("click", "#beginEditConverted:not(.disable)", function () {
        var fileId = encodeURIComponent(jq('#hiddenFileName').attr("data"));
        if (UrlEditor == "wopi-action"){
            var url = UrlEditor + "/" + fileId + "?action=edit" + collectParams(true);
        }else{
            var url = UrlEditor + "?fileName=" + fileId + collectParams(true);
        }
        window.open(url, "_blank");
        jq('#hiddenFileName').val("");
        jq.unblockUI();
        window.location = collectParams();
    });

    jq(document).on("click", "#beginViewConverted:not(.disable)", function () {
        var fileId = encodeURIComponent(jq('#hiddenFileName').attr("data"));
        if (UrlEditor == "wopi-action"){
            var url = UrlEditor + "/" + fileId + "?action=view" + collectParams(true);
        }else{
            var url = UrlEditor + "?mode=view&fileName=" + fileId + collectParams(true);
        }
        window.open(url, "_blank");
        jq('#hiddenFileName').val("");
        jq.unblockUI();
        window.location = collectParams();
    });

    jq(document).on("click", "#downloadConverted:not(.disable)", function () {
        var fileId = jq('#hiddenFileName').attr("data");
        if (jq("#downloadConverted").attr("data") == "fromConverter") window.location.assign(fileId);
        else window.location.href = "download?fileName=" + encodeURIComponent(fileId);
    });

    jq(document).on("click", ".reload-page", function () {
        setTimeout(function () { window.location = collectParams(); }, 1000);
        return true;
    });

    jq(document).on("mouseup", ".reload-page", function (event) {
        if (event.which == 2) {
            setTimeout(function () { window.location = collectParams(); }, 1000);
        }
        return true;
    });

    jq(document).on("click", "#cancelEdit, .dialog-close", function () {
        jq('#hiddenFileName').val("");
        jq("#embeddedView").remove();
        jq.unblockUI();
        if (mustReload) {
            window.location = collectParams();
        }
    });

    jq(document).on("click", ".convert-file", function () {
        const currentElement = jq(this);
        var fileName = currentElement.attr("data");
        var type = currentElement.attr("data-type");

        jq.blockUI({
            theme: true,
            title: "Converting file" + "<div class=\"dialog-close\"></div>",
            message: jq("#convertingProgress"),
            overlayCSS: { "background-color": "#aaa" },
            themedCSS: { width: "539px", top: "20%", left: "50%", marginLeft: "-269px" }
        });

        jq("#convertFileName").text(decodeURIComponent(fileName));
        jq("#convertFileName").removeClass("word slide cell");
        jq("#convertFileName").addClass(type);
        jq("#convTypes").empty();

        let format = formatManager.findByExtension(fileName.split('.').pop());
        if (format) {
            format.convert.forEach(ext => {
                jq("#convTypes").append(jq(`<td name="convertingTypeButton" id="wordTo${ext}" class="button hoar" data="${ext}">${ext}</td>`));
            });
        }

        jq("#hiddenFileName").val(fileName);
        jq("#convertStep1").addClass("done");
        jq("#convertStep2").addClass("waiting");
    });

    jq(document).on("click", "td[name='convertingTypeButton']:not(.disable, .orange)", function () {
        const currentElement = jq(this);
        let id = currentElement[0].id;
        let fileExt = jq(`#${id}`).attr("data");
        jq(`#${id}`).addClass("orange");
        jq("td[name='convertingTypeButton']").addClass("disable");
        jq("#convertStep2").removeClass("waiting").removeClass("done").addClass("current");
        jq("#convertStep2").text('2. File conversion');
        jq("#convert-descr").removeClass("disable");
        jq("#convertPercent").text("0 %");
        jq("#hiddenFileName").attr("placeholder",fileExt);
        jq("#downloadConverted").addClass("disable");
        jq("#beginEditConverted").addClass("disable");
        jq("#beginViewConverted").addClass("disable");
        mustReload = true;
        
        convertFile();
    });

    function convertFile (filePass) {
        let fileName = decodeURIComponent(jq("#hiddenFileName").val());
        let fileExt = jq("#hiddenFileName").attr("placeholder");

        if (timer != null) {
            clearTimeout(timer);
        }
        timer = setTimeout(function () {
            jq.ajaxSetup({ cache: false });
            jq.ajax({
                async: true,
                type: "post",
                dataType: "json",
                data: {filename: fileName, filePass: filePass, lang: language, fileExt: fileExt, keepOriginal: true},
                url: UrlConverter,
                complete: function (data) {
                    try {
                        var response = jq.parseJSON(data.responseText);
                    } catch (e)	{
                        response = { error: e };
                    }
                    if(response.step !==100) {
                        jq("#convertPercent").text(`${response.step} %`);
                        convertFile();
                    } else {
                        jq("#convertPercent").text(`${response.step} %`);
                        jq("#convertStep2").removeClass("current").addClass("done");
                        jq("#convertStep2").text(`2. File conversion to ${fileExt}`);
                        jq("#downloadConverted").removeClass("disable");
                        if (response.error !== "FileTypeIsNotSupported") {
                            jq("#hiddenFileName").attr("data",response.filename);
                            jq("#beginEditConverted").removeClass("disable");
                            jq("#beginViewConverted").removeClass("disable");
                            jq("#downloadConverted").attr("data","fromStorage");
                        } else {
                            let newFilename = fileName.split('.').slice(0,-1).join('.')
                            jq("#hiddenFileName").attr("data",response.filename.split("&filename=download").join(`&filename=${newFilename}`));
                            jq("#downloadConverted").attr("data","fromConverter");
                        }
                        jq("td[name='convertingTypeButton']").removeClass("disable orange");
                    }
                }
            });
        }, 1000);
    }

    jq(document).on("click", ".delete-file", function () {
        const currentElement = jq(this);
        var fileName = currentElement.attr("data");

        var requestAddress = "file?filename=" + fileName;

        jq.ajax({
            async: true,
            contentType: "text/xml",
            type: "delete",
            url: requestAddress,
            complete: function (data) {
                if (JSON.parse(data.responseText).success) {
                    const parentRow = currentElement.parents('tr')[0];
                    if (parentRow) {
                        jq(parentRow).remove();
                    }
                    const remainingRows = jq('tr.tableRow');
                    if (remainingRows.length === 0) {
                        window.location = collectParams();
                    }
                }
            }
        });
    });

    jq(document).on("click", ".clear-all", function () {
        if (confirm("Delete all the files?")) {
            jq.ajax({
                async: true,
                contentType: "text/xml",
                type: "delete",
                url: "file",
                complete: function (data) {
                    if (JSON.parse(data.responseText).success) {
                        window.location = collectParams();
                    }
                }
            });
        }
    });

    jq("#createSample").click(function () {
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

function collectParams(startParams) {
    let paramsObjects = Array.prototype.slice.call(document.getElementsByClassName('collectable'));
    let params = [];
    let startChar = startParams ? "&" : "?";
    paramsObjects.forEach( function (element) {
        if (element.name) {
            switch (element.type) {
                case "select-one":
                case "text":
                    if (element.value) {
                        params.push(element.name + "=" + element.value);
                    }
                    break;
                case "checkbox":
                    params.push(element.name + "=" + element.checked);
                    break;
                case "radio":
                    if (element.checked) {
                        params.push(element.name + "=" + element.value);
                    }
                    break;
                default:
            }
        }
    });
    return startChar + params.join("&");
}
