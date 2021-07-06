package com.example.news.ui.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.news.R
import com.example.news.model.Article
import com.example.news.ui.activities.ReadNewsActivity
import com.example.news.utils.Constants.EXTRA_ARTICLE_CONTENT
import com.example.news.utils.Constants.EXTRA_ARTICLE_DESCRIPTION
import com.example.news.utils.Constants.EXTRA_ARTICLE_IMAGE
import com.example.news.utils.Constants.EXTRA_ARTICLE_TITLE
import com.example.news.utils.Constants.EXTRA_ARTICLE_URL
import com.example.news.utils.Constants.EXTRA_ERROR
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_news.view.*

class NewsAdapter(
    private val context: Context,
    private var articleList: ArrayList<Article>
) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article: Article = articleList.get(position)
        setPropertiesForNewsViewHolder(holder, article)
        holder.cardViewNews.setOnClickListener {
            openReadNewsActivity(article)
        }
    }

    override fun getItemCount(): Int = articleList.size

    private fun openReadNewsActivity(article: Article) {
        context.startActivity(Intent(context, ReadNewsActivity::class.java).apply {

            if (article.urlToImage == null || article.urlToImage.isEmpty()) {
                putExtra(EXTRA_ARTICLE_IMAGE, EXTRA_ERROR)
            } else {
                putExtra(EXTRA_ARTICLE_IMAGE, article.urlToImage)
            }

            if (article.title == null || article.title.isEmpty()) {
                putExtra(EXTRA_ARTICLE_TITLE, EXTRA_ERROR)
            } else {
                putExtra(EXTRA_ARTICLE_TITLE, article.title)
            }
//
            if (article.description == null || article.description.isEmpty()) {
                putExtra(EXTRA_ARTICLE_DESCRIPTION, EXTRA_ERROR)
            } else {
                putExtra(EXTRA_ARTICLE_DESCRIPTION, article.description)
            }

            if (article.url == null || article.url.isEmpty()) {
                putExtra(EXTRA_ARTICLE_URL, EXTRA_ERROR)
            } else {
                putExtra(EXTRA_ARTICLE_URL, article.url)
            }

            if (article.content == null || article.content.isEmpty()) {
                putExtra(EXTRA_ARTICLE_CONTENT, EXTRA_ERROR)
            } else {
                putExtra(EXTRA_ARTICLE_CONTENT, article.content)
            }
        })
    }

    private fun checkForUrlToImage(article: Article, newsViewHolder: NewsViewHolder) {
        if (article.urlToImage != null) {
            Picasso.get()
                .load(article.urlToImage)
                .centerCrop()
                .fit()
                .into(newsViewHolder.imageNews)
        }
    }

    private fun checkTexts(article: Article, newsViewHolder: NewsViewHolder) {
        if (article.title != null) {
            newsViewHolder.titleNews.text = article?.title
        } else {
            newsViewHolder.titleNews.text = context.getText(R.string.text_not_found)
        }

        if (article.description != null) {
            newsViewHolder.descriptionNews.text = article?.description
        } else {
            newsViewHolder.descriptionNews.text = context.getText(R.string.text_not_found)
        }
    }


    private fun setPropertiesForNewsViewHolder(newsViewHolder: NewsViewHolder, article: Article) {
        checkForUrlToImage(article, newsViewHolder)
        checkTexts(article, newsViewHolder)
    }

    fun setArticles(articles: ArrayList<Article>) {
        articleList = articles
        notifyDataSetChanged()
    }

    inner class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardViewNews: CardView by lazy { itemView.card_news }
        val imageNews: ImageView by lazy { itemView.image_news }
        val titleNews: TextView by lazy { itemView.tittle_news }
        val descriptionNews: TextView by lazy { itemView.description_news }
    }
}