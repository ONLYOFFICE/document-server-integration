import { FileRow } from "./file-row.js";
import { FileTable } from "./file-table.js";
customElements.define("file-row", FileRow, { extends: "tr" });
customElements.define("file-table", FileTable);