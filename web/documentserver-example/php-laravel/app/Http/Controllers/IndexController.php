<?php

namespace App\Http\Controllers;

use OnlyOffice\Languages;
use Illuminate\Http\Request;
use OnlyOffice\DocumentStorage;
use OnlyOffice\Users;

class IndexController extends Controller
{
    public function index(Request $request, DocumentStorage $storage, Users $users, Languages $languages)
    {
        return view('index', [
            'users' => $users->getAll(),
            'languages' => $languages->get(),
            'files' => $storage->all($request->ip()),
            'user' => $request->user ?? cache('user'),
            'directUrlArg' => $request->input('directUrl', false),
        ]);
    }
}
