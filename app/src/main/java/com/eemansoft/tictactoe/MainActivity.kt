package com.eemansoft.tictactoe

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.GravityCompat
import androidx.core.view.allViews
import androidx.core.view.drawToBitmap
import com.eemansoft.tictactoe.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var dialog: Dialog
    private lateinit var symbolP1: Button
    private lateinit var symbolP2: Button
    private lateinit var imgP1: ImageView
    private lateinit var imgP2: ImageView
    private var enableSoundEffects = true
    private var enableDarkMode = false
    private var count = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val sharedPrefEditor = sharedPref.edit()
        var isFirstTime = sharedPref.getBoolean("isFirstTime", true)
        enableDarkMode = sharedPref.getBoolean("enableDarkMode", false)
        enableSoundEffects = sharedPref.getBoolean("enableSoundEffects", true)

        count = savedInstanceState?.getInt("count", 0) ?: 0

        with(findViewById<NavigationView>(R.id.navigationView1)) {
            menu.findItem(R.id.itemStartNewGame).setOnMenuItemClickListener {

                if (count > 0)
                    showNewGameAlert("Current Progress will be lost!")
                else {
                    startNewGame()
                }
                false
            }
            menu.findItem(R.id.itemEnableSound).actionView?.apply {

                this as SwitchCompat
                this.isChecked = enableSoundEffects

                this.setOnClickListener {
                    enableSoundEffects = this.isChecked
                    sharedPrefEditor.putBoolean("enableSoundEffects", enableSoundEffects).apply()
                }
            }
            menu.findItem(R.id.itemEnableDarkMode).actionView?.apply {

                this as SwitchCompat
                this.isChecked = enableDarkMode
                setDarKMode(enableDarkMode)

                this.setOnClickListener {
                    enableDarkMode = this.isChecked
                    setDarKMode(enableDarkMode)
                    sharedPrefEditor.putBoolean("enableDarkMode", enableDarkMode).apply()
                }
            }
        }

        dialog = Dialog(this)
        dialog.setContentView(R.layout.playersinfo_dialog)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // set background of dialog transparent
        if (isFirstTime) {
            dialog.setOnShowListener {

                getBalloon("Click on Image to set your Image", ArrowOrientation.TOP, 0.5F)
                    .showAlignBottom(imgP2)
                getBalloon("Click on Icon to change it", ArrowOrientation.BOTTOM, 0.5F)
                    .showAlignTop(symbolP1)
                dialog.setOnShowListener { }
            }
        }

        val btnStart = dialog.findViewById<Button>(R.id.btnstart)
        imgP1 = dialog.findViewById(R.id.imgp1)
        imgP2 = dialog.findViewById(R.id.imgp2)
        symbolP1 = dialog.findViewById(R.id.symbolp1)
        symbolP2 = dialog.findViewById(R.id.symbolp2)

        imgP1.setOnClickListener {
            selectImageFromGallery(101)
        }

        imgP2.setOnClickListener {
            selectImageFromGallery(102)
        }

        btnStart.setOnClickListener {

            val edTxtP1 = dialog.findViewById<EditText>(R.id.edtxtp1)
            val edTxtP2 = dialog.findViewById<EditText>(R.id.edtxtp2)

            binding.p1name.text = if (edTxtP1.text.toString().isNotEmpty()) {
                edTxtP1.text.toString() + " (${symbolP1.text}) "
            } else {
                "Player 1 (${symbolP1.text}) "
            }
            binding.p2name.text = if (edTxtP2.text.toString().isNotEmpty()) {
                edTxtP2.text.toString() + " (${symbolP2.text}) "
            } else {
                "Player 2 (${symbolP2.text}) "
            }

            binding.p1img.setImageBitmap(imgP1.drawToBitmap())
            binding.p2img.setImageBitmap(imgP2.drawToBitmap())

            dialog.cancel() // we have to set it at last. if not then it is crashing
            binding.drawerLayout1.close()

            if (isFirstTime) {

                with(binding.drawerLayout1) {

                    openDrawer(GravityCompat.START, true)

                    Handler(Looper.getMainLooper()).postDelayed({
                        this.closeDrawers()
                        getBalloon(
                            "Swipe to open options drawer",
                            ArrowOrientation.START,
                            1F
                        ).showAlignStart(binding.root)
                    }, 1900)
                }

                isFirstTime = false
                sharedPrefEditor.putBoolean("isFirstTime", false).apply()
            }
        }

        if (savedInstanceState == null || count == 0) {
            startNewGame()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        with(outState) {
            putInt("count", count)
            putParcelable("imgP1", imgP1.drawable.toBitmap())
            putParcelable("imgP2", imgP2.drawable.toBitmap())
            putBoolean("isP1Turn", binding.txtcurrentp1.visibility == View.VISIBLE)
            putString("p1name", binding.p1name.text.toString())
            putString("p2name", binding.p2name.text.toString())
            binding.gridview1.allViews.filterIsInstance<Button>().forEach {
                if (it.text.isNotEmpty())
                    putString(it.id.toString(), it.text.toString())
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        count = savedInstanceState.getInt("count", 0)
        binding.p1img.setImageBitmap(savedInstanceState.getParcelable("imgP1"))
        binding.p2img.setImageBitmap(savedInstanceState.getParcelable("imgP2"))
        binding.p1name.text = savedInstanceState.getString("p1name", "")
        binding.p2name.text = savedInstanceState.getString("p2name", "")
        val isP1Turn = savedInstanceState.getBoolean("isP1Turn", false)
        if (isP1Turn) {
            binding.txtcurrentp1.visibility = View.VISIBLE
            binding.txtcurrentp2.visibility = View.INVISIBLE
        } else {
            binding.txtcurrentp1.visibility = View.INVISIBLE
            binding.txtcurrentp2.visibility = View.VISIBLE
        }
        binding.gridview1.allViews.filterIsInstance<Button>().forEach {
            with(it) {
                text = savedInstanceState.getString(it.id.toString())
                if (text != "") {
                    setBackgroundColor(Color.BLACK)
                    paint.shader = getTextShader(
                        this,
                        if (text == "X") "#FFD700" else "#ADD8E6", // Starting color - Gold
                        if (text == "X") "#FFA500" else "#87CEFA", // Ending color - Orange
                        if (text == "X") "#FFD700" else "#ADD8E6"  // Starting color - Gold
                    )
                }
            }
        }
    }

    private fun setDarKMode(mode: Boolean) {
        val nightMode =
            if (mode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        delegate.localNightMode = nightMode
    }

    // if you want to show some balloons etc on dialog, make sure to set them before show() of dialog. otherwise they may not appear
    private fun getBalloon(
        message: String,
        arrowOrientation: ArrowOrientation,
        arrowPosition: Float
    ): Balloon {
        return with(Balloon.Builder(this)) {
            setWidth(BalloonSizeSpec.WRAP)
            setHeight(BalloonSizeSpec.WRAP)
            setText(message)
            setTextColorResource(R.color.white)
            setAutoDismissDuration(2800)
            setArrowSize(10)
            setArrowOrientation(arrowOrientation)
            setArrowPosition(arrowPosition)
            setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            setPadding(9)
            setCornerRadius(9f)
            setBackgroundColorResource(androidx.appcompat.R.color.material_blue_grey_800)
            setBalloonAnimation(BalloonAnimation.ELASTIC)
            build()
        }
    }

    fun check(v: View) {

        val btnCurrent = v as Button

        if (btnCurrent.text == "") {

            if (enableSoundEffects) {

                val mySoundPool = SoundPool(1, AudioManager.STREAM_MUSIC, 0)
                val soundId = mySoundPool.load(this, R.raw.game_jump, 1)
                mySoundPool.setOnLoadCompleteListener { soundPool, _, _ ->
                    soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                }
            }

            btnCurrent.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_anim))

            val currentPlayerText = if (binding.txtcurrentp1.visibility == View.VISIBLE) {
                binding.txtcurrentp1.visibility = View.INVISIBLE
                binding.txtcurrentp2.visibility = View.VISIBLE
                symbolP1.text
            } else {
                binding.txtcurrentp1.visibility = View.VISIBLE
                binding.txtcurrentp2.visibility = View.INVISIBLE
                symbolP2.text
            }

            with(btnCurrent) {
                text = currentPlayerText
                setBackgroundColor(Color.BLACK)
                paint.shader = getTextShader(
                    this,
                    if (text == "X") "#FFD700" else "#ADD8E6", // Starting color - Gold
                    if (text == "X") "#FFA500" else "#87CEFA", // Ending color - Orange
                    if (text == "X") "#FFD700" else "#ADD8E6"  // Starting color - Gold
                )
            }

            for (combination in getWinningCombination()) {
                if (currentPlayerText == combination[0].text && combination[0].text == combination[1].text && combination[1].text == combination[2].text && combination[2].text == combination[3].text) {

                    announceWinner(
                        if (currentPlayerText == symbolP1.text) {
                            showKonfetti(
                                binding.viewKonfetti.height + 50f,
                                binding.viewKonfetti.height + 50f
                            )
                            binding.p1name
                        } else {
                            showKonfetti(-50f, -50f)
                            binding.p2name
                        }
                    )
                    return
                }
            }

            if (++count >= 16) {
                showNewGameAlert("Withdraw!")
            }

        } else
            Toast.makeText(this, "Cell is already marked", Toast.LENGTH_SHORT).show()

    }

    private fun showKonfetti(startY: Float, endY: Float) {
        binding.viewKonfetti.build()
            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
            .setDirection(0.0, 359.0)
            .setSpeed(1f, 5f)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000L)
            .addShapes(Shape.Square, Shape.Circle)
            .addSizes(Size(12, 5f))
            .setPosition(-50f, binding.viewKonfetti.width + 50f, startY, endY)
            .streamFor(500, 6000L)
    }

    private fun getTextShader(btn: Button, c1: String, c2: String, c3: String): Shader {
        return LinearGradient(
            0f, 0f, btn.paint.textSize * btn.text.length, btn.textSize,
            intArrayOf(
                Color.parseColor(c1),
                Color.parseColor(c2),
                Color.parseColor(c3)
            ), null, Shader.TileMode.CLAMP
        )
    }

    private fun getWinningCombination(): List<List<Button>> {
        return listOf(
            // for Rows
            listOf(binding.b1, binding.b2, binding.b3, binding.b4),
            listOf(binding.b5, binding.b6, binding.b7, binding.b8),
            listOf(binding.b9, binding.b10, binding.b11, binding.b12),
            listOf(binding.b13, binding.b14, binding.b15, binding.b16),
            // for Columns
            listOf(binding.b1, binding.b5, binding.b9, binding.b13),
            listOf(binding.b2, binding.b6, binding.b10, binding.b14),
            listOf(binding.b3, binding.b7, binding.b11, binding.b15),
            listOf(binding.b4, binding.b8, binding.b12, binding.b16),
            // for Diagonals
            listOf(binding.b1, binding.b6, binding.b11, binding.b16),
            listOf(binding.b4, binding.b7, binding.b10, binding.b13)
        )
    }

    private fun enableCellClick(status: Boolean) {
        for (i in 0 until binding.gridview1.childCount) {
            binding.gridview1.getChildAt(i).isClickable = status
        }
    }

    fun symbolP1(v: View) {
        toggleSymbol(v, symbolP2)
    }

    fun symbolP2(v: View) {
        toggleSymbol(v, symbolP1)
    }

    private fun toggleSymbol(v: View, otherSymbol: TextView) {
        if ((v as Button).text == "X") {
            v.text = "O"
            otherSymbol.text = "X"
        } else {
            v.text = "X"
            otherSymbol.text = "O"
        }
    }

    private fun showNewGameAlert(message: String) {

        val alertDialog = AlertDialog.Builder(this)
        with(alertDialog) {
            setIcon(R.drawable.baseline_info_24)
            setMessage(message)
            setTitle("Start New Game?")
            setPositiveButton("Yes") { _, _ -> // wca use DialogInterface.OnClickListener { DialogInterface, i ->
                startNewGame()
            }
            setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            show()
        }
    }

    private fun announceWinner(winner: TextView) {

        enableCellClick(false)

        if (enableSoundEffects) {

            with(MediaPlayer.create(this@MainActivity, R.raw.rocket_whoosh)) {
                start()
                setOnCompletionListener { mediaPlayer ->
                    mediaPlayer.release()
                    MediaPlayer.create(this@MainActivity, R.raw.group_applause).apply {
                        start()
                        mediaPlayer.setOnCompletionListener {
                            it.release()
                        }
                    }
                }
            }
        }

        binding.gridview1.animate().alpha(0F).duration = 1100

        with(winner) {
            text = winner.text.substring(0, winner.text.indexOf('('))

            animate()
                .setInterpolator(BounceInterpolator())
                .setInterpolator(AccelerateInterpolator())
                .x(resources.displayMetrics.widthPixels / 2F - width / 2)
                .y(resources.displayMetrics.heightPixels / 2F - height / 2)
                .scaleX(4F).scaleY(4F)
                .duration = 1400
        }

        val (gunImg, enemyImg) = if (winner == binding.p1name) {
            Pair(binding.p1gunimg, binding.p2img)
        } else {
            Pair(binding.p2gunimg, binding.p1img)
        }

        with(gunImg) {
            visibility = View.VISIBLE
            animate()
                .y(enemyImg.y)
                .x(enemyImg.x)
                .duration = 1400

            Handler().postDelayed({
                runOnUiThread {
                    gunImg.setImageResource(R.drawable.explosion)
                }
            }, 1400)
        }

        clearScreenData()
    }

    private fun startNewGame() {
        // no need to to set name and img of player, as when user click start button name and image will be set from there

        dialog.show()

        binding.p1gunimg.visibility = View.INVISIBLE
        binding.p2gunimg.visibility = View.INVISIBLE

        // clearing animations
        binding.p1name.translationX = 0F
        binding.p1name.translationY = 0F
        binding.p1name.scaleX = 1F
        binding.p1name.scaleY = 1F
        binding.p2name.translationX = 0F
        binding.p2name.translationY = 0F
        binding.p2name.scaleX = 1F
        binding.p2name.scaleY = 1F

        // positioning images back
        binding.p1gunimg.y = binding.p1img.y
        binding.p1gunimg.x = binding.p1img.x
        binding.p2gunimg.y = binding.p2img.y
        binding.p2gunimg.x = binding.p2img.x

        binding.p1gunimg.setImageResource(R.drawable.rocket)
        binding.p2gunimg.setImageResource(R.drawable.rocket)

        binding.txtcurrentp1.visibility = View.VISIBLE
        binding.txtcurrentp2.visibility = View.INVISIBLE
        clearScreenData()

        enableCellClick(true)
        binding.gridview1.animate().alpha(1F)
    }

    private fun clearScreenData() {

        count = 0
        binding.gridview1.allViews.filterIsInstance<Button>().forEach {

            it.text = ""
            it.setBackgroundColor(
                if (enableDarkMode) resources.getColor(R.color.darkThemeColor)
                else Color.WHITE
            )
        }
    }

    private fun selectImageFromGallery(requestCode: Int) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                100
            )
            return
        }

        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
    }

    @Deprecated("Deprecated in Java")
    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        ) // calling super constructor is necessary but in onBackPress it is not
        if (resultCode == RESULT_OK) {
            if (requestCode == 101) imgP1.setImageURI(data?.data)
            else if (requestCode == 102) imgP2.setImageURI(data?.data)
        }
    }

    @Deprecated("Deprecated in Java")
    @Override
    override fun onBackPressed() {

        with(binding.drawerLayout1) {

            if (this.isOpen) {
                this.close()
            } else {

                val alertDialog = AlertDialog.Builder(this@MainActivity)
                alertDialog.setIcon(R.drawable.baseline_sentiment_very_dissatisfied_24)
                    .setMessage("Are You want to quite game?").setTitle("Confirmation")
                    .setPositiveButton("No") { dialogInterface, _ -> // wca use DialogInterface.OnClickListener { DialogInterface, i ->
                        dialogInterface.cancel()
                    }
                    .setNegativeButton("Yes") { _, _ ->
                        super.onBackPressed()
                    }
                    .show()
            }
        }
    }
}
