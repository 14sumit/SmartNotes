package com.example.ccaa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val noteList: MutableList<Note>,
    private val onEditClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.noteTitle)
        private val content: TextView = itemView.findViewById(R.id.noteContent)

        private val reminderLayout: LinearLayout =
            itemView.findViewById(R.id.reminderLayout)

        private val reminderText: TextView =
            itemView.findViewById(R.id.reminderText)

        private val pinIcon: TextView =
            itemView.findViewById(R.id.pinIcon)

        private val btnEdit: TextView =
            itemView.findViewById(R.id.btnEdit)

        private val btnDelete: TextView =
            itemView.findViewById(R.id.btnDelete)

        fun bind(note: Note) {

            title.text = note.title
            content.text = note.content

            // 🔔 Reminder UI
            val reminderTime = note.reminderTime

            if (reminderTime != null) {

                val sdf = SimpleDateFormat(
                    "dd MMM yyyy  HH:mm",
                    Locale.getDefault()
                )

                reminderText.text = sdf.format(Date(reminderTime))

                reminderLayout.visibility = View.VISIBLE

            } else {

                reminderLayout.visibility = View.GONE
            }

            // 📌 Pin UI
            pinIcon.visibility = if (note.isPinned) View.VISIBLE else View.GONE

            // ✏ Edit
            btnEdit.setOnClickListener {
                onEditClick(note)
            }

            // 🗑 Delete
            btnDelete.setOnClickListener {
                onDeleteClick(note)
            }

            // 📌 Pin toggle
            pinIcon.setOnClickListener {
                onPinClick(note)
            }

            // 👆 Open note
            itemView.setOnClickListener {
                onEditClick(note)
            }

            // 👇 Long press pin
            itemView.setOnLongClickListener {
                onPinClick(note)
                true
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NoteViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)

        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: NoteViewHolder,
        position: Int
    ) {
        holder.bind(noteList[position])
    }

    override fun getItemCount(): Int = noteList.size
}