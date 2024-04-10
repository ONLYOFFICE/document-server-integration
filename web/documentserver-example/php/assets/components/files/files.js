/**
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

class FilesList extends HTMLElement {
    constructor() {
        super();
        this.template = null;
    }

    async connectedCallback() {
        if (!this.template) {
            await this.initTemplate();
        }
    }

    async initTemplate() {
        let templateHtml = await fetch("assets/components/files/template.html")
            .then((stream) => stream.text());

        this.template = document.createElement("template");
        this.template.innerHTML = templateHtml;
        const templateContent = this.template.content.cloneNode(true);
        this.style.display = "block";
        this.classList.add("stored-list");
        this.container = templateContent.querySelector("tbody");
        this.appendChild(templateContent);

    }

    render() {
        this.files.forEach(file => {
            const row = document.createElement("tr", { is: "files-row" })
            row.dataset.file = JSON.stringify(file);
            row.dataset.user = this.user;
            row.dataset.directUrl = this.directUrl;

            this.container.appendChild(row);
        });
    }

    static get observedAttributes() {
        return ["data"];
    }

    attributeChangedCallback(name, oldValue, newValue) {
        if (name === "data") {
            this.container.innerHTML = "";
            this.render();
        }
    }

    get data() {
        return JSON.parse(this.getAttribute("data")) || {}
    }

    get files() {
        return this.data.files;
    }

    get user() {
        return this.data.user;
    }

    get encodedUser() {
        return encodeURIComponent(this.user);
    }

    get directUrl() {
        return this.data.directUrl;
    }

    get encodedDirectUrl() {
        return encodeURIComponent(this.directUrl);
    }
}

class FilesRow extends HTMLTableRowElement {
    constructor() {
        super();
        this.viewersTemplate = null;
        this.editorsTemplate = null;
        this.actionsTemplate = null;
    }

    async connectedCallback() {
        if (!this.viewersTemplate || !this.editorsTemplate || !this.actionsTemplate)
            await this.initTemplates();
        this.render();
    }

    async initTemplates() {
        this.viewersTemplate = await fetch("assets/components/files/viewers.html")
            .then((stream) => stream.text());
        this.editorsTemplate = await fetch("assets/components/files/editors.html")
            .then((stream) => stream.text());
        this.actionsTemplate = await fetch("assets/components/files/actions.html")
            .then((stream) => stream.text());
    }

    render() {
        this.initSelf();
        this.appendChild(this.createNameColumn());

        let editors = this.createEditorsColumns();
        for (let i = 0; i < editors.length; i++) {
            this.appendChild(editors[i]);
        }

        let viewers = this.createViewersColumns();
        for (let i = 0; i < viewers.length; i++) {
            this.appendChild(viewers[i]);
        }

        let actions = this.createActionsColumns();
        for (let i = 0; i < actions.length; i++) {
            this.appendChild(actions[i]);
        }
    }

    initSelf() {
        this.classList.add("tableRow");
        this.title = this.name;
    }

    createNameColumn() {
        let column = document.createElement("td", { is: "files-name-column" })
        column.dataset.type = this.file.type;
        column.dataset.url = this.editorUrl;
        column.dataset.name = this.file.title;

        return column;
    }

    createEditorsColumns() {
        let template = document.createElement("template");
        template.innerHTML = this.editorsTemplate;
        let columns = [];

        if (this.file.editable) {
            let container = template.content.querySelector("#action-editable").content.cloneNode(true);
            container.querySelectorAll("td").forEach(td => {
                container.querySelectorAll("a").forEach(link => {
                    link.href = this.editorUrl + link.href;
                });
                columns.push(td);
            })
        }
        if (this.file.type === "word") {
            let container = template.content.querySelector("#type-word").content.cloneNode(true);
            container.querySelectorAll("td").forEach(td => {
                container.querySelectorAll("a").forEach(link => {
                    link.href = this.editorUrl + link.href;
                });
                columns.push(td);
            })
        }
        if (this.file.type === "cell") {
            let container = template.content.querySelector("#type-cell").content.cloneNode(true);
            container.querySelectorAll("td").forEach(td => {
                container.querySelectorAll("a").forEach(link => {
                    link.href = this.editorUrl + link.href;
                });
                columns.push(td);
            })
        }
        if (this.file.fillable) {
            let container = template.content.querySelector("#action-fillable").content.cloneNode(true);
            container.querySelectorAll("td").forEach(td => {
                container.querySelectorAll("a").forEach(link => {
                    link.href = this.editorUrl + link.href;
                });
                columns.push(td);
            })
        }
        return columns;
    }

    createViewersColumns() {
        let template = document.createElement("template");
        template.innerHTML = this.viewersTemplate;
        let columns = template.content.querySelectorAll("td");
        columns.forEach(column => {
            let link = column.querySelector("a");
            link.href = this.editorUrl + link.href;
        });
        return columns;
    }

    createActionsColumns() {
        let template = document.createElement("template");
        template.innerHTML = this.actionsTemplate;
        let columns = template.content.querySelectorAll("td");
        columns.forEach(column => {
            let link = column.querySelector("a");
            let img = link.querySelector("img");
            if (img.title === "Download") link.href += this.encodedTitle;
            if (img.title === "Delete") link.setAttribute("data", this.file.title);
        });
        return columns;
    }

    get file() {
        return JSON.parse(this.dataset.file || {});
    }

    get name() {
        return `${this.file.title} ${this.file.version}`;
    }

    get encodedTitle() {
        return encodeURIComponent(this.file.title);
    }

    get user() {
        return this.dataset.user;
    }

    get encodedUser() {
        return encodeURIComponent(this.user);
    }

    get directUrl() {
        return this.dataset.directUrl;
    }

    get encodedDirectUrl() {
        return encodeURIComponent(this.directUrl);
    }

    get editorUrl() {
        return `editor?fileID=${this.encodedTitle}&user=${this.encodedUser}&directUrl=${this.encodedDirectUrl}`;
    }
}

class FilesNameColumn extends HTMLTableCellElement {
    connectedCallback() {
        this.classList.add("contentCells");

        let link = document.createElement("a");
        link.classList.add("stored-edit", this.type);
        link.href = this.url;
        link.target = "_blank";

        let span = document.createElement("span");
        span.innerHTML = this.name;

        link.appendChild(span);
        this.appendChild(link);
    }

    get type() {
        return this.dataset.type || "";
    }

    get url() {
        return this.dataset.url || "";
    }

    get name() {
        return this.dataset.name || "";
    }
}

customElements.define("files-list", FilesList);
customElements.define("files-row", FilesRow, { extends: "tr" });
customElements.define("files-name-column", FilesNameColumn, { extends: "td" });
