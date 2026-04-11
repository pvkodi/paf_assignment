#!/usr/bin/env python3
"""
Render Mermaid diagram to PNG using Python
"""

import os
import json
import subprocess
import sys

# Install required package if not present
try:
    import requests
except ImportError:
    subprocess.check_call([sys.executable, "-m", "pip", "install", "requests"])
    import requests

def render_mermaid_to_png(mmd_file, output_file, width=2400, height=1800):
    """
    Render Mermaid diagram to PNG using Kroki service
    """
    
    # Read the Mermaid diagram
    with open(mmd_file, 'r', encoding='utf-8') as f:
        diagram_content = f.read()
    
    # Remove YAML frontmatter if present
    lines = diagram_content.split('\n')
    start_idx = 0
    if lines[0].strip() == '---':
        for i in range(1, len(lines)):
            if lines[i].strip() == '---':
                start_idx = i + 1
                break
    
    diagram_content = '\n'.join(lines[start_idx:])
    
    # Use Kroki online service to render
    kroki_url = "https://kroki.io/mermaid/png"
    
    print(f"Rendering diagram: {mmd_file}")
    print(f"Output file: {output_file}")
    
    try:
        response = requests.post(
            kroki_url,
            data=diagram_content,
            headers={"Content-Type": "text/plain"},
            timeout=60
        )
        
        if response.status_code == 200:
            with open(output_file, 'wb') as f:
                f.write(response.content)
            print(f"✓ Successfully rendered diagram to {output_file}")
            print(f"✓ File size: {os.path.getsize(output_file)} bytes")
            return True
        else:
            print(f"✗ Failed to render diagram. Status: {response.status_code}")
            print(f"  Response: {response.text}")
            return False
    except Exception as e:
        print(f"✗ Error rendering diagram: {e}")
        return False

if __name__ == "__main__":
    mmd_file = r"e:\University\3rd Year\3rd Year 2nd sem\2026 Things\PAF\Group project\PAF group assignment\paf_assignment\docs\architecture\class-diagram.mmd"
    output_file = r"e:\University\3rd Year\3rd Year 2nd sem\2026 Things\PAF\Group project\PAF group assignment\paf_assignment\docs\architecture\class-diagram.png"
    
    success = render_mermaid_to_png(mmd_file, output_file)
    sys.exit(0 if success else 1)
