export class FileTable extends HTMLElement {
  constructor() {
    super();
  }

  connectedCallback() {
    const files = this.data.files;
    const user = this.data.user;
    const directUrl = this.data.directUrl

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
                    <tr 
                      is="file-row"
                      data-file='${JSON.stringify(file)}'
                      data-user="${user}"
                      data-direct-url="${directUrl}"
                    ></tr>
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