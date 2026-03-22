import os
import re
import sys

def fix_all_modules(root_dir):
    files_fixed = 0
    # Regex to find ' not preceded by \
    apostrophe_pattern = re.compile(r"(?<!\\)'")
    # Regex to find & that isn't already part of an entity like &amp; or &#123;
    ampersand_pattern = re.compile(r"&(?!amp;|lt;|gt;|quot;|apos;|#)")

    for root, dirs, files in os.walk(root_dir):
        if "res" in root and "values" in root and "strings.xml" in files:
            file_path = os.path.join(root, "strings.xml")

            with open(file_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()

            new_lines = []
            file_changed = False

            for line in lines:
                if "<string" in line and "</string>" in line:
                    # Isolate the text between the tags
                    tag_match = re.search(r'(<string.*?>)(.*)(</string>)', line)
                    if tag_match:
                        prefix, content, suffix = tag_match.groups()

                        # Apply fixes to the content only
                        fixed_content = apostrophe_pattern.sub(r"\'", content)
                        fixed_content = ampersand_pattern.sub(r"&amp;", fixed_content)

                        if fixed_content != content:
                            line = f"{prefix}{fixed_content}{suffix}\n"
                            file_changed = True

                new_lines.append(line)

            if file_changed:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.writelines(new_lines)
                print(f"✅ Fixed: {file_path}")
                files_fixed += 1

    return files_fixed

if __name__ == "__main__":
    count = fix_all_modules(".")
    if count > 0:
        print(f"\n🚀 Successfully fixed {count} files. You can now build the project.")
    else:
        print("✨ No issues found. Your XML strings are clean!")