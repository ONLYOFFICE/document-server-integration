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
