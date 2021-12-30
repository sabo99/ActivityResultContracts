package com.sabo.activityresultcontracts.document

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DocumentViewModel: ViewModel() {
    var mutableLiveListDocument = MutableLiveData<List<DocumentModel>?>()
    var docList = arrayListOf<DocumentModel>()

    fun add(documentModel: DocumentModel){
        docList.add(documentModel)
        mutableLiveListDocument.value = docList
    }

    fun remove(documentModel: DocumentModel){
        docList.remove(documentModel)
        mutableLiveListDocument.value = docList
    }

    fun clear(){
        docList.clear()
    }
}