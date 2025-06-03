import fs from 'fs';
import path from 'path';

const distPath = path.resolve('dist');
const publicPath = path.resolve('public');

const filesToCopy = ['widget.js', 'index.css'];

for (const file of filesToCopy) {
    const src = path.join(distPath, file);
    const dest = path.join(publicPath, file);
    if (fs.existsSync(src)) {
        fs.copyFileSync(src, dest);
        console.log(`Copied ${file} to public/`);
    } else {
        console.warn(`File ${file} not found in dist/`);
    }
}
