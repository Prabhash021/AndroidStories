@file:Suppress("NAME_SHADOWING")

package com.example.mylibrary

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.util.*

class Stories @JvmOverloads
constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr), View.OnTouchListener{
    private lateinit var storiesList: List<StoryItem>
    private lateinit var loadingViewLayout: ConstraintLayout
    private lateinit var leftTouchPanel: FrameLayout
    private lateinit var rightTouchPanel: FrameLayout
    private lateinit var imageContentView: ImageView
    private lateinit var videoContentView: VideoView
    private lateinit var loadingView: ProgressBar

    private var progressBarBackgroundColor: Int = ContextCompat.getColor(context, R.color.progressGray)
    private var progressColor: Int = ContextCompat.getColor(context, R.color.progressWhite)
    private var loadingViewProgressColor: Int = ContextCompat.getColor(context ,R.color.progressWhite)
    private lateinit var storyDuration: String
    private lateinit var animation: ObjectAnimator
    private var mMediaPlayer: MediaPlayer? = null

    private var storyIndex: Int = 1
    private var userClicked: Boolean = false
    private lateinit var storiesListener: StoriesCallback

    private var videoDuration: Long = 0
    private var videoCurrentPosition: Int =0


    init {
        applyAttributes(attrs)
        initLayout()
    }

    @SuppressLint("CustomViewStyleable")
    private fun applyAttributes(attrs: AttributeSet?) {
        val attrs = context.obtainStyledAttributes(attrs, R.styleable.AndroidStories)

        progressBarBackgroundColor = attrs.getColor(R.styleable.AndroidStories_progressBarBackgroundColor, ContextCompat.getColor(context, R.color.progressGray))
        progressColor = attrs.getColor(R.styleable.AndroidStories_progressBarColor, ContextCompat.getColor(context, android.R.color.holo_orange_dark))
        storyDuration = attrs.getString(R.styleable.AndroidStories_storyDuration) ?: "10"
        loadingViewProgressColor = attrs.getColor(R.styleable.AndroidStories_loadingViewProgressColor, ContextCompat.getColor(context ,R.color.progressWhite))
        attrs.recycle()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initLayout() {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.stories_view_layout, this, false)
        addView(view)

        if (context is StoriesCallback)
            storiesListener = context as StoriesCallback

        loadingViewLayout = findViewById(R.id.progressBarContainer)
        leftTouchPanel = findViewById(R.id.leftTouchPanel)
        rightTouchPanel = findViewById(R.id.rightTouchPanel)
        imageContentView = findViewById(R.id.contentImageView)
        videoContentView = findViewById(R.id.contentVideoView)
        loadingView = findViewById(R.id.androidStoriesLoadingView)

        leftTouchPanel.setOnTouchListener(this)
        rightTouchPanel.setOnTouchListener(this)

        loadingView.indeterminateTintList = ColorStateList.valueOf((loadingViewProgressColor))
    }


    fun setStoriesList(storiesList: List<StoryItem>) {
        this.storiesList = storiesList
        addLoadingViews(storiesList)
    }

    private fun addLoadingViews(storiesList: List<StoryItem>) {
        var idcounter = 1
        for (story in storiesList) {
            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
            progressBar.visibility = View.VISIBLE
            progressBar.id = idcounter
            progressBar.tag = "story${idcounter++}"
            progressBar.progressBackgroundTintList = ColorStateList.valueOf((progressBarBackgroundColor))
            progressBar.progressTintList = ColorStateList.valueOf((progressColor))
            val params = LayoutParams(0, LayoutParams.WRAP_CONTENT)
            params.marginEnd = 5
            params.marginStart = 5
            loadingViewLayout.addView(progressBar, params)
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(loadingViewLayout)

        var counter = storiesList.size
        for (story in storiesList) {
            val progressBar = findViewWithTag<ProgressBar>("story${counter}")
            if (progressBar != null) {
                if (storiesList.size > 1) {
                    when (counter) {
                        storiesList.size -> {
                            constraintSet.connect(getId("story${counter}"), ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
                            constraintSet.connect(getId("story${counter}"), ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
                            constraintSet.connect(getId("story${counter}"), ConstraintSet.START, getId("story${counter-1}"), ConstraintSet.END)
                        }
                        1 -> {
                            constraintSet.connect(getId("story${counter}"), ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
                            constraintSet.connect(getId("story${counter}"), ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
                            constraintSet.connect(getId("story${counter}"), ConstraintSet.END, getId("story${counter + 1}"), ConstraintSet.START)
                        }
                        else -> {
                            constraintSet.connect(getId("story${counter}"), ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
                            constraintSet.connect(getId("story${counter}"), ConstraintSet.START, getId("story${counter-1}"), ConstraintSet.END)
                            constraintSet.connect(getId("story${counter}"), ConstraintSet.END, getId("story${counter + 1}"), ConstraintSet.START)
                        }
                    }
                } else {
                    constraintSet.connect(getId("story${counter}"), ConstraintSet.END, LayoutParams.PARENT_ID, ConstraintSet.END)
                    constraintSet.connect(getId("story${counter}"), ConstraintSet.TOP, LayoutParams.PARENT_ID, ConstraintSet.TOP)
                    constraintSet.connect(getId("story${counter}"), ConstraintSet.START, LayoutParams.PARENT_ID, ConstraintSet.START)
                }
            }
            counter--
        }
        constraintSet.applyTo(loadingViewLayout)
        startShowContent()
    }

    private fun startShowContent() {
        showStory()
    }

    private fun showStory() {
        val progressBar = findViewWithTag<ProgressBar>("story${storyIndex}")

        // defining animation for linear progress bar on top
        animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 100)
        animation.duration = secondsToMillis(storyDuration)
        animation.interpolator = LinearInterpolator()
        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {}
            override fun onAnimationEnd(animator: Animator) {

                if (storyIndex - 1 <= storiesList.size) {
                    if (userClicked) {
                        userClicked = false
                    } else {
                        if (storyIndex < storiesList.size) {
                            storyIndex += 1
                            showStory()
                        } else {
                            // on stories end
                            loadingView.visibility = View.GONE
                            onStoriesCompleted()
                        }
                    }
                } else {
                    // on stories end
                    loadingView.visibility = View.GONE
                    onStoriesCompleted()
                }
            }

            override fun onAnimationCancel(animator: Animator) {
                progressBar.progress = 100
            }
            override fun onAnimationRepeat(animator: Animator) {}
        })


        loadingView.visibility = View.VISIBLE

        val check = Uri.parse(storiesList[storyIndex -1].toString())
        val storyType = check.getQueryParameter("story_type")
        val format = storiesList[(storyIndex-1)].src    // whether the url is of video or img

        Log.d("st_TAG","type > $format")
        Log.d("st_TAG","format > $storyType")
        // 1 for video and 0 for image
        if(format == 0)
            loadImageStory(storiesList[storyIndex - 1])
        else{
            loadVideoStory(storiesList[storyIndex -1])
        }
    }

    private fun getId(tag: String): Int {
        return findViewWithTag<ProgressBar>(tag).id
    }

    private fun resetProgressBar(storyIndex: Int) {
        val currentProgressBar = findViewWithTag<ProgressBar>("story${storyIndex}")
        val lastProgressBar = findViewWithTag<ProgressBar>("story${storyIndex - 1}")
        currentProgressBar?.let {
            it.progress = 0
        }
        lastProgressBar?.let {
            it.progress = 0
        }
    }

    private fun completeProgressBar(storyIndex: Int) {
        val lastProgressBar = findViewWithTag<ProgressBar>("story${storyIndex}")
        lastProgressBar?.let {
            it.progress = 100
        }
    }

    private fun secondsToMillis(seconds: String): Long {
        return (seconds.toLongOrNull() ?: 3).times(1000)
    }

    private var startClickTime = 0L
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        val maxClickDuration = 200
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                startClickTime = Calendar.getInstance().timeInMillis
                if(animation.isRunning)
                    animation.pause()

                if(videoContentView.isPlaying && videoContentView.isVisible && mMediaPlayer != null){
                    mMediaPlayer?.pause()
                }
            }
            MotionEvent.ACTION_UP -> {
                // check it was small tap or long tap
                val clickDuration = Calendar.getInstance().timeInMillis - startClickTime
                if (clickDuration < maxClickDuration) {
                    //click occurred
                    view?.let {
                        if (it.id == R.id.leftTouchPanel) {
                            leftPanelTouch()
                        } else if (it.id == R.id.rightTouchPanel) {
                            rightPanelTouch()
                        }
                    }
                } else {
                    //hold click occurred
                    if(animation.isPaused)
                        animation.resume()

                    if(videoContentView.isVisible && !videoContentView.isPlaying && mMediaPlayer != null){
                        mMediaPlayer!!.start()
                        val currentProgressBar = findViewWithTag<ProgressBar>("story${storyIndex}")
                        updateProgressBar(mMediaPlayer, videoDuration, currentProgressBar)
                    }
                }
            }
        }
        return true
    }

    private fun rightPanelTouch() {
        // next story
        if (storyIndex == storiesList.size) {
            completeProgressBar(storyIndex)
            onStoriesCompleted()
            return
        }
        userClicked = true
        animation.end()

        if(videoContentView.isVisible && videoContentView.isPlaying){
            videoContentView.pause()
        }

        if (storyIndex < storiesList.size){
            storyIndex += 1
        }
        showStory()
    }

    private fun leftPanelTouch() {
        // previous story
        userClicked = true
        animation.end()

        if(videoContentView.isVisible && videoContentView.isPlaying){
            videoContentView.pause()
        }

        resetProgressBar(storyIndex)

        if (storyIndex > 1){
            storyIndex -= 1
        }
        showStory()
    }

    private fun onStoriesCompleted() {
        if (::storiesListener.isInitialized)
            storiesListener.onStoriesEnd()

        videoContentView.pause()
        videoCurrentPosition = 0

        if(mMediaPlayer != null){
            mMediaPlayer = null
        }
    }

    private fun loadImageStory(story: StoryItem) {

        videoContentView.visibility = GONE
        imageContentView.visibility = visibility

        Glide.with(context)
            .load(story.getResource())
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .fitCenter()
            .placeholder(imageContentView.drawable)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    loadingView.visibility = View.GONE
                    animation.start()
                    return false
                }
            }).into(imageContentView)
    }

    private fun loadVideoStory(storyItem: StoryItem) {

        val path:String = storyItem.getResource().toString()
        val currentProgressBar = findViewWithTag<ProgressBar>("story${storyIndex}")

        val uri: Uri = Uri.parse(path)
        videoContentView.setVideoURI(uri)

        imageContentView.visibility = GONE
        videoContentView.visibility = VISIBLE

        if(mMediaPlayer != null){
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }

        videoContentView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.setOnBufferingUpdateListener { player, percent ->
                if (percent == 100) {
                    mMediaPlayer = player

                    videoDuration = player.duration.toLong()

                    if(videoDuration > 45000) videoDuration = 45000

                    updateProgressBar(player, videoDuration, currentProgressBar)

                    videoContentView.start()
                    loadingView.visibility = GONE
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun updateProgressBar(player: MediaPlayer?, videoDuration: Long, progressBar: ProgressBar) {

        handler.postDelayed({
            try{
                videoCurrentPosition = videoContentView.currentPosition
                if(videoContentView.currentPosition >= videoDuration){
                    rightPanelTouch()
                    videoCurrentPosition = 0
                } else if(videoContentView.isPlaying){
                    val currentPosition = player?.currentPosition
                    val progress = (currentPosition!! * 100) / videoDuration
                    progressBar.progress = progress.toInt()
                    updateProgressBar(player, videoDuration, progressBar)
                }
            }catch (e: Exception){
                println("Exception: $e")
            }
        }, 10)
    }
}