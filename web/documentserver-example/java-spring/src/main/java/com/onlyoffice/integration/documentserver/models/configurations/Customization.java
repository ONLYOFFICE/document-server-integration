/**
 *
 * (c) Copyright Ascensio System SIA 2023
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

package com.onlyoffice.integration.documentserver.models.configurations;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Getter
@Setter
/* The parameters which allow to customize the editor interface so that it looked like your
 other products (if there are any) and change the presence or absence of the additional buttons,
  links, change logos and editor owner details. */
public class Customization {
    @Autowired
    private Logo logo;  // the image file at the top left corner of the Editor header
    @Autowired
    private Goback goback;  // the settings for the Open file location menu button and upper right corner button
    private Boolean autosave = true;  // if the Autosave menu option is enabled or disabled
    private Boolean comments = true;  // if the Comments menu button is displayed or hidden
    private Boolean compactHeader = false;  /* if the additional action buttons are displayed
    in the upper part of the editor window header next to the logo (false) or in the toolbar (true) */
    private Boolean compactToolbar = false;  // if the top toolbar type displayed is full (false) or compact (true)
    private Boolean compatibleFeatures = false;  // the use of functionality only compatible with the OOXML format
    private Boolean forcesave = false;  /* add the request for the forced file saving to the callback handler
     when saving the document within the document editing service */
    private Boolean help = true;  //  if the Help menu button is displayed or hidden
    private Boolean hideRightMenu = false;  // if the right menu is displayed or hidden on first loading
    private Boolean hideRulers = false;  // if the editor rulers are displayed or hidden
    private Boolean submitForm = false;  // if the Submit form button is displayed or hidden
    private Boolean about = true;
    private Boolean feedback = true;
}
