<?php

namespace App\Http\Middleware;

use App\Services\JWT;
use App\Services\ServerConfig;
use Closure;
use Illuminate\Http\Request;
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
        $config = app(ServerConfig::class);
        $jwt = app(JWT::class);
        $embeded = $request->has('dmode');

        if ($config->get('jwt.enabled') && $embeded == null && $config->get('jwt.use_for_request')) {
            if ($request->hasHeader($config->get('jwt.header'))) {
                $token = $jwt->decode($request->bearerToken());

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
