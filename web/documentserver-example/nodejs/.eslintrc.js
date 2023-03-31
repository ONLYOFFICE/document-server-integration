module.exports = {
  env: {
    browser: true,
    commonjs: true,
    es2021: true,
  },
  extends: ['airbnb-base'],
  overrides: [
  ],
  parserOptions: {
    ecmaVersion: 'latest',
  },
  rules: {
    'max-len': ['error', { code: 120 }],
    'quote-props': ['error', 'as-needed'],
    'prefer-destructuring': ['error', {
      array: true,
      object: true,
    }, {
      enforceForRenamedProperties: false,
    }],
    'func-style': ['error', 'expression'],
    'wrap-iife': ['error', 'outside'],
    'space-before-function-paren': ['error', 'always'],
    'function-paren-newline': ['error', 'multiline'],
    'arrow-parens': ['error', 'always'],
    'arrow-body-style': ['error', 'always'],
    // eslint-disable-next-line no-restricted-syntax
    'no-restricted-syntax': ['error', 'WithStatement', 'BinaryExpression[operator="in"]'],
    'no-console': 'off',
    'no-await-in-loop': 'off',
    'func-names': 'off',
    'no-prototype-builtins': 'off',
    'no-throw-literal': 'off',
    'no-continue': 'off',
    'default-param-last': 'off',
    'consistent-return': 'off',
    'no-constant-condition': 'off',
    'operator-linebreak': ['error', 'before'],
    'nonblock-statement-body-position': ['error', 'beside'],
    'spaced-comment': ['error', 'always'],
    'padded-blocks': ['error', 'never'],
    'template-curly-spacing': ['error', 'never'],
    'no-extend-native': ['error', { exceptions: ['String'] }],
    'object-curly-spacing': ['error', 'always'],
    'comma-dangle': ['error', 'only-multiline'],
    radix: ['error', 'as-needed']
  },
};
