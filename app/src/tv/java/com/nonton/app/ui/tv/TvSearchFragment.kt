package com.nonton.app.ui.tv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.SpeechRecognitionCallback
import com.nonton.app.R
import com.nonton.app.data.model.Media

class TvSearchFragment : SearchSupportFragment() {

    private val resultsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(object : SearchResultProvider {
            override fun getResultsAdapter(): ObjectAdapter = resultsAdapter

            override fun onQueryTextChange(newQuery: String): Boolean {
                showResults(newQuery)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                showResults(query)
                return true
            }
        })
        setSpeechRecognitionCallback(object : SpeechRecognitionCallback {
            override fun recognizeSpeech() = Unit
        })
    }

    private fun showResults(query: String) {
        val resultAdapter = ArrayObjectAdapter(CardPresenter())
        resultsAdapter.clear()
        resultsAdapter.add(
            ListRow(
                HeaderItem(1L, "Hasil untuk \"$query\""),
                resultAdapter
            )
        )
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
