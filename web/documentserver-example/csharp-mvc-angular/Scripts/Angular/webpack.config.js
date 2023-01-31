const path = require('path');
const webpack = require('webpack');
module.exports = {
    entry: {
        'polyfills': './src/polyfills.ts',
        'app': './src/main.ts'
    },
    output: {
        path: path.resolve(__dirname, './public'),
        publicPath: '/public/',
        filename: "[name].js"
    },
    resolve: {
        extensions: ['.ts', '.js']
    },
    module: {
        rules: [
            {
                test: /\.ts$/,
                use: [
                    {
                        loader: 'ts-loader',
                        options: { configFile: path.resolve(__dirname, 'tsconfig.json') }
                    },
                    'angular2-template-loader'
                ]
            }
        ]
    },
    plugins: [
        new webpack.ContextReplacementPlugin(
            /angular(\|\/)core/,
            path.resolve(__dirname, 'src'),
            {}
        )
    ]
}