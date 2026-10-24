package com.gradtrack.app.ui.review

import com.gradtrack.app.ui.common.GradFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gradtrack.app.R
import com.gradtrack.app.data.model.ReviewDecision
import com.gradtrack.app.data.model.Submission
import com.gradtrack.app.data.model.SubmissionStatus
import com.gradtrack.app.data.model.SubmissionsOverview
import com.gradtrack.app.data.repository.ReviewRepository
import com.gradtrack.app.databinding.DialogReviewSubmissionBinding
import com.gradtrack.app.databinding.FragmentSubmissionReviewBinding
import com.gradtrack.app.ui.common.LoadableViewModel
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.applyPill
import com.gradtrack.app.ui.common.asShortDate
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.pill
import com.gradtrack.app.ui.common.setVisible
import com.gradtrack.app.ui.milestones.SubmissionAdapter

class SubmissionReviewViewModel(private val repository: ReviewRepository) : LoadableViewModel<SubmissionsOverview>({ repository.submissions() }) {
    fun review(submission: Submission, decision: ReviewDecision, comment: String) =
        act({ repository.reviewSubmission(submission.id, decision, comment) }) {
            send(UiEvent.Message(R.string.decision_sent))
            refresh()
        }
}

class SubmissionReviewFragment : GradFragment() {
    private var _binding: FragmentSubmissionReviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { SubmissionReviewViewModel(it.reviews) }
    private val adapter = SubmissionAdapter(showTeam = true, onClick = ::openReview)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSubmissionReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.adapter = adapter
        binding.empty.bind(R.drawable.ic_submissions, getString(R.string.empty_submissions_title), getString(R.string.no_submissions_to_review))
        binding.refresh.setOnRefreshListener { viewModel.refresh() }

        collect(viewModel.state) { state ->
            binding.state.render(state, viewModel::load)
            if (state is UiState.Success) {
                val data = state.data
                binding.statAwaiting.bind(getString(R.string.stat_awaiting_review_short), data.awaitingReview.toString(), null)
                binding.statReviewed.bind(getString(R.string.stat_reviewed_week), data.reviewedThisWeek.toString(), null)
                binding.statRevision.bind(getString(R.string.stat_need_revisions), data.needRevision.toString(), null)
                adapter.submitList(data.submissions)
                binding.empty.setVisible(data.submissions.isEmpty())
            }
        }
        collect(viewModel.refreshing) { binding.refresh.isRefreshing = it }
        observeEvents(viewModel)
    }

    /** Opens one submission: read its details, write feedback, then approve or send it back for revision. */
    private fun openReview(submission: Submission) {
        val dialogBinding = DialogReviewSubmissionBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.meta.text = getString(R.string.submission_meta, submission.fileName, submission.teamName.orEmpty(), submission.type.orEmpty(), submission.submittedAt.asShortDate())
        dialogBinding.status.applyPill(submission.status.pill())
        val reviewable = submission.status == SubmissionStatus.REVIEW
        dialogBinding.commentLayout.setVisible(reviewable)

        val builder =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(submission.fileName)
                .setView(dialogBinding.root)
                .setNegativeButton(if (reviewable) R.string.cancel else R.string.close, null)
        if (reviewable) {
            builder.setNeutralButton(R.string.request_revision, null).setPositiveButton(R.string.approve, null)
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            if (!reviewable) return@setOnShowListener
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                viewModel.review(submission, ReviewDecision.APPROVE, dialogBinding.comment.text?.toString().orEmpty())
                dialog.dismiss()
            }
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val comment = dialogBinding.comment.text?.toString().orEmpty().trim()
                if (comment.isEmpty()) {
                    dialogBinding.commentLayout.error = getString(R.string.error_feedback_required)
                } else {
                    viewModel.review(submission, ReviewDecision.CHANGES, comment)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
