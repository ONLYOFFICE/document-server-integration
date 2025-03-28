<?php

use App\Http\Controllers\API\Files\ReferenceController;
use App\Http\Controllers\API\Files\VersionController as FilesVersionController;
use App\Http\Controllers\API\FormatController;
use App\Http\Controllers\EditorController;
use App\Http\Controllers\FileController;
use App\Http\Controllers\ForgottenController;
use App\Http\Controllers\IndexController;
use App\Http\Controllers\VersionController;
use App\Http\Middleware\CheckAndDecodeJWTPayload;
use App\Http\Middleware\EnsureForgottenPageEnabled;
use App\Http\Middleware\EnsureJWTTokenIsPresent;
use App\Http\Middleware\EnsureUserDirectoryExists;
use Illuminate\Support\Facades\Route;

Route::middleware(EnsureUserDirectoryExists::class)->group(function () {
    Route::get('/', [IndexController::class, 'index'])->name('home');
    Route::prefix('files')->name('files.')->group(function () {
        Route::get('/', [FileController::class, 'index'])->name('index');
        Route::post('/upload', [FileController::class, 'upload'])->name('upload');
        Route::post('/convert', [FileController::class, 'convert'])->name('convert');
        Route::delete('/delete', [FileController::class, 'destroy'])->name('delete');
        Route::post('/saveas', [FileController::class, 'saveAs'])->name('saveas');
        Route::get('/history', [FileController::class, 'history'])->name('history');
        Route::post('/rename', [FileController::class, 'rename'])->name('rename');
        Route::get('/config', [FileController::class, 'config'])->name('config');

        Route::middleware(EnsureJWTTokenIsPresent::class)->group(function () {
            Route::get('/download', [FileController::class, 'download'])->name('download');

            Route::get('/versions/changes', [VersionController::class, 'changes'])->name('versions.changes');
            Route::get('/versions/previous', [VersionController::class, 'previous'])->name('versions.previous');
        });

        Route::prefix('forgotten')->name('forgotten.')->middleware(EnsureForgottenPageEnabled::class)->group(function () {
            Route::get('/', [ForgottenController::class, 'index'])->name('index');
            Route::delete('/{key}/delete', [ForgottenController::class, 'destroy'])->name('delete');
        });
    });
    Route::prefix('api')->group(function () {
        Route::get('/formats', [FormatController::class, 'index']);

        Route::prefix('files')->name('files.')->group(function () {
            Route::put('/versions/restore', [FilesVersionController::class, 'restore']);
            Route::post('/reference', [ReferenceController::class, 'get']);
        });
    });
    Route::prefix('editor')->name('editor.')->group(function () {
        Route::get('/', [EditorController::class, 'index'])->name('index');
        Route::middleware(CheckAndDecodeJWTPayload::class)
            ->post('/track', [EditorController::class, 'track'])
            ->name('track');
    });
});
