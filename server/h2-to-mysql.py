#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 H2 CSVWRITE 导出结果转换成 MySQL 可直接执行的 data-only SQL。

背景：
    `server/db-snapshot/wisestar.sql` 是 H2 的 `SCRIPT` 全量导出，含
    `CREATE CACHED TABLE`、`ALTER TABLE ... ADD CONSTRAINT`、
    `U&'\\4e2d\\6587'` 等 H2 专有语法，MySQL 无法直接执行。本脚本读取
    `db-export-mysql.sh` 逐表导出的 CSV，输出列清单显式的
    `INSERT INTO \\`t_x\\` (...) VALUES (...);`，只搬数据、不碰结构，
    结构仍由 `init-mysql.sql` 负责。

用法（一般由 `server/db-export-mysql.sh` 调用）：
    python3 server/h2-to-mysql.py <csv_dir> <init-mysql.sql> <out.sql>

NULL 判定：
    H2 的 CSVWRITE 对空字符串输出 `""`（带引号），对 SQL NULL 输出空字段
    （不带引号）。因此这里用自带的最小 CSV 解析器区分「字段是否被引号包裹」，
    从而准确还原 NULL 与空串，无需哨兵值。

列对齐：
    目标表与目标列以 `init-mysql.sql` 为准（CREATE TABLE 与后续
    ADD COLUMN 的并集，保持脚本顺序）。CSV 里多出的列（H2 有、MySQL 无）
    跳过并打印警告，因此不会把废弃列带过去。

其它：
    - 每表先 `TRUNCATE` 再批量 `INSERT`，重复执行结果一致。
    - 字符串按 MySQL 默认转义模式（未开 NO_BACKSLASH_ESCAPES）处理。
    - 整份脚本包在单个事务内，末尾 `COMMIT`。
