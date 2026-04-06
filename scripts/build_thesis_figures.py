#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageOps


ROOT = Path(__file__).resolve().parents[1]
MODEL_FILE = ROOT / "系统建模图.md"
OUTPUT_DIR = ROOT / "_doc_extract" / "generated_figures"
SOURCE_DIR = OUTPUT_DIR / "_mermaid_src"
CONFIG_FILE = OUTPUT_DIR / "mermaid.config.json"
MANIFEST_FILE = OUTPUT_DIR / "manifest.json"
SECTION_MARKER = "## 9. 论文正文版图稿映射"


@dataclass
class FigureSpec:
    number: str
    title: str
    mermaid: str

    @property
    def stem(self) -> str:
        return f"figure_{self.number.replace('-', '_')}"

    @property
    def source_path(self) -> Path:
        return SOURCE_DIR / f"{self.stem}.mmd"

    @property
    def image_path(self) -> Path:
        return OUTPUT_DIR / f"{self.stem}.png"

    @property
    def caption(self) -> str:
        return f"图{self.number} {self.title}"


def parse_figures() -> list[FigureSpec]:
    text = MODEL_FILE.read_text(encoding="utf-8")
    if SECTION_MARKER not in text:
        raise RuntimeError(f"未找到论文正文版图稿映射章节: {SECTION_MARKER}")

    section = text.split(SECTION_MARKER, 1)[1]
    pattern = re.compile(
        r"^### 图(?P<number>\d-\d+) (?P<title>.+?)\n\n```mermaid\n(?P<body>.*?)\n```",
        re.MULTILINE | re.DOTALL,
    )
    figures = [
        FigureSpec(number=match.group("number"), title=match.group("title").strip(), mermaid=match.group("body").strip() + "\n")
        for match in pattern.finditer(section)
    ]
    if len(figures) != 17:
        raise RuntimeError(f"论文正文版图稿数量异常，期望 17 张，实际 {len(figures)} 张。")
    return figures


def write_mermaid_config() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    config = {
        "theme": "base",
        "themeVariables": {
            "fontFamily": "Microsoft YaHei, SimSun, Times New Roman, serif",
            "primaryColor": "#f8fafc",
            "primaryTextColor": "#1f2937",
            "primaryBorderColor": "#334155",
            "lineColor": "#334155",
            "secondaryColor": "#ffffff",
            "tertiaryColor": "#ffffff",
            "clusterBkg": "#ffffff",
            "clusterBorder": "#475569",
            "edgeLabelBackground": "#ffffff",
            "mainBkg": "#ffffff",
        },
        "flowchart": {
            "htmlLabels": True,
            "curve": "basis",
            "padding": 18,
            "nodeSpacing": 42,
            "rankSpacing": 46,
            "diagramPadding": 16,
            "useMaxWidth": False,
        },
        "er": {
            "layoutDirection": "TB",
            "diagramPadding": 18,
        },
        "class": {
            "hideEmptyMembersBox": True,
        },
        "sequence": {
            "diagramMarginX": 28,
            "diagramMarginY": 18,
        },
    }
    CONFIG_FILE.write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8")


def render_figure(spec: FigureSpec, browser_path: str) -> None:
    spec.source_path.write_text(spec.mermaid, encoding="utf-8")
    cmd = (
        f'npx -y @mermaid-js/mermaid-cli '
        f'-i "{spec.source_path}" '
        f'-o "{spec.image_path}" '
        f'-c "{CONFIG_FILE}" '
        f'-b white -s 2 -w 2200'
    )
    env = os.environ.copy()
    env["PUPPETEER_EXECUTABLE_PATH"] = browser_path
    subprocess.run(cmd, check=True, cwd=ROOT, shell=True, env=env)

    with Image.open(spec.image_path) as image:
        converted = image.convert("RGB")
        padded = ImageOps.expand(converted, border=72, fill="white")
        padded.save(spec.image_path, format="PNG", optimize=True)


def write_manifest(figures: list[FigureSpec]) -> None:
    payload = {
        "source": str(MODEL_FILE),
        "output_dir": str(OUTPUT_DIR),
        "figures": [
            {
                "number": spec.number,
                "title": spec.title,
                "caption": spec.caption,
                "file": str(spec.image_path),
            }
            for spec in figures
        ],
        "aliases": {
            "4-1": {
                "caption": "图4-1 交易订单处理流程图",
                "file": str(OUTPUT_DIR / "figure_3_6.png"),
            }
        },
    }
    MANIFEST_FILE.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def find_browser_executable() -> str:
    candidates = [
        Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe"),
        Path(r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"),
        Path(r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"),
        Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"),
    ]
    for path in candidates:
        if path.exists():
            return str(path)
    raise RuntimeError("未找到可供 mermaid-cli 使用的 Chrome / Edge 浏览器。")


def clean_output() -> None:
    if OUTPUT_DIR.exists():
        for path in OUTPUT_DIR.glob("figure_*.png"):
            path.unlink()
        if SOURCE_DIR.exists():
            shutil.rmtree(SOURCE_DIR)


def main() -> int:
    figures = parse_figures()
    clean_output()
    write_mermaid_config()
    browser_path = find_browser_executable()
    for spec in figures:
        print(f"Rendering {spec.caption} ...")
        render_figure(spec, browser_path)
    write_manifest(figures)
    print(f"Generated {len(figures)} figures in: {OUTPUT_DIR}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
