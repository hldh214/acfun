package cc.yii2.acfun

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import cc.yii2.acfun.models.MediaObject
import cc.yii2.acfun.util.VerticalSpacingItemDecorator
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {
    private var mRecyclerView: VideoPlayerRecyclerView? = null
    private var editTextVideoListUrl: EditText? = null

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

        initRecyclerView()
    }

    private fun initRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        mRecyclerView!!.layoutManager = layoutManager
        val itemDecorator = VerticalSpacingItemDecorator(10)
        mRecyclerView!!.addItemDecoration(itemDecorator)

        val video_list_url = editTextVideoListUrl!!.text.toString()
        val client = OkHttpClient()
        val request = Request.Builder().get().url(video_list_url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
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