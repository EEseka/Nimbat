// Runs SQLite for Room on the web. Speaks the message protocol documented on androidx's WebWorkerSQLiteDriver.
import sqlite3InitModule from '@sqlite.org/sqlite-wasm';

const databases = new Map();
const statements = new Map();
let nextDatabaseId = 0;
let nextStatementId = 0;

// The SAH pool keeps files in the browser's private storage without the cross-origin headers GitHub Pages can't send.
// Only one tab can hold it, so a second tab falls back to memory and forgets on close.
const ready = sqlite3InitModule().then(async (sqlite3) => {
    try {
        const pool = await sqlite3.installOpfsSAHPoolVfs({ name: 'nimbat' });
        return { sqlite3, openDatabase: (fileName) => new pool.OpfsSAHPoolDb(fileName) };
    } catch (error) {
        console.warn('Nimbat: saving on this device is unavailable, keeping data in memory', error);
        return { sqlite3, openDatabase: () => new sqlite3.oo1.DB(':memory:') };
    }
});

function open({ fileName }, { openDatabase }) {
    const databaseId = nextDatabaseId++;
    databases.set(databaseId, openDatabase(fileName));
    return { databaseId };
}

function prepare({ databaseId, sql }, { sqlite3 }) {
    const database = databases.get(databaseId);
    if (!database) throw new Error('Invalid database ID: ' + databaseId);

    const statement = database.prepare(sql);
    const statementId = nextStatementId++;
    statements.set(statementId, statement);

    const columnNames = [];
    for (let i = 0; i < statement.columnCount; i++) {
        columnNames.push(sqlite3.capi.sqlite3_column_name(statement, i));
    }
    return {
        statementId,
        parameterCount: sqlite3.capi.sqlite3_bind_parameter_count(statement),
        columnNames
    };
}

function step({ statementId, bindings }, { sqlite3 }) {
    const statement = statements.get(statementId);
    if (!statement) throw new Error('Invalid statement ID: ' + statementId);

    statement.reset();
    statement.clearBindings();
    bindings.forEach((value, index) => statement.bind(index + 1, value));

    const rows = [];
    const columnTypes = [];
    while (statement.step()) {
        if (columnTypes.length === 0) {
            for (let i = 0; i < statement.columnCount; i++) {
                columnTypes.push(sqlite3.capi.sqlite3_column_type(statement, i));
            }
        }
        rows.push(statement.get([]));
    }
    return { rows, columnTypes };
}

// IDs start at 0, which JavaScript treats as false in a plain if, so a missing ID is checked with != null
function close({ statementId, databaseId }) {
    if (statementId != null) {
        statements.get(statementId)?.finalize();
        statements.delete(statementId);
    }
    if (databaseId != null) {
        databases.get(databaseId)?.close();
        databases.delete(databaseId);
    }
}

const commands = { open, prepare, step };

self.onmessage = async ({ data: request }) => {
    const { id, data } = request;
    if (data?.cmd === 'close') {
        close(data);
        return;
    }
    try {
        const command = commands[data?.cmd];
        if (!command) throw new Error("Invalid request, unknown command: '" + data?.cmd + "'.");
        postMessage({ id, data: command(data, await ready) });
    } catch (error) {
        postMessage({ id, error: error.message });
    }
};
