package com.example.news.ui.activities

import android.Manifest
import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.news.R
import com.example.news.utils.Constants.EXTRA_ARTICLE_CONTENT
import com.example.news.utils.Constants.EXTRA_ARTICLE_DESCRIPTION
import com.example.news.utils.Constants.EXTRA_ARTICLE_IMAGE
import com.example.news.utils.Constants.EXTRA_ARTICLE_TITLE
import com.example.news.utils.Constants.EXTRA_ARTICLE_URL
import com.example.news.utils.Constants.EXTRA_ERROR
import com.example.news.utils.NewsPreferences
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_read_news.*
import java.io.File

class ReadNewsActivity : AppCompatActivity() {

    private var readTitle = ""
    private var readDescription = ""
    private var readContent = ""
    private var readURL = ""
    private var readImage = ""

    var msg: String? = ""
    var lastMsg = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_news)

        setSupportActionBar(toolbar_read_news)
        extras()
        setupActionBar()
        setLayout()
        buttonDownload(readImage)
        buttonShare(readURL)
    }

    private fun extras() {
        readImage = intent.getStringExtra(EXTRA_ARTICLE_IMAGE)!!
        readTitle = intent.getStringExtra(EXTRA_ARTICLE_TITLE)!!
        readDescription = intent.getStringExtra(EXTRA_ARTICLE_DESCRIPTION)!!
        readURL = intent.getStringExtra(EXTRA_ARTICLE_URL)!!
        readContent = intent.getStringExtra(EXTRA_ARTICLE_CONTENT)!!
    }

    private fun setLayout() {
        checkForUrlToImage(readImage)
        if (readTitle == EXTRA_ERROR) {
            tittle_read_news.text = getString(R.string.text_not_found)
        } else {
            tittle_read_news.text = readTitle
        }

        if (readDescription == EXTRA_ERROR) {
            description_read_news.text = getString(R.string.text_not_found)
        } else {
            description_read_news.text = readDescription
        }

        if (readContent == EXTRA_ERROR) {
            content_read_news.text = getString(R.string.text_not_found)
        } else {
            content_read_news.text = readContent
        }
    }

    private fun checkForUrlToImage(imageUrl: String) {
        if (imageUrl != EXTRA_ERROR) {
            Picasso.get()
                .load(imageUrl)
                .centerCrop()
                .fit()
                .into(image_read_news)
        }
    }

    private fun buttonShare(urlShare: String) {
        if (readURL != EXTRA_ERROR){
            button_share.setOnClickListener {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, urlShare)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                startActivity(shareIntent)
            }
        }else{
            Toast.makeText(this, getString(R.string.link_not_found), Toast.LENGTH_SHORT).show()
        }

    }

    private fun buttonDownload(imageUrl: String) {
        button_store.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                askPermissions()
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.download_image)
                builder.setMessage(R.string.download_image_message)
                builder.setCancelable(false)
                builder.setPositiveButton(R.string.yes) { _, _ ->
                    downloadImageNews(imageUrl)
                }
                builder.setNegativeButton(R.string.no) { _, _ ->
                    Toast.makeText(
                        applicationContext,
                        R.string.no, Toast.LENGTH_SHORT
                    ).show()
                }
                builder.show()
            }
        }
    }

    private fun downloadImageNews(image: String) {
        val directoryDownload = File(Environment.DIRECTORY_DOWNLOADS)

        if (!directoryDownload.exists()) {
            directoryDownload.mkdirs()
        }

        val downloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(image)
        val request = DownloadManager.Request(downloadUri).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(image.substring(image.lastIndexOf("/") + 1))
                .setDescription("")
                .setDestinationInExternalPublicDir(
                    directoryDownload.toString(),
                    image.substring(image.lastIndexOf("/") + 1)
                )
        }

        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)
        Thread(Runnable {
            var downloading = true
            while (downloading) {
                val cursor: Cursor = downloadManager.query(query)
                cursor.moveToFirst()
                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false
                }
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                msg = statusMessage(image, directoryDownload, status)
                if (msg != lastMsg) {
                    this.runOnUiThread {
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    lastMsg = msg ?: ""
                }
                cursor.close()
            }
        }).start()
    }

    private fun statusMessage(url: String, directory: File, status: Int): String? {
        var msg = ""
        msg = when (status) {
            DownloadManager.STATUS_FAILED -> getString(R.string.download_failed)
            DownloadManager.STATUS_PAUSED -> getString(R.string.paused)
            DownloadManager.STATUS_PENDING -> getString(R.string.peding)
            DownloadManager.STATUS_RUNNING -> getString(R.string.downloading)
            DownloadManager.STATUS_SUCCESSFUL -> getString(R.string.download_successfully) +
                    "$directory" + File.separator + url.substring(
                url.lastIndexOf("/") + 1
            )
            else -> getString(R.string.theres_nothing_download)
        }
        return msg
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun askPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                AlertDialog.Builder(this)
                    .setTitle("Permission required")
                    .setMessage("Permission required to save photos from the Web.")
                    .setPositiveButton("Accept") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
                        )
                        finish()
                    }
                    .setNegativeButton("Deny") { dialog, _ -> dialog.cancel() }
                    .show()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
                )
            }
        } else {
            buttonDownload(readImage)
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    buttonDownload(readImage)
                }
                return
            }
        }
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

    private fun setupActionBar() {
        setSupportActionBar(toolbar_read_news)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        }
        toolbar_read_news.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_read, menu)
        return super.onCreateOptionsMenu(menu)
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
}