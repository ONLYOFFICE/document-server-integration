<?php

namespace App\Http\Middleware;

use App\OnlyOffice\Managers\JWTManager;
use App\OnlyOffice\Managers\SettingsManager;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class CheckAndDecodeJWTPayload
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
        $payload = null;
        $embeded = $request->has('dmode');

        if ($settings->getSetting('jwt.enabled') && $embeded == null && $settings->getSetting('jwt.use_for_request')) {
            if ($request->token) {
                $payload = $jwt->decode($request->token, $settings->getSetting('jwt.secret'));
                $payload = json_decode(json_encode($payload), true);
            } elseif ($request->hasHeader($settings->getSetting('jwt.header'))) {
                $payload = $jwt->decode($request->bearerToken(), $settings->getSetting('jwt.secret'));
            } else {
                abort(499, 'Expected JWT token');
            }

            if (! $payload) {
                abort(498, 'Invalid JWT signature');
            }

            $request->merge(['payload' => $payload]);
        }

        return $next($request);
    }
}
