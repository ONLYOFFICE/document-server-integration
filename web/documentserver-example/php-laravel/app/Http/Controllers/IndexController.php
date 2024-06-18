<?php

namespace App\Http\Controllers;

use OnlyOffice\Storage;
use OnlyOffice\Languages;
use Illuminate\Http\Request;
use OnlyOffice\Users;

class IndexController extends Controller
{
    public function index(Request $request, Storage $storage, Users $users, Languages $languages)
    {
        return view('index', [
            'users' => $users->getAll(),
            'languages' => $languages->get(),
            'files' => $storage->all(),
            'user' => $request->user ?? cache('user'),
            'directUrlArg' => $request->input('directUrl', false),
        ]);
    }
}
