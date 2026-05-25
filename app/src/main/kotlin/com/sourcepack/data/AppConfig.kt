package com.sourcepack.data

/**
 * Packer configuration model
 * Includes all switches and options that affect the packaging result.
 */
data class PackerConfig(
    val compress: Boolean = false,           // Compress content (remove extra blank lines)?
    val ignoreGit: Boolean = true,           // Do you want to ignore the .git directory?
    val ignoreBuild: Boolean = true,         // Should the build artifacts directory be ignored?
    val ignoreGradle: Boolean = true,        // Should the .gradle cache directory be ignored?
    val useGitIgnore: Boolean = true,        // Whether to parse and apply .gitignore rules (reserved fields)
    val removeComments: Boolean = false,     // Should the comments be removed? (Default: false)
    val format: Format = Format.MARKDOWN,    // Output file format
    val mode: Mode = Mode.FULL               // Output mode: Full content or directory tree only
)

enum class Format { MARKDOWN, XML, TEXT }
enum class Mode { FULL, TREE }
enum class AppTheme { SYSTEM, BLUE, PURPLE, GRAY }