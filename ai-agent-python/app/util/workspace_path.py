from pathlib import Path


def ensure_parent_directory(file_path: Path) -> Path:
    file_path.parent.mkdir(parents=True, exist_ok=True)
    return file_path
