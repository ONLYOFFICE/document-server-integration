var config = {}

config.maxFileSize = 5242880;
config.storageFolder = "";

config.viewedDocs = [".ppt", ".pps", ".odp", ".pdf", ".djvu", ".epub", ".xps"];
config.editedDocs = [".docx", ".doc", ".odt", ".xlsx", ".xls", ".ods", ".csv", ".pptx", ".ppsx", ".rtf", ".txt", ".mht", ".html", ".htm"];
config.convertedDocs = [".doc", ".odt", ".xls", ".ods", ".ppt", ".pps", ".odp", ".rtf", ".mht", ".html", ".htm", ".epub"];

config.commandUrl = "https://doc.onlyoffice.com/coauthoring/CommandService.ashx";
config.storageUrl = "https://doc.onlyoffice.com/FileUploader.ashx";
config.converterUrl = "https://doc.onlyoffice.com/ConvertService.ashx";
config.tempStorageUrl = "https://doc.onlyoffice.com/ResourceService.ashx";
config.apiUrl = "https://doc.onlyoffice.com/OfficeWeb/apps/api/documents/api.js";
config.preloaderUrl = "https://doc.onlyoffice.com/OfficeWeb/apps/api/documents/cache-scripts.html";

config.haveExternalIp = false; //service can access the document on the url

module.exports = config;