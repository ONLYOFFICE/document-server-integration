<?php

/**
 * (c) Copyright Ascensio System SIA 2024
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

namespace App\Http\Controllers;

use App\UseCases\Forgotten\Delete\DeleteForgottenFileCommand;
use App\UseCases\Forgotten\Delete\DeleteForgottenFileRequest;
use App\UseCases\Forgotten\Find\FindAllForgottenFilesQuery;
use App\UseCases\Forgotten\Find\FindAllForgottenFilesQueryHandler;

class ForgottenController extends Controller
{
    public function index()
    {
        $files = app(FindAllForgottenFilesQueryHandler::class)
            ->__invoke(new FindAllForgottenFilesQuery);

        return view('forgotten', compact('files'));
    }

    public function destroy(string $key)
    {
        app(DeleteForgottenFileCommand::class)
            ->__invoke(new DeleteForgottenFileRequest($key));

        return response(status: 204);
    }
}
