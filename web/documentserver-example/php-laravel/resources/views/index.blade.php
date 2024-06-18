<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <meta name="server-version" content="{{ env('SERVER_VERSION') }}">
    <title>ONLYOFFICE Document Editors</title>

    <link rel="icon" href="{{ Vite::asset('resources/images/favicon.ico') }}" type="image/x-icon" />

    <!-- Fonts -->
    <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:900,
            800,700,600,500,400,300&subset=latin,cyrillic-ext,cyrillic,latin-ext" />

    <!-- Styles -->
    @vite(['resources/css/app.css', 'resources/css/media.css', 'resources/js/static.js'])
</head>

<body>
    <form id="form1">
        <header>
            <div class="center">
                <a href="./">
                    <img src="{{ Vite::asset('resources/images/logo.svg') }}" alt="ONLYOFFICE" />
                </a>
            </div>
        </header>
        <div class="center main">
            <table class="table-main">
                <tbody>
                    <tr>
                        <td class="left-panel section">
                            <div class="help-block">
                                <span>Create new</span>
                                <div class="clearFix">
                                    <div class="create-panel clearFix">
                                        <ul class="try-editor-list clearFix">
                                            <li>
                                                <a class="try-editor word reload-page" target="_blank" href="editor?fileExt=docx&user={user}">Document</a>
                                            </li>
                                            <li>
                                                <a class="try-editor cell reload-page" target="_blank" href="editor?fileExt=xlsx&user={user}">Spreadsheet</a>
                                            </li>
                                            <li>
                                                <a class="try-editor slide reload-page" target="_blank" href="editor?fileExt=pptx&user={user}">Presentation</a>
                                            </li>
                                            <li>
                                                <a class="try-editor form reload-page" target="_blank" href="editor?fileExt=docxf&user={user}">PDF form</a>
                                            </li>
                                        </ul>
                                        <label class="side-option">
                                            <input type="checkbox" id="createSample" class="checkbox" />
                                            With sample content
                                        </label>
                                    </div>
                                    <div class="upload-panel clearFix">
                                        <a class="file-upload">Upload file
                                            @csrf
                                            <input type="file" id="fileupload" name="file" data-url="/files/upload?user={user}" />
                                        </a>
                                    </div>

                                    <table class="user-block-table" cellspacing="0" cellpadding="0">
                                        <tr>
                                            <td valign="middle">
                                                <span class="select-user">Username</span>
                                                <img id="info" class="info" src="{{ Vite::asset('resources/images/info.svg') }}" />
                                                <select class="select-user" id="user">
                                                    @foreach ($users as $exampleUser)
                                                    <option value="{{ $exampleUser->id }}">{{ $exampleUser->name ?? 'Anonymous' }}</option>
                                                    @endforeach
                                                </select>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td valign="middle">
                                                <span class="select-user">Language</span>
                                                <img class="info info-tooltip" data-id="language" data-tooltip="Choose the language for ONLYOFFICE editors interface" src="{{ Vite::asset('resources/images/info.svg') }}" />
                                                <select class="select-user" id="language">
                                                    @foreach ($languages as $key => $language)
                                                    <option value="{{ $key }}">{{ $language }}</option>
                                                    @endforeach
                                                </select>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td valign="middle">
                                                <label class="side-option">
                                                    <input id="directUrl" type="checkbox" class="checkbox" />
                                                    Try opening on client
                                                    <img id="directUrlInfo" class="info info-tooltip" data-id="directUrlInfo" data-tooltip="Some files can be opened in the user's
                                                             browser without connecting to the document server." src="{{ Vite::asset('resources/images/info.svg') }}" />
                                                </label>
                                            </td>
                                        </tr>
                                    </table>
                                </div>
                            </div>
                        </td>
                        <td class="section">
                            <div class="main-panel">
                                <menu class="links">
                                    <li class="home-link active">
                                        <a href="./">
                                            <img src="{{ Vite::asset('resources/images/home.svg') }}" alt="Home" />
                                        </a>
                                    </li>
                                    {forgottenLink}
                                </menu>
                                @if($files)
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
                                                <td class="tableHeaderCell tableHeaderCellFileName">
                                                    Filename
                                                </td>
                                                <td class="tableHeaderCell tableHeaderCellEditors
                                                        contentCells-shift">
                                                    Editors
                                                </td>
                                                <td class="tableHeaderCell tableHeaderCellViewers">
                                                    Viewers
                                                </td>
                                                <td class="tableHeaderCell tableHeaderCellDownload">
                                                    Download
                                                </td>
                                                <td class="tableHeaderCell tableHeaderCellRemove">
                                                    Remove
                                                </td>
                                            </tr>
                                        </thead>
                                    </table>
                                    <div class="scroll-table-body">
                                        <table cellspacing="0" cellpadding="0" width="100%">
                                            <tbody>
                                                @foreach ($files as $file)
                                                <tr class="tableRow" title="{{ $file->basename }}">
                                                    <td class="contentCells">
                                                        <a class="stored-edit {{ $file->format->type }}" href="editor?fileID={{ urlencode($file->basename) }}&user={{ $user . $directUrlArg }}" target="_blank">
                                                            <span>{{ $file->basename }}</span>
                                                        </a>
                                                    </td>
                                                    @if ($file->format->editable())
                                                    <td class="contentCells contentCells-icon">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}'&action=edit&type=desktop" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/desktop.svg') }}" alt="Open in editor for full size screens" title="Open in editor for full size screens" />
                                                        </a>
                                                    </td>
                                                    <td class="contentCells contentCells-icon">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}'&action=edit&type=mobile" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/mobile.svg') }}" alt="Open in editor for mobile devices" title="Open in editor for mobile devices" />
                                                        </a>
                                                    </td>
                                                    <td class="contentCells contentCells-icon">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}'&user={{ htmlentities($user) . $directUrlArg }}&action=comment&type=desktop" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/comment.svg') }}" alt="Open in editor for comment" title="Open in editor for comment" />
                                                        </a>
                                                    </td>
                                                    @if ($file->format->isWord())
                                                    <td class="contentCells contentCells-icon">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}&action=review&type=desktop" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/review.svg') }}" alt="Open in editor for review" title="Open in editor for review" />
                                                        </a>
                                                    </td>
                                                    <td class="contentCells contentCells-icon ">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}&action=blockcontent&type=desktop" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/block-content.svg') }}" alt="Open in editor without content control modification" title="Open in editor without content control modification" />
                                                        </a>
                                                    </td>
                                                    @elseif ($file->format->isCell())
                                                    <td class="contentCells contentCells-icon">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}&action=filter&type=desktop" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/filter.svg') }}" alt="Open in editor without access to change the filter" title="Open in editor without access to change the filter" />
                                                        </a>
                                                    </td>
                                                    @else
                                                    <td class="contentCells contentCells-icon"></td>
                                                    <td class="contentCells contentCells-icon"></td>
                                                    @endif
                                                    @if ($file->format->fillable())
                                                    <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}&action=fillForms&type=desktop" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/fill-forms.svg') }}" alt="Open in editor for filling in forms" title="Open in editor for filling in forms" />
                                                        </a>
                                                    </td>
                                                    @else
                                                    <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift"></td>
                                                    @endif
                                                    @elseif ($file->format->fillable())
                                                    <td class="contentCells contentCells-icon">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}&action=fillForms&type=desktop" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/mobile-fill-forms.svg') }}" alt="Open in editor for filling in forms for mobile devices" title="Open in editor for filling in forms for mobile devices" />
                                                        </a>
                                                    </td>
                                                    <td class="contentCells contentCells-icon"></td>
                                                    <td class="contentCells contentCells-icon"></td>
                                                    <td class="contentCells contentCells-icon"></td>
                                                    <td class="contentCells contentCells-shift contentCells-icon firstContentCellShift">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}&action=fillForms&type=desktop" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/fill-forms.svg') }}" alt="Open in editor for filling in forms" title="Open in editor for filling in forms" />
                                                        </a>
                                                    </td>
                                                    @else
                                                    <td class="contentCells contentCells-shift contentCells-icon contentCellsEmpty" colspan="6"></td>
                                                    @endif
                                                    <td class="contentCells contentCells-icon firstContentCellViewers">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}&action=view&type=desktop" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/desktop.svg') }}" alt="Open in viewer for full size screens" title="Open in viewer for full size screens" />
                                                        </a>
                                                    </td>
                                                    <td class="contentCells contentCells-icon">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}&action=view&type=mobile" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/mobile.svg') }}" alt="Open in viewer for mobile devices" title="Open in viewer for mobile devices" />
                                                        </a>
                                                    </td>
                                                    <td class="contentCells contentCells-icon contentCells-shift">
                                                        <a href="editor?fileID={{ urlencode($file->basename) }}&user={{ htmlentities($user) . $directUrlArg }}&action=embedded&type=embedded" target="_blank">
                                                            <img src="{{ Vite::asset('resources/images/embeded.svg') }}" alt="Open in embedded mode" title="Open in embedded mode" />
                                                        </a>
                                                    </td>
                                                    <td class="contentCells contentCells-icon contentCells-shift  downloadContentCellShift">
                                                        <a href="{{ route('files.download', ['fileName' => $file->basename]) }}">
                                                            <img class="icon-download" src="{{ Vite::asset('resources/images/download.svg') }}" alt="Download" title="Download" />
                                                        </a>
                                                    </td>
                                                    <td class="contentCells contentCells-icon contentCells-shift">
                                                        <a class="delete-file" data="{{ $file->basename }}">
                                                            <img class="icon-delete" src="{{ Vite::asset('resources/images/delete.svg') }}" alt="Delete" title="Delete" /></a>
                                                    </td>
                                                </tr>
                                                @endforeach
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                                @else
                                <div id="portal-info">
                                    <span class="portal-name">ONLYOFFICE Document Editors – Welcome!</span>
                                    <span class="portal-descr">
                                        Get started with a demo-sample of ONLYOFFICE Document Editors,
                                        the first html5-based editors.
                                        <br /> You may upload your own documents for testing using the
                                        "<b>Upload file</b>" button and <b>selecting</b>
                                        the necessary files on your PC.
                                    </span>
                                    <span class="portal-descr">
                                        Please do NOT use this integration example on your own server without
                                        proper code modifications, it is intended for testing purposes only.
                                        In case you enabled this test example, disable it before going for
                                        production.
                                    </span>
                                    <span class="portal-descr">
                                        You can open the same document using different
                                        users in different Web browser sessions, so you can check out multi-user
                                        editing functions.
                                    </span>
                                    @foreach ($users as $user)
                                    <div class="user-descr">
                                        <b>{{ $user->name ?? 'Anonymous' }}</b>
                                        <ul>
                                            @foreach ($user->descriptions as $description)
                                            <li>{{ $description }}</li>
                                            @endforeach
                                        </ul>
                                    </div>
                                    @endforeach
                                </div>
                                @endif
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div id="mainProgress">
            <div id="uploadSteps">
                <span id="uploadFileName" class="uploadFileName"></span>
                <div class="describeUpload">After these steps are completed, you can work with your document.</div>
                <span id="step1" class="step">1. Loading the file.</span>
                <span class="step-descr">The loading speed depends on file size
                    and additional elements it contains.</span>
                <div id="select-file-type" class="invisible">
                    <br />
                    <span class="step">Please select the current document type</span>
                    <div class="buttonsMobile indent">
                        <div class="button file-type document" data="docx">Document</div>
                        <div class="button file-type spreadsheet" data="xlsx">Spreadsheet</div>
                        <div class="button file-type presentation" data="pptx">Presentation</div>
                    </div>
                </div>
                <br />
                <span id="step2" class="step">2. Conversion.</span>
                <span class="step-descr">The file is converted to OOXML so that you can edit it.</span>
                <br />
                <div id="blockPassword">
                    <span class="descrFilePass">The file is password protected.</span>
                    <br />
                    <div>
                        <input id="filePass" type="password" />
                        <div id="enterPass" class="button orange">Enter</div>
                        <div id="skipPass" class="button gray">Skip</div>
                    </div>
                    <span class="errorPass"></span>
                    <br />
                </div>
                <span id="step3" class="step">3. Loading editor scripts.</span>
                <span class="step-descr">They are loaded only once, they will be cached on your computer.</span>
                <input type="hidden" name="hiddenFileName" id="hiddenFileName" />
                <br />
                <span class="progress-descr">Note the speed of all operations depends
                    on your connection quality and server location.</span>
                <br />
                <div class="error-message">
                    <b>Upload error: </b><span></span>
                    <br />
                    Please select another file and try again.
                </div>
            </div>
            <iframe id="embeddedView" src="" height="345px" width="432px" frameborder="0" scrolling="no" allowtransparency></iframe>
            <br />
            <div class="buttonsMobile">
                {editButton}
                <div id="beginView" class="button gray disable">View</div>
                <div id="beginEmbedded" class="button gray disable">Embedded view</div>
                <div id="cancelEdit" class="button gray">Cancel</div>
            </div>
        </div>

        <span id="loadScripts" data-docs="{dataDocs}"></span>

        <footer>
            <div class="center">
                <table>
                    <tbody>
                        <tr>
                            <td>
                                <a href="http://api.onlyoffice.com/editors/howitworks" target="_blank">
                                    API Documentation
                                </a>
                            </td>
                            <td>
                                <a href="mailto:sales@onlyoffice.com">Submit your request</a>
                            </td>
                            <td class="copy">
                                &copy; Ascensio Systems SIA {date}. All rights reserved.
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </footer>
    </form>

    <script type="text/javascript" src="/js/jquery-3.6.4.min.js"></script>
    <script type="text/javascript" src="/js/jquery-migrate-3.4.1.min.js"></script>
    <script type="text/javascript" src="/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/js/jquery.blockUI.js"></script>
    <script type="text/javascript" src="/js/jquery.iframe-transport.js"></script>
    <script type="text/javascript" src="/js/jquery.fileupload.js"></script>
    <script type="text/javascript" src="/js/jquery.dropdownToggle.js"></script>
    @vite(['resources/js/app.js'])
</body>

</html>