import { Component } from '@angular/core';
import { IConfig } from '@onlyoffice/document-editor-angular';

@Component({
    selector: 'my-app',
    template: `<document-editor
                    id="docEditor"
                    [documentServerUrl]="documentServerUrl"
                    [config]="config"
                ></document-editor>`
})
export class AppComponent {
    documentServerUrl:string = null;
    config:any = null;

    ngOnInit(): void {
        this.documentServerUrl = this.getDocServerUrl();
        this.config = this.getConfig();
    }

    getDocServerUrl = () => {
        return document.getElementById("root").getAttribute("data-docserverurl");
    };

    getConfig = () => {
        return JSON.parse(document.getElementById("root").getAttribute("data-config"));
    };
}