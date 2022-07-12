package jp.techacademy.kenji.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog

import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmObject
import io.realm.Sort

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_input.*
import java.lang.reflect.TypeVariable
import java.util.*

const val EXTRA_TASK ="jp.techacademy.kenji.taskapp.TASK"

class MainActivity : AppCompatActivity() {

    public var category_string: String? = null

    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(element: Realm) {
            reloadListView()
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this, InputActivity::class.java)
            startActivity(intent)
        }

        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        mTaskAdapter = TaskAdapter(this)

        listView1.setOnItemClickListener { parent, view, position, id ->
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        listView1.setOnItemLongClickListener { parent, view, position, id ->
            val task = parent.adapter.getItem(position) as Task

            val builder = AlertDialog.Builder(this)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか？")
            builder.setPositiveButton("OK") { textID, listener ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)


                reloadListView()
            }
            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        find_button.setOnClickListener{ view ->
            category_string = category_find_text.text.toString()
            reloadListView()
        }


        reloadListView()
        
    }

    private fun reloadListView() {

        val taskRealmResults =
            if (category_string == null || category_string == ""){
                mRealm.where(Task::class.java)
                    .findAll()
                    .sort("date", Sort.DESCENDING)
            }else{
                mRealm.where(Task::class.java)
                    .equalTo("category",category_string)
                    .sort("date", Sort.DESCENDING)
                    .findAll()
        }


        mTaskAdapter.mTaskList = mRealm.copyFromRealm(taskRealmResults)

        listView1.adapter = mTaskAdapter
        mTaskAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }
}