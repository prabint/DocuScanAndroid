package prabin.timsina.documentscanner.ui.preview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import prabin.timsina.documentscanner.ui.common.deleteCachePhoto
import prabin.timsina.documentscanner.ui.navArgs

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class PreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val navArgs = savedStateHandle.navArgs<PreviewScreenNavArgs>()

    internal val bitmap = BitmapFactory.decodeFile(navArgs.uri)

    override fun onCleared() {
        deleteCachePhoto(appContext, navArgs.uri.substringAfterLast("/"))
        super.onCleared()
    }
}
