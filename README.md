# 📦 SourcePack
Android

Kotlin

License
**SourcePack** is a powerful Android utility designed to consolidate your project's source code into a single, structured file. Whether you need to feed your entire codebase into a Large Language Model (LLM) for analysis or generate comprehensive documentation, SourcePack makes it seamless.
## ✨ Key Features
 * [x] **Source Code Integration** Recursively scans local directories to generate a unified output file containing both the directory tree and file contents.
 * [x] **GitHub Integration** Directly download and process GitHub repositories. Enter a URL, and SourcePack handles the HEAD branch ZIP in-memory—no Git client required.
 * [x] **Structural Context** Generates a visual directory tree at the start of every export. Even filtered files (like binaries) appear in the tree to preserve architectural context for AI analyzers.
## 🛡️ Smart Filtering & Privacy
SourcePack is designed to be efficient with tokens and respectful of your build environment:
 * **System Auto-Ignore**: Automatically skips build artifacts and metadata folders like .git, .gradle, build, and node_modules.
 * **Binary Detection**: Smart analysis skips binary files (images, executables, etc.) to keep the output text-focused.
 * **Custom Rules**: Full control via a user-defined blacklist for specific filenames or file extensions.
## 🛠️ Output Options
Tailor the output to your specific needs:
| Format | Description |
|---|---|
| **Markdown** | Wraps code in language-specific triple backtick blocks (e.g., ```kotlin). |
| **XML** | Hierarchical tags perfect for programmatic parsing and structured tools. |
| **Plain Text** | Simple, clean delimiters for minimal overhead. |
| **Compressed** | An optional mode that strips redundant whitespace and line breaks to save space. |
## 🚀 Tech Stack
 * **Language:** Kotlin 2.0
 * **UI Framework:** Jetpack Compose (Material 3)
 * **Architecture:** MVVM with Coroutines and Flow
 * **Min SDK:** Android 7.0 (API 24)
## 📖 How to Use
 1. **Choose your Source**:
   * **Folder**: Use the system picker to grant access to a local project.
   * **Files**: Hand-pick specific files for targeted analysis.
   * **GitHub**: Paste a repo URL (e.g., https://github.com/user/repo).
 2. **Configure**: Visit **Settings** to toggle compression, pick your format (MD/XML/TXT), or refine exclusion rules.
 3. **Export**: SourcePack processes the data and saves it to your chosen location via the Storage Access Framework.
## 📄 License
Copyright 2025 **Qingsu**
Licensed under the Apache License, Version 2.0 (the "License"). You may obtain a copy of the License at:
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
