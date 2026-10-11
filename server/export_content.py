#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
智慧星内容导出处理器。

读取 ``<out>/raw/*.csv``（由 export-content.sh 用 H2 CSVWRITE 导出），生成：

- ``math_structure.xlsx``  知识点体系：学科 / 章节 / 小节 / 知识点 / 绑定题目 / 题库
- ``math_questions.xlsx``  题目清单（前 30 列为标准单表模板，可回导）
- ``english_words.xlsx`` / ``english_sentences.xlsx`` / ``english_grammar.xlsx``
- ``math_content.json`` / ``english_content.json``  全字段结构化数据
- ``files/``               题目/单词/句子引用的图片与音频
- ``file_index.csv``       引用地址 -> 本地文件 的映射与落盘情况
- ``README.md``            导出说明

用法：
    python3 export_content.py [out_dir] [--english-media] [--all-files]
``out_dir`` 默认取脚本所在目录。默认不导出英语单词图（可由 AI 重新生成）。
"""

import csv
import json
import os
import re
import shutil
import sys
from collections import OrderedDict, defaultdict
from datetime import datetime

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.worksheet import Worksheet

# ---------------------------------------------------------------------------
# 路径与常量
# ---------------------------------------------------------------------------
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
# 默认取脚本上一级作为导出根目录（脚本位于 <repo>/server/ 时输出到 <repo>/export）
DEFAULT_OUT = os.environ.get("EXPORT_OUT_DIR") or os.path.join(
    os.path.dirname(SCRIPT_DIR), "export"
)
# 本地存储目录：preview profile 下 file-storage.local.root-path = server/api/files
FILES_DIR = os.environ.get("WISESTAR_FILES_DIR") or "/workspace/server/api/files"

FILE_ID_RE = re.compile(r"/api/file\?id=([A-Za-z0-9_\-]+)")

STANDARD_HEADERS = [
    "学科", "题型", "章节", "小节", "知识点", "题目",
    "选项A", "选项B", "选项C", "选项D", "选项E", "选项F", "选项G", "选项H",
    "难易程度",
    "正确答案1", "正确答案2", "正确答案3", "正确答案4", "正确答案5", "正确答案6",
    "正确答案7", "正确答案8", "正确答案9", "正确答案10", "正确答案11", "正确答案12",
    "解析", "标签", "图片",
]
STANDARD_WIDTHS = [14, 12, 16, 14, 22, 50, 12, 12, 12, 12, 12, 12, 12, 12, 10,
                   12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 42, 24, 40]

TYPE_LABELS = {
    "Judge": "判断", "Radio": "单选", "FillBlank": "单项填空",
    "Checkbox": "多选", "MultipleBlank": "多项填空", "Textarea": "简答题",
}
DIFFICULTY_LABELS = {"easy": "简单", "medium": "中等", "hard": "困难"}
MAX_ANSWER_CELLS = 12
HEADER_FILL = PatternFill("solid", fgColor="DDEBF7")
HEADER_FONT = Font(bold=True)


# ---------------------------------------------------------------------------
# 读取
# ---------------------------------------------------------------------------
def load(name, where):
    path = os.path.join(where, name)
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8") as fh:
        return list(csv.DictReader(fh))


def live(rows):
    return [r for r in rows if r.get("is_deleted", "0") == "0"]


def first(*vals):
    for v in vals:
        if v:
            return v
    return ""


# ---------------------------------------------------------------------------
# 题目解析（对齐 RepoServiceImpl 标准模板口径）
# ---------------------------------------------------------------------------
def type_label(qtype):
    return TYPE_LABELS.get(qtype, qtype or "")


def difficulty_label(v):
    if not v:
        return ""
    return DIFFICULTY_LABELS.get(str(v).strip().lower(), str(v).strip())


def letter_of(i):
    return chr(ord("A") + i)


def split_answers(text):
    if not text:
        return []
    return [t.strip() for t in re.split(r"[\n,，、|;；]", text) if t.strip()]


def normalize_judge(value):
    if not value:
        return ""
    v = value.strip().lower()
    if v in ("正确", "对", "true", "1", "t", "√", "y"):
        return "正确"
    if v in ("错误", "错", "false", "0", "f", "×", "n"):
        return "错误"
    return value.strip()


def index_of_option(children, token):
    if token is None:
        return None
    t = token.strip()
    if len(t) == 1 and t.isalpha():
        idx = ord(t.upper()) - ord("A")
        if 0 <= idx < len(children):
            return idx
    for i, opt in enumerate(children):
        oid = opt.get("id")
        title = opt.get("title")
        if oid and t.lower() == str(oid).lower():
            return i
        if title and t == str(title).strip():
            return i
    return None


def answer_cells(qtype, attr, children):
    cells = []
    top = attr.get("examCorrectAnswer")
    if qtype == "Judge":
        value = top
        if not value:
            for c in children:
                ca = (c.get("attribute") or {}).get("examCorrectAnswer")
                if ca:
                    value = c.get("title") or ca
                    break
        cells.append(normalize_judge(value))
        return cells[:MAX_ANSWER_CELLS]
    if qtype in ("Radio", "Checkbox"):
        if top and str(top).strip():
            direct = index_of_option(children, str(top).strip())
            if direct is not None:
                cells.append(letter_of(direct))
            else:
                for tok in split_answers(str(top)):
                    idx = index_of_option(children, tok)
                    cells.append(letter_of(idx) if idx is not None else tok)
        else:
            for i, c in enumerate(children):
                ca = (c.get("attribute") or {}).get("examCorrectAnswer")
                if ca:
                    cells.append(letter_of(i))
        return cells[:MAX_ANSWER_CELLS]
    # FillBlank / MultipleBlank / 其他
    if top and str(top).strip():
        for piece in str(top).split("|"):
            cells.append(piece.strip())
    else:
        for c in children:
            ca = (c.get("attribute") or {}).get("examCorrectAnswer")
            if ca:
                cells.append(str(ca).strip())
    return cells[:MAX_ANSWER_CELLS]


def decode_question(row):
    """把一条 t_template 记录解析成结构化字典。"""
    qtype = row.get("question_type") or ""
    schema = {}
    try:
        schema = json.loads(row.get("template") or "{}")
    except Exception:
        schema = {}
    attr = schema.get("attribute") or {}
    children = schema.get("children") or []

    subject = first(row.get("subject"), attr.get("subject"))
    chapter = first(row.get("chapter"), attr.get("chapter"))
    section = first(row.get("section"), attr.get("section"))
    difficulty = first(row.get("difficulty"), attr.get("difficulty"))
    top_kps = parse_json_array(row.get("knowledge_point"))
    attr_kps = attr.get("knowledgePoint") or []
    kps = top_kps or (attr_kps if isinstance(attr_kps, list) else [])
    tags = parse_json_array(row.get("tag")) or parse_json_array(schema.get("tags"))

    exam_images = attr.get("examImages") or []
    if isinstance(exam_images, str):
        exam_images = [exam_images]
    return {
        "id": row.get("id"),
        "repoId": row.get("repo_id") or "",
        "serialNo": row.get("serial_no") or "",
        "name": row.get("name") or "",
        "questionType": qtype,
        "typeLabel": type_label(qtype),
        "title": schema.get("title") or "",
        "options": [{"id": c.get("id"), "title": c.get("title") or "",
                     "correct": bool((c.get("attribute") or {}).get("examCorrectAnswer"))}
                    for c in children],
        "answers": answer_cells(qtype, attr, children),
        "analysis": attr.get("examAnalysis") or "",
        "subject": subject,
        "chapter": chapter,
        "section": section,
        "knowledgePoints": kps,
        "difficulty": difficulty,
        "difficultyLabel": difficulty_label(difficulty),
        "category": row.get("category") or "",
        "mode": row.get("mode") or "",
        "priority": row.get("priority") or "",
        "grade": row.get("grade") or "",
        "shared": row.get("shared") or "",
        "previewUrl": row.get("preview_url") or "",
        "examImages": exam_images,
        "examOptionImage": attr.get("examOptionImage") or "",
        "tags": tags,
        "templateRaw": row.get("template") or "",
    }


def parse_json_array(value):
    if not value:
        return []
    try:
        data = json.loads(value)
    except Exception:
        return [value]
    if isinstance(data, list):
        return data
    if isinstance(data, str):
        return [data]
    return []


# ---------------------------------------------------------------------------
# Excel 工具
# ---------------------------------------------------------------------------
def style_header(ws, ncols, widths=None):
    for c in range(1, ncols + 1):
        cell = ws.cell(row=1, column=c)
        cell.fill = HEADER_FILL
        cell.font = HEADER_FONT
        cell.alignment = Alignment(vertical="center")
        ws.column_dimensions[get_column_letter(c)].width = (
            widths[c - 1] if widths and c - 1 < len(widths) else 16
        )
    ws.freeze_panes = "A2"


def write_sheet(wb, title, headers, rows, widths=None):
    ws = wb.create_sheet(title)
    ws.append(headers)
    for r in rows:
        ws.append(r)
    style_header(ws, len(headers), widths)
    return ws


# ---------------------------------------------------------------------------
# 引用收集与文件落盘
# ---------------------------------------------------------------------------
def collect_refs(questions, kps, english_words, english_sentences, sections,
                 include_english=False):
    """返回 (ref->来源集合, file_id 集合)。

    默认不收集英语单词/句子媒体（单词图可由 AI 重新生成，省去大量小图）；
    传 ``include_english=True`` 时一并导出。
    """
    refs = defaultdict(set)          # 原始引用文本 -> 来源
    file_ids = set()                 # 解析出的文件 id

    def add(ref, source):
        ref = (ref or "").strip()
        if not ref:
            return
        refs[ref].add(source)
        m = FILE_ID_RE.search(ref)
        if m:
            file_ids.add(m.group(1))

    for q in questions:
        for u in q["examImages"]:
            add(u, "题目配图")
            if isinstance(u, str) and "/" not in u and u:
                file_ids.add(u.strip())
        add(q["examOptionImage"], "选项图")
        # 题干/选项/解析中的内联引用
        for blob in (q["title"], q["analysis"]):
            for m in FILE_ID_RE.findall(blob or ""):
                file_ids.add(m)
        for opt in q["options"]:
            for m in FILE_ID_RE.findall(opt.get("title") or ""):
                file_ids.add(m)
    for kp in kps:
        add(kp.get("image_url"), "知识点图片")
    for w in (english_words if include_english else []):
        add(w.get("image_url"), "英语单词图片")
        add(w.get("audio_url"), "英语单词音频")
    for s in (english_sentences if include_english else []):
        add(s.get("audio_url"), "英语句子音频")
    for s in sections:
        for m in FILE_ID_RE.findall(s.get("content") or ""):
            file_ids.add(m)
    return refs, file_ids


def export_files(file_rows, file_ids, refs, out_files, copy_all, clear=True):
    """把引用到的文件复制到 out_files，返回 (index_rows, stats)。

    ``clear=True`` 时先清空目标目录，保证导出结果与该次引用集合完全一致
    （避免上一轮的旧文件残留）。
    """
    by_id = {r["id"]: r for r in file_rows}
    index_rows = []
    to_copy = set(file_ids)
    if copy_all:
        to_copy |= {r["id"] for r in live(file_rows)}
    copied, missing_src, missing_meta, total_bytes = 0, 0, 0, 0
    if clear and os.path.isdir(out_files):
        shutil.rmtree(out_files)
    os.makedirs(out_files, exist_ok=True)
    # 记录每个 id 被哪些来源引用
    id_sources = defaultdict(set)
    for ref, srcs in refs.items():
        m = FILE_ID_RE.search(ref)
        fid = m.group(1) if m else (ref if "/" not in ref and ref in by_id else None)
        if fid:
            id_sources[fid] |= srcs

    for fid in sorted(to_copy):
        meta = by_id.get(fid)
        if not meta:
            missing_meta += 1
            index_rows.append([fid, "", "", "", "在库中未找到该文件记录",
                               "；".join(sorted(id_sources.get(fid, [])))])
            continue
        src = os.path.join(os.path.dirname(FILES_ABS), "files", meta["file_path"])
        # 兼容不同存储目录
        if not os.path.exists(src):
            src = os.path.join(FILES_DIR, meta["file_path"])
        dst = os.path.join(out_files, meta["file_path"])
        if os.path.exists(src):
            shutil.copy2(src, dst)
            copied += 1
            total_bytes += os.path.getsize(src)
            status = "已导出"
        else:
            missing_src += 1
            status = "磁盘文件缺失"
        index_rows.append([fid, meta["original_name"], meta["file_path"],
                           meta["thumb_file_path"], status,
                           "；".join(sorted(id_sources.get(fid, [])))])
    stats = {"copied": copied, "missing_src": missing_src,
             "missing_meta": missing_meta, "bytes": total_bytes}
    return index_rows, stats


FILES_ABS = FILES_DIR


# ---------------------------------------------------------------------------
# 主流程
# ---------------------------------------------------------------------------
def main():
    global FILES_ABS
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    copy_all = "--all-files" in sys.argv
    include_english = "--english-media" in sys.argv
    out = args[0] if args else DEFAULT_OUT
    raw = os.path.join(out, "raw")
    FILES_ABS = FILES_DIR

    read = lambda n: load(n, raw)
    subjects = live(read("t_subject.csv"))
    chapters = live(read("t_chapter.csv"))
    sections = live(read("t_section.csv"))
    kps = live(read("t_knowledge_point.csv"))
    kp_q = live(read("t_knowledge_point_question.csv"))
    repos = live(read("t_repo.csv"))
    templates = live(read("t_template.csv"))
    files = read("t_file.csv")
    en_words = live(read("t_english_word.csv"))
    en_sentences = live(read("t_english_sentence.csv"))
    en_grammar = live(read("t_english_grammar.csv"))
    en_grammar_book = live(read("t_english_grammar_book.csv"))
    en_word_book = live(read("t_english_word_book.csv"))

    questions = [decode_question(r) for r in templates]

    # 索引
    subj_by_id = {s["id"]: s for s in subjects}
    chap_by_id = {c["id"]: c for c in chapters}
    sect_by_id = {s["id"]: s for s in sections}
    repo_by_id = {r["id"]: r for r in repos}
    q_by_id = {q["id"]: q for q in questions}
    kp_by_id = {k["id"]: k for k in kps}

    # 题目名索引（用于知识点名称匹配）
    q_by_kpname = defaultdict(list)
    for q in questions:
        for name in q["knowledgePoints"]:
            q_by_kpname[str(name).strip()].append(q["id"])

    kp_bound = defaultdict(list)
    for link in kp_q:
        kp_bound[link["knowledge_point_id"]].append(link["question_id"])

    # ---------- math_structure.xlsx ----------
    wb = Workbook()
    wb.remove(wb.active)

    ws_subj = write_sheet(wb, "学科", ["学科ID", "名称", "编码", "图标", "排序"],
                          [[s["id"], s["name"], s.get("code", ""), s.get("icon", ""), s.get("sort", "")]
                           for s in subjects], [20, 14, 12, 10, 8])

    ws_chap = write_sheet(
        wb, "章节",
        ["章节ID", "学科", "章节名称", "年级", "学期", "版本", "重要程度", "排序"],
        [[c["id"], subj_by_id.get(c["subject_id"], {}).get("name", ""), c["name"],
          c.get("grade", ""), c.get("term", ""), c.get("version", ""),
          c.get("importance", ""), c.get("sort", "")] for c in chapters],
        [20, 12, 28, 10, 8, 12, 10, 8])

    sect_rows = []
    for s in sections:
        ch = chap_by_id.get(s["chapter_id"], {})
        subs = subj_by_id.get(ch.get("subject_id", ""), {})
        sect_rows.append([s["id"], subs.get("name", ""), ch.get("name", ""), s["name"],
                          s.get("grade", ""), s.get("term", ""), s.get("importance", ""),
                          s.get("type", ""), s.get("sort", "")])
    write_sheet(wb, "小节",
                ["小节ID", "学科", "章节", "小节名称", "年级", "学期", "重要程度", "类型", "排序"],
                sect_rows, [20, 12, 22, 26, 10, 8, 10, 10, 8])

    kp_rows = []
    for k in kps:
        sect = sect_by_id.get(k["section_id"], {})
        ch = chap_by_id.get(sect.get("chapter_id", ""), {})
        subs = subj_by_id.get(ch.get("subject_id", ""), {})
        bound = kp_bound.get(k["id"], [])
        matched = q_by_kpname.get(str(k["name"]).strip(), [])
        kp_rows.append([
            k["id"], subs.get("name", ""), ch.get("name", ""), sect.get("name", ""),
            k["name"], k.get("importance", ""), k.get("grade", ""), k.get("term", ""),
            k.get("sort", ""), len(set(bound) | set(matched)),
            k.get("content", ""), k.get("image_url", ""),
        ])
    write_sheet(wb, "知识点",
                ["知识点ID", "学科", "章节", "小节", "知识点", "重要程度", "年级", "学期",
                 "排序", "关联题目数", "内容(JSON)", "图片"],
                kp_rows, [20, 10, 20, 22, 26, 10, 8, 8, 8, 12, 50, 30])

    bind_rows = []
    for k in kps:
        sect = sect_by_id.get(k["section_id"], {})
        for qid in sorted(set(kp_bound.get(k["id"], []))):
            q = q_by_id.get(qid)
            bind_rows.append([k["name"], sect.get("name", ""), qid,
                              q["name"] if q else "(题目不存在)"])
    write_sheet(wb, "知识点绑定题目", ["知识点", "小节", "题目ID", "题目"], bind_rows, [26, 22, 22, 50])

    write_sheet(wb, "题库",
                ["题库ID", "名称", "分类", "模式", "学科", "年级", "难度", "共享", "优先级", "描述"],
                [[r["id"], r["name"], r.get("category", ""), r.get("mode", ""),
                  r.get("subject", ""), r.get("grade", ""), r.get("difficulty", ""),
                  r.get("shared", ""), r.get("priority", ""), (r.get("description") or "")[:200]]
                 for r in repos], [22, 26, 14, 10, 10, 10, 10, 8, 8, 50])
    wb.save(os.path.join(out, "math_structure.xlsx"))

    # ---------- math_questions.xlsx（前 30 列标准模板） ----------
    wb = Workbook()
    ws = wb.active
    ws.title = "题目列表"
    extra = ["题目ID", "所属题库", "原始题型", "选项图URL", "知识点(JSON)", "分类", "模式", "分级", "图片JSON"]
    ws.append(STANDARD_HEADERS + extra)
    for q in questions:
        opts = [o["title"] for o in q["options"]]
        opt_cells = (opts + [""] * 8)[:8]
        ans = (q["answers"] + [""] * MAX_ANSWER_CELLS)[:MAX_ANSWER_CELLS]
        kp_text = "、".join(str(x) for x in q["knowledgePoints"])
        img_text = "\n".join(str(x) for x in q["examImages"] if x)
        repo_name = repo_by_id.get(q["repoId"], {}).get("name", "")
        ws.append([
            q["subject"], q["typeLabel"], q["chapter"], q["section"], kp_text, q["title"],
            *opt_cells, q["difficultyLabel"], *ans, q["analysis"],
            "、".join(str(t) for t in q["tags"]), img_text,
            q["id"], repo_name, q["questionType"], q["examOptionImage"],
            json.dumps(q["knowledgePoints"], ensure_ascii=False),
            q["category"], q["mode"], q["grade"], json.dumps(q["examImages"], ensure_ascii=False),
        ])
    style_header(ws, len(STANDARD_HEADERS) + len(extra), STANDARD_WIDTHS + [22, 22, 12, 30, 22, 12, 10, 8, 30])
    wb.save(os.path.join(out, "math_questions.xlsx"))

    # ---------- english ----------
    wb = Workbook()
    wb.remove(wb.active)
    write_sheet(wb, "单词",
                ["ID", "拼写", "音标", "释义", "图片", "音频", "例句", "版本", "年级", "学期", "单元", "小节"],
                [[w["id"], w["spell"], w.get("phonetic", ""), w.get("meaning", ""),
                  w.get("image_url", ""), w.get("audio_url", ""), w.get("example_sentence", ""),
                  w.get("version", ""), w.get("grade", ""), w.get("term", ""),
                  w.get("unit", ""), w.get("section", "")] for w in en_words],
                [20, 16, 16, 30, 30, 30, 40, 10, 10, 8, 10, 10])
    wb.save(os.path.join(out, "english_words.xlsx"))

    wb = Workbook()
    wb.remove(wb.active)
    write_sheet(wb, "句子",
                ["ID", "英文", "中文", "音频", "版本", "年级", "学期", "单元", "小节", "排序"],
                [[s["id"], s["en"], s.get("zh", ""), s.get("audio_url", ""),
                  s.get("version", ""), s.get("grade", ""), s.get("term", ""),
                  s.get("unit", ""), s.get("section", ""), s.get("sort", "")] for s in en_sentences],
                [20, 40, 40, 30, 10, 10, 8, 10, 10, 8])
    wb.save(os.path.join(out, "english_sentences.xlsx"))

    wb = Workbook()
    wb.remove(wb.active)
    write_sheet(wb, "语法",
                ["ID", "标题", "内容", "例句", "练习", "年级", "版本", "学期", "单元", "小节", "排序"],
                [[g["id"], g["title"], g.get("content", ""), g.get("examples", ""),
                  g.get("exercises", ""), g.get("grade", ""), g.get("version", ""),
                  g.get("term", ""), g.get("unit", ""), g.get("section", ""), g.get("sort", "")]
                 for g in en_grammar],
                [20, 26, 50, 50, 50, 10, 10, 8, 10, 10, 8])
    wb.save(os.path.join(out, "english_grammar.xlsx"))

    # ---------- JSON ----------
    def build_math_json():
        tree = OrderedDict()
        tree["generatedAt"] = datetime.now().isoformat(timespec="seconds")
        subjects_out = []
        for s in subjects:
            s_out = OrderedDict(s)
            chs = []
            for c in chapters:
                if c.get("subject_id") != s["id"]:
                    continue
                c_out = OrderedDict(c)
                sects = []
                for sec in sections:
                    if sec.get("chapter_id") != c["id"]:
                        continue
                    sec_out = OrderedDict(sec)
                    kps_out = []
                    for k in kps:
                        if k.get("section_id") != sec["id"]:
                            continue
                        bound = sorted(set(kp_bound.get(k["id"], [])) |
                                       set(q_by_kpname.get(str(k["name"]).strip(), [])))
                        k_out = OrderedDict(k)
                        k_out["questionIds"] = bound
                        kps_out.append(k_out)
                    sec_out["knowledgePoints"] = kps_out
                    sects.append(sec_out)
                c_out["sections"] = sects
                chs.append(c_out)
            s_out["chapters"] = chs
            subjects_out.append(s_out)
        tree["subjects"] = subjects_out
        tree["repos"] = repos
        return tree

    with open(os.path.join(out, "math_content.json"), "w", encoding="utf-8") as fh:
        json.dump(build_math_json(), fh, ensure_ascii=False, indent=1)

    with open(os.path.join(out, "math_questions.json"), "w", encoding="utf-8") as fh:
        json.dump(questions, fh, ensure_ascii=False, indent=1)

    english_json = OrderedDict()
    english_json["generatedAt"] = datetime.now().isoformat(timespec="seconds")
    english_json["words"] = en_words
    english_json["sentences"] = en_sentences
    english_json["grammar"] = en_grammar
    english_json["grammarBook"] = en_grammar_book
    english_json["wordBook"] = en_word_book
    with open(os.path.join(out, "english_content.json"), "w", encoding="utf-8") as fh:
        json.dump(english_json, fh, ensure_ascii=False, indent=1)

    # ---------- 图片/音频文件 ----------
    refs, file_ids = collect_refs(questions, kps, en_words, en_sentences, sections,
                                  include_english=include_english)
    index_rows, stats = export_files(files, file_ids, refs,
                                     os.path.join(out, "files"), copy_all)
    with open(os.path.join(out, "file_index.csv"), "w", encoding="utf-8", newline="") as fh:
        wr = csv.writer(fh)
        wr.writerow(["文件ID", "原始文件名", "磁盘文件名", "缩略图文件名", "状态", "被引用场景"])
        wr.writerows(index_rows)

    # ---------- README ----------
    mb = stats["bytes"] / 1024 / 1024
    readme = f"""# 智慧星内容导出

生成时间：{datetime.now().isoformat(timespec='seconds')}

## 内容一览

| 文件 | 说明 |
| --- | --- |
| `math_structure.xlsx` | 知识点体系：学科 / 章节 / 小节 / 知识点 / 知识点绑定题目 / 题库 |
| `math_questions.xlsx` | 全部在用题目（{len(questions)} 道）。前 30 列为标准单表模板，可直接回导；后附 ID、题库、选项图等元信息 |
| `english_words.xlsx` | 英语单词（{len(en_words)} 条，含图片/音频地址） |
| `english_sentences.xlsx` | 英语句子（{len(en_sentences)} 条，含音频地址） |
| `english_grammar.xlsx` | 英语语法（{len(en_grammar)} 条） |
| `math_content.json` | 学科→章节→小节→知识点的完整层级 JSON（含原始字段） |
| `math_questions.json` | 全部题目结构化 JSON（含 `templateRaw` 原始题面，信息零丢失） |
| `english_content.json` | 英语全部内容 JSON |
| `files/` | 题目配图、选项图、知识点图片等被引用的文件（默认不含英语单词图，可由 AI 重新生成） |
| `file_index.csv` | 引用地址 → 本地文件对照表（含落盘状态） |
| `raw/*.csv` | 数据库原表 CSV（H2 CSVWRITE 导出，UTF-8） |

## 图片/音频怎么用

- `math_questions.xlsx` 的「图片」列为题目配图地址，`选项图URL` 列为选项整块图，形式为
  `/api/file?id=<文件ID>`（部分历史数据仅存裸文件 ID）。
- 用 `file_index.csv` 的「文件ID」列，在 `files/` 目录里找到对应「磁盘文件名」的文件。
- 新环境部署时，把 `files/` 下文件放回后端 `file-storage.local.root-path`（preview 为
  `server/api/files/`）即可让题目图片恢复显示。

## 图片导出统计

- 引用到的文件：复制 {stats['copied']} 个（约 {mb:.1f} MB）
- 磁盘文件缺失：{stats['missing_src']} 个
- 库中无记录：{stats['missing_meta']} 个

## 部署到新环境

- 数据库：使用仓库内的 `server/db-snapshot/wisestar.sql` 快照（`start-preview.sh` 会自动恢复）。
- 媒体文件：把本目录 `files/` 下文件拷入新环境后端的本地存储目录
  （preview profile 为 `server/api/files/`），题目图片即可正常显示。
- 若要连未在此处列出的其它附件一起迁移，直接整目录拷贝 `server/api/files/` 更稳妥。

## 重新导出

```bash
# 1. 停止预览后端（H2 文件库单进程独占）
# 2. 在仓库根目录执行
server/export-content.sh
```

脚本会重新导出数据库 CSV、重建本目录下的 Excel / JSON / files 与对照表。
追加 `--all-files` 可连同文件表中未被题目引用的文件一起导出；
追加 `--english-media` 可一并导出英语单词图片/音频。
"""
    with open(os.path.join(out, "README.md"), "w", encoding="utf-8") as fh:
        fh.write(readme)

    print(f"[export] 题目 {len(questions)} 道，知识点 {len(kps)} 个，"
          f"英语单词 {len(en_words)} / 句子 {len(en_sentences)} / 语法 {len(en_grammar)}")
    print(f"[export] 文件复制 {stats['copied']} 个（{mb:.1f} MB），"
          f"缺失 {stats['missing_src']}，无记录 {stats['missing_meta']}")
    print(f"[export] 输出目录：{out}")


if __name__ == "__main__":
    main()
