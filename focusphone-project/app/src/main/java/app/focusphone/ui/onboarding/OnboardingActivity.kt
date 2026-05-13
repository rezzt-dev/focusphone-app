package app.focusphone.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import app.focusphone.R
import app.focusphone.data.preferences.ThemePreferences
import app.focusphone.databinding.ActivityOnboardingBinding
import app.focusphone.ui.appselect.AppSelectActivity
import app.focusphone.ui.base.BaseActivity

class OnboardingActivity : BaseActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var pageChangeCallback: androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback? = null

    private val pages = listOf(
        OnboardingPage.Welcome,
        OnboardingPage.SetLauncher,
        OnboardingPage.PickApps
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInsets()
        setupViewPager()
        setupDots()
        setupButtons()
    }

    private fun setupViewPager() {
        binding.vpOnboarding.adapter = OnboardingPagerAdapter(pages, ::onPageAction)
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateUi(position)
        }
        binding.vpOnboarding.registerOnPageChangeCallback(pageChangeCallback!!)
    }

    private fun setupInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            binding.root.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pageChangeCallback?.let { binding.vpOnboarding.unregisterOnPageChangeCallback(it) }
    }

    private fun setupDots() {
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp6 = (6 * resources.displayMetrics.density).toInt()
        pages.forEach { _ ->
            val dot = View(this).apply {
                val lp = ViewGroup.MarginLayoutParams(dp8, dp8)
                lp.marginEnd = dp6
                layoutParams = lp
                setBackgroundResource(R.drawable.bg_dot_inactive)
            }
            binding.dotsContainer.addView(dot)
        }
        updateDots(0)
    }

    private fun updateDots(currentPage: Int) {
        for (i in 0 until binding.dotsContainer.childCount) {
            binding.dotsContainer.getChildAt(i).setBackgroundResource(
                if (i == currentPage) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
            )
        }
    }

    private fun setupButtons() {
        binding.btnSkip.setOnClickListener { finishOnboarding() }
        binding.btnNext.setOnClickListener {
            val cur = binding.vpOnboarding.currentItem
            if (cur < pages.lastIndex) binding.vpOnboarding.currentItem = cur + 1
            else finishOnboarding()
        }
    }

    private fun updateUi(position: Int) {
        updateDots(position)
        val isLast = position == pages.lastIndex
        binding.btnNext.text = getString(if (isLast) R.string.onboarding_get_started else R.string.onboarding_next)
        binding.btnSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
    }

    private fun onPageAction(page: OnboardingPage) {
        when (page) {
            OnboardingPage.SetLauncher -> startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            OnboardingPage.PickApps   -> startActivity(Intent(this, AppSelectActivity::class.java))
            else -> Unit
        }
    }

    private fun finishOnboarding() {
        ThemePreferences.markOnboardingComplete(this)
        finish()
    }
}

// ── Onboarding page descriptors ────────────────────────────────────

sealed class OnboardingPage(val titleRes: Int, val bodyRes: Int, val actionRes: Int? = null) {
    object Welcome     : OnboardingPage(R.string.onboarding_step1_title, R.string.onboarding_step1_body)
    object SetLauncher : OnboardingPage(R.string.onboarding_step2_title, R.string.onboarding_step2_body, R.string.onboarding_step2_action)
    object PickApps    : OnboardingPage(R.string.onboarding_step3_title, R.string.onboarding_step3_body, R.string.onboarding_step3_action)
}

// ── Adapter ────────────────────────────────────────────────────────

private class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>,
    private val onAction: (OnboardingPage) -> Unit
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder =
        PageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_onboarding_page, parent, false))

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) =
        holder.bind(pages[position], onAction)

    override fun getItemCount() = pages.size

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(page: OnboardingPage, onAction: (OnboardingPage) -> Unit) {
            itemView.findViewById<TextView>(R.id.tv_page_title).setText(page.titleRes)
            itemView.findViewById<TextView>(R.id.tv_page_body).setText(page.bodyRes)
            val btn = itemView.findViewById<TextView>(R.id.btn_page_action)
            if (page.actionRes != null) {
                btn.visibility = View.VISIBLE
                btn.setText(page.actionRes)
                btn.setOnClickListener { onAction(page) }
            } else {
                btn.visibility = View.GONE
            }
        }
    }
}