"""

import os
import re
import sys
from collections import OrderedDict

# 单条 INSERT 的最大行数与最大字符数（先到者生效），避免超出 max_allowed_packet。
BATCH_ROWS = 200
BATCH_CHARS = 512 * 1024

CREATE_RE = re.compile(
    r"CREATE TABLE IF NOT EXISTS\s+`([a-z0-9_]+)`\s*\((.*?)\n\)",
    re.IGNORECASE | re.DOTALL,
)
ADD_COLUMN_RE = re.compile(
    r"ALTER TABLE\s+`([a-z0-9_]+)`\s+ADD COLUMN(?:\s+IF NOT EXISTS)?\s+`([a-z0-9_]+)`",
    re.IGNORECASE,
)
# 直接匹配全局的「反引号列名 + 类型」。索引声明里的 `` KEY `idx` (`col`) `` 因为
# 类型词不在其后紧随，不会命中；`PRIMARY KEY (`id`)` 同理。这样即使同一行写了
# 多个列定义（如 t_sys_info 的 setting/ai_setting）也不会漏读。
COLUMN_RE = re.compile(
    r"`([a-z0-9_]+)`\s+"
    r"(varchar|char|text|longtext|mediumtext|tinytext|int|bigint|smallint|"
    r"tinyint|timestamp|datetime|date|time|decimal|double|float|json|blob|boolean|bit)",
    re.IGNORECASE,
)


def parse_mysql_schema(path):
    """返回 OrderedDict{表名: [列名...]}，顺序为 CREATE 顺序 + 后续 ADD COLUMN 追加。"""
    text = open(path, encoding="utf-8").read()
    tables = OrderedDict()
    for match in CREATE_RE.finditer(text):
        table = match.group(1).lower()
        columns = []
        for col in COLUMN_RE.finditer(match.group(2)):
            name = col.group(1).lower()
            if name not in columns:
                columns.append(name)
        tables[table] = columns
    for match in ADD_COLUMN_RE.finditer(text):
        table, column = match.group(1).lower(), match.group(2).lower()
        if table in tables and column not in tables[table]:
            tables[table].append(column)
    return tables


def parse_csv(text):
    """解析 CSV，返回 [(值, 是否带引号), ...] 的行列表。

    支持双引号包裹、`""` 转义、字段内换行与逗号；不依赖 csv 模块，
    因为需要保留「字段是否被引号包裹」这一信息来区分 NULL 与空串。
    """
    rows = []
    row = []
    field = []
    quoted = False
    in_quotes = False
    i = 0
    length = len(text)
    while i < length:
        ch = text[i]
        if in_quotes:
            if ch == '"':
                if i + 1 < length and text[i + 1] == '"':
                    field.append('"')
                    i += 2
                    continue
                in_quotes = False
                i += 1
                continue
            field.append(ch)
            i += 1
            continue
        if ch == '"':
            in_quotes = True
            quoted = True
            i += 1
            continue
        if ch == ",":
            row.append(("".join(field), quoted))
            field = []
            quoted = False
            i += 1
            continue
        if ch == "\n":
            row.append(("".join(field), quoted))
            rows.append(row)
            row = []
            field = []
            quoted = False
            i += 1
            continue
        if ch == "\r":
            i += 1
            continue
        field.append(ch)
        i += 1
    if field or quoted or row:
        row.append(("".join(field), quoted))
        rows.append(row)
    return [r for r in rows if r]


def sql_literal(value, quoted):
    """把 CSV 字段转成 SQL 字符串字面量。

    单引号用标准 SQL 的 `''` 翻倍表示（MySQL 与 H2 都接受），反斜杠按 MySQL
    默认转义模式写成 `\\\\`。H2 的 CSVWRITE 原样输出反斜杠，若不翻倍，MySQL
    会把 `\\n` 等序列误当转义。
    """
    if not quoted:
        return "NULL"
    out = ["'"]
    for ch in value:
        if ch == "\\":
            out.append("\\\\")
        elif ch == "'":
            out.append("''")
        elif ch == "\0":
            out.append("\\0")
        elif ch == "\n":
            out.append("\\n")
        elif ch == "\r":
            out.append("\\r")
        elif ch == "\x1a":
            out.append("\\Z")
        else:
            out.append(ch)
    out.append("'")
    return "".join(out)


def convert(csv_dir, schema_path, out_path):
    schema = parse_mysql_schema(schema_path)
    files = {
        name[: -len(".csv")]: name
        for name in os.listdir(csv_dir)
        if name.endswith(".csv")
    }

    stats = []
    warnings = []
    with open(out_path, "w", encoding="utf-8", newline="\n") as out:
        out.write("-- 由 server/h2-to-mysql.py 生成，请勿手改。\n")
        out.write("-- 用途：把 H2 预览库的业务数据导入已由 init-mysql.sql 建好结构的 MySQL 库。\n")
        out.write("-- 用法：gunzip -c server/db-snapshot/wisestar-mysql.sql.gz | mysql -u root -p wisestar\n\n")
        out.write("SET NAMES utf8mb4;\n")
        out.write("SET FOREIGN_KEY_CHECKS = 0;\n")
        out.write("SET UNIQUE_CHECKS = 0;\n")
        out.write("SET autocommit = 0;\n\n")

        for table, columns in schema.items():
            csv_name = files.get(table)
            if not csv_name:
                warnings.append(f"H2 中不存在表 {table}，跳过")
                continue
            text = open(os.path.join(csv_dir, csv_name), encoding="utf-8", newline="").read()
            rows = parse_csv(text)
            if not rows:
                warnings.append(f"{table}: CSV 为空，跳过")
                continue
            header = [h.lower() for h, _ in rows[0]]
            body = rows[1:]
            keep = [(header.index(c), c) for c in columns if c in header]
            dropped = [h for h in header if h not in columns]
            if dropped:
                warnings.append(f"{table}: H2 有而 MySQL 无的列已跳过 {dropped}")
            if not keep:
                warnings.append(f"{table}: 没有交集列，跳过")
                continue

            out.write(f"-- {table}: {len(body)} 行\n")
            out.write(f"TRUNCATE TABLE `{table}`;\n")
            col_sql = ", ".join(f"`{c}`" for _, c in keep)
            prefix = f"INSERT INTO `{table}` ({col_sql}) VALUES\n"

            batch = []
            size = 0
            written = 0

            def flush():
                nonlocal batch, size, written
                if not batch:
                    return
                out.write(prefix)
                out.write(",\n".join(batch))
                out.write(";\n")
                written += len(batch)
                batch = []
                size = 0

            for row in body:
                if len(row) != len(header):
                    warnings.append(
                        f"{table}: 行列数 {len(row)} 与表头 {len(header)} 不一致，已跳过"
                    )
                    continue
                values = "(" + ", ".join(sql_literal(row[i][0], row[i][1]) for i, _ in keep) + ")"
                batch.append(values)
                size += len(values) + 2
                if len(batch) >= BATCH_ROWS or size >= BATCH_CHARS:
                    flush()
            flush()
            out.write("\n")
            stats.append((table, written, len(body)))

        out.write("COMMIT;\n")
        out.write("SET UNIQUE_CHECKS = 1;\n")
        out.write("SET FOREIGN_KEY_CHECKS = 1;\n")

    return stats, warnings


def main():
    if len(sys.argv) != 4:
        print("用法：h2-to-mysql.py <csv_dir> <init-mysql.sql> <out.sql>", file=sys.stderr)
        return 2
    csv_dir, schema_path, out_path = sys.argv[1:]
    stats, warnings = convert(csv_dir, schema_path, out_path)

    total = sum(written for _, written, _ in stats)
    print(f"已生成 {out_path}")
    print(f"表 {len(stats)} 张，写入 {total} 行，{os.path.getsize(out_path)} bytes")
    short = [(t, w, e) for t, w, e in stats if w != e]
    if short:
        print("以下表写入行数少于 CSV 行数：")
        for table, written, expected in short:
            print(f"  {table}: {written}/{expected}")
    if warnings:
        print("警告：")
        for item in warnings:
            print(f"  - {item}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
