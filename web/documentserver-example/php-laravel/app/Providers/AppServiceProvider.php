<?php

namespace App\Providers;

use OnlyOffice\Repositories\Files\FileRepository;
use OnlyOffice\Repositories\Files\LocalFileRepository;
use Illuminate\Support\ServiceProvider;
use OnlyOffice\Config;

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
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        //
    }
}
