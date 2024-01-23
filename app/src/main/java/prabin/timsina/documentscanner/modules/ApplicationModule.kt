package prabin.timsina.documentscanner.modules

import android.content.Context
import android.hardware.camera2.CameraManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Provides
    fun provideCameraManager(@ApplicationContext context: Context): CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
}
