package net.ballmerlabs.subrosaproto

class NativeLib {

    /**
     * A native method that is implemented by the 'subrosaproto' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'subrosaproto' library on application startup.
        init {
            System.loadLibrary("subrosaproto")
        }
    }
}