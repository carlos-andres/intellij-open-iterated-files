package com.calo

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager

/**
 * An action that opens all modified and un-versioned new files in the editor.
 */
class OpenModifiedFilesAction : AnAction(), DumbAware {

    /**
     * Triggered when the action is performed. It opens all files that are either newly added or modified,
     * as well as all un-versioned files (not yet under version control).
     *
     * @param event The event containing context about the action invocation.
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        // Avoid acting on uninitialized or disposed projects
        if (!project.isInitialized || project.isDisposed) return

        openModifiedAndUnversionedFiles(event)
    }

    /**
     * Opens all versioned (new or modified) and un-versioned files in the editor.
     *
     * @param event The event with context including the project and editor state.
     */
    private fun openModifiedAndUnversionedFiles(event: AnActionEvent) {
        val project = event.project ?: return
        val changeListManager = ChangeListManager.getInstance(project)
        val fileEditorManager = FileEditorManager.getInstance(project)

        // Open modified or new files under version control
        val versionedFilesToOpen = changeListManager.defaultChangeList.changes
            .filter { it.type == Change.Type.NEW || it.type == Change.Type.MODIFICATION }
            .mapNotNull { it.virtualFile }

        versionedFilesToOpen.forEach { fileEditorManager.openFile(it, true) }

        // Open un-versioned (not yet tracked) files
        val unVersionedFiles = changeListManager.unversionedFilesPaths
            .mapNotNull { it.virtualFile }

        unVersionedFiles.forEach { fileEditorManager.openFile(it, true) }
    }

    /**
     * Updates the action's visibility and enabled state based on whether there are files to open.
     *
     * @param event The action event containing the project and context.
     */
    override fun update(event: AnActionEvent) {
        val project = event.project
        val changeListManager = project?.let { ChangeListManager.getInstance(it) }

        event.presentation.isEnabledAndVisible =
            project != null && (changeListManager?.hasChangesToOpen() == true)
    }

    /**
     * Indicates that the action update logic should run on a background thread.
     */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     * Helper extension to check for any versioned or un-versioned files that haven't been committed.
     */
    private fun ChangeListManager.hasChangesToOpen(): Boolean {
        val hasVersionedChanges = defaultChangeList.changes.any {
            it.type == Change.Type.NEW || it.type == Change.Type.MODIFICATION
        }
        val hasUnversionedFiles = unversionedFilesPaths.isNotEmpty()
        return hasVersionedChanges || hasUnversionedFiles
    }
}
