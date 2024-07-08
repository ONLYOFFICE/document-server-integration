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
    <link rel="stylesheet" type="text/css" href="/css/jquery-ui.css" />
    @vite(['resources/css/app.css', 'resources/css/media.css', 'resources/css/forgotten.css', 'resources/js/static.js'])
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
                        <td class="left-panel section"></td>
                        <td class="section">
                            <div class="main-panel">
                                <menu class="links">
                                    <li class="home-link">
                                        <a href="{{ route('home') }}">
                                            <img src="{{ Vite::asset('resources/images/home.svg') }}" alt="Home" />
                                        </a>
                                    </li>
                                    <li class="active">
                                        <a href="{{ route('files.forgotten.index') }}">Forgotten files</a>
                                    </li>
                                </menu>
                                <div class="stored-list">
                                    <div class="storedHeader">
                                        <div class="storedHeaderText">
                                            <span class="header-list">Forgotten files</span>
                                        </div>
                                    </div>
                                    <table class="tableHeader" cellspacing="0" cellpadding="0" width="100%">
                                        <thead>
                                            <tr>
                                                <td class="tableHeaderCell">Filename</td>
                                                <td class="tableHeaderCell">Action</td>
                                            </tr>
                                        </thead>
                                    </table>
                                    <div class="scroll-table-body">
                                        <table cellspacing="0" cellpadding="0" width="100%">
                                            <tbody>
                                                @foreach ($files as $file)
                                                <tr class="tableRow" title="{{ $file->key }}">
                                                    <td>
                                                        <a class="stored-edit action-link {{ $file->format->type }}" href="{{ $file->basename }}" target="_blank">
                                                            <span>{{ $file->key }}</span>
                                                        </a>
                                                    </td>
                                                    <td>
                                                        <a href="{{ $file->basename }}">
                                                            <img class="icon-download" src="{{ Vite::asset('resources/images/download.svg') }}" alt="Download" title="Download" />
                                                        </a>
                                                        <a class="delete-file" data="{{ $file->key }}">
                                                            <img class="icon-delete" src="{{ Vite::asset('resources/images/delete.svg') }}" alt="Delete" title="Delete" /></a>
                                                    </td>
                                                </tr>
                                                @endforeach
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

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

    @vite(['resources/js/forgotten.js'])
</body>

</html>