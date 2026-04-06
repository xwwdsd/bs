#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import annotations

import argparse
from pathlib import Path

from docx import Document


def main() -> int:
    parser = argparse.ArgumentParser(description="Inspect docx paragraphs with indexes.")
    parser.add_argument("docx", type=Path, help="Path to the target .docx file")
    parser.add_argument("--contains", action="append", default=[], help="Only show paragraphs containing the given text")
    parser.add_argument("--starts-with", default="", help="Only show paragraphs starting with the given text")
    parser.add_argument("--context", type=int, default=2, help="Context paragraphs before/after a match")
    args = parser.parse_args()

    doc = Document(args.docx)
    paragraphs = [p.text.strip() for p in doc.paragraphs]

    if args.starts_with:
        for idx, text in enumerate(paragraphs):
            if text.startswith(args.starts_with):
                print(f"{idx}: {text}")
        return 0

    if not args.contains:
        for idx, text in enumerate(paragraphs):
            if text:
                print(f"{idx}: {text}")
        return 0

    seen = set()
    for needle in args.contains:
        for idx, text in enumerate(paragraphs):
            if needle in text:
                for pos in range(max(0, idx - args.context), min(len(paragraphs), idx + args.context + 1)):
                    if pos not in seen:
                        print(f"{pos}: {paragraphs[pos]}")
                        seen.add(pos)
                print("----")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
