<?php

namespace App\Providers;

use App\Repositories\FormatRepository;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        $this->app->singleton(FormatRepository::class, function () {
            $path = public_path('assets/document-formats/onlyoffice-docs-formats.json');

            return new FormatRepository($path);
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
