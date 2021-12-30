package com.sabo.activityresultcontracts.document

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sabo.activityresultcontracts.R

class DocumentAdapter(
    private val viewModel: DocumentViewModel,
    private val documentList: List<DocumentModel>
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {
    inner class DocumentViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
        fun bind(doc: DocumentModel) {
            v.findViewById<TextView>(R.id.tvFilePath).text = doc.filePath
            v.findViewById<TextView>(R.id.tvFileRealName).text = doc.fileRealName
            v.findViewById<TextView>(R.id.tvFileGenerateName).text = doc.fileGenerateName
            v.findViewById<TextView>(R.id.tvFileSize).text = doc.fileSize
            v.findViewById<TextView>(R.id.tvFileMimeType).text = doc.fileMimeType
            v.findViewById<ImageView>(R.id.ivRemove).setOnClickListener {
                viewModel.remove(doc)
                notifyItemRemoved(documentList.indexOf(doc))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        return DocumentViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file_document, parent, false)
        )
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(documentList[position])
    }

    override fun getItemCount(): Int {
        return  documentList.size
    }
}