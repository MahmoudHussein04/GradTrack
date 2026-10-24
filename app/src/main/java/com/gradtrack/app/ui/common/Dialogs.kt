package com.gradtrack.app.ui.common

import android.text.InputType
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gradtrack.app.R
import com.gradtrack.app.databinding.DialogTextInputBinding

/**
 * Asks for one piece of text (feedback, a university ID...). The dialog stays open with an inline error until the
 * text is valid; [onSubmit] receives the trimmed value.
 */
fun Fragment.promptText(
    @StringRes title: Int,
    @StringRes hint: Int,
    @StringRes positive: Int,
    multiline: Boolean = true,
    numeric: Boolean = !multiline,
    onSubmit: (String) -> Unit,
) {
    val binding = DialogTextInputBinding.inflate(LayoutInflater.from(requireContext()))
    binding.inputLayout.hint = getString(hint)
    if (!multiline) {
        binding.input.minLines = 1
        binding.input.maxLines = 1
        binding.input.inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }
    val dialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(positive, null)
            .create()
    dialog.setOnShowListener {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val text = binding.input.text?.toString().orEmpty().trim()
            if (text.isEmpty()) {
                binding.inputLayout.error = getString(R.string.error_required)
            } else {
                dialog.dismiss()
                onSubmit(text)
            }
        }
    }
    dialog.show()
}
