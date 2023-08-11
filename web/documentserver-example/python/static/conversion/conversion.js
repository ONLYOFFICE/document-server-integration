//
// (c) Copyright Ascensio System SIA 2023
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

// @ts-check
// https://github.com/microsoft/TypeScript/issues/13206
// https://github.com/microsoft/TypeScript/issues/38985

// click on button
// show popup
// loading on select step
// fetch formats
// show select step

// mount
// unmount <- removeEventListener

;(function () {
"use strict"

/**
 * @class
 */
function Conversion() {
  /** @type {Element | null} */
  this.element = null
}

/**
 * @param {Element} root
 */
Conversion.prototype.connectedCallback = function connectedCallback(root) {
  this.element = root.querySelector(".conversion")
}




Conversion.prototype.mount = function mount() {
  var parameters = {
    fileName: "document.docx",
    conversionTimeout: 1000
  }

  this.selectStep.start()

  var formatsParameters = {
    fileName: parameters.fileName
  }
  this.fetchFormats(formatsParameters, function (error, formats) {
    if (error) {
      this.selectStep.fail()
      this.fault.show(error)
      return
    }

    this.selectStep.abort()

    this.options.setup(formats)
    // this!!!!!!!
    this.options.onSelect = function onSelect(error, format) {}

    this.addOptionsClick(options, function (error, format) {
      if (error) {
        this.selectStep.fail()
        this.fault.show(error)
        return
      }

      this.selectStep.success()
      this.conversionStep.start()

      var conversionParameters = {
        timeout: parameters.conversionTimeout
      }
      this.fetchConversion(conversionParameters, function (error, data, cancel) {
        if (error) {
          cancel()
          this.conversionStep.fail()
          this.fault.show(error)
          return
        }

        this.progress.setup(data.percent)

        if (data.percent !== 100) {
          return
        }

        cancel()

        if (!data.fileUrl) {
          this.conversionStep.fail()
          this.fault.show(new ConversionError2())
          return
        }

        this.conversionStep.success()

        this.actions.setupDownload(data.fileUrl)
        this.actions.enable()
      })
    })
  })
}





















/**
 * @callback ConversionOptionsOnSelect
 * @param {ConversionOptions} this
 * @param {string} format
 * @returns {void}
 */

/**
 * @class
 */
function ConversionOptions() {
  /** @type {Element | null} */
  this.element = null

  /** @type {ConversionOptionsOnSelect | undefined} */
  this.onSelect = undefined
}

/**
 * @param {Element} root
 */
ConversionOptions.prototype.connectedCallback = function connectedCallback(root) {
  var element = root.querySelector(".conversion-options")
  if (!element) {
    return
  }

  element.addEventListener("click", this.handleSelect.bind(this))
  this.element = element
}

/**
 * @param {string[]} values
 */
ConversionOptions.prototype.setValues = function setValues(values) {
  var element = this.element
  if (!element) {
    return
  }

  for (var index = 0; index < values.length; index += 1) {
    var value = values[index]
    var option = ConversionOptions.createOption(value)
    element.appendChild(option)
  }
}

/**
 * @param {string} value
 * @returns {Element}
 */
ConversionOptions.createOption = function createOption(value) {
  var control = this.createControl(value)
  var indicator = this.createIndicator(value)
  var option = document.createElement("label")
  option.setAttribute("class", "conversion-options__option")
  option.appendChild(control)
  option.appendChild(indicator)
  return option
}

/**
 * @param {string} value
 * @returns {Element}
 */
ConversionOptions.createControl = function createControl(value) {
  var control = document.createElement("input")
  control.setAttribute("class", "conversion-options__control")
  control.setAttribute("type", "radio")
  control.setAttribute("name", "format")
  control.setAttribute("value", value)
  return control
}

/**
 * @param {Element} element
 * @returns {boolean}
 */
ConversionOptions.isIndicator = function isIndicator(element) {
  var classes = element.getAttribute("class")
  return (
    !!classes &&
    classes.indexOf("conversion-options__indicator") !== -1
  )
}

/**
 * @param {string} value
 * @returns {Element}
 */
ConversionOptions.createIndicator = function createIndicator(value) {
  var indicator = document.createElement("span")
  indicator.setAttribute("class", "conversion-options__indicator")
  indicator.textContent = value
  return indicator
}

/**
 * @param {MouseEvent} event
 */
ConversionOptions.prototype.handleSelect = function handleSelect(event) {
  event.preventDefault()

  var element = this.element
  if (!element) {
    return
  }

  if (
    !event.target ||
    !(event.target instanceof Element) ||
    !ConversionOptions.isIndicator(event.target)
  ) {
    return
  }

  var control = event.target.previousSibling
  if (
    !control ||
    !(control instanceof Element) ||
    !control.hasAttribute("checked") ||
    control.hasAttribute("required")
  ) {
    return
  }

  var controls = element.querySelectorAll(".conversion-options__control")
  for (var index = 0; index < controls.length; index += 1) {
    var sub = controls[index]
    if (sub.hasAttribute("checked")) {
      continue
    }
    sub.setAttribute("disabled", "")
  }

  control.setAttribute("required", "")

  var onSelect = this.onSelect
  if (!onSelect) {
    return
  }

  var format = control.getAttribute("value")
  if (!format) {
    return
  }

  // @ts-ignore
  onSelect(format)
}

/**
 * @class
 */
function ConversionProgress() {
  /** @type {Element | null} */
  this.element = null
}

/**
 * @param {Element} root
 */
ConversionProgress.prototype.connectedCallback = function connectedCallback(root) {
  this.element = root.querySelector(".conversion-progress")
}

ConversionProgress.prototype.reset = function reset() {
  this.setValue(0)
}

/**
 * @param {number} value
 */
ConversionProgress.prototype.setValue = function setValue(value) {
  var element = this.element
  if (!element) {
    return
  }

  var bar = element.querySelector("progress")
  if (!bar) {
    return
  }

  var indicator = element.querySelector(".conversion-progress__indicator")
  if (!indicator) {
    return
  }

  bar.setAttribute("value", String(value))
  bar.textContent = value + "%"
  indicator.textContent = value + "%"
}

/**
 * @class
 */
function ConversionError() {
  /** @type {Element | null} */
  this.element = null
}

ConversionError.prototype.connectedCallback = function connectedCallback(root) {
  this.element = root.querySelector(".conversion-error")
}

/**
 * @param {Error} error
 */
ConversionError.prototype.show = function show(error) {
  var element = this.element
  if (!element) {
    return
  }

  var type = element.querySelector(".conversion-error__type")
  if (!type) {
    return
  }

  var body = type.nextSibling
  if (!body) {
    return
  }

  type.textContent = error.name
  body.textContent = error.message
  element.removeAttribute("hidden")
}

ConversionError.prototype.hide = function hide() {
  var element = this.element
  if (!element) {
    return
  }

  var type = element.querySelector(".conversion-error__type")
  if (!type) {
    return
  }

  var body = type.nextSibling
  if (!body) {
    return
  }

  type.textContent = null
  body.textContent = null
  element.setAttribute("hidden", "")
}

/**
 * @class
 */
function ConversionActions() {
  /** @type {Element | null} */
  this.element = null
}

/**
 * @param {Element} root
 */
ConversionActions.prototype.connectedCallback = function connectedCallback(root) {
  this.element = root.querySelector(".conversion-actions")
}

ConversionActions.prototype.enable = function enable() {
  this.enableNth(1)
  this.enableNth(2)
  this.enableNth(3)
}

/**
 * @param {number} nth
 */
ConversionActions.prototype.enableNth = function enableNth(nth) {
  var element = this.element
  if (!element) {
    return
  }

  var action = element.querySelector(
    ".conversion-actions__action:nth-of-type(" + String(nth) + ")"
  )
  if (!action) {
    return
  }

  action.removeAttribute("disabled")
}

ConversionActions.prototype.disable = function disable() {
  this.disableNth(1)
  this.disableNth(2)
  this.disableNth(3)
}

/**
 * @param {number} nth
 */
ConversionActions.prototype.disableNth = function disableNth(nth) {
  var element = this.element
  if (!element) {
    return
  }

  var action = element.querySelector(
    ".conversion-actions__action:nth-of-type(" + String(nth) + ")"
  )
  if (!action) {
    return
  }

  action.setAttribute("disabled", "")
}

/**
 * @param {string} url
 */
ConversionActions.prototype.setDownload = function setDownload(url) {
  this.setNth(1, url)
}

/**
 * @param {string} url
 */
ConversionActions.prototype.setView = function setView(url) {
  this.setNth(2, url)
}

/**
 * @param {string} url
 */
ConversionActions.prototype.setEdit = function setEdit(url) {
  this.setNth(3, url)
}

/**
 * @param {number} nth
 * @param {string} url
 */
ConversionActions.prototype.setNth = function setNth(nth, url) {
  var element = this.element
  if (!element) {
    return
  }

  var action = element.querySelector(
    ".conversion-actions__action:nth-of-type(" + String(nth) + "1)"
  )
  if (!action) {
    return
  }

  action.setAttribute("href", url)
}

ConversionActions.prototype.unsetDownload = function unsetDownload() {
  this.unsetNth(1)
}

ConversionActions.prototype.unsetView = function unsetView() {
  this.unsetNth(2)
}

ConversionActions.prototype.unsetEdit = function unsetEdit() {
  this.unsetNth(3)
}

/**
 * @param {number} nth
 */
ConversionActions.prototype.unsetNth = function unsetNth(nth) {
  var element = this.element
  if (!element) {
    return
  }

  var action = element.querySelector(
    ".conversion-actions__action:nth-of-type(" + String(nth) + "1)"
  )
  if (!action) {
    return
  }

  action.removeAttribute("href")
}






































































/**
 * @class
 */
function ConversionStep() {
  /** @type {Element | null} */
  this.element = null
}

ConversionStep.prototype.start = function start() {
  var element = this.element
  if (!element) {
    return
  }

  element.setAttribute(
    "class",
    "conversion-steps__step conversion-steps__step_loading"
  )
}

ConversionStep.prototype.fail = function fail() {
  var element = this.element
  if (!element) {
    return
  }

  element.setAttribute(
    "class",
    "conversion-steps__step conversion-steps__step_failed"
  )
}

ConversionStep.prototype.abort = function abort() {
  var element = this.element
  if (!element) {
    return
  }

  element.setAttribute(
    "class",
    "conversion-steps__step"
  )
}

ConversionStep.prototype.success = function success() {
  var element = this.element
  if (!element) {
    return
  }

  element.setAttribute(
    "class",
    "conversion-steps__step conversion-steps__step_succeeded"
  )
}

ConversionStep.prototype.skip = function skip() {
  var element = this.element
  if (!element) {
    return
  }

  element.setAttribute(
    "class",
    "conversion-steps__step conversion-steps__step_skipped"
  )
}






// Select Step

/**
 * @class
 * @extends ConversionStep
 * @param {Element} root
 */
function ConversionSelectStep(root) {
  var element = ConversionSelectStep.queryStep(root)
  ConversionSelectStep.call(this, element)
}

ConversionSelectStep.prototype = Object.create(ConversionStep.prototype)
ConversionSelectStep.prototype.constructor = ConversionSelectStep

/**
 * @param {Element} root
 * @returns {Element}
 */
ConversionSelectStep.queryStep = function queryStep(root) {
  var step = root.querySelector(".conversion-steps__step:nth-of-type(1)")
  if (!step) {
    throw new ConversionError2()
  }
  return step
}

// Conversion Step

/**
 * @class
 * @extends ConversionStep
 * @param {Element} root
 */
function ConversionConversionStep(root) {
  var element = ConversionConversionStep.queryStep(root)
  ConversionSelectStep.call(this, element)
}

ConversionSelectStep.prototype = Object.create(ConversionStep.prototype)
ConversionSelectStep.prototype.constructor = ConversionSelectStep

/**
 * @param {Element} root
 * @returns {Element}
 */
ConversionConversionStep.queryStep = function queryStep(root) {
  var step = root.querySelector(".conversion-steps__step:nth-of-type(2)")
  if (!step) {
    throw new ConversionError2()
  }
  return step
}









/**
 * @typedef {Object} ConversionFetchFormatsParameters
 * @property {string} fileName
 */

/**
 * @callback ConversionFetchFormatsCallback
 * @param {Conversion} this
 * @param {ConversionError2 | undefined} error
 * @param {string[]} formats
 */

/**
 * @param {ConversionFetchFormatsParameters} parameters
 * @param {ConversionFetchFormatsCallback} callback
 */
Conversion.prototype.fetchFormats = function fetchFormats(parameters, callback) {
  // var payload = JSON.stringify(parameters)
  // var request = new XMLHttpRequest()
  // request.open("POST", "formats-convertible")
  // request.send(payload)
  // request.onload = function () {
  //   /**
  //    * @type {string[] | { error: string }}
  //    */
  //   var response = JSON.parse(this.response)

  //   if (this.status != 200) {
  //     var error = Array.isArray(response)
  //       ? "Unknown error. Please, contact the administrator."
  //       : response.error
  //     callback(undefined, error)
  //     return
  //   }

  //   if (!Array.isArray(response)) {
  //     callback(undefined, response.error)
  //     return
  //   }

  //   callback(response, undefined)
  // }

  setTimeout(
    (function () {
      callback.call(this, undefined, [
        "pdf",  "pdfа", "docxf",
        "docx", "jpg",  "png",
        "txt",  "bmp",  "rtf",
        "ebub", "fb2",  "html",
        "docm", "dotm", "odt",
        "ott",  "dotx"
      ])
    }).bind(this),
    5000
  )
}

/**
 * @typedef {Object} ConversionFetchConversionParameters
 * @property {number} timeout
 */

/**
 * @typedef {Object} ConversionFetchConversionCallbackData
 * @property {number} percent
 * @property {string | undefined} fileUrl
 */

/**
 * @callback ConversionFetchConversionCallback
 * @param {Conversion} this
 * @param {ConversionError2 | undefined} error
 * @param {ConversionFetchConversionCallbackData} data
 * @param {() => void} cancel
 */

/**
 * @typedef {Object} ConversionFormatsConvertibleResponse
 * @property {string=} error
 * @property {number} percent
 * @property {string | undefined} fileUrl
 */

/**
 * @param {ConversionFetchConversionParameters} parameters
 * @param {ConversionFetchConversionCallback} callback
 */
Conversion.prototype.fetchConversion = function fetchConversion(parameters, callback) {
  /**
   * @type {number | undefined}
   */
  var timer

  function cancel() {
    clearTimeout(timer)
  }

  /**
   * @this Conversion
   */
  function handler() {
    // var payload = JSON.stringify(parameters)
    var request = new XMLHttpRequest()
    request.open("POST", "formats-convertible")
    request.send(payload)

    /**
     * @this Conversion
     */
    function onLoad() {
      /**
       * @type {ConversionFormatsConvertibleResponse}
       */
      var response = JSON.parse(request.response)
      if (response.error) {
        callback.call(this, new ConversionError2(), undefined, cancel)
        return
      }

      /**
       * @type {ConversionFetchConversionCallbackData}
       */
      var data = {
        percent: response.percent,
        fileUrl: response.fileUrl
      }
      callback.call(this, undefined, data, cancel)
    }

    request.onload = onLoad.bind(this)
  }

  setTimeout(handler.bind(this), parameters.timeout)
}








// Error

/**
 * @class {Error}
 * @extends Error
 * @param {string} message
 */
function ConversionError2(message) {
  Error.call(this, message)
  this.name = "ConversionError2"
}

ConversionError2.prototype = Object.create(Error.prototype)
ConversionError2.prototype.constructor = ConversionError2

})()
