module.exports = {
    context: __dirname,
    entry: "./src/app.js",
    output: {
        path: __dirname + "/dist",
        filename: "bundle.js",
    },
    watch: true,
    module: {
        rules: [{
            test: /\.js$/,
            exclude: /(node_modules)/,
            use: {
                loader: 'babel-loader',
                options: {
                    presets: ['@babel/preset-env', '@babel/preset-react']
                }
            }
        }]
    },
    plugins: [
        {
            apply: (compiler) => {
                compiler.hooks.done.tap('DonePlugin', (stats) => {
                    console.log('Compile is done !')
                    setTimeout(() => {
                        process.exit(0)
                    })
                });
            }
        }
    ]
}