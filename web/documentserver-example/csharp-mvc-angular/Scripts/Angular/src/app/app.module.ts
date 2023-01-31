import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { AppComponent } from './app.component';
import { DocumentEditorModule } from '@onlyoffice/document-editor-angular';

@NgModule({
    imports: [BrowserModule, FormsModule, DocumentEditorModule],
    declarations: [AppComponent],
    bootstrap: [AppComponent]
})

export class AppModule { }