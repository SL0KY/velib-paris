package com.domquentin.eemi

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.JsonReader
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.core.database.getStringOrNull
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

class StupidAdapter (activity: MainActivity, ctx: Context, resid: Int): ArrayAdapter<Velib>(ctx, resid) {
    var act = activity
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v  = convertView ?: act.layoutInflater.inflate(R.layout.line, null)
        v.findViewById<TextView>(R.id.lineText).text = this.getItem(position).velib_name
        v.findViewById<TextView>(R.id.number).text = this.getItem(position).velib_nbBike.toString() + "/" + this.getItem(position).velib_capacity.toString()
        return v
    }
}


class MainActivity : AppCompatActivity() {

    var adapter : StupidAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = StupidAdapter(this, this, R.id.lineText)
        generateListView(adapter!!)
        findViewById<TextView>(R.id.lastRefresh).text = getCurrentDate()

        findViewById<Button>(R.id.refresh).setOnClickListener { y ->
            adapter!!.clear()
            adapter!!.notifyDataSetChanged()
            generateListView(adapter!!)
            findViewById<TextView>(R.id.lastRefresh).text = getCurrentDate()
        }



        /**
        var prefs = this.getSharedPreferences("masuperapp", Context.MODE_PRIVATE)
        val fs = prefs.getFloat("fontSize", 14.0F)
        var ed = prefs.edit()
        ed.putFloat("fontSize", 18.0F)
        ed.apply() // ed.commit()

        //Stockage internal
        var root = filesDir
        // root.freeSpace
        val contents = "test"
        var writer = FileWriter(root)
        var bw = BufferedWriter(writer)
        bw.write(contents)

        // Stockage external
        var ext = getExternalFilesDir(null)
        // var cache = externalCacheDir

        //SQLite
        val helper = MySQLiteHelper(this)
        val db = helper.readableDatabase
        val result = db.query("test", arrayOf("test"), "", arrayOf<String>(), "", "", "")
        while (!result.isLast) {
            Log.e("db", result.getStringOrNull(0))
            result.moveToNext()
        }
        result.close()**/
    }

    fun generateListView(adapter: StupidAdapter) {
        var t = Thread(Runnable {
            try {
                var u = URL("https://opendata.paris.fr/api/records/1.0/search/?dataset=velib-disponibilite-en-temps-reel&facet=name&facet=is_installed&facet=is_renting&facet=is_returning&facet=nom_arrondissement_communes")
                var c = u.openConnection()
                var reader = JsonReader(InputStreamReader(c.getInputStream()))
                var buffer = mutableListOf<Velib>()

                reader.beginObject()

                while(reader.hasNext()) {
                    val name = reader.nextName()
                    if(name.equals("records")) {
                        reader.beginArray()
                        while(reader.hasNext()) {
                            reader.beginObject()
                            var velibName: String = ""
                            var capacity: Int = 0
                            var nbBike: Int = 0
                            while(reader.hasNext()) {
                                var name2 = reader.nextName()
                                if (name2.equals("fields")) {
                                    reader.beginObject()
                                    while (reader.hasNext()) {
                                        var name3 = reader.nextName()
                                        when (name3) {
                                            "numbikesavailable" -> nbBike = reader.nextInt()
                                            "capacity" -> capacity = reader.nextInt()
                                            "name" -> velibName = reader.nextString()
                                            else -> reader.skipValue()
                                        }
                                    }
                                    reader.endObject()
                                } else {
                                    reader.skipValue()
                                }
                            }
                            var v = Velib(nbBike, capacity, velibName)
                            buffer.add(v)
                            reader.endObject()
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()

                runOnUiThread {
                    if(buffer.size == 0) {
                        findViewById<TextView>(R.id.error).text = "No data, please refresh"
                    } else {
                        findViewById<TextView>(R.id.error).text = ""
                    }
                    for(n in buffer) {
                        adapter!!.add(n)
                    }
                    adapter!!.notifyDataSetChanged()
                }


                /**
                var line : String? = reader.readLine()
                runOnUiThread {
                while(line != null) {
                adapter?.add(line)
                line = reader.readLine()
                }
                adapter?.notifyDataSetChanged()
                }**/
            } catch (e : Exception) {
                Log.e("EXC", e.message ?: "NO MESSAGE")
                e.printStackTrace()
            }
        })
        t.start()

        findViewById<ListView>(R.id.list).adapter = adapter
    }

    fun getCurrentDate() : String {
        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())
        return currentDate
    }
}

class MySQLiteHelper(context: Context) : SQLiteOpenHelper(context, "LocalDB", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE test(id INT NOT NULL, test TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class Velib(nbBike: Int, capacity: Int, name: String) {
    val velib_nbBike: Int = nbBike
    val velib_capacity: Int = capacity
    val velib_name: String = name
}
/**
data class Coordonnees(var lat : Double, val lng: Double) {
    Gson().fromJson(json, Coordonnees::class.java)
}**/
