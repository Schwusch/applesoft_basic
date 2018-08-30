package com.bocker.applesoft

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.List as UIList
import com.badlogic.gdx.utils.viewport.StretchViewport

sealed class Result<out V : Any> {
    data class Ok<out V : Any>(val value: V) : Result<V>()
    data class Err<out V : Any>(val error: String) : Result<V>()
}

class AppleSoft : ApplicationAdapter() {
    private lateinit var stage: Stage
    private lateinit var table: Table
    private lateinit var atlas: TextureAtlas
    private lateinit var skin: Skin
    private lateinit var input: TextField
    private lateinit var list: UIList<String>
    private val interpreter = Interpreter()


    override fun create() {
        atlas = TextureAtlas("commodore64/skin/uiskin.atlas")
        skin = Skin(Gdx.files.internal("commodore64/skin/uiskin.json"), atlas)
        input = TextField("", skin, "nobg").apply {
            addListener {
                if (it is InputEvent && it.type == InputEvent.Type.keyTyped) {
                    when(it.keyCode) {
                        Input.Keys.ENTER ->  {
                            val txt = input.text
                            interpreter.onPrint?.invoke(txt)
                            interpreter.interpretCommand(parseToCommand(tokenizeLine(txt)))
                            input.text = ""
                        }
                    }
                }
                false
            }
        }

        list =  UIList(skin)
        list.selection.isDisabled = true

        interpreter.onPrint = { text ->
            if(list.items.size > 22) list.items.removeIndex(0)
            list.items.add(text)
            list.invalidate()
            list.invalidateHierarchy()
        }

        table = Table(skin).apply {
            //debug = true
            setFillParent(true)
            add(list).left().top().expandX()
            row()
            add(input).left().top().expandX().fillX()

            background("window")
            top().left()
        }

        stage = Stage(StretchViewport(640f, 480f)).apply {
            addActor(table)
        }

        Gdx.input.inputProcessor = stage
        stage.keyboardFocus = input
    }

    override fun render() {
        val delta = Gdx.graphics.deltaTime
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
    }
}
