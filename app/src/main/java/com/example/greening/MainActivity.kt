package com.example.greening

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.MenuItem
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.greening.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mp: MediaPlayer
    private lateinit var drawerToggle: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    private var totalTime = 0
    private var currentSongIndex = 0
    private lateinit var songLibrary: List<Song>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use View Binding to inflate the layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the song library
        songLibrary = listOf(
            Song("Lovely Day", "Bill Withers", R.raw.lovelyday, R.drawable.lovelyday),
            Song("L-O-V-E", "Nat cole king", R.raw.nat, R.drawable.nat),
            Song("Lovely Day 3", "Bill Withers 3", R.raw.twoofus, R.drawable.lovelyday3),
            Song("stand by me", "Ben E. King", R.raw.byme, R.drawable.benking),
            // Add more songs as needed
        )

        mp = MediaPlayer.create(this, songLibrary[currentSongIndex].resourceId)

        drawerToggle = binding.drawerlayout
        toggle = ActionBarDrawerToggle(this, drawerToggle, R.string.open, R.string.close)
        binding.navView

        // Set up initial song
        setCurrentSong()

        binding.play.setOnClickListener {
            if (mp.isPlaying) {
                mp.pause()
                binding.play.setImageResource(R.drawable.ply2)
            } else {
                mp.start()
                binding.play.setImageResource(R.drawable.pause)
            }
        }

        // Set up other button functionalities (download, volume, etc.) as needed

        // Set up volume control
        binding.volume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val volumeLevel = progress / 100.0f
                mp.setVolume(volumeLevel, volumeLevel)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Set up position tracking
        mp.isLooping = true
        totalTime = mp.duration
        binding.position.max = totalTime

        Music.setSeekbar(binding.volume, mp, true)
        Music.setSeekbar(binding.position, mp, null, true)

        mp.setOnCompletionListener {
            // Reset position seekbar and played text when the song completes
            binding.position.progress = 0
            binding.played.text = "0:00"
            // Play the next song
            playNextSong()
        }

        binding.position.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mp.seekTo(progress)
                    updatePlayedText(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Update position seekbar and played text as the song plays
        val handler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                val currentPosition = msg.what
                binding.position.progress = currentPosition
                val elapsedTime = Music.createTimeLabel(currentPosition)
                binding.played.text = elapsedTime
                val remainingTime = Music.createTimeLabel(totalTime - currentPosition)
                binding.remaining.text = "-$remainingTime"
            }
        }
        Thread(Runnable {
            while (true) {
                try {
                    val msg = Message()
                    msg.what = mp.currentPosition
                    handler.sendMessage(msg)
                    Thread.sleep(1000)

                } catch (e: InterruptedException) {
                    Log.d("Thread", e.message.toString())
                }
            }
        }).start()

        drawerToggle.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.home -> Toast.makeText(this, "Clicked Home", Toast.LENGTH_SHORT).show()
                R.id.playlist -> Toast.makeText(this, "Clicked playlist", Toast.LENGTH_SHORT).show()
                R.id.sync -> Toast.makeText(this, "Clicked sync", Toast.LENGTH_SHORT).show()
                R.id.delete -> Toast.makeText(this, "Clicked delete", Toast.LENGTH_SHORT).show()
                R.id.setting -> Toast.makeText(this, "Clicked setting", Toast.LENGTH_SHORT).show()
                R.id.login -> Toast.makeText(this, "Clicked login", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // Set up previous and next button click listeners
        binding.previous.setOnClickListener {
            playPreviousSong()
        }

        binding.nextm.setOnClickListener {
            playNextSong()
        }
    }

    private fun setCurrentSong() {
        val currentSong = songLibrary[currentSongIndex]
        binding.tittle.text = currentSong.title
        binding.artist.text = currentSong.artist

        // Load poster using Glide or your preferred image loading library
        Glide.with(this).load(currentSong.posterUrl).into(binding.poster)
    }

    private fun updatePlayedText(currentPosition: Int) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(currentPosition.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(currentPosition.toLong()) -
                TimeUnit.MINUTES.toSeconds(minutes)
        binding.played.text = String.format("%d:%02d", minutes, seconds)
    }

    private fun playPreviousSong() {
        if (currentSongIndex > 0) {
            currentSongIndex--
            mp.release()
            mp = MediaPlayer.create(this, songLibrary[currentSongIndex].resourceId)
            setCurrentSong()
            mp.start()
        } else {
            Toast.makeText(this, "No previous song available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playNextSong() {
        if (currentSongIndex < songLibrary.size - 1) {
            currentSongIndex++
            mp.release()
            mp = MediaPlayer.create(this, songLibrary[currentSongIndex].resourceId)
            setCurrentSong()
            mp.start()
        } else {
            Toast.makeText(this, "No next song available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mp.release()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
