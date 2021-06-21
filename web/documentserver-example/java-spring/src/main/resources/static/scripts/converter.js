var ConverExtList;
var EditedExtList;
var UrlConverter;
var UrlEditor;

if (typeof jQuery !== "undefined") {
    jQuery.post('/config',
        function(data) {
            ConverExtList = data.ConverExtList;
            EditedExtList = data.ConverExtList;
            UrlConverter = data.UrlConverter;
            UrlEditor = data.UrlEditor;
    });
}