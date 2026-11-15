package com.nonton.app.ui.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import com.nonton.app.R
import com.nonton.app.data.model.Media
import com.nonton.app.data.model.MediaCategory
import com.nonton.app.data.model.MediaType

class TvBrowseFragment : BrowseSupportFragment() {

    private val adapter = ArrayObjectAdapter(ListRowPresenter())
    private val rows = mutableListOf<Row>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupUI()
        loadData()
        setupEventListeners()
    }

    private fun setupUI() {
        title = "Nonton"
        headersState = BrowseSupportFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        setBrandColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
        setSearchAffordanceColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
    }

    private fun loadData() {
        rows.clear()

        val heroAdapter = ArrayObjectAdapter(CardPresenter())
        heroAdapter.add(
            Media(
                id = "hero_1",
                title = "Nonton",
                mediaType = MediaType.MOVIE,
                voteAverage = 9.2,
                releaseDate = "2024-01-01"
            )
        )
        rows.add(ListRow(HeaderItem(0L, "Featured"), heroAdapter))

        val continueAdapter = ArrayObjectAdapter(CardPresenter())
        rows.add(ListRow(HeaderItem(1L, "Lanjutkan Menonton"), continueAdapter))

        listOf(
            "Trending Film" to MediaCategory.POPULAR,
            "Series Populer" to MediaCategory.POPULAR_SERIES,
            "Drakor Terbaru" to MediaCategory.POPULAR_DRAKOR,
            "Anime Populer" to MediaCategory.POPULAR_ANIME
        ).forEachIndexed { index, (title, _) ->
            val rowAdapter = ArrayObjectAdapter(CardPresenter())
            rows.add(ListRow(HeaderItem((index + 2).toLong(), title), rowAdapter))
        }

        adapter.clear()
        adapter.addAll(0, rows)
    }

    private fun setupEventListeners() {
        setOnItemViewClickedListener(OnItemViewClickedListener { _, item, _, _ ->
            if (item is Media) {
                // Navigation can be connected here when a TV detail route is available.
            }
        })
        setOnItemViewSelectedListener(OnItemViewSelectedListener { _, _, _, _ -> })
    }

    inner class CardPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.tv_movie_card, parent, false)
            return TvMediaViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
            if (item is Media) {
                (viewHolder as TvMediaViewHolder).bind(item)
            }
        }

        override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) = Unit
    }
}

class TvMediaViewHolder(view: View) : Presenter.ViewHolder(view) {
    fun bind(media: Media) {
        // Bind media data to views
    }

    fun bindSuggestion(query: String) {
        // Bind search suggestion data to views
    }
}
