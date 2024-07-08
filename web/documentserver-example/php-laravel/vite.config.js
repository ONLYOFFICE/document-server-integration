import { defineConfig } from 'vite';
import laravel from 'laravel-vite-plugin';

export default defineConfig({
    plugins: [
        laravel({
            input: [
                'resources/css/app.css',
                'resources/css/media.css',
                'resources/js/forgotten.css',
                'resources/css/formats.js',
                'resources/js/app.js',
                'resources/js/forgotten.js',
            ],
            refresh: true,
        }),
    ],
    server: {
        host: '0.0.0.0',
        hmr: {
            host: 'localhost'
        },
    },
});
