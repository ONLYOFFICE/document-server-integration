<?php

namespace App\Http\Middleware;

use App\OnlyOffice\Managers\JWTManager;
use App\OnlyOffice\Managers\SettingsManager;
use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Symfony\Component\HttpFoundation\Response;

class EnsureJWTTokenIsPresent
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $jwt = app(JWTManager::class);
        $settings = app(SettingsManager::class);
        $embeded = $request->has('dmode');

        if ($settings->getSetting('jwt.enabled') && $embeded == null && $settings->getSetting('jwt.use_for_request')) {
            if ($request->hasHeader($settings->getSetting('jwt.header'))) {
                $bearerToken = Str::after($request->header($settings->getSetting('jwt.header')), 'Bearer ');
                $token = $jwt->decode($bearerToken, $settings->getSetting('jwt.secret'));

                if (empty($token)) {
                    abort(498, 'Invalid JWT signature');
                }
            } else {
                abort(499, 'Expected JWT token');
            }
        }

        return $next($request);
    }
}
