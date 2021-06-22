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

package com.onlyoffice.integration.entities.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Scope("prototype")
public class Logo {

    @Value("${logo.image}")
    private String logoImage;

    @Value("${logo.imageEmbedded}")
    private String logoImageEmbedded;

    @Value("${logo.url}")
    private String logoUrl;

    private String image;
    private String imageEmbedded;
    private String url;

    @PostConstruct
    private void init(){
        this.image = logoImage;
        this.imageEmbedded = logoImageEmbedded;
        this.url = logoUrl;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImageEmbedded() {
        return imageEmbedded;
    }

    public void setImageEmbedded(String imageEmbedded) {
        this.imageEmbedded = imageEmbedded;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
