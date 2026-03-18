package com.example.ccaa

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class DashboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var noteAdapter: NoteAdapter

    private val noteList = ArrayList<Note>()
    private val fullNoteList = ArrayList<Note>()

    private var noteListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.notesRecyclerView)
        fab = findViewById(R.id.fabAddNote)

        recyclerView.layoutManager = LinearLayoutManager(this)

        noteAdapter = NoteAdapter(
            noteList,
            { note -> openEditNote(note) },
            { note -> togglePin(note) },
            { note -> deleteNote(note) }
        )

        recyclerView.adapter = noteAdapter

        setupSwipeToDelete()

        fab.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }
        val fabAI = findViewById<FloatingActionButton>(R.id.fabAI)

        fabAI.setOnClickListener {
            startActivity(Intent(this, AIChatActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        attachNoteListener()
    }

    override fun onStop() {
        super.onStop()
        noteListener?.remove()
        noteListener = null
    }

    // ✏ Open Edit Screen
    private fun openEditNote(note: Note) {

        val intent = Intent(this, EditNoteActivity::class.java)

        intent.putExtra("noteId", note.id)
        intent.putExtra("noteTitle", note.title)
        intent.putExtra("noteContent", note.content)

        startActivity(intent)
    }

    // 🔥 Firestore Realtime Notes
    private fun attachNoteListener() {

        val userId = auth.currentUser?.uid ?: return

        noteListener = firestore.collection("notes")
            .whereEqualTo("userId", userId)
            .orderBy("isPinned", Query.Direction.DESCENDING)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->

                if (error != null) {
                    Toast.makeText(this, error.message, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                fullNoteList.clear()
                noteList.clear()

                value?.documents?.forEach { document ->

                    val note = document.toObject(Note::class.java)

                    if (note != null) {
                        note.id = document.id
                        fullNoteList.add(note)
                        noteList.add(note)
                    }
                }

                noteAdapter.notifyDataSetChanged()
            }
    }

    // 📌 Pin / Unpin Note
    private fun togglePin(note: Note) {

        val newStatus = !note.isPinned

        firestore.collection("notes")
            .document(note.id)
            .update("isPinned", newStatus)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    if (newStatus) "📌 Pinned" else "Unpinned",
                    Toast.LENGTH_SHORT
                ).show()

            }.addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to update pin",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // 🗑 Delete Button with Undo
    private fun deleteNote(note: Note) {

        val position = noteList.indexOf(note)

        if (position == -1) return

        noteList.removeAt(position)
        noteAdapter.notifyItemRemoved(position)

        Snackbar.make(
            findViewById(android.R.id.content),
            "Note deleted",
            Snackbar.LENGTH_LONG
        ).setAction("UNDO") {

            noteList.add(position, note)
            noteAdapter.notifyItemInserted(position)

        }.addCallback(object : Snackbar.Callback() {

            override fun onDismissed(
                transientBottomBar: Snackbar,
                event: Int
            ) {

                if (event != DISMISS_EVENT_ACTION) {

                    firestore.collection("notes")
                        .document(note.id)
                        .delete()
                        .addOnSuccessListener {
                            cancelReminder(note.id)
                        }
                }
            }

        }).show()
    }

    // 👉 Swipe Delete
    private fun setupSwipeToDelete() {

        val swipeCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ) = false

                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Int
                ) {

                    val position = viewHolder.bindingAdapterPosition

                    if (position == RecyclerView.NO_POSITION) return

                    val deletedNote = noteList[position]

                    noteList.removeAt(position)
                    noteAdapter.notifyItemRemoved(position)

                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Note deleted",
                        Snackbar.LENGTH_LONG
                    ).setAction("UNDO") {

                        noteList.add(position, deletedNote)
                        noteAdapter.notifyItemInserted(position)

                    }.addCallback(object : Snackbar.Callback() {

                        override fun onDismissed(
                            transientBottomBar: Snackbar,
                            event: Int
                        ) {

                            if (event != DISMISS_EVENT_ACTION) {

                                firestore.collection("notes")
                                    .document(deletedNote.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        cancelReminder(deletedNote.id)
                                    }
                            }
                        }

                    }).show()
                }
            }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    // ⏰ Cancel Reminder
    private fun cancelReminder(noteId: String) {

        val intent = Intent(this, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            noteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
    }

    // 🔎 Search Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.dashboard_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        searchView.queryHint = "Search notes..."

        searchView.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {

                filterNotes(newText)
                return true
            }
        })

        return true
    }

    // 🔎 Filter Notes
    private fun filterNotes(query: String?) {

        val filteredList =
            if (query.isNullOrEmpty()) {
                fullNoteList
            } else {
                fullNoteList.filter {
                    it.title.contains(query, true) ||
                            it.content.contains(query, true)
                }
            }

        noteList.clear()
        noteList.addAll(filteredList)

        noteAdapter.notifyDataSetChanged()
    }

    // 🚪 Logout
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.logout) {

            auth.signOut()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        return super.onOptionsItemSelected(item)
    }
}