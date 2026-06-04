package app.focusphone.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import app.focusphone.R
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.data.preferences.TutorialPreferences
import app.focusphone.databinding.ActivityOnboardingBinding
import app.focusphone.ui.appselect.AppSelectActivity
import app.focusphone.ui.base.BaseActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced onboarding flow for first-time users.
 *
 * A 5-step ViewPager2 carousel that introduces FocusPhone philosophy, guides the user to set it as
 * the default launcher (with real-time verification), invites them to pick favourite apps, and
 * celebrates completion.
 *
 * This activity is launched from [HomeActivity] when onboarding is not complete. It can be
 * restarted from [app.focusphone.ui.settings.AboutSettingsActivity].
 */
class OnboardingActivity : BaseActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var isLauncherVerified = false

    private val pages = listOf(
        OnboardingPage(
            iconRes = R.drawable.ic_tutorial_home,
            titleRes = R.string.onboarding_step_welcome_title,
            bodyRes = R.string.onboarding_step_welcome_body,
            taglineRes = R.string.onboarding_step_welcome_tagline,
            type = OnboardingPageType.INFO
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_tutorial_pause,
            titleRes = R.string.onboarding_step_philosophy_title,
            bodyRes = R.string.onboarding_step_philosophy_body,
            type = OnboardingPageType.INFO
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_tutorial_home,
            titleRes = R.string.onboarding_step_launcher_title,
            bodyRes = R.string.onboarding_step_launcher_body,
            actionRes = R.string.onboarding_step_launcher_action,
            type = OnboardingPageType.LAUNCHER
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_tutorial_edit,
            titleRes = R.string.onboarding_step_apps_title,
            bodyRes = R.string.onboarding_step_apps_body,
            actionRes = R.string.onboarding_step_apps_action,
            type = OnboardingPageType.APPS
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_tutorial_home,
            titleRes = R.string.onboarding_step_complete_title,
            bodyRes = R.string.onboarding_step_complete_body,
            actionRes = R.string.onboarding_step_complete_action,
            type = OnboardingPageType.COMPLETE
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        setupViewPager()
        setupPageTransformer()
        setupDots()
        setupButtons()
        observeLauncherState()
    }

    private fun setupViewPager() {
        binding.vpOnboarding.adapter = OnboardingPagerAdapter(pages, ::onPageAction)
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateUi(position)
        }
        pageChangeCallback = callback
        binding.vpOnboarding.registerOnPageChangeCallback(callback)
    }

    /** A subtle fade + scale transformer for calm, professional transitions. */
    private fun setupPageTransformer() {
        binding.vpOnboarding.setPageTransformer { page, position ->
            val absPos = kotlin.math.abs(position)
            page.alpha = 1f - (absPos * 0.35f)
            page.scaleX = 1f - (absPos * 0.08f)
            page.scaleY = 1f - (absPos * 0.08f)
            page.translationX = position * -40f * resources.displayMetrics.density
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        pageChangeCallback?.let { binding.vpOnboarding.unregisterOnPageChangeCallback(it) }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        checkLauncherStatus()
    }

    // ── Launcher verification ──────────────────────────────────────────────────────

    private fun checkLauncherStatus() {
        val wasVerified = isLauncherVerified
        isLauncherVerified = isDefaultLauncher()
        if (wasVerified != isLauncherVerified) {
            // Refresh the launcher page if it is currently visible
            val adapter = binding.vpOnboarding.adapter as? OnboardingPagerAdapter
            val launcherPosition = pages.indexOfFirst { it.type == OnboardingPageType.LAUNCHER }
            if (launcherPosition >= 0) {
                adapter?.notifyItemChanged(launcherPosition)
                if (binding.vpOnboarding.currentItem == launcherPosition) {
                    updateUi(launcherPosition)
                }
            }
        }
    }

    private fun observeLauncherState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(1000)
                    if (!isLauncherVerified) {
                        checkLauncherStatus()
                    }
                }
            }
        }
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(intent, 0)
        return resolved?.activityInfo?.packageName == packageName
    }

    // ── Dots indicator ─────────────────────────────────────────────────────────────

    private fun setupDots() {
        val dp8 = dp(8)
        val dp6 = dp(6)
        val primaryColor = resolveColorAttr(android.R.attr.textColorPrimary)
        val secondaryColor = resolveColorAttr(android.R.attr.textColorSecondary)

        pages.forEachIndexed { index, _ ->
            val dot = View(this).apply {
                val lp = ViewGroup.MarginLayoutParams(dp8, dp8)
                lp.marginEnd = dp6
                layoutParams = lp
                setBackgroundResource(R.drawable.bg_dot_inactive)
                backgroundTintList = android.content.res.ColorStateList.valueOf(secondaryColor)
                contentDescription = getString(R.string.onboarding_step_counter, index + 1, pages.size)
            }
            binding.dotsContainer.addView(dot)
        }
        updateDots(0, primaryColor, secondaryColor)
    }

    private fun updateDots(currentPage: Int, primaryColor: Int, secondaryColor: Int) {
        for (i in 0 until binding.dotsContainer.childCount) {
            val dot = binding.dotsContainer.getChildAt(i)
            if (i == currentPage) {
                dot.setBackgroundResource(R.drawable.bg_dot_active)
                dot.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                dot.animate().scaleX(1.3f).scaleY(1.3f).setDuration(180).start()
            } else {
                dot.setBackgroundResource(R.drawable.bg_dot_inactive)
                dot.backgroundTintList = android.content.res.ColorStateList.valueOf(secondaryColor)
                dot.animate().scaleX(1f).scaleY(1f).setDuration(180).start()
            }
        }
    }

    // ── Navigation buttons ─────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnSkip.setOnClickListener { finishOnboarding() }
        binding.btnNext.setOnClickListener {
            val cur = binding.vpOnboarding.currentItem
            if (cur < pages.lastIndex) {
                binding.vpOnboarding.currentItem = cur + 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun updateUi(position: Int) {
        val primaryColor = resolveColorAttr(android.R.attr.textColorPrimary)
        val secondaryColor = resolveColorAttr(android.R.attr.textColorSecondary)
        updateDots(position, primaryColor, secondaryColor)

        val page = pages[position]
        val isLast = position == pages.lastIndex

        binding.btnNext.text = getString(
            when {
                isLast -> R.string.onboarding_step_complete_action
                page.type == OnboardingPageType.LAUNCHER && !isLauncherVerified -> R.string.onboarding_next
                else -> R.string.onboarding_next
            }
        )
        binding.btnSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE

        // When on launcher page and verified, allow skipping directly to completion
        if (page.type == OnboardingPageType.LAUNCHER && isLauncherVerified) {
            binding.btnNext.text = getString(R.string.onboarding_next)
        }
    }

    private fun resolveColorAttr(attrId: Int): Int {
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(attrId, typedValue, true)
        return typedValue.data
    }

    // ── Page actions ───────────────────────────────────────────────────────────────

    private fun onPageAction(page: OnboardingPage) {
        when (page.type) {
            OnboardingPageType.LAUNCHER -> startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            OnboardingPageType.APPS -> startActivity(Intent(this, AppSelectActivity::class.java))
            OnboardingPageType.COMPLETE -> finishOnboarding()
            else -> Unit
        }
    }

    private fun finishOnboarding() {
        ThemePreferences.markOnboardingComplete(this)
        TutorialPreferences.setOnboardingFlowCompleted(this)
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

// ── Data model ───────────────────────────────────────────────────────────────────

data class OnboardingPage(
    val iconRes: Int,
    val titleRes: Int,
    val bodyRes: Int,
    val actionRes: Int? = null,
    val taglineRes: Int? = null,
    val type: OnboardingPageType = OnboardingPageType.INFO
)

enum class OnboardingPageType { INFO, LAUNCHER, APPS, COMPLETE }

// ── Adapter ──────────────────────────────────────────────────────────────────────

private class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>,
    private val onAction: (OnboardingPage) -> Unit
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder =
        PageViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.fragment_onboarding_page, parent, false)
        )

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) =
        holder.bind(pages[position], onAction)

    override fun getItemCount() = pages.size

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_page_icon)
        private val tvTagline: TextView = itemView.findViewById(R.id.tv_page_tagline)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_page_title)
        private val tvBody: TextView = itemView.findViewById(R.id.tv_page_body)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_page_status)
        private val btnAction: TextView = itemView.findViewById(R.id.btn_page_action)

        fun bind(page: OnboardingPage, onAction: (OnboardingPage) -> Unit) {
            val context = itemView.context

            ivIcon.setImageResource(page.iconRes)
            ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                resolveColor(context, android.R.attr.textColorPrimary)
            )

            // Animate icon entry
            ivIcon.alpha = 0f
            ivIcon.scaleX = 0.8f
            ivIcon.scaleY = 0.8f
            ivIcon.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(100)
                .start()

            if (page.taglineRes != null) {
                tvTagline.visibility = View.VISIBLE
                tvTagline.setText(page.taglineRes)
            } else {
                tvTagline.visibility = View.GONE
            }

            tvTitle.setText(page.titleRes)
            tvBody.setText(page.bodyRes)

            // Launcher page shows verification status
            if (page.type == OnboardingPageType.LAUNCHER) {
                val activity = context as? OnboardingActivity
                val verified = activity?.let {
                    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    val resolved = it.packageManager.resolveActivity(intent, 0)
                    resolved?.activityInfo?.packageName == it.packageName
                } ?: false

                tvStatus.visibility = View.VISIBLE
                if (verified) {
                    tvStatus.setText(R.string.onboarding_step_launcher_verified)
                    tvStatus.setTextColor(resolveColor(context, android.R.attr.textColorPrimary))
                    btnAction.visibility = View.GONE
                } else {
                    tvStatus.setText(R.string.onboarding_step_launcher_not_verified)
                    tvStatus.setTextColor(resolveColor(context, android.R.attr.textColorSecondary))
                    btnAction.visibility = View.VISIBLE
                    page.actionRes?.let { res -> btnAction.setText(res) }
                    btnAction.setOnClickListener { onAction(page) }
                }
            } else if (page.actionRes != null) {
                tvStatus.visibility = View.GONE
                btnAction.visibility = View.VISIBLE
                btnAction.setText(page.actionRes)
                btnAction.setOnClickListener { onAction(page) }
            } else {
                tvStatus.visibility = View.GONE
                btnAction.visibility = View.GONE
                btnAction.setOnClickListener(null)
            }
        }

        private fun resolveColor(context: android.content.Context, attrId: Int): Int {
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(attrId, typedValue, true)
            return typedValue.data
        }
    }
}
