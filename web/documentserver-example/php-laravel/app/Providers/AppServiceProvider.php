<?php

namespace App\Providers;

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
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        //
    }
}
