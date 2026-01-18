const fs = require('fs');

const [, , jsonPath, outPath] = process.argv;

const cfg = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

const content = `/**
 * ⚠️ AUTO GENERATED FILE
 * DO NOT EDIT MANUALLY
 * Generated from: ${jsonPath}
 */

export const ChannelConfig = ${JSON.stringify(cfg, null, 2)} as const;
`;

fs.writeFileSync(outPath, content, 'utf8');