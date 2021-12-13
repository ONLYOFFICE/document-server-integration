/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

package com.onlyoffice.integration.services.configurers.implementations;

import com.onlyoffice.integration.documentserver.models.enums.Action;
import com.onlyoffice.integration.documentserver.models.configurations.Customization;
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.services.configurers.CustomizationConfigurer;
import com.onlyoffice.integration.services.configurers.wrappers.DefaultCustomizationWrapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class DefaultCustomizationConfigurer implements CustomizationConfigurer<DefaultCustomizationWrapper> {
    @Override
    public void configure(Customization customization, DefaultCustomizationWrapper wrapper) {  // define the customization configurer
        Action action = wrapper.getAction();  // get the action parameter from the customization wrapper
        User user = wrapper.getUser();
        customization.setSubmitForm(action.equals(Action.fillForms) && user.getId() == 1 && false);  // set the submitForm parameter to the customization config
    }
}
