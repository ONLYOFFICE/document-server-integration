<?php
/**
 * (c) Copyright Ascensio System SIA 2025
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
 */
 ?>
 
<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <meta name="csrf-token" content="{{ csrf_token() }}">
    <meta name="server-version" content="{{ env('DOCUMENT_SERVER_VERSION') }}">
    <title>ONLYOFFICE Document Editors</title>

    <link rel="icon" href="/images/favicon.ico" type="image/x-icon" />

    <!-- Fonts -->
    <link rel="stylesheet" type="text/css" href="https://fonts.googleapis.com/css?family=Open+Sans:900,
            800,700,600,500,400,300&subset=latin,cyrillic-ext,cyrillic,latin-ext" />

    <!-- Styles -->
    <link rel="stylesheet" type="text/css" href="/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/css/app.css" />
    <link rel="stylesheet" type="text/css" href="/css/media.css" />
    <link rel="stylesheet" type="text/css" href="/css/forgotten.css" />
</head>

<body>
    <form id="form1">
        <header>
            <div class="center">
                <a href="./">
                    <img src="/images/logo.svg" alt="ONLYOFFICE" />
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
                                            <img src="/images/home.svg" alt="Home" />
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
                                                <tr class="tableRow" title="{{ $file['key'] }}">
                                                    <td>
                                                        <a class="stored-edit action-link {{ $file['format']->getType() }}" href="{{ $file['filename'] }}" target="_blank">
                                                            <span>{{ $file['key'] }}</span>
                                                        </a>
                                                    </td>
                                                    <td>
                                                        <a href="{{ $file['filename'] }}">
                                                            <img class="icon-download" src="/images/download.svg" alt="Download" title="Download" />
                                                        </a>
                                                        <a class="delete-file" data="{{ $file['key'] }}">
                                                            <img class="icon-delete" src="/images/delete.svg" alt="Delete" title="Delete" /></a>
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