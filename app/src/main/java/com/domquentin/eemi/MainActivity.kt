package com.domquentin.eemi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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
import java.io.InputStreamReader
import java.net.URL
import java.sql.Types.ROWID
import java.text.SimpleDateFormat
import java.util.*

class StupidAdapter (activity: MainActivity, ctx: Context, resid: Int): ArrayAdapter<Velib>(ctx, resid) {
    var act = activity
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v  = convertView ?: act.layoutInflater.inflate(R.layout.line, null)
        v.findViewById<TextView>(R.id.lineText).text = this.getItem(position).velibName
        v.findViewById<TextView>(R.id.number).text = this.getItem(position).velibNbBike.toString() + "/" + this.getItem(position).velibCapacity.toString()
        v.findViewById<TextView>(R.id.coordinates).text = "Long : " + this.getItem(position).velibLongitude.toString() + " - Lat : " + this.getItem(position).velibLatitude.toString()
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
        findViewById<TextView>(R.id.lastRefresh).text = "Last refresh :\n" + getCurrentDate()

        findViewById<Button>(R.id.refresh).setOnClickListener { y ->
            adapter!!.clear()
            adapter!!.notifyDataSetChanged()
            generateListView(adapter!!)
            findViewById<TextView>(R.id.lastRefresh).text = "Last refresh :\n" + getCurrentDate()
        }

        findViewById<Button>(R.id.getData).setOnClickListener { y ->
            adapter!!.clear()
            adapter!!.notifyDataSetChanged()
            getDataFromDB(adapter!!)
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
        **/
    }

    fun insertVelib(velib: Velib) {
        //SQLite
        val helper = MySQLiteHelper(this)
        val db = helper.writableDatabase

        val result = db.query("velib", arrayOf("code"), "code = ?", arrayOf<String>(velib.velibCode), "", "", "")
        if(result.count == 0) {
            Log.e("INSERT velib in db", velib.velibCode);
            val values = ContentValues().apply {
                put("name", velib.velibName)
                put("nbBike", velib.velibNbBike)
                put("capacity", velib.velibCapacity)
                put("longitude", velib.velibLongitude)
                put("latitude", velib.velibLatitude)
                put("code", velib.velibCode)
            }
            val newRowId = db?.insert("velib", null, values)
        } else {
            with(result) {
                while(moveToNext()) {
                    val values = ContentValues().apply {
                        put("name", velib.velibName)
                        put("nbBike", velib.velibNbBike)
                        put("capacity", velib.velibCapacity)
                        put("longitude", velib.velibLongitude)
                        put("latitude", velib.velibLatitude)
                        put("code", velib.velibCode)
                    }

                    val selection = "code = ?"
                    val selectionArgs = arrayOf(velib.velibCode)
                    val count = db.update("velib", values, selection, selectionArgs)
                    Log.e("UPDATE velib in db", velib.velibCode);
                }
            }
        }
        result.close()
        db.close()
    }

    fun getAllStations(buffer: MutableList<Velib>) : MutableList<Velib> {
        var b = buffer
        val helper = MySQLiteHelper(this)
        val db = helper.writableDatabase

        val cursor = db.query("velib", null, "", arrayOf<String>(), "", "", "")
        if(cursor.count != 0) {
            with(cursor) {
                while (moveToNext()) {
                    var nbBike = getInt(getColumnIndexOrThrow("nbBike"))
                    var capacity = getInt(getColumnIndexOrThrow("capacity"))
                    var velibName = getString(getColumnIndexOrThrow("name"))
                    var longitude = getDouble(getColumnIndexOrThrow("longitude"))
                    var latitude = getDouble(getColumnIndexOrThrow("latitude"))
                    var code = getString(getColumnIndexOrThrow("code"))
                    var v = Velib(nbBike, capacity, velibName, longitude, latitude, code)
                    b.add(v)
                }
            }
        }
        Log.e("get list from database ", "ok")
        return b
    }

    fun getDataFromDB(adapter: StupidAdapter) {
        var buffer = mutableListOf<Velib>()
        buffer = getAllStations(buffer)
        if(buffer.size == 0) {
            findViewById<TextView>(R.id.error).text = "No data, please refresh in database"
        }
        for(n in buffer) {
            adapter!!.add(n)
        }
        adapter!!.notifyDataSetChanged()
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
                            var code: String = ""
                            var velibName: String = ""
                            var capacity: Int = 0
                            var nbBike: Int = 0
                            var longitude: Double = 0.0
                            var latitude: Double = 0.0
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
                                            "coordonnees_geo" -> {
                                                reader.beginArray()
                                                var i: Int = 0
                                                while(reader.hasNext()) {
                                                    if(i == 0) {
                                                        longitude = reader.nextDouble()
                                                    } else {
                                                        latitude = reader.nextDouble()
                                                    }
                                                    i++
                                                }
                                                reader.endArray()
                                            }
                                            else -> reader.skipValue()
                                        }
                                    }
                                    reader.endObject()
                                } else if(name2.equals("recordid")) {
                                    code = reader.nextString()
                                }
                                else {
                                    reader.skipValue()
                                }
                            }
                            var v = Velib(nbBike, capacity, velibName, longitude, latitude, code)
                            buffer.add(v)
                            insertVelib(v)
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
                        buffer = getAllStations(buffer)
                        if(buffer.size == 0) {
                            findViewById<TextView>(R.id.error).text = "No data, please refresh in database"
                        }
                    } else {
                        findViewById<TextView>(R.id.error).text = ""
                    }
                    for(n in buffer) {
                        adapter!!.add(n)
                    }
                    adapter!!.notifyDataSetChanged()
                }
            } catch (e : Exception) {
                Log.e("EXC", e.message ?: "NO MESSAGE")
                e.printStackTrace()
            }
        })
        t.start()

        findViewById<ListView>(R.id.list).adapter = adapter
    }

    fun getCurrentDate() : String {
        val sdf = SimpleDateFormat("dd/MM/yyyy kk:mm:ss")
        val currentDate = sdf.format(Date())
        return currentDate
    }
}

class MySQLiteHelper(context: Context) : SQLiteOpenHelper(context, "VelibDB", null, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE IF NOT EXISTS velib(name VARCHAR(50), nbBike INT, capacity INT, longitude DOUBLE, latitude DOUBLE, code VARCHAR(100) PRIMARY KEY UNIQUE)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }
}

class Velib(nbBike: Int, capacity: Int, name: String, longitude: Double, latitude: Double, code: String) {
    val velibNbBike: Int = nbBike
    val velibCapacity: Int = capacity
    val velibName: String = name
    val velibLongitude: Double = longitude
    val velibLatitude: Double = latitude
    val velibCode: String = code
}
