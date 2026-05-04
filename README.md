SourcePack
SourcePack is an Android utility designed to consolidate project source code into a single structured file (Markdown, XML, or Plain Text format). It facilitates the export of codebases for analysis using Large Language Models (LLMs) or for documentation purposes.
Key Features
* [x] Source Code Integration: Recursively scans local directories to generate a unified output file containing the directory structure and file contents.
* [x] GitHub Integration: Accepts GitHub repository URLs to download the HEAD branch as a ZIP archive. The file tree is processed entirely in memory, requiring no local Git client.
* [x] Structural Context: Generates a complete directory tree visualization at the beginning of the output file. Files excluded by filters (e.g., binary files) remain in the tree to help analyzers maintain architectural context.
Intelligent Filtering
* System Ignore: Automatically excludes build artifacts and metadata directories (e.g., .git, .gradle, build, node_modules).
* Binary File Detection: Skips binary files based on extension and content analysis to reduce token usage.
* Custom Rules: Supports user-defined blacklists for specific filenames and extensions.
Output Formats
* Markdown: Wraps code in language-specific triple backtick blocks.
* XML: Wraps content in hierarchical tags for structured parsing.
* Plain Text: Uses simple delimiters between files.
* Compression: Optional mode to remove redundant spaces and line breaks.
Tech Stack
* Language: Kotlin 2.0
* UI Framework: Jetpack Compose (Material 3)
* Architecture: MVVM with Coroutines and Flow
* Minimum SDK: Android 7.0 (API 24)
Usage
* Select Source:
   * Folder: Grant access to a local project directory via the system document picker.
   * Files: Select multiple specific files.
   * GitHub: Enter a repository URL (e.g., https://github.com/username/repo).
* Configuration: Access settings to toggle compression, select output formats (MD/XML/TXT), or modify exclusion rules.
* Export: The app processes the input and writes the result to a destination URI selected by the user.
License
Copyright 2025 Qingsu
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at Apache License 2.0.
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.