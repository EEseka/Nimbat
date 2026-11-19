// Applies webApp/webpack/skiko-no-tail-call.cjs, and fails the build if minifying turns a wrapper back into a tail call
{
    const loaderPath = require('path').resolve(__dirname, '../../../../webApp/webpack/skiko-no-tail-call.cjs');
    const { findTailCallWrappers } = require(loaderPath);

    config.module.rules.push({ test: /[\\/]skiko\.mjs$/, loader: loaderPath });

    config.plugins.push({
        apply(compiler) {
            const { Compilation, WebpackError } = compiler.webpack;
            compiler.hooks.thisCompilation.tap('SkikoNoTailCallCheck', (compilation) => {
                compilation.hooks.processAssets.tap(
                    { name: 'SkikoNoTailCallCheck', stage: Compilation.PROCESS_ASSETS_STAGE_ANALYSE },
                    (assets) => {
                        for (const [name, asset] of Object.entries(assets)) {
                            if (!/\.m?js$/.test(name)) continue;
                            const left = findTailCallWrappers(asset.source().toString());
                            if (left.length > 0) {
                                compilation.errors.push(
                                    new WebpackError(`${name}: ${left.length} skiko wrapper(s) still tail-call Wasm, e.g. ${left[0]}`)
                                );
                            }
                        }
                    }
                );
            });
        }
    });
}
