export class FileTable extends HTMLElement {
  constructor() {
    super();
  }

  connectedCallback() {
    const files = this.data.files;
    const user = this.data.user;
    const directUrl = this.data.directUrl ? "&directUrl=true&" : "";

    this.innerHTML = `
      <div class="stored-list">
        <div class="storedHeader">
            <div class="storedHeaderText">
                <span class="header-list">Your documents</span>
            </div>
            <div class="storedHeaderClearAll">
                <div class="clear-all">Clear all</div>
            </div>
        </div>
        <table class="tableHeader" cellspacing="0" cellpadding="0" width="100%">
            <thead>
            <tr>
                <td class="tableHeaderCell tableHeaderCellFileName">Filename</td>
                <td class="tableHeaderCell tableHeaderCellEditors contentCells-shift">Editors</td>
                <td class="tableHeaderCell tableHeaderCellViewers">Viewers</td>
                <td class="tableHeaderCell tableHeaderCellDownload">Download</td>
                <td class="tableHeaderCell tableHeaderCellRemove">Remove</td>
            </tr>
            </thead>
        </table>
        <div class="scroll-table-body">
            <table cellspacing="0" cellpadding="0" width="100%">
                <tbody>
                  ${files.map(file =>`
                    <tr class="tableRow" title="${file.title} [1]">
                      <td class="contentCells">
                        <a class="stored-edit ${file.type}" href="editor?fileID=${file.title}&user=${user + directUrl}" target="_blank">
                          <span>${file.title}</span>
                        </a>
                      </td>
                      ${file.editable ? `
                        <td class="contentCells contentCells-icon">
                          <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=edit&type=desktop" target="_blank">
                            <img src="assets/images/desktop.svg" alt="Open in editor for full size screens" title="Open in editor for full size screens"/>
                          </a>
                        </td>
                        <td class="contentCells contentCells-icon">
                          <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=edit&type=mobile" target="_blank">
                            <img src="assets/images/mobile.svg" alt="Open in editor for mobile devices" title="Open in editor for mobile devices" />
                          </a>
                        </td>
                        <td class="contentCells contentCells-icon">
                          <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=comment&type=desktop" target="_blank">
                            <img src="assets/images/comment.svg" alt="Open in editor for comment" title="Open in editor for comment" />
                          </a>
                        </td>
                        ${file.type === "word" ? `
                          <td class="contentCells contentCells-icon">
                            <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=review&type=desktop" target="_blank">
                              <img src="assets/images/review.svg" alt="Open in editor for review" title="Open in editor for review" />
                            </a>
                          </td>
                          <td class="contentCells contentCells-icon ">
                            <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=blockcontent&type=desktop" target="_blank">
                              <img src="assets/images/block-content.svg" alt="Open in editor without content control modification" title="Open in editor without content control modification"
                            </a>
                          </td>
                        `: ``}
                        ${file.type === "cell" ? `
                          <td class="contentCells contentCells-icon">
                            <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=filter&type=desktop" target="_blank">
                              <img src="assets/images/filter.svg" alt="Open in editor without access to change the filter" title="Open in editor without access to change the filter" />
                            </a>
                          </td>
                        `: ``}
                        ${file.type !== "word" && file.type !== "cell" ? `<td class="contentCells  contentCells-icon"></td><td class="contentCells  contentCells-icon"></td>` : ``}
                        ${file.fillable ? `
                          <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">
                            <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=fillForms&type=desktop" target="_blank">
                              <img src="assets/images/fill-forms.svg" alt="Open in editor for filling in forms" title="Open in editor for filling in forms" />
                            </a>
                          </td>
                        `: `<td class="contentCells contentCells-shift contentCells-iconfirstContentCellShift"></td>`}
                      ` : ``}
                      ${file.fillable ? `
                        <td class="contentCells contentCells-icon">
                          <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=fillForms&type=desktop" target="_blank">
                            <img src="assets/images/mobile-fill-forms.svg" alt="Open in editor for filling in forms for mobile devices" title="Open in editor for filling in forms for mobile devices" />
                          </a>
                        </td>
                        <td class="contentCells contentCells-icon"></td>
                        <td class="contentCells contentCells-icon"></td>
                        <td class="contentCells contentCells-icon"></td>
                        <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">
                          <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=fillForms&type=desktop" target="_blank">
                            <img src="assets/images/fill-forms.svg" alt="Open in editor for filling in forms" title="Open in editor for filling in forms"/>
                          </a>
                        </td>
                      ` : `<td class="contentCells contentCells-shift contentCells-icon contentCellsEmpty" colspan="6"></td>`}
                      <td class="contentCells contentCells-icon firstContentCellViewers">
                        <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=view&type=desktop" target="_blank">
                          <img src="assets/images/desktop.svg" alt="Open in viewer for full size screens" title="Open in viewer for full size screens" />
                        </a>
                      </td>
                      <td class="contentCells contentCells-icon">
                        <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=view&type=mobile" target="_blank">
                          <img src="assets/images/mobile.svg" alt="Open in viewer for mobile devices" title="Open in viewer for mobile devices" />
                        </a>
                      </td>
                      <td class="contentCells contentCells-icon contentCells-shift">
                        <a href="editor?fileID=${file.title}&user=${user + directUrl}&action=embedded&type=embedded" target="_blank">
                          <img src="assets/images/embeded.svg" alt="Open in embedded mode" title="Open in embedded mode" />
                        </a>
                      </td>
                      <td class="contentCells contentCells-icon contentCells-shift  downloadContentCellShift">
                        <a href="download?fileName=${file.title}">
                          <img class="icon-download" src="assets/images/download.svg"  alt="Download" title="Download"/>
                        </a>
                      </td>
                      <td class="contentCells contentCells-icon contentCells-shift">
                        <a class="delete-file" data="${file.title}">
                          <img class="icon-delete" src="assets/images/delete.svg" alt="Delete" title="Delete" />
                        </a>
                      </td>
                    </tr>
                  `).join("")}
                </tbody>
            </table>
        </div>
      </div>
    `;
  }

  get data() {
    return JSON.parse(this.getAttribute("data")) || {}
  }

}