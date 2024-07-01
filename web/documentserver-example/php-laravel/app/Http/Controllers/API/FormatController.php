<?php

namespace App\Http\Controllers\API;

use App\Http\Controllers\Controller;
use OnlyOffice\Formats;
use Illuminate\Http\Request;

class FormatController extends Controller
{
    public function index(Formats $formats)
    {
        return response()->json([
            'formats' => $formats->all()
        ]);
    }
}
