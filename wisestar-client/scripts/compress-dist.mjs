/**
 * 构建产物预压缩。
 *
 * 为什么在构建期压：低配电脑（双核老 i7）上，运行时压 2MB+ 的 JS 会白占 CPU，
 * 预压一次后请求直接回 .gz，服务端只做文件 IO。
 * 只压文本类资源（js/css/html/svg/json），字体和二进图片本身已压缩。
 */
import { createReadStream, createWriteStream, promises as fs } from 'node:fs';
import { join, extname } from 'node:path';
import { createGzip } from 'node:zlib';
import { pipeline } from 'node:stream/promises';

const DIST = 'dist';
const TEXT_EXT = new Set(['.js', '.mjs', '.css', '.html', '.svg', '.json', '.txt']);
const MIN_SIZE = 1024;

async function walk(dir) {
  const entries = await fs.readdir(dir, { withFileTypes: true });
  for (const entry of entries) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) {
      await walk(full);
    } else if (TEXT_EXT.has(extname(entry.name))) {
      await compress(full);
    }
  }
}

async function compress(file) {
  const { size } = await fs.stat(file);
  if (size < MIN_SIZE) return;
  const target = `${file}.gz`;
  await pipeline(
    createReadStream(file),
    createGzip({ level: 9 }),
    createWriteStream(target),
  );
  const out = await fs.stat(target);
  console.log(`  ${file} ${(size / 1024).toFixed(0)}KB → ${(out.size / 1024).toFixed(0)}KB`);
}

await walk(DIST);
console.log('预压缩完成');
