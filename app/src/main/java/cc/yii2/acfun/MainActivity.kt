package cc.yii2.acfun

import android.content.Context
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cc.yii2.acfun.models.MediaObject
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private var mRecyclerView: VideoPlayerRecyclerView? = null
    private var editTextVideoListUrl: EditText? = null
    private var button_previous_page: Button? = null
    private var button_next_page: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mRecyclerView = findViewById(R.id.recycler_view)
        editTextVideoListUrl = findViewById(R.id.edit_text_video_list_url)

        editTextVideoListUrl!!.setOnEditorActionListener(OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                initRecyclerView()
                val imm =
                    this.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                return@OnEditorActionListener true
            }
            false
        })

        button_previous_page = findViewById(R.id.button_previous_page)
        button_next_page = findViewById(R.id.button_next_page)

        button_previous_page!!.setOnClickListener {
            val uri = paginate(
                Uri.parse(editTextVideoListUrl!!.text.toString()),
                "prev"
            )
            editTextVideoListUrl!!.setText(uri.toString())

            initRecyclerView()
        }

        button_next_page!!.setOnClickListener {
            val uri = paginate(
                Uri.parse(editTextVideoListUrl!!.text.toString()),
                "next"
            )
            editTextVideoListUrl!!.setText(uri.toString())

            initRecyclerView()
        }

        initRecyclerView()
    }

    private fun paginate(
        uri: Uri,
        type: String,
        page_param: String = "page"
    ): Uri? {
        val params: Set<String> = uri.getQueryParameterNames()
        val newUri: Uri.Builder = uri.buildUpon().clearQuery()

        for (param in params) {
            if (param != page_param) {
                newUri.appendQueryParameter(param, uri.getQueryParameter(param))

                continue
            }

            if (type == "prev") {
                var page_num = uri.getQueryParameter(param)!!.toInt() - 1

                if (page_num <= 0) {
                    page_num = 1
                }

                newUri.appendQueryParameter(param, page_num.toString())
            } else {
                newUri.appendQueryParameter(
                    param,
                    (uri.getQueryParameter(param)!!.toInt() + 1).toString()
                )
            }
        }

        if (!params.contains(page_param)) {
            newUri.appendQueryParameter(page_param, "1")
        }

        return newUri.build()
    }

    private fun initRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        mRecyclerView!!.layoutManager = layoutManager

        val video_list_url = editTextVideoListUrl!!.text.toString()
        val client = OkHttpClient()
        val request = Request.Builder().get().url(video_list_url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG)
                call.cancel()
            }

            @Throws(IOException::class)
            override fun onResponse(
                call: Call,
                response: Response
            ) {
                val myResponse = response.body!!.string()

                val document = Jsoup.parse(myResponse)
                val cols = document.select("div.col-sm-6.col-md-4.col-lg-4")
                val mediaObjects: ArrayList<MediaObject> = ArrayList()

                runOnUiThread {
                    for (col: Element in cols) {
                        val href: String = col.selectFirst("a").attr("href")
                        val title: String = col.selectFirst("span.video-title").text()
                        val thumbnail: String =
                            col.selectFirst("img.lazy.img-responsive").attr("data-original")
                        val duration: String = col.selectFirst("div.duration").text()
                        val video_added: String = col.selectFirst("div.video-added").text()
                        val video_views: String = col.selectFirst("div.video-views").text()
                        val video_rating: String = col.selectFirst("div.video-rating").text()
                        val media_url = thumbnail.replace("\\d+.jpg".toRegex(), "preview.mp4")

                        mediaObjects.add(
                            MediaObject(
                                title,
                                media_url,
                                thumbnail,
                                href,
                                duration,
                                video_added,
                                video_views,
                                video_rating
                            )
                        )
                    }

                    mRecyclerView!!.setMediaObjects(mediaObjects)
                    val adapter = VideoPlayerRecyclerAdapter(mediaObjects, initGlide())
                    mRecyclerView!!.adapter = adapter
                }
            }
        })
    }

    private fun initGlide(): RequestManager {
        val options = RequestOptions()
            .placeholder(R.drawable.white_background)
            .error(R.drawable.white_background)
        return Glide.with(this)
            .setDefaultRequestOptions(options)
    }

    override fun onDestroy() {
        if (mRecyclerView != null) mRecyclerView!!.releasePlayer()
        super.onDestroy()
    }
}