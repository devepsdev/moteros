import sys
from pathlib import Path

# Los tests importan core/, pipeline/ y services/ como en main.py.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
