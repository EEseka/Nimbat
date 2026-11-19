// Safari 27.2 workaround (WebKit bug 329421, JetBrains CMP-10986): a Wasm call reached through a JS tail call
// receives an f32/f64 argument of 0 or 1 as NaN, so Compose draws nothing. Every skiko.mjs export wrapper is
// such a call; inside try it no longer is. Remove once Safari ships the fix.
// Adapted from White-Wind-LLC/table (Apache-2.0).
'use strict';

// `(...a) => (X = loadedWasm._["X"])(...a)` as skiko ships it, and minified as `(...e)=>(y=m._.X)(...e)`
const TAIL_CALL_WRAPPER =
    /\(\.\.\.([\w$]+)\)\s*=>\s*\(\s*([\w$]+)\s*=\s*([\w$]+)\._(\.[\w$]+|\[(["'])[\w$]+\5\])\s*\)\s*\(\s*\.\.\.\1\s*\)/g;

function rewriteTailCallWrappers(source) {
    let count = 0;
    const code = source.replace(TAIL_CALL_WRAPPER, (match, rest, binding, holder, member) => {
        count++;
        return `(...${rest}) => { try { return (${binding} = ${holder}._${member})(...${rest}) } catch (e) { throw e } }`;
    });
    return { code, count };
}

function findTailCallWrappers(code) {
    return Array.from(code.matchAll(TAIL_CALL_WRAPPER), (match) => match[0]);
}

function skikoNoTailCallLoader(source) {
    const { code, count } = rewriteTailCallWrappers(source);
    if (count === 0) {
        throw new Error(`${this.resourcePath}: no skiko wrapper matched, so the Safari 27.2 workaround would not apply`);
    }
    return code;
}

module.exports = skikoNoTailCallLoader;
module.exports.findTailCallWrappers = findTailCallWrappers;
