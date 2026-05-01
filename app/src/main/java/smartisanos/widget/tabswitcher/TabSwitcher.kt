package smartisanos.widget.tabswitcher

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import com.smartisanos.music.R
import com.smartisanos.music.ui.navigation.MusicDestination

class TabSwitcher @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {
    private val radioGroup: RadioGroup
    private val destinationIds = mutableMapOf<MusicDestination, Int>()
    private val idDestinations = mutableMapOf<Int, MusicDestination>()
    private var selectedDestination = MusicDestination.DefaultMain
    private var suppressSelectionCallback = false
    private var onDestinationSelected: ((MusicDestination) -> Unit)? = null

    init {
        val barHeight = resources.getDimensionPixelSize(R.dimen.smartisan_tabswitch_tabbar_height)

        radioGroup = RadioGroup(context).apply {
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.sb_repeat_tabbar_bg)
            weightSum = 5f
            setOnCheckedChangeListener { _, checkedId ->
                if (suppressSelectionCallback) {
                    return@setOnCheckedChangeListener
                }
                val destination = idDestinations[checkedId] ?: return@setOnCheckedChangeListener
                selectedDestination = destination
                onDestinationSelected?.invoke(destination)
            }
        }
        addView(
            radioGroup,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                barHeight,
            ).apply {
                addRule(ALIGN_PARENT_BOTTOM)
            },
        )

        addView(
            ImageView(context).apply {
                setBackgroundResource(R.drawable.tab_bar_shadow)
            },
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                addRule(ALIGN_PARENT_TOP)
            },
        )

        addView(
            View(context).apply {
                setBackgroundColor(context.getColor(R.color.nav_list_line))
            },
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.nav_divider_height),
            ).apply {
                addRule(ALIGN_PARENT_TOP)
            },
        )

        setDestinations(MusicDestination.entries)
    }

    fun setOnDestinationSelectedListener(listener: ((MusicDestination) -> Unit)?) {
        onDestinationSelected = listener
    }

    fun setCurrentDestination(destination: MusicDestination) {
        selectedDestination = destination
        val id = destinationIds[destination] ?: return
        suppressSelectionCallback = true
        radioGroup.check(id)
        suppressSelectionCallback = false
    }

    fun setDestinations(destinations: List<MusicDestination>) {
        radioGroup.removeAllViews()
        destinationIds.clear()
        idDestinations.clear()

        destinations.forEach { destination ->
            val itemId = View.generateViewId()
            val button = LayoutInflater.from(context)
                .inflate(R.layout.sb_tab_item, radioGroup, false) as RadioButton
            button.id = itemId
            button.text = destination.label
            button.setTextColor(context.getColorStateList(R.color.tab_bar_text_color))
            button.setCompoundDrawablesWithIntrinsicBounds(
                null,
                context.getDrawable(destination.tabIconSelectorRes()),
                null,
                null,
            )
            button.compoundDrawablePadding = resources.getDimensionPixelSize(
                R.dimen.smartisan_switch_bar_drawablePadding,
            )
            button.setOnClickListener {
                radioGroup.check(itemId)
            }
            radioGroup.addView(button)
            destinationIds[destination] = itemId
            idDestinations[itemId] = destination
        }

        setCurrentDestination(selectedDestination)
    }

    private fun MusicDestination.tabIconSelectorRes(): Int {
        return when (this) {
            MusicDestination.Playlist -> R.drawable.tabbar_playlist_selector
            MusicDestination.Artist -> R.drawable.tabbar_artist_selector
            MusicDestination.Album -> R.drawable.tabbar_album_selector
            MusicDestination.Songs -> R.drawable.tabbar_song_selector
            MusicDestination.More -> R.drawable.tabbar_more_selector
        }
    }
}
