<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class EnsureForgottenPageEnabled
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $forgottenPageEnabled = env('DOCUMENT_STORAGE_DISPLAY_FORGOTTEN_PAGE', false);

        if (! $forgottenPageEnabled) {
            abort(403);
        }

        return $next($request);
    }
}
