#!/usr/bin/env python3
"""DEX 索引器：只读 dex 的 string/type/proto/method 四张表，导出类与方法签名。

用途：给 hook 定位提供「这个类在不在、方法签名是什么」的离线索引，
         避免为了一次查询就整包 jadx 反编译。

用法：
    python3 scripts/dex_index.py <dex 目录或单个 dex> [-o out.tsv]

输出 TSV 列：
    L<类描述符>\t<方法名>\t(<参数签名>)\t<返回签名>

示例行：
    Lcom/bilibili/lib/blconfig/ConfigManager;\tisHitFF\t(Ljava/lang/String;)Z

说明：DEX 头为小端；字符串为 MUTF-8，按 UTF-8 尽力解码（非法字节替换）。
"""
from __future__ import annotations

import argparse
import os
import struct
import sys
from typing import BinaryIO, Iterator


def _uleb128(buf: bytes, pos: int) -> tuple[int, int]:
    result = 0
    shift = 0
    while True:
        b = buf[pos]
        pos += 1
        result |= (b & 0x7F) << shift
        if not b & 0x80:
            return result, pos
        shift += 7


class Dex:
    def __init__(self, path: str) -> None:
        self.path = path
        self.f: BinaryIO = open(path, "rb")
        self.f.seek(0)
        header = self.f.read(112)
        if header[:4] != b"dex\n":
            raise ValueError(f"not a dex: {path}")
        (self.string_ids_size, self.string_ids_off,
         self.type_ids_size, self.type_ids_off,
         self.proto_ids_size, self.proto_ids_off,
         self.field_ids_size, self.field_ids_off,
         self.method_ids_size, self.method_ids_off,
         self.class_defs_size, self.class_defs_off) = struct.unpack_from(
            "<12I", header, 56)  # 从 string_ids_size 开始连续 12 个 u4

        self.strings = self._load_strings()
        self.types = self._load_types()
        self.protos = self._load_protos()

    # --- 基础表 ---
    def _load_strings(self) -> list[str]:
        self.f.seek(self.string_ids_off)
        offsets = struct.unpack(f"<{self.string_ids_size}I", self.f.read(4 * self.string_ids_size))
        out: list[str] = []
        for off in offsets:
            self.f.seek(off)
            # 先读 uleb128 的 utf16 长度，紧接着是 MUTF-8 数据
            chunk = self.f.read(1)
            n = 0
            shift = 0
            while chunk:
                b = chunk[0]
                n |= (b & 0x7F) << shift
                if not b & 0x80:
                    break
                shift += 7
                chunk = self.f.read(1)
            raw = self.f.read(n + 1)  # MUTF-8 最多 n 字节 + 终止符
            out.append(raw.rstrip(b"\x00").decode("utf-8", errors="replace"))
        return out

    def _load_types(self) -> list[str]:
        self.f.seek(self.type_ids_off)
        idxs = struct.unpack(f"<{self.type_ids_size}I", self.f.read(4 * self.type_ids_size))
        return [self.strings[i] for i in idxs]

    def _type_list(self, off: int) -> list[str]:
        if not off:
            return []
        self.f.seek(off)
        size = struct.unpack("<I", self.f.read(4))[0]
        idxs = struct.unpack(f"<{size}H", self.f.read(2 * size))
        return [self.types[i] for i in idxs]

    def _load_protos(self) -> list[tuple[str, list[str]]]:
        self.f.seek(self.proto_ids_off)
        raw = self.f.read(12 * self.proto_ids_size)
        protos: list[tuple[str, list[str]]] = []
        for i in range(self.proto_ids_size):
            _shorty, ret_idx, params_off = struct.unpack_from("<III", raw, i * 12)
            protos.append((self.types[ret_idx], self._type_list(params_off)))
        return protos

    def _load_class_defs(self) -> list[str]:
        self.f.seek(self.class_defs_off)
        raw = self.f.read(32 * self.class_defs_size)
        return [self.types[struct.unpack_from("<I", raw, i * 32)[0]]
                for i in range(self.class_defs_size)]

    # --- 导出 ---
    def fields(self, class_filter: str | None = None) -> Iterator[tuple[str, str, str]]:
        """导出字段：类描述符 / 字段名 / 类型描述符。"""
        self.f.seek(self.field_ids_off)
        raw = self.f.read(8 * self.field_ids_size)
        for i in range(self.field_ids_size):
            class_idx, type_idx, name_idx = struct.unpack_from("<HHI", raw, i * 8)
            cls = self.types[class_idx]
            if class_filter and class_filter not in cls:
                continue
            yield cls, self.strings[name_idx], self.types[type_idx]

    def methods(self, class_filter: str | None = None) -> Iterator[tuple[str, str, str, str]]:
        self.f.seek(self.method_ids_off)
        raw = self.f.read(8 * self.method_ids_size)
        for i in range(self.method_ids_size):
            class_idx, proto_idx, name_idx = struct.unpack_from("<HHI", raw, i * 8)
            cls = self.types[class_idx]
            if class_filter and class_filter not in cls:
                continue
            ret, params = self.protos[proto_idx]
            yield cls, self.strings[name_idx], "(" + "".join(params) + ")", ret

    def close(self) -> None:
        self.f.close()


def iter_dex(path: str) -> Iterator[str]:
    if os.path.isfile(path):
        yield path
        return
    for root, _dirs, files in os.walk(path):
        for name in sorted(files):
            if name.endswith(".dex"):
                yield os.path.join(root, name)


def main() -> int:
    ap = argparse.ArgumentParser(description="导出 dex 的类与方法签名索引")
    ap.add_argument("input", help="dex 文件或包含 dex 的目录")
    ap.add_argument("-o", "--output", default="-", help="输出 TSV 路径，默认 stdout")
    ap.add_argument("--classes-only", action="store_true", help="只导出类名")
    ap.add_argument("--fields", action="store_true", help="导出字段而非方法")
    ap.add_argument("--filter", default=None, help="只导出类名包含该子串的条目")
    args = ap.parse_args()

    out = sys.stdout if args.output == "-" else open(args.output, "w", encoding="utf-8")
    total = 0
    for path in iter_dex(args.input):
        try:
            dex = Dex(path)
        except Exception as exc:  # 单个 dex 解析失败不影响整体
            print(f"[skip] {path}: {exc}", file=sys.stderr)
            continue
        try:
            if args.classes_only:
                for cls in dex._load_class_defs():
                    if args.filter and args.filter not in cls:
                        continue
                    out.write(cls + "\n")
                    total += 1
            elif args.fields:
                for cls, name, typ in dex.fields(args.filter):
                    out.write(f"{cls}\t{name}\t{typ}\n")
                    total += 1
            else:
                for cls, name, params, ret in dex.methods(args.filter):
                    out.write(f"{cls}\t{name}\t{params}\t{ret}\n")
                    total += 1
        finally:
            dex.close()
        print(f"[done] {os.path.basename(path)} -> {total} rows", file=sys.stderr)

    if out is not sys.stdout:
        out.close()
    print(f"[total] {total} rows", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
