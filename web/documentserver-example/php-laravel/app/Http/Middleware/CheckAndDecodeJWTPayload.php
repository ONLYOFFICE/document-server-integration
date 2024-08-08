<?php

namespace App\Http\Middleware;

use App\Services\JWT;
use App\Services\ServerConfig;
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
        $config = app(ServerConfig::class);
        $jwt = app(JWT::class);
        $payload = null;
        $embeded = $request->has('dmode');

        if ($config->get('jwt.enabled') && $embeded == null && $config->get('jwt.use_for_request')) {
            if ($request->token) {
                $payload = $jwt->decode($request->token);
                $payload = json_decode(json_encode($payload), true);
            } elseif ($request->hasHeader($config->get('jwt.header'))) {
                $payload = $jwt->decode($request->bearerToken());
                $payload = json_decode($payload);
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
