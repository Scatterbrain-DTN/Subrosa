package net.ballmerlabs.subrosa

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.ballmerlabs.subrosa.scatterbrain.NewsGroup
import net.ballmerlabs.subrosa.scatterbrain.Post
import javax.inject.Inject

enum class PostListType {
    TYPE_GROUP,
    TYPE_POST
}

@HiltViewModel
class MainViewModel @Inject constructor(): ViewModel() {
    val path = MutableLiveData<List<NewsGroup>>(ArrayList())
    val collapsed = MutableLiveData(false)
    var search: MutableLiveData<String?> = MutableLiveData(null)
    val postListType: MutableLiveData<PostListType> = MutableLiveData(PostListType.TYPE_POST)
    val strPath: List<String>
    get() = path.value!!.map { v -> v.groupName}.toMutableList().apply { add(0, "/") }
}