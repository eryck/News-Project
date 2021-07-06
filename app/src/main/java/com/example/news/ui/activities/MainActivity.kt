package com.example.news.ui.activities

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.news.R
import com.example.news.api.NewsInterface
import com.example.news.model.Article
import com.example.news.model.News
import com.example.news.ui.adapter.NewsAdapter
import com.example.news.utils.ApiKey.API_KEY
import com.example.news.utils.Constants.BASE_URL
import com.example.news.utils.Constants.COUNTRY
import com.example.news.utils.NewsPreferences
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var newsInterface: NewsInterface
    private lateinit var newsApiConfig: String
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var articleList: ArrayList<Article>
    private lateinit var userKeyWordInput: String

    private lateinit var newsObservable: Observable<News>
    private lateinit var compositeDisposable: CompositeDisposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val retrofit: Retrofit = generateRetrofitBuilder()
        newsInterface = retrofit.create(NewsInterface::class.java)
        newsApiConfig = API_KEY
        main_swipe_refresh.setOnRefreshListener(this)
        articleList = ArrayList()
        newsAdapter = NewsAdapter(this, articleList)
        userKeyWordInput = ""
        compositeDisposable = CompositeDisposable()

        setSupportActionBar(toolbar_main)
        setRecyclerView()
        checkTheme()

    }

    override fun onStart() {
        super.onStart()
        checkUserKeywordInput()
    }

    override fun onRefresh() {
        checkUserKeywordInput()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    private fun setRecyclerView() {
        main_recyclerView.setHasFixedSize(true)
        main_recyclerView.layoutManager = LinearLayoutManager(this)
        main_recyclerView.itemAnimator = DefaultItemAnimator()
        main_recyclerView.adapter = newsAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_theme_mode -> {
                chooseThemeDialog(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun chooseThemeDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.choose_your_theme))
        val checkedItem = NewsPreferences(this).themeMode
        val themes = arrayOf(
            getString(R.string.light),
            getString(R.string.dark)
        )
        builder.setSingleChoiceItems(themes, checkedItem) { dialog, which ->
            when (which) {
                0 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    NewsPreferences(this).themeMode = 0
                    delegate.applyDayNight()
                    dialog.dismiss()
                }
                1 -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    NewsPreferences(this).themeMode = 1
                    delegate.applyDayNight()
                    dialog.dismiss()
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun checkTheme() {
        when (NewsPreferences(this).themeMode) {
            0 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                delegate.applyDayNight()
            }
            1 -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                delegate.applyDayNight()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menu != null) {
            val inflater = menuInflater
            inflater.inflate(R.menu.menu_main, menu)
            setUpSearchMenuItem(menu)
        }
        return true
    }

    private fun checkUserKeywordInput() {
        if (userKeyWordInput.isEmpty()) {
            queryNews()
        } else {
            getKeyWordQuery(userKeyWordInput)
        }
    }

    private fun setUpSearchMenuItem(menu: Menu) {
        val searchManager: SearchManager =
            (getSystemService(Context.SEARCH_SERVICE)) as SearchManager
        val searchView: SearchView = ((menu.findItem(R.id.action_search)?.actionView)) as SearchView
        val searchMenuItem: MenuItem = menu.findItem(R.id.action_search)

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.queryHint = getString(R.string.search)
        searchView.setOnQueryTextListener(onQueryTextListenerCallback())
        searchMenuItem.icon.setVisible(false, false)
    }

    private fun onQueryTextListenerCallback(): SearchView.OnQueryTextListener {
        return object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return checkQueryText(query)
            }

            override fun onQueryTextChange(query: String?): Boolean {
                return checkQueryText(query)
            }
        }
    }

    private fun checkQueryText(query: String?): Boolean {
        if (query != null && query.length > 1) {
            userKeyWordInput = query
            getKeyWordQuery(query)
        } else if (query != null && query == "") {
            userKeyWordInput = ""
            queryNews()
        }
        return false
    }

    private fun getKeyWordQuery(userKeyWordInput: String) {
        main_swipe_refresh.isRefreshing = true
        if (userKeyWordInput != null && userKeyWordInput.isNotEmpty()) {
            newsObservable = newsInterface.getUserSearchInput(newsApiConfig, userKeyWordInput)
            subscribeObservableOfArticle()
        } else {
            queryNews()
        }
    }

    private fun queryNews() {
        main_swipe_refresh.isRefreshing = true
        newsObservable = newsInterface.getTopHeadlines(COUNTRY, newsApiConfig)
        subscribeObservableOfArticle()
    }

    private fun subscribeObservableOfArticle() {
        articleList.clear()
        compositeDisposable.add(
            newsObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap {
                    Observable.fromIterable(it.articles)
                }
                .subscribeWith(createNewsObserver())
        )
    }

    private fun createNewsObserver(): DisposableObserver<Article> {
        return object : DisposableObserver<Article>() {
            override fun onNext(article: Article) {
                if (!articleList.contains(article)) {
                    articleList.add(article)
                }
            }

            override fun onComplete() {
                showArticlesOnRecyclerView()
            }

            override fun onError(e: Throwable) {
                Log.e("createArticleObserver", "Article error: ${e.message}")
            }
        }
    }

    private fun showArticlesOnRecyclerView() {
        if (articleList.size > 0) {
            empty_text.visibility = View.GONE
            main_recyclerView.visibility = View.VISIBLE
            newsAdapter.setArticles(articleList)
        } else {
            main_recyclerView.visibility = View.GONE
            empty_text.visibility = View.VISIBLE
        }

        main_swipe_refresh.isRefreshing = false
    }

    private fun generateRetrofitBuilder(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }

}