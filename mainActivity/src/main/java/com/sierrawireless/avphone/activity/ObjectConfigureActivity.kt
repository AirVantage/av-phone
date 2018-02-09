package com.sierrawireless.avphone.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import com.baoyz.swipemenulistview.SwipeMenuCreator
import com.baoyz.swipemenulistview.SwipeMenuItem
import com.sierrawireless.avphone.ConfigureFragment
import com.sierrawireless.avphone.ObjectsManager
import com.sierrawireless.avphone.R
import com.sierrawireless.avphone.adapter.ObjectDataAdapter
import com.sierrawireless.avphone.model.AvPhoneObject
import com.sierrawireless.avphone.tools.Tools
import kotlinx.android.synthetic.main.activity_object_configure.*
import java.util.*

class ObjectConfigureActivity : Activity() {
    internal var objectsManager: ObjectsManager = ObjectsManager.getInstance()
    private var menu: ArrayList<String> = ArrayList()
    private var position: Int = 0
    private var add = false
    internal var obj: AvPhoneObject? = null

    private var context: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_configure)
        instance = this

        objectsManager = ObjectsManager.getInstance()

        context = this





        val intent = intent
        position = intent.getIntExtra(ConfigureFragment.INDEX, -1)
        add = position == -1

        cancel.setOnClickListener {
            //reload object from list
            objectsManager.reload()
            val i = Intent()
            setResult(Activity.RESULT_CANCELED, i)
            finish()
        }

        save.setOnClickListener {
            if (objectNameEdit.visibility != View.GONE || add) {
                obj!!.name = objectNameEdit.text.toString()
            }
            obj!!.datas
                    .filter { it.isInteger }
                    .forEach { it.current = it.defaults.toInt() }

            objectsManager.save()
            val i = Intent()
            i.putExtra(ConfigureFragment.POS, position)
            setResult(Activity.RESULT_OK, i)
            finish()
        }


        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, i, _ ->
            obj!!.name = objectNameEdit.text.toString()
            val lIntent = Intent(view.context, ObjectDataActivity::class.java)
            lIntent.putExtra(OBJECT_POSITION, position)
            lIntent.putExtra(DATA_POSITION, i)
            if (i == menu.size - 1) {
                lIntent.putExtra(ADD, true)
            } else {
                lIntent.putExtra(ADD, false)
            }
            startActivity(lIntent)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onResume() {
        super.onResume()
        if (position == -1) {
            add = true
            obj = AvPhoneObject()
            obj!!.name = ""
            titleObject.setText(R.string.add_new_object)
            objectsManager.objects.add(obj!!)
            position = objectsManager.objects.size - 1
        } else {
            obj = objectsManager.getObjectByIndex(position)
            if (!add) {
                nameObject.visibility = View.GONE
            }
            titleObject.text = obj!!.name
        }
        menuGeneration()
    }

    fun menuGeneration() {
        menu = ArrayList()
        for (data in obj!!.datas) {
            menu.add(data.name)
        }
        menu.add(getString(R.string.add_new_data))
        val adapter = ObjectDataAdapter(this, R.layout.menu_objects, menu)

        listView.adapter = adapter
        listView.invalidateViews()
    }

    companion object {
        var OBJECT_POSITION = "object_pos"
        var DATA_POSITION = "data_position"
        var ADD = "add"
        @SuppressLint("StaticFieldLeak")
        var instance:ObjectConfigureActivity? = null
    }
}
