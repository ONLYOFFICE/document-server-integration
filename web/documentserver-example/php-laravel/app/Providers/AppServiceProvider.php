<?php

namespace App\Providers;

use App\OnlyOffice\Storage\LocalStorage;
use OnlyOffice\Repositories\Files\FileRepository;
use OnlyOffice\Repositories\Files\LocalFileRepository;
use Illuminate\Support\ServiceProvider;
use OnlyOffice\Config;
use OnlyOffice\Storage\Storage;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        $this->app->singleton(Config::class, function () {
            return new Config();
        });
        $this->app->bind(FileRepository::class, LocalFileRepository::class);
        $this->app->bind(Storage::class, LocalStorage::class);
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        //
    }
}
