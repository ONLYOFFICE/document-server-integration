<?php

namespace App\Http\Controllers\API;

use App\Http\Controllers\Controller;
use App\Repositories\FormatRepository;

class FormatController extends Controller
{
    public function index(FormatRepository $formats)
    {
        return response()->json([
            'formats' => $formats->all(),
        ]);
    }
}
