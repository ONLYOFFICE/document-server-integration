<?php

use App\Http\Controllers\FileController;
use App\Http\Controllers\IndexController;
use Illuminate\Support\Facades\Route;

Route::get('/', [IndexController::class, 'index'])->name('home');
Route::prefix('files')->name('files.')->group(function() {
    Route::delete('/delete', [FileController::class, 'destroy'])->name('delete');
});