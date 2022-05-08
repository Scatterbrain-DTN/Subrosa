package net.ballmerlabs.subrosa

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(): ViewModel() {
    val path = MutableLiveData<List<NewsGroup>>(ArrayList())
    val collapsed = MutableLiveData(false)
    var search: MutableLiveData<String?> = MutableLiveData(null)
    val strPath: List<String>
    get() = path.value!!.map { v -> v.groupName}.toMutableList().apply { add(0, "/") }
}