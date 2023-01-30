var React = require('react');
import { DocumentEditor } from "@onlyoffice/document-editor-react";

class Main extends React.Component {
    render() {
        const documentServerUrl = document.getElementById("root").getAttribute("data-docserverurl");
        const config = JSON.parse(document.getElementById("root").getAttribute("data-config"));

        return (
            <DocumentEditor
                id="docxEditor1"
                documentServerUrl={documentServerUrl}
                config={config}
                height="100%"
                width="100%"
        />)  
    }  
}

export default Main; 