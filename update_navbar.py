import os
import re

directory = "app/src/main/java/com/gymtracker/ui/screens"
pattern = re.compile(r'NavBar\(\s*items\s*=\s*listOf\([^)]+\)\s*,', re.DOTALL)

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if 'NavBar(' in content and 'items = listOf(' in content:
                # Replace NavBar( items = listOf(...), with NavBar(
                new_content = pattern.sub('NavBar(', content)
                if new_content != content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated {filepath}")
