<?php

use App\Http\Controllers\API\FormatController;
use App\Http\Controllers\EditorController;
use App\Http\Controllers\FileController;
use App\Http\Controllers\IndexController;
use App\Http\Middleware\EnsureJWTTokenIsPresent;
use Illuminate\Support\Facades\Route;

Route::get('/', [IndexController::class, 'index'])->name('home');
Route::prefix('files')->name('files.')->group(function() {
    Route::post('/upload', [FileController::class, 'upload'])->name('upload');
    Route::post('/convert', [FileController::class, 'convert'])->name('convert');
    Route::delete('/delete', [FileController::class, 'destroy'])->name('delete');
    Route::get('/download', [FileController::class, 'download'])->name('download');
    Route::post('/saveas', [FileController::class, 'saveAs'])->name('saveas');
});
Route::prefix('api')->group(function() {
    Route::get('/formats', [FormatController::class, 'index']);
});
Route::prefix('editor')->name('editor.')->group(function () {
    Route::get('/', [EditorController::class, 'index'])->name('index');
    Route::middleware(EnsureJWTTokenIsPresent::class)
        ->post('/track', [EditorController::class, 'track'])
        ->name('track');
});