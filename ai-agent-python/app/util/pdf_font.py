from pathlib import Path

CHINESE_FONT_FAMILY = "ChineseFont"

_FONT_CANDIDATES: list[tuple[Path, int]] = [
    (Path(__file__).resolve().parents[1] / "resources" / "fonts" / "NotoSansSC-Regular.otf", 0),
    (Path("C:/Windows/Fonts/simhei.ttf"), 0),
    (Path("C:/Windows/Fonts/simsun.ttc"), 0),
    (Path("C:/Windows/Fonts/msyh.ttc"), 0),
    (Path("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"), 0),
    (Path("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"), 0),
]


def resolve_chinese_font() -> tuple[Path, int]:
    for font_path, collection_font_number in _FONT_CANDIDATES:
        if font_path.is_file():
            return font_path, collection_font_number
    raise FileNotFoundError(
        "No Chinese font found. Place NotoSansSC-Regular.otf under app/resources/fonts/ "
        "or install a system CJK font."
    )
