package com.gradtrack.app.ui.widget

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import coil3.load
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import com.google.android.material.card.MaterialCardView
import com.gradtrack.app.R
import com.gradtrack.app.databinding.ViewEmptyStateBinding
import com.gradtrack.app.databinding.ViewStateBinding
import com.gradtrack.app.databinding.ViewStatCardBinding
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.initials
import com.gradtrack.app.ui.common.message

/**
 * Wraps a screen's content and shows the Loading (skeleton) / Error states over it. Put exactly one content view
 * inside; call [render] with the ViewModel's state. Content is visible only in [UiState.Success], and slides in the
 * first time it appears.
 */
class StateLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val overlay = ViewStateBinding.inflate(LayoutInflater.from(context), this, false)
    private var pulse: ObjectAnimator? = null
    private var revealed = false

    init {
        addView(overlay.root)
    }

    fun render(state: UiState<*>, onRetry: () -> Unit) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child !== overlay.root) child.isVisible = state is UiState.Success
        }
        overlay.root.isVisible = state !is UiState.Success
        overlay.stateSkeleton.isVisible = state is UiState.Loading
        overlay.stateErrorGroup.isVisible = state is UiState.Error
        if (state is UiState.Loading) startPulse() else stopPulse()
        if (state is UiState.Error) {
            overlay.stateMessage.text = state.error.message(context)
            overlay.stateRetry.setOnClickListener { onRetry() }
        }
        if (state is UiState.Success && !revealed) {
            revealed = true
            content()?.let { column -> reveal(column) }
        }
    }

    private fun content(): View? = (0 until childCount).map { getChildAt(it) }.firstOrNull { it !== overlay.root }

    /** Staggered fade-up of the first multi-child column found under the content (skipped when animations are off). */
    private fun reveal(root: View) {
        if (!ValueAnimator.areAnimatorsEnabled()) return
        var view: View? = root
        while (view is ViewGroup && view.childCount == 1) view = view.getChildAt(0)
        val column = view as? ViewGroup ?: return
        column.layoutAnimation = LayoutAnimationController(AnimationUtils.loadAnimation(context, R.anim.item_fade_up), 0.08f)
        column.scheduleLayoutAnimation()
    }

    private fun startPulse() {
        if (pulse != null || !ValueAnimator.areAnimatorsEnabled()) return
        pulse =
            ObjectAnimator.ofFloat(overlay.stateSkeleton, View.ALPHA, 1f, 0.45f).apply {
                duration = 750
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
    }

    private fun stopPulse() {
        pulse?.cancel()
        pulse = null
        overlay.stateSkeleton.alpha = 1f
    }

    override fun onDetachedFromWindow() {
        stopPulse()
        super.onDetachedFromWindow()
    }
}

/** Friendly "nothing here yet" block: icon, why it is empty, and the next step as a button. */
class EmptyStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewEmptyStateBinding

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val pad = (24 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        binding = ViewEmptyStateBinding.inflate(LayoutInflater.from(context), this)
    }

    fun bind(
        @DrawableRes icon: Int,
        title: String,
        body: String,
        primary: String? = null,
        onPrimary: (() -> Unit)? = null,
        secondary: String? = null,
        onSecondary: (() -> Unit)? = null,
    ) {
        binding.emptyIcon.setImageResource(icon)
        binding.emptyTitle.text = title
        binding.emptyBody.text = body
        binding.emptyPrimary.isVisible = primary != null
        binding.emptyPrimary.text = primary
        binding.emptyPrimary.setOnClickListener { onPrimary?.invoke() }
        binding.emptySecondary.isVisible = secondary != null
        binding.emptySecondary.text = secondary
        binding.emptySecondary.setOnClickListener { onSecondary?.invoke() }
    }
}

/** Number + label tile used on dashboards (e.g. "Project progress / 65% / Overall completion"). */
class StatCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs, com.google.android.material.R.attr.materialCardViewStyle) {
    private val binding = ViewStatCardBinding.inflate(LayoutInflater.from(context), this)

    init {
        val styled = context.obtainStyledAttributes(attrs, R.styleable.StatCardView)
        binding.icon.isVisible = styled.getBoolean(R.styleable.StatCardView_showIcon, false)
        styled.recycle()
    }

    fun bind(label: String, value: String, caption: String? = null, @DrawableRes icon: Int? = null) {
        if (icon != null) {
            binding.icon.setImageResource(icon)
            binding.icon.isVisible = true
        }
        binding.label.text = label
        binding.value.text = value
        binding.caption.text = caption
        binding.caption.isVisible = !caption.isNullOrBlank()
    }
}

/** Round avatar: initials on a tinted circle, replaced by the photo (loaded with Coil) once it arrives. */
class AvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val initialsView = TextView(context)
    private val imageView = ImageView(context)

    init {
        val styled = context.obtainStyledAttributes(attrs, R.styleable.AvatarView)
        val textSize = styled.getDimension(R.styleable.AvatarView_avatarTextSize, 0f)
        styled.recycle()
        background = ContextCompat.getDrawable(context, R.drawable.bg_avatar)
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
        initialsView.apply {
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, if (textSize > 0f) textSize else TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics))
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.inter_semibold)
        }
        imageView.apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        addView(initialsView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun bind(name: String, imageUrl: String?) {
        initialsView.text = name.initials()
        imageView.visibility = View.GONE
        if (imageUrl.isNullOrBlank()) {
            imageView.setImageDrawable(null)
            return
        }
        imageView.load(imageUrl) {
            crossfade(true)
            listener(
                object : ImageRequest.Listener {
                    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                        imageView.visibility = View.VISIBLE
                    }

                    override fun onError(request: ImageRequest, result: ErrorResult) {
                        imageView.visibility = View.GONE
                    }
                },
            )
        }
    }
}
