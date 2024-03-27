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
        const template = document.getElementById("files-list-template");
        const templateContent = template.content.cloneNode(true);
        this.container = templateContent.querySelector("tbody");
        this.appendChild(templateContent);
    }

    render() {
        let encodedUser = encodeURIComponent(this.user);
        let encodedDirectUrl = encodeURIComponent(this.directUrlArg);
        this.files.forEach(file => {
            let encodedTitle = encodeURIComponent(file.title);
            let editorUrl = `editor?fileID=${encodedTitle}&user=${encodedUser}&directUrl=${encodedDirectUrl}`;
            
            const row = this.createElement("tr", {
                class: ["tableRow"],
                title: `${file.title} [${file.version}]`
            });

            row.appendChild(this.createHierarchy({
                td: {
                    class: ["contentCells"],
                    title: `${file.title} [${file.version}]`
                },
                a: {
                    class: ["stored-edit", file.type],
                    href: editorUrl,
                    target: "_blank",
                    innerHTML: `<span>${file.title}</span>`
                }
            }));

            if (file.editable) {    
                row.appendChild(this.createIconColumn({
                    a: {
                        href: `${editorUrl}&action=edit&type=desktop`
                    },
                    img: {
                        src: "assets/images/desktop.svg",
                        title: "Open in editor for full size screens",
                        alt: "Open in editor for full size screens"
                    }
                }));

                row.appendChild(this.createIconColumn({
                    a: {
                        href: `${editorUrl}&action=edit&type=mobile`
                    },
                    img: {
                        src: "assets/images/mobile.svg",
                        title: "Open in editor for mobile devices",
                        alt: "Open in editor for mobile devices"
                    }
                }));

                row.appendChild(this.createIconColumn({
                    a: {
                        href: `${editorUrl}&action=comment&type=desktop`
                    },
                    img: {
                        src: "assets/images/comment.svg",
                        title: "Open in editor for comment",
                        alt: "Open in editor for comment"
                    }
                }));

                if (file.type === "word") {
                    row.appendChild(this.createIconColumn({
                        a: {
                            href: `${editorUrl}&action=review&type=desktop`
                        },
                        img: {
                            src: "assets/images/review.svg",
                            title: "Open in editor for review",
                            alt: "Open in editor for review"
                        }
                    }));

                    row.appendChild(this.createIconColumn({
                        a: {
                            href: `${editorUrl}&action=blockcontent&type=desktop`
                        },
                        img: {
                            src: "assets/images/block-content.svg",
                            title: "Open in editor without content control modification",
                            alt: "Open in editor without content control modification"
                        }
                    }));

                } else if (file.type == "cell") {
                    row.appendChild(this.createIconColumn({
                        a: {
                            href: `${editorUrl}&action=filter&type=desktop`
                        },
                        img: {
                            src: "assets/images/filter.svg",
                            title: "Open in editor without access to change the filter",
                            alt: "Open in editor without access to change the filter"
                        }
                    }));
                } else {
                    for(let i = 0; i < 2; i++) {
                        row.appendChild(this.createIconColumn({}));
                    }
                }
                if (file.fillable) {
                    row.appendChild(this.createIconColumn({
                        td: {
                            class: ["firstContentCellShift", "contentCells-shift"]
                        },
                        a: {
                            href: `${editorUrl}&action=fillForms&type=desktop`
                        },
                        img: {
                            src: "assets/images/fill-forms.svg",
                            title: "Open in editor for filling in forms",
                            alt: "Open in editor for filling in forms"
                        }
                    }));
                } else {
                    row.appendChild(this.createIconColumn({
                        td: {
                            class: ["firstContentCellShift", "contentCells-shift"],
                        }
                    }));
                }
            } else if (file.fillable) {
                row.appendChild(this.createIconColumn({
                    a: {
                        href: `${editorUrl}&action=fillForms&type=desktop`
                    },
                    img: {
                        src: "assets/images/mobile-fill-forms.svg",
                        title: "Open in editor for filling in forms for mobile devices",
                        alt: "Open in editor for filling in forms for mobile devices"
                    }
                }));

                for(let i = 0; i < 3; i++) {
                    row.appendChild(this.createIconColumn({}));
                }

                row.appendChild(this.createIconColumn({
                    td: {
                        class: ["firstContentCellShift", "contentCells-shift"],
                    },
                    a: {
                        href: `${editorUrl}&action=fillForms&type=desktop`
                    },
                    img: {
                        src: "assets/images/fill-forms.svg",
                        title: "Open in editor for filling in forms",
                        alt: "Open in editor for filling in forms"
                    }
                }));
            } else {
                row.appendChild(this.createIconColumn({
                    td: {
                        class: ["contentCellsEmpty", "contentCells-shift"],
                        colspan: 6
                    }
                }));
            }

            row.appendChild(this.createIconColumn({
                td: {
                    class: ["firstContentCellViewers"],
                },
                a: {
                    href: `${editorUrl}&action=view&type=desktop`
                },
                img: {
                    src: "assets/images/desktop.svg",
                    title: "Open in viewer for full size screens",
                    alt: "Open in viewer for full size screens"
                }
            }));

            row.appendChild(this.createIconColumn({
                a: {
                    href: `${editorUrl}&action=view&type=mobile`
                },
                img: {
                    src: "assets/images/mobile.svg",
                    title: "Open in viewer for mobile devices",
                    alt: "Open in viewer for mobile devices"
                }
            }));

            row.appendChild(this.createIconColumn({
                td: {
                    class: ["contentCells-shift"],
                },
                a: {
                    href: `${editorUrl}&action=embedded&type=embedded`
                },
                img: {
                    src: "assets/images/embeded.svg",
                    title: "Open in embedded mode",
                    alt: "Open in embedded mode"
                }
            }));

            row.appendChild(this.createIconColumn({
                td: {
                    class: ["contentCells-shift", "downloadContentCellShift"],
                },
                a: {
                    href: `download?fileName=${encodedTitle}`
                },
                img: {
                    class: ["icon-download"],
                    src: "assets/images/download.svg",
                    title: "Download",
                    alt: "Download"
                }
            }));

            row.appendChild(this.createIconColumn({
                td: {
                    class: ["contentCells-shift"],
                },
                a: {
                    data: file.title,
                    class: ["delete-file"]
                },
                img: {
                    class: ["icon-delete"],
                    src: "assets/images/delete.svg",
                    title: "Delete",
                    alt: "Delete"
                }
            }));

            this.container.appendChild(row);
        });
    }

    createElement(name, properties) {
        let element = document.createElement(name);
        Object.entries(properties).forEach(([property, value], index) => {
            if(property === "class") {
                element.classList.add(...value);
            } else if(property === "innerHTML") {
                element[property] = value;
            } else {
                element.setAttribute(property, value);
            }
        });
        return element;
    }

    createHierarchy(elements) {
        let lastNode = null;
        let firstNode = null;

        for(const[element, attributes] of Object.entries(elements)) {
            let newElement = this.createElement(element, attributes);
            if (lastNode) lastNode.appendChild(newElement);
            else firstNode = newElement;
            lastNode = newElement;
        }
        return firstNode;
    }

    createIconColumn(elements) {
        if(elements.td) {
            elements.td.class.push("contentCells", "contentCells-icon");
        } else {
            elements = Object.assign({td : {class: ["contentCells", "contentCells-icon"]}}, elements);
        }
        if (elements.a)
            elements.a.target = "_blank";
        return this.createHierarchy(elements);
    }

    static get observedAttributes() {
        return ["data"];
    }

    attributeChangedCallback(name, oldValue, newValue) {
        if(name === "data") {
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

    get directUrl() {
        return this.data.directUrl;
    }
}

customElements.define("files-list", FilesList);