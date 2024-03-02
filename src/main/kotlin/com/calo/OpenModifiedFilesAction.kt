package com.calo

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager

/**
 * An action that opens all modified and un-versioned new files in the editor.
 */
class OpenModifiedFilesAction : AnAction(), DumbAware {

    /**
     * Opens all modified and un-versioned new files in the editor when the action is performed.
     * @param event Carries information on the invocation place and data available
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val changeListManager = ChangeListManager.getInstance(project)

        val changes = changeListManager.defaultChangeList.changes

        // Get changes from the default change list and filter for modified/new files
        val versionedFilesToOpen = changes
            .filter { it.type == Change.Type.NEW || it.type == Change.Type.MODIFICATION }
            .mapNotNull { it.virtualFile }

        val fileEditorManager = FileEditorManager.getInstance(project)

        versionedFilesToOpen.forEach { file ->
            fileEditorManager.openFile(file, true)
        }

        // Get un-versioned file paths and convert to VirtualFile
        val unVersionedFilePaths = changeListManager.unversionedFilesPaths

        val unVersionedFiles = unVersionedFilePaths.mapNotNull { it.virtualFile }
        unVersionedFiles.forEach { file ->
            fileEditorManager.openFile(file, true)
        }
    }

    /**
     * Updates the presentation of the action based on the current context.
     * @param event Carries information on the invocation place and data available
     */
    override fun update(event: AnActionEvent) {
        val project = event.project
        val changeListManager = project?.let { ChangeListManager.getInstance(it) }
        event.presentation.isEnabledAndVisible = project != null && (changeListManager?.hasChangesWithoutCommit() ?: false)
    }

    /**
     * Specifies that the action update should run on the background thread.
     * @return The thread to use for update.
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    /**
     * Determines if there are any versioned changes or unversioned files.
     * @return True if there are changes without commit, false otherwise.
     */
    private fun ChangeListManager.hasChangesWithoutCommit(): Boolean {
        val hasVersionedChanges = this.defaultChangeList.changes.any {
            it.type == Change.Type.NEW || it.type == Change.Type.MODIFICATION
        }
        val hasUnversionedFiles = this.unversionedFilesPaths.isNotEmpty()
        return hasVersionedChanges || hasUnversionedFiles
    }

}
