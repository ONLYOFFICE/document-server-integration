<?php

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
            ->__invoke(new FindAllForgottenFilesQuery());

        return view('forgotten', compact('files'));
    }

    public function destroy(string $key)
    {
        app(DeleteForgottenFileCommand::class)
            ->__invoke(new DeleteForgottenFileRequest($key));

        return response(status: 204);
    }
}
