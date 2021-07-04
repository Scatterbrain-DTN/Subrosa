package net.ballmerlabs.subrosa.listing

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import net.ballmerlabs.subrosa.NewsRepository
import javax.inject.Inject

@HiltViewModel
class GroupListViewModel @Inject constructor(
    val repository: NewsRepository
) : ViewModel(){
}