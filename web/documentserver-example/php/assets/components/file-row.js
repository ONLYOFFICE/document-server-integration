export class FileRow extends HTMLTableRowElement {
    constructor() {
        super();
    }

    connectedCallback() {
        const encodedTitle = encodeURIComponent(this.file.title);
        const encodedUser = encodeURIComponent(this.user);
        const directUrl = this.directUrl ? "&directUrl=true&" : "";

        this.innerHTML = `
            <td class="contentCells">
                <a class="stored-edit ${this.file.type}" href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}" target="_blank">
                <span>${this.file.title}</span>
                </a>
            </td>
            ${this.file.editable ? `
                <td class="contentCells contentCells-icon">
                <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=edit&type=desktop" target="_blank">
                    <img src="assets/images/desktop.svg" alt="Open in editor for full size screens" title="Open in editor for full size screens"/>
                </a>
                </td>
                <td class="contentCells contentCells-icon">
                <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=edit&type=mobile" target="_blank">
                    <img src="assets/images/mobile.svg" alt="Open in editor for mobile devices" title="Open in editor for mobile devices" />
                </a>
                </td>
                <td class="contentCells contentCells-icon">
                <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=comment&type=desktop" target="_blank">
                    <img src="assets/images/comment.svg" alt="Open in editor for comment" title="Open in editor for comment" />
                </a>
                </td>
                ${this.file.type === "word" ? `
                <td class="contentCells contentCells-icon">
                    <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=review&type=desktop" target="_blank">
                    <img src="assets/images/review.svg" alt="Open in editor for review" title="Open in editor for review" />
                    </a>
                </td>
                <td class="contentCells contentCells-icon ">
                    <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=blockcontent&type=desktop" target="_blank">
                    <img src="assets/images/block-content.svg" alt="Open in editor without content control modification" title="Open in editor without content control modification"
                    </a>
                </td>
                `: ``}
                ${this.file.type === "cell" ? `
                <td class="contentCells contentCells-icon">
                    <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=filter&type=desktop" target="_blank">
                    <img src="assets/images/filter.svg" alt="Open in editor without access to change the filter" title="Open in editor without access to change the filter" />
                    </a>
                </td>
                `: ``}
                ${this.file.type !== "word" && this.file.type !== "cell" ? `<td class="contentCells  contentCells-icon"></td><td class="contentCells  contentCells-icon"></td>` : ``}
                ${this.file.fillable ? `
                <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">
                    <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=fillForms&type=desktop" target="_blank">
                    <img src="assets/images/fill-forms.svg" alt="Open in editor for filling in forms" title="Open in editor for filling in forms" />
                    </a>
                </td>
                `: `<td class="contentCells contentCells-shift contentCells-iconfirstContentCellShift"></td>`}
            ` : ``}
            ${this.file.fillable ? `
                <td class="contentCells contentCells-icon">
                <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=fillForms&type=desktop" target="_blank">
                    <img src="assets/images/mobile-fill-forms.svg" alt="Open in editor for filling in forms for mobile devices" title="Open in editor for filling in forms for mobile devices" />
                </a>
                </td>
                <td class="contentCells contentCells-icon"></td>
                <td class="contentCells contentCells-icon"></td>
                <td class="contentCells contentCells-icon"></td>
                <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">
                <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=fillForms&type=desktop" target="_blank">
                    <img src="assets/images/fill-forms.svg" alt="Open in editor for filling in forms" title="Open in editor for filling in forms"/>
                </a>
                </td>
            ` : `<td class="contentCells contentCells-shift contentCells-icon contentCellsEmpty" colspan="6"></td>`}
            <td class="contentCells contentCells-icon firstContentCellViewers">
                <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=view&type=desktop" target="_blank">
                <img src="assets/images/desktop.svg" alt="Open in viewer for full size screens" title="Open in viewer for full size screens" />
                </a>
            </td>
            <td class="contentCells contentCells-icon">
                <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=view&type=mobile" target="_blank">
                <img src="assets/images/mobile.svg" alt="Open in viewer for mobile devices" title="Open in viewer for mobile devices" />
                </a>
            </td>
            <td class="contentCells contentCells-icon contentCells-shift">
                <a href="editor?fileID=${encodedTitle}&user=${encodedUser + directUrl}&action=embedded&type=embedded" target="_blank">
                <img src="assets/images/embeded.svg" alt="Open in embedded mode" title="Open in embedded mode" />
                </a>
            </td>
            <td class="contentCells contentCells-icon contentCells-shift  downloadContentCellShift">
                <a href="download?fileName=${encodedTitle}">
                <img class="icon-download" src="assets/images/download.svg"  alt="Download" title="Download"/>
                </a>
            </td>
            <td class="contentCells contentCells-icon contentCells-shift">
                <a class="delete-file" data="${encodedTitle}">
                <img class="icon-delete" src="assets/images/delete.svg" alt="Delete" title="Delete" />
                </a>
            </td>
        `;
        this.classList.add("tableRow");
        this.title = `${this.file.title} ${this.file.version}`;
    }

    get file() {
        return JSON.parse(this.dataset.file || {});
    }

    get user() {
        return this.dataset.user;
    }

    get directUrl() {
        return this.dataset.directUrl;
    }
}