// tools/js/read_version.js
const fs = require('fs');

const file = process.argv[2];
const json = JSON.parse(fs.readFileSync(file, 'utf8'));

const key = process.argv[3];
console.log(json[key]);
