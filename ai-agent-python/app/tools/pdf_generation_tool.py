from pathlib import Path

from fpdf import FPDF
from fpdf.enums import XPos, YPos
from langchain.tools import ToolRuntime, tool

from app.settings import settings
from app.util.chat_context import ChatContext
from app.util.pdf_font import CHINESE_FONT_FAMILY, resolve_chinese_font
from app.util.tool_stream import emit_file, emit_tool_end, emit_tool_error, emit_tool_start
from app.util.workspace_path import ensure_parent_directory

_TOOL_NAME = "generate_pdf"
_TOOL_LABEL = "生成 PDF"


def _resolve_pdf_path(runtime: ToolRuntime[ChatContext], file_name: str) -> Path:
    user_id = runtime.context.user_id
    chat_id = runtime.context.chat_id
    base_dir = Path(settings.FILE_SAVE_DIR) / user_id / chat_id / "pdf"
    file_path = (base_dir / file_name).resolve()
    base_resolved = base_dir.resolve()
    try:
        file_path.relative_to(base_resolved)
    except ValueError as exc:
        raise ValueError(f"Invalid file path: {file_name}") from exc
    return file_path


def _normalize_pdf_name(file_name: str) -> str:
    name = file_name.strip()
    if not name.lower().endswith(".pdf"):
        name = f"{name}.pdf"
    return name


def _build_pdf(content: str, file_path: Path) -> None:
    font_path, collection_font_number = resolve_chinese_font()
    pdf = FPDF()
    pdf.add_font(
        CHINESE_FONT_FAMILY,
        "",
        str(font_path),
        collection_font_number=collection_font_number,
    )
    pdf.set_font(CHINESE_FONT_FAMILY, size=12)
    pdf.set_margins(15, 15, 15)
    pdf.set_auto_page_break(auto=True, margin=15)
    pdf.add_page()
    for line in content.splitlines() or [""]:
        pdf.multi_cell(
            pdf.epw,
            8,
            line,
            new_x=XPos.LMARGIN,
            new_y=YPos.NEXT,
        )
    pdf.output(str(file_path))


@tool
def generate_pdf(
    file_name: str,
    content: str,
    runtime: ToolRuntime[ChatContext],
) -> str:
    """Generate a PDF file with the given name and content in the current chat workspace."""
    emit_tool_start(_TOOL_NAME, _TOOL_LABEL, file_name)
    try:
        pdf_name = _normalize_pdf_name(file_name)
        file_path = ensure_parent_directory(_resolve_pdf_path(runtime, pdf_name))
        _build_pdf(content, file_path)
        emit_tool_end(_TOOL_NAME, _TOOL_LABEL, "生成 PDF 完成")
        emit_file(pdf_name, action="created", summary=f"PDF {pdf_name} 已生成")
        return f"PDF generated successfully to: {file_path}"
    except Exception as exc:
        emit_tool_error(_TOOL_NAME, _TOOL_LABEL, f"生成 PDF 失败: {exc}", file_name)
        return f"Error generating PDF: {exc}"
