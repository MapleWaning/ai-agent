from pathlib import Path

from dotenv import load_dotenv

ROOT = Path(__file__).resolve().parent.parent

for env_path in (
    ROOT / "app" / ".env.local",
    ROOT / ".env.local",
    ROOT / ".env.dev",
):
    if env_path.exists():
        load_dotenv(env_path)
        break
